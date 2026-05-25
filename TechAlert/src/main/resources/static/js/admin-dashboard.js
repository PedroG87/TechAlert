const adminConfig = window.techAlertAdminConfig || {};

const adminElements = {
    feedbackMessage: document.getElementById("feedbackMessage"),
    errorMessage: document.getElementById("errorMessage"),
    summaryUsers: document.getElementById("summaryUsers"),
    summaryCitizens: document.getElementById("summaryCitizens"),
    summaryNotifications: document.getElementById("summaryNotifications"),
    summarySettings: document.getElementById("summarySettings"),
    userForm: document.getElementById("userForm"),
    resetUserForm: document.getElementById("resetUserForm"),
    userId: document.getElementById("userId"),
    userNome: document.getElementById("userNome"),
    userEmail: document.getElementById("userEmail"),
    userSenha: document.getElementById("userSenha"),
    userRole: document.getElementById("userRole"),
    userCpf: document.getElementById("userCpf"),
    userTelefone: document.getElementById("userTelefone"),
    userEndereco: document.getElementById("userEndereco"),
    userDataNascimento: document.getElementById("userDataNascimento"),
    usersTableBody: document.getElementById("usersTableBody"),
    notificationForm: document.getElementById("notificationForm"),
    notificationTitulo: document.getElementById("notificationTitulo"),
    notificationTipo: document.getElementById("notificationTipo"),
    notificationSeverity: document.getElementById("notificationSeverity"),
    notificationConteudo: document.getElementById("notificationConteudo"),
    notificationTargetUser: document.getElementById("notificationTargetUser"),
    notificationsTableBody: document.getElementById("notificationsTableBody"),
    settingsList: document.getElementById("settingsList")
};

let usersCache = [];

document.addEventListener("DOMContentLoaded", async () => {
    bindAdminEvents();
    await reloadDashboard();
});

function bindAdminEvents() {
    adminElements.userForm?.addEventListener("submit", async (event) => {
        event.preventDefault();

        const payload = {
            nome: adminElements.userNome.value.trim(),
            email: adminElements.userEmail.value.trim(),
            senha: adminElements.userSenha.value,
            cpf: adminElements.userCpf.value.trim(),
            telefone: adminElements.userTelefone.value.trim(),
            endereco: adminElements.userEndereco.value.trim(),
            dataNascimento: adminElements.userDataNascimento.value || null,
            role: adminElements.userRole.value
        };

        const userId = adminElements.userId.value;
        const url = userId ? `${adminConfig.usersUrl}/${userId}` : adminConfig.usersUrl;
        const method = userId ? "PUT" : "POST";

        try {
            await requestJson(url, {
                method,
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify(payload)
            });
            showFeedback(userId ? "Usuário atualizado com sucesso." : "Usuário criado com sucesso.");
            resetUserForm();
            await reloadDashboard();
        } catch (error) {
            showError(error.message || "Não foi possível salvar o usuário.");
        }
    });

    adminElements.resetUserForm?.addEventListener("click", () => {
        resetUserForm();
    });

    adminElements.notificationForm?.addEventListener("submit", async (event) => {
        event.preventDefault();

        try {
            await requestJson(adminConfig.notificationsUrl, {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({
                    titulo: adminElements.notificationTitulo.value.trim(),
                    conteudo: adminElements.notificationConteudo.value.trim(),
                    tipo: adminElements.notificationTipo.value,
                    nivelPericulosidade: adminElements.notificationSeverity.value,
                    usuarioAlvoId: adminElements.notificationTargetUser.value ? Number(adminElements.notificationTargetUser.value) : null
                })
            });
            showFeedback("Notificação publicada com sucesso.");
            adminElements.notificationForm.reset();
            await reloadDashboard();
        } catch (error) {
            showError(error.message || "Não foi possível publicar a notificação.");
        }
    });
}

async function reloadDashboard() {
    hideError();
    await Promise.all([
        loadSummary(),
        loadUsers(),
        loadNotifications(),
        loadSettings()
    ]);
}

async function loadSummary() {
    const summary = await requestJson(adminConfig.summaryUrl);
    adminElements.summaryUsers.textContent = summary.totalUsuarios;
    adminElements.summaryCitizens.textContent = summary.totalCidadaos;
    adminElements.summaryNotifications.textContent = summary.notificacoesAtivas;
    adminElements.summarySettings.textContent = summary.configuracoesBasicas;
}

async function loadUsers() {
    usersCache = await requestJson(adminConfig.usersUrl);
    adminElements.usersTableBody.innerHTML = "";
    adminElements.notificationTargetUser.innerHTML = '<option value="">Todos os cidadãos</option>';

    usersCache
        .sort((a, b) => a.nome.localeCompare(b.nome, "pt-BR"))
        .forEach((user) => {
            const row = document.createElement("tr");
            row.innerHTML = `
                <td>${escapeHtml(user.nome)}</td>
                <td>${escapeHtml(user.email)}</td>
                <td>${formatRole(user.role)}</td>
                <td>${escapeHtml(user.cpf || "")}</td>
                <td class="admin-actions-cell">
                    <button type="button" class="btn btn-outline-custom btn-sm js-edit">Editar</button>
                    <button type="button" class="btn btn-danger-custom btn-sm js-delete">Excluir</button>
                </td>
            `;

            row.querySelector(".js-edit")?.addEventListener("click", () => fillUserForm(user));
            row.querySelector(".js-delete")?.addEventListener("click", async () => {
                if (!window.confirm(`Deseja remover o usuário ${user.nome}?`)) {
                    return;
                }
                try {
                    await requestJson(`${adminConfig.usersUrl}/${user.id}`, { method: "DELETE" }, false);
                    showFeedback("Usuário removido com sucesso.");
                    resetUserForm();
                    await reloadDashboard();
                } catch (error) {
                    showError(error.message || "Não foi possível remover o usuário.");
                }
            });

            adminElements.usersTableBody.appendChild(row);

            if (user.role === "CIDADAO") {
                const option = document.createElement("option");
                option.value = String(user.id);
                option.textContent = user.nome;
                adminElements.notificationTargetUser.appendChild(option);
            }
        });
}

async function loadNotifications() {
    const page = await requestJson(`${adminConfig.notificationsUrl}?status=ATIVA&page=0&size=20`);
    adminElements.notificationsTableBody.innerHTML = "";

    page.content.forEach((notification) => {
        const row = document.createElement("tr");
        row.innerHTML = `
            <td>${escapeHtml(notification.titulo)}</td>
            <td>${escapeHtml(notification.usuarioAlvo)}</td>
            <td>${formatType(notification.tipo)}</td>
            <td>${formatSeverity(notification.nivelPericulosidade)}</td>
            <td>${escapeHtml(notification.status)}</td>
            <td class="admin-actions-cell">
                <button type="button" class="btn btn-outline-custom btn-sm js-archive">${notification.status === "ATIVA" ? "Arquivar" : "Reativar"}</button>
                <button type="button" class="btn btn-danger-custom btn-sm js-delete">Excluir</button>
            </td>
        `;

        row.querySelector(".js-archive")?.addEventListener("click", async () => {
            const nextStatus = notification.status === "ATIVA" ? "ARQUIVADA" : "ATIVA";
            try {
                await requestJson(`${adminConfig.notificationsUrl}/${notification.id}/status?status=${nextStatus}`, {
                    method: "PATCH"
                });
                showFeedback("Status da notificação atualizado.");
                await reloadDashboard();
            } catch (error) {
                showError(error.message || "Não foi possível atualizar a notificação.");
            }
        });

        row.querySelector(".js-delete")?.addEventListener("click", async () => {
            if (!window.confirm(`Deseja excluir a notificação "${notification.titulo}"?`)) {
                return;
            }
            try {
                await requestJson(`${adminConfig.notificationsUrl}/${notification.id}`, { method: "DELETE" }, false);
                showFeedback("Notificação removida com sucesso.");
                await reloadDashboard();
            } catch (error) {
                showError(error.message || "Não foi possível excluir a notificação.");
            }
        });

        adminElements.notificationsTableBody.appendChild(row);
    });
}

async function loadSettings() {
    const settings = await requestJson(adminConfig.settingsUrl);
    adminElements.settingsList.innerHTML = "";

    settings.forEach((setting) => {
        const article = document.createElement("article");
        article.className = "setting-card";
        article.innerHTML = `
            <div class="setting-card-header">
                <h3>${escapeHtml(setting.chave)}</h3>
                <p>${escapeHtml(setting.descricao || "")}</p>
            </div>
            <input type="text" class="form-control setting-input" value="${escapeHtmlAttribute(setting.valor)}">
            <button type="button" class="btn btn-primary-custom mt-3 js-save-setting">Salvar</button>
        `;

        article.querySelector(".js-save-setting")?.addEventListener("click", async () => {
            const value = article.querySelector(".setting-input")?.value || "";
            try {
                await requestJson(`${adminConfig.settingsUrl}/${encodeURIComponent(setting.chave)}`, {
                    method: "PATCH",
                    headers: { "Content-Type": "application/json" },
                    body: JSON.stringify({ valor: value })
                });
                showFeedback(`Configuração ${setting.chave} atualizada.`);
            } catch (error) {
                showError(error.message || "Não foi possível salvar a configuração.");
            }
        });

        adminElements.settingsList.appendChild(article);
    });
}

function fillUserForm(user) {
    adminElements.userId.value = user.id;
    adminElements.userNome.value = user.nome;
    adminElements.userEmail.value = user.email;
    adminElements.userSenha.value = "";
    adminElements.userRole.value = user.role;
    adminElements.userCpf.value = user.cpf || "";
    adminElements.userTelefone.value = user.telefone || "";
    adminElements.userEndereco.value = user.endereco || "";
}

function resetUserForm() {
    adminElements.userForm.reset();
    adminElements.userId.value = "";
    adminElements.userRole.value = "CIDADAO";
}

async function requestJson(url, options = {}, expectJson = true) {
    const response = await fetch(url, {
        credentials: "same-origin",
        headers: {
            Accept: "application/json",
            ...(options.headers || {})
        },
        ...options
    });

    if (!response.ok) {
        let message = "Operação não concluída.";
        try {
            const payload = await response.json();
            if (payload.message) {
                message = payload.message;
            }
        } catch (error) {
            message = response.status === 401 ? "Sessão expirada." : message;
        }
        throw new Error(message);
    }

    if (!expectJson || response.status === 204) {
        return null;
    }

    return response.json();
}

function showFeedback(message) {
    adminElements.feedbackMessage.textContent = message;
    adminElements.feedbackMessage.className = "feedback-banner success";
    setTimeout(() => {
        adminElements.feedbackMessage.className = "feedback-banner success d-none";
    }, 3000);
}

function showError(message) {
    adminElements.errorMessage.textContent = message;
    adminElements.errorMessage.className = "feedback-banner error";
}

function hideError() {
    adminElements.errorMessage.className = "feedback-banner error d-none";
}

function formatRole(role) {
    return role === "ADM" ? "Administrador" : "Cidadão";
}

function formatType(type) {
    if (type === "ALERTA") return "Alerta";
    if (type === "ATUALIZACAO") return "Atualização";
    return "Sistema";
}

function formatSeverity(level) {
    if (level === "CRITICA") return "Crítica";
    if (level === "ALTA") return "Alta";
    if (level === "MEDIA") return "Média";
    return "Baixa";
}

function escapeHtml(value) {
    return String(value ?? "")
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#39;");
}

function escapeHtmlAttribute(value) {
    return escapeHtml(value);
}

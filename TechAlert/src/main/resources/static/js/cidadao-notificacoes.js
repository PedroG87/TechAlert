const citizenConfig = window.techAlertCitizenConfig || {};

const citizenState = {
    page: 0,
    size: 8,
    hasNext: false,
    filters: {
        search: "",
        readStatus: "",
        type: ""
    }
};

const citizenElements = {
    filterForm: document.getElementById("filterForm"),
    clearFilters: document.getElementById("clearFilters"),
    loadMoreButton: document.getElementById("loadMoreButton"),
    notificationList: document.getElementById("notificationList"),
    loadingState: document.getElementById("loadingState"),
    emptyState: document.getElementById("emptyState"),
    feedbackMessage: document.getElementById("feedbackMessage"),
    errorMessage: document.getElementById("errorMessage"),
    summaryTotal: document.getElementById("summaryTotal"),
    summaryUnread: document.getElementById("summaryUnread"),
    summaryActive: document.getElementById("summaryActive"),
    summaryCriticals: document.getElementById("summaryCriticals"),
    searchInput: document.getElementById("searchInput"),
    statusFilter: document.getElementById("statusFilter"),
    typeFilter: document.getElementById("typeFilter")
};

document.addEventListener("DOMContentLoaded", async () => {
    bindCitizenEvents();
    await Promise.all([loadSummary(), loadNotifications(true)]);
});

function bindCitizenEvents() {
    citizenElements.filterForm?.addEventListener("submit", async (event) => {
        event.preventDefault();
        citizenState.filters = {
            search: citizenElements.searchInput.value.trim(),
            readStatus: citizenElements.statusFilter.value,
            type: citizenElements.typeFilter.value
        };
        await loadNotifications(true);
    });

    citizenElements.clearFilters?.addEventListener("click", async () => {
        citizenElements.filterForm.reset();
        citizenState.filters = { search: "", readStatus: "", type: "" };
        await loadNotifications(true);
    });

    citizenElements.loadMoreButton?.addEventListener("click", async () => {
        citizenState.page += 1;
        await loadNotifications(false);
    });
}

async function loadSummary() {
    const summary = await requestJson(citizenConfig.summaryUrl);
    citizenElements.summaryTotal.textContent = summary.total;
    citizenElements.summaryUnread.textContent = summary.naoLidas;
    citizenElements.summaryActive.textContent = summary.ativas;
    citizenElements.summaryCriticals.textContent = summary.criticas;
}

async function loadNotifications(reset) {
    toggleLoading(true);
    hideError();

    if (reset) {
        citizenState.page = 0;
        citizenElements.notificationList.innerHTML = "";
    }

    try {
        const query = new URLSearchParams({
            page: String(citizenState.page),
            size: String(citizenState.size)
        });

        Object.entries(citizenState.filters).forEach(([key, value]) => {
            if (value) {
                query.set(key, value);
            }
        });

        const page = await requestJson(`${citizenConfig.listUrl}?${query.toString()}`);
        renderNotifications(page.content, reset);
        citizenState.hasNext = page.hasNext;
        citizenElements.loadMoreButton.classList.toggle("d-none", !page.hasNext);
        citizenElements.emptyState.classList.toggle("d-none", page.content.length > 0 || citizenElements.notificationList.children.length > 0);
    } catch (error) {
        showError(error.message || "Não foi possível carregar as notificações.");
    } finally {
        toggleLoading(false);
    }
}

function renderNotifications(notifications, reset) {
    if (reset) {
        citizenElements.notificationList.innerHTML = "";
    }

    notifications.forEach((notification) => {
        const article = document.createElement("article");
        article.className = `notification-card ${notification.lida ? "" : "is-unread"}`.trim();
        article.innerHTML = `
            <div class="notification-card-header">
                <div>
                    <h2 class="notification-title">${escapeHtml(notification.titulo)}</h2>
                    <div class="notification-meta mt-2">
                        <span class="meta-pill"><i class="bi bi-calendar-event"></i>${escapeHtml(notification.dataEnvio)}</span>
                        <span class="type-pill ${notification.tipo}">${formatType(notification.tipo)}</span>
                        <span class="severity-pill ${notification.nivelPericulosidade}">${formatSeverity(notification.nivelPericulosidade)}</span>
                    </div>
                </div>
                <span class="status-pill ${notification.lida ? "read" : "unread"}">${escapeHtml(notification.statusLeitura)}</span>
            </div>
            <p class="notification-content">${escapeHtml(notification.conteudo)}</p>
            <div class="notification-card-footer">
                <button type="button" class="btn btn-outline-custom js-read" ${notification.lida ? "disabled" : ""}>
                    ${notification.lida ? "Lida" : "Marcar como lida"}
                </button>
            </div>
        `;

        article.querySelector(".js-read")?.addEventListener("click", async () => {
            try {
                await requestJson(`${citizenConfig.listUrl}/${notification.id}/read`, { method: "PATCH" });
                showFeedback("Notificação marcada como lida.");
                await Promise.all([loadSummary(), loadNotifications(true)]);
            } catch (error) {
                showError(error.message || "Não foi possível atualizar a notificação.");
            }
        });

        citizenElements.notificationList.appendChild(article);
    });
}

async function requestJson(url, options = {}) {
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

    if (response.status === 204) {
        return null;
    }

    return response.json();
}

function toggleLoading(loading) {
    citizenElements.loadingState.classList.toggle("d-none", !loading);
}

function showFeedback(message) {
    citizenElements.feedbackMessage.textContent = message;
    citizenElements.feedbackMessage.className = "feedback-banner success";
    setTimeout(() => {
        citizenElements.feedbackMessage.className = "feedback-banner success d-none";
    }, 3000);
}

function showError(message) {
    citizenElements.errorMessage.textContent = message;
    citizenElements.errorMessage.className = "feedback-banner error";
}

function hideError() {
    citizenElements.errorMessage.className = "feedback-banner error d-none";
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

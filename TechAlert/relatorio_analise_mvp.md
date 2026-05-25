# Relatório De Análise E Reestruturação MVP

## 1. Visão Geral Do Sistema Anterior

O sistema existente já possuía:

- autenticação por sessão com dois perfis: `ADM` e `CIDADAO`;
- home pública, login, cadastro e área do cidadão;
- uma área administrativa orientada principalmente ao gerenciamento detalhado de notificações;
- integração ORM com PostgreSQL via JPA/Hibernate.

Também havia componentes que ultrapassavam o necessário para um MVP:

- camada separada de entrega de notificações (`NotificationDelivery`);
- atualização em tempo real por SSE;
- ações em lote, filtros extensos e fluxos administrativos mais densos que o valor inicial do produto exige;
- dependências de operação mais complexas para um primeiro lançamento.

## 2. Funcionalidades Core Identificadas

As funcionalidades de maior valor para o MVP foram mapeadas como:

1. cadastro e login de usuários;
2. separação segura entre cidadão e administrador;
3. recebimento e leitura de notificações pelo cidadão;
4. moderação administrativa das notificações do sistema;
5. gestão básica de usuários;
6. métricas mínimas para acompanhamento da plataforma;
7. configurações simples para personalização do produto.

## 3. Componentes Mantidos

- autenticação por sessão;
- perfis `ADM` e `CIDADAO`;
- persistência via ORM com PostgreSQL;
- home, login, cadastro e área do cidadão;
- classificação de notificações por tipo e nível de periculosidade.

## 4. Componentes Removidos

- camada `NotificationDelivery`;
- repositório dedicado de entregas de notificação;
- template administrativo antigo centrado apenas em notificações;
- script antigo de notificações compartilhado entre áreas;
- atualização em tempo real por SSE;
- ações em lote, remoção pelo cidadão e filtros não essenciais ao lançamento.

## 5. Componentes Adaptados

### Usuários

- `AppUser` foi mantido em formato simples, alinhado ao antigo modelo de cidadão.
- O serviço de usuários passou a suportar listagem, criação, atualização e exclusão com regras mínimas de segurança.

### Notificações

- `SystemNotification` passou a representar diretamente a notificação de um usuário.
- Cada registro agora possui destinatário, leitura, status, tipo e nível de periculosidade.
- A moderação administrativa foi simplificada para criar, arquivar, listar e excluir notificações.

### Configurações

- Foi criado o bloco `PlatformSetting` para armazenar os parâmetros básicos do MVP.
- Essas configurações abastecem a apresentação da home e podem ser alteradas pela área ADM.

### Área Do Cidadão

- Foi reduzida ao fluxo essencial de consulta de notificações, filtros mínimos e marcação como lida.

### Área ADM

- Foi reformulada para o escopo mínimo viável:
  - gestão de usuários;
  - métricas principais;
  - moderação de notificações;
  - configurações básicas.

## 6. Novo Escopo Da Área ADM MVP

O painel administrativo implementado no MVP contempla:

1. **Cadastro e gerenciamento de usuários**
   - criar usuários;
   - editar dados principais;
   - alterar perfil;
   - excluir usuários.

2. **Visualização de métricas principais**
   - total de usuários;
   - total de cidadãos;
   - notificações ativas;
   - quantidade de configurações básicas.

3. **Moderação de conteúdo core**
   - criar notificações;
   - definir tipo e periculosidade;
   - direcionar para um cidadão específico ou para todos os cidadãos;
   - arquivar ou excluir notificações.

4. **Configurações básicas da plataforma**
   - nome da plataforma;
   - mensagem principal da home;
   - e-mail de suporte.

## 7. Benefícios Da Reestruturação

- redução de complexidade técnica e operacional;
- menor custo de manutenção para o lançamento inicial;
- fluxo mais fácil de entender para usuários e equipe de desenvolvimento;
- base persistente simples, coerente com MVP e pronta para evolução incremental.

## 8. Conclusão

O sistema foi alinhado ao conceito de MVP ao concentrar o produto em três eixos principais:

- usuários;
- notificações;
- configurações básicas.

A nova área ADM implementa exatamente o mínimo necessário para operar esse recorte com usabilidade, estabilidade e foco no valor core do projeto.

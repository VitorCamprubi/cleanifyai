# CleanifyAI

SaaS para esteticas automotivas. Multi-tenant, com agenda, ordens de servico, financeiro basico e base pronta para evoluir com WhatsApp e IA.

## Estado atual

Funcionalidades vendaveis:

- Cadastro de empresa (signup) + login JWT com refresh token rotativo
- Multi-tenant real por `empresa_id` (isolamento via `TenantContext` + listener JPA)
- CRUD de Clientes (com soft-delete), Servicos e Agendamentos com maquina de estados
- Ordens de Servico com itens, valor total agregado e fluxo `ABERTA -> EM_EXECUCAO -> CONCLUIDA -> ENTREGUE / CANCELADA`
- Financeiro: lancamentos de entrada/saida, vinculo opcional com OS, resumo agregado por forma de pagamento, estorno no mesmo dia
- Dashboard com totais e proximos agendamentos por empresa
- Auditoria automatica (`criado_em`, `atualizado_em`) em todas as entidades tenanteadas
- Audit log HTTP para acoes de escrita em `/api/**`, com tenant, usuario, recurso, status, IP, user-agent e duracao
- Roles: `ADMIN` (escrita financeira + servicos) e `ATENDENTE` (operacional)
- Flyway com migrations versionadas e profiles separados para `dev` e `prod`
- Actuator com health, liveness e readiness para deploy
- Rate limit em tentativas de login por email + IP
- Docker Compose para subir MySQL, API e Web

Adiados (proximas iteracoes):

- Refatoracao hexagonal por modulo (Fase 2 do roadmap)
- Paginacao em listagens
- Adapter real de WhatsApp e IA
- Pagina publica de agendamento

## Stack

- Backend: **Spring Boot 3.2.5**, Java 21, Spring Security + JWT (jjwt 0.12.5), Spring Data JPA, Flyway, MySQL 8.x.
- Frontend: **Angular 18** standalone components, Reactive Forms, RxJS.

## Estrutura

```text
cleanifyai/
  docs/                          # auditoria, arquitetura, roadmap, notas por fase
  cleanifyai-api/
    pom.xml
    database/init-mysql.sql
    src/main/resources/db/migration/ # Flyway V1 schema base + V2 audit_logs + V3 refresh_tokens
    src/main/java/com/cleanifyai/api/
      config/                    # security, CORS, seed, properties
      controller/                # endpoints REST
      domain/entity/             # JPA entities + EntidadeTenantBase
      domain/enums/              # StatusAgendamento, StatusOrdem, FormaPagamento, ...
      dto/                       # records de request/response por modulo
      exception/                 # BusinessException + GlobalExceptionHandler
      integration/whatsapp,ai/   # NoOp + interfaces para futura integracao
      repository/                # Spring Data
      security/                  # JWT, filter, UserDetails
      service/                   # casos de uso
      shared/tenant/             # TenantContext + EntityListener
  cleanifyai-web/
    angular.json
    src/app/
      core/{guards,interceptors,models,services}/
      features/{auth,clientes,servicos,agendamentos,ordens,financeiro,dashboard}/
      layout/app-shell/
```

## Como rodar

### Pre-requisitos

- Java 21
- Maven (ou usar `mvnw`)
- Node 18+ e npm
- MySQL 8.x rodando local na porta 3306
- Docker Desktop rodando, se for usar `docker compose`

### Docker Compose

```bash
docker compose up --build
```

Servicos expostos:

- Web: `http://localhost:4200`
- API: `http://localhost:8080`
- MySQL: `localhost:3306` (`root` / `cleanifyai-dev`)

### Backend

```bash
cd cleanifyai-api
# profile padrao: dev
# DB_HOST=localhost DB_PORT=3306 DB_NAME=cleanifyai DB_USERNAME=root DB_PASSWORD=212420
# APP_JWT_SECRET=<chave de 32+ caracteres>
# APP_JWT_EXPIRATION_SECONDS=43200
# APP_REFRESH_TOKEN_EXPIRATION_SECONDS=2592000
# APP_LOGIN_RATE_LIMIT_MAX_ATTEMPTS=5
# APP_LOGIN_RATE_LIMIT_WINDOW_SECONDS=900
# APP_SEED_ENABLED=true     # cria empresa e usuarios demo

mvnw.cmd spring-boot:run    # Windows
./mvnw spring-boot:run      # Linux/macOS
```

A primeira execucao em banco vazio cria o schema via Flyway. O JPA valida o schema por padrao (`JPA_DDL_AUTO=validate`). Para bancos locais antigos criados por `ddl-auto: update`, use `FLYWAY_ENABLED=true` com `baseline-on-migrate` ja configurado ou recrie o banco em desenvolvimento.

O profile `dev` usa `DB_PASSWORD=212420` como fallback para manter o ambiente local atual funcionando. Em producao, rode com `SPRING_PROFILES_ACTIVE=prod` e informe obrigatoriamente `SPRING_DATASOURCE_URL`, `DB_USERNAME`, `DB_PASSWORD`, `APP_CORS_ALLOWED_ORIGINS` e `APP_JWT_SECRET`.

Usuarios de demo (criados pelo seed):

- `admin@cleanifyai.local` / `admin123`
- `atendente@cleanifyai.local` / `atendente123`

Endpoints publicos: `GET /api/ping`, `POST /api/auth/login`, `POST /api/auth/register-company`, `POST /api/auth/refresh`, `POST /api/auth/logout`, `GET /actuator/health`, `GET /actuator/health/liveness`, `GET /actuator/health/readiness`.

### Frontend

```bash
cd cleanifyai-web
npm install
npm start
```

Acesse `http://localhost:4200`. Use o login do seed ou cadastre uma nova empresa em `/signup`.

### Testes

Backend:

```bash
cd cleanifyai-api
mvnw.cmd test
```

Cobertura atual: `ApiSecurityAndCrudIntegrationTest` (auth, CRUD, agendamento), `AuthSessionAndHealthIntegrationTest` (Actuator, rate limit, refresh token rotativo e logout), `OrdemServicoIntegrationTest` (OS + maquina de estados + roles), `FinanceiroIntegrationTest` (lancamentos + estorno + agregacao + roles), `AuditLogIntegrationTest` (registro de escrita auditavel) e `TenantIsolationIntegrationTest` (isolamento cross-tenant explicito por modulo).

Frontend:

```bash
cd cleanifyai-web
npm run build
npm run test:ci
```

## Documentacao

Todas as decisoes vivem em `docs/`:

- `AUDITORIA.md` - diagnostico do MVP inicial
- `ARQUITETURA.md` - alvo arquitetural (hexagonal por vertical slice + multi-tenant por coluna)
- `ROADMAP.md` - fases 0 a 6 com criterios de pronto
- `FASE-1-MULTITENANT.md` - entrega: empresa, signup, JWT com `empresaId`
- `FASE-3-ORDEM-SERVICO.md` - entrega: OS + itens + maquina de estados
- `FASE-4-FINANCEIRO.md` - entrega: lancamentos + resumo + estorno
- `PLANO-EXECUCAO-SAAS.md` - plano vivo de evolucao por ondas

## Decisoes ativas

- **Multi-tenant por coluna** (sem schema/db por tenant). `EntidadeTenantBase` + `TenantEntityListener` injetam `empresaId` automaticamente.
- **Soft-delete em Cliente** via flag `ativo` consultada no service. OS e Agendamentos historicos continuam apontando para o cliente mesmo apos inativacao.
- **JWT stateless** com claim `empresaId` e refresh token opaco persistido como hash, com rotacao a cada renovacao.
- **Audit log sem payload** para evitar gravar dados sensiveis; registra metadados da acao e nunca aceita tenant vindo do front.
- **Profiles separados**: `dev` tem defaults locais e seed opcional; `prod` exige segredos/configuracoes por variavel de ambiente.
- **Rate limit de login em memoria** por email + IP. Em deploy horizontal, trocar por Redis ou gateway compartilhado.
- **WhatsApp e IA** sao portas (interfaces) com adapter `NoOp` em dev. Trocar pelo provider real e isolado.
- **Sem MapStruct, sem Lombok**: domain anemico, mappers manuais. Reduz ferramental, mantem MVP enxuto.

## Proximos passos sugeridos

1. **Paginacao** - preparar para clientes maiores.
2. **Adapter WhatsApp** - confirmacao automatica e lembrete D-1.
3. **Pagina publica de agendamento** - diferenciacao competitiva.
4. **Agenda visual diaria/semanal** - elevar valor percebido do produto.
5. **Horarios de funcionamento e bloqueios** - base para agendamento publico real.
6. **Orcamentos com PDF/link** - acelerar venda e conversao.

# CleanifyAI

SaaS para esteticas automotivas. Multi-tenant, com agenda, ordens de servico, financeiro basico e base pronta para evoluir com WhatsApp e IA.

## Estado atual

Funcionalidades vendaveis:

- Cadastro de empresa (signup) + login JWT
- Multi-tenant real por `empresa_id` (isolamento via `TenantContext` + listener JPA)
- CRUD de Clientes (com soft-delete), Servicos e Agendamentos com maquina de estados
- Ordens de Servico com itens, valor total agregado e fluxo `ABERTA -> EM_EXECUCAO -> CONCLUIDA -> ENTREGUE / CANCELADA`
- Financeiro: lancamentos de entrada/saida, vinculo opcional com OS, resumo agregado por forma de pagamento, estorno no mesmo dia
- Dashboard com totais e proximos agendamentos por empresa
- Auditoria automatica (`criado_em`, `atualizado_em`) em todas as entidades tenanteadas
- Roles: `ADMIN` (escrita financeira + servicos) e `ATENDENTE` (operacional)

Adiados (proximas iteracoes):

- Refatoracao hexagonal por modulo (Fase 2 do roadmap)
- Flyway substituindo `ddl-auto: update`
- Refresh token + tela de sessao expirada
- Paginacao em listagens
- Adapter real de WhatsApp e IA
- Pagina publica de agendamento

## Stack

- Backend: **Spring Boot 3.2.5**, Java 21, Spring Security + JWT (jjwt 0.12.5), Spring Data JPA, MySQL 8.x.
- Frontend: **Angular 17** standalone components, Reactive Forms, RxJS.

## Estrutura

```text
cleanifyai/
  docs/                          # auditoria, arquitetura, roadmap, notas por fase
  cleanifyai-api/
    pom.xml
    database/init-mysql.sql
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

### Backend

```bash
cd cleanifyai-api
# variaveis (opcional - tem defaults sensatos no application.yml)
# DB_HOST=localhost DB_PORT=3306 DB_NAME=cleanifyai DB_USERNAME=root DB_PASSWORD=...
# APP_JWT_SECRET=<chave de 32+ caracteres>
# APP_SEED_ENABLED=true     # cria empresa e usuarios demo

mvnw.cmd spring-boot:run    # Windows
./mvnw spring-boot:run      # Linux/macOS
```

A primeira execucao cria automaticamente as tabelas via `ddl-auto: update`. Em desenvolvimento isso e suficiente; em producao, troque por Flyway antes de subir.

Usuarios de demo (criados pelo seed):

- `admin@cleanifyai.local` / `admin123`
- `atendente@cleanifyai.local` / `atendente123`

Endpoints publicos: `GET /api/ping`, `POST /api/auth/login`, `POST /api/auth/register-company`.

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

Cobertura atual: `ApiSecurityAndCrudIntegrationTest` (auth, CRUD, agendamento), `OrdemServicoIntegrationTest` (OS + maquina de estados + roles), `FinanceiroIntegrationTest` (lancamentos + estorno + agregacao + roles).

Frontend:

```bash
cd cleanifyai-web
npm run build
```

## Documentacao

Todas as decisoes vivem em `docs/`:

- `AUDITORIA.md` — diagnostico do MVP inicial
- `ARQUITETURA.md` — alvo arquitetural (hexagonal por vertical slice + multi-tenant por coluna)
- `ROADMAP.md` — fases 0 a 6 com criterios de pronto
- `FASE-1-MULTITENANT.md` — entrega: empresa, signup, JWT com `empresaId`
- `FASE-3-ORDEM-SERVICO.md` — entrega: OS + itens + maquina de estados
- `FASE-4-FINANCEIRO.md` — entrega: lancamentos + resumo + estorno

## Decisoes ativas

- **Multi-tenant por coluna** (sem schema/db por tenant). `EntidadeTenantBase` + `TenantEntityListener` injetam `empresaId` automaticamente.
- **Soft-delete em Cliente** via flag `ativo` consultada no service. OS e Agendamentos historicos continuam apontando para o cliente mesmo apos inativacao.
- **JWT stateless** com claim `empresaId`. Sem refresh token ainda.
- **WhatsApp e IA** sao portas (interfaces) com adapter `NoOp` em dev. Trocar pelo provider real e isolado.
- **Sem MapStruct, sem Lombok**: domain anemico, mappers manuais. Reduz ferramental, mantem MVP enxuto.

## Proximos passos sugeridos

1. **Flyway** — versionar schema antes do primeiro deploy.
2. **Refresh token** — UX de sessao mais profissional.
3. **Paginacao** — preparar para clientes maiores.
4. **Refator hexagonal** — quando dois ou mais novos modulos entrarem.
5. **Adapter WhatsApp** — confirmacao automatica e lembrete D-1.
6. **Pagina publica de agendamento** — diferenciacao competitiva.

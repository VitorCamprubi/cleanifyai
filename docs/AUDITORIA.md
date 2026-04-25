# Auditoria do MVP CleanifyAI

> Data: 2026-04-25
> Escopo auditado: `cleanifyai-api` (Spring Boot 3.2.5 / Java 21) e `cleanifyai-web` (Angular standalone).

## 1. Visão geral

O projeto está em estado **"MVP funcional, não vendável ainda"**. A base é sólida, a separação em camadas é honesta, e há testes de integração reais. Mas existem furos críticos que impedem comercializar para múltiplas estéticas hoje.

| Área | Estado | Pronto para venda? |
|---|---|---|
| Estrutura de projeto | Spring Boot + Angular, ambos modulares | Sim |
| CRUD de Clientes / Serviços / Agendamentos | Implementado com validações | Sim |
| Autenticação JWT | Implementada (login, filter, roles) | Parcial — falta signup e refresh |
| Multi-tenant | **Apenas placeholder (`empresaId` hardcoded como 1L)** | Não |
| Ordem de Serviço | Não existe | Não |
| Financeiro | Não existe | Não |
| Integração WhatsApp | Interface NoOp pronta | Aceitável para MVP |
| Integração IA | Interface NoOp pronta | Aceitável para MVP |
| Testes | `ApiSecurityAndCrudIntegrationTest` cobre os fluxos atuais | Bom para o que existe |

## 2. Pontos fortes do que existe

- **Spring Security já configurado com JWT real** (jjwt 0.12.5), `UserDetailsService` próprio, role-based access (`ADMIN` / `ATENDENTE`).
- **Tratamento global de exceções** via `GlobalExceptionHandler` retornando `ApiErrorResponse` consistente.
- **DTOs como `record`** — imutáveis, idiomáticos para Java 21.
- **Validações de negócio reais** no `AgendamentoService` (transição de status, data passada, serviço inativo) e `ClienteService` (telefone, placa Mercosul).
- **Frontend Angular 17 standalone** com guards (`authGuard`, `loginGuard`), interceptor JWT, toast service e separação clara `core/features/layout`.
- **CORS configurado** via `AppProperties` com origens vindo do YAML.
- **Seed opcional** controlado por flag (`APP_SEED_ENABLED`).
- **Testes de integração** cobrindo segurança + CRUD com `@SpringBootTest` + H2.

## 3. Gaps críticos (bloqueiam venda)

### 3.1 Multi-tenant é fake
- `EntidadeTenantBase` tem `empresaId`, mas:
  - Nenhum filtro automático aplicado nas queries.
  - `empresaId` é setado manualmente como `1L` no seed.
  - Serviços não usam `empresaId` no `findAll` — qualquer usuário vê dados de qualquer empresa.
- Não existe entidade `Empresa`. O `User` não está vinculado a uma empresa.
- **Impacto:** É impossível vender para mais de uma estética sem vazamento de dados entre clientes.

### 3.2 Não há cadastro de empresa (signup)
- Existe `POST /api/auth/login` mas não existe rota pública para criar uma empresa nova com usuário admin.
- **Impacto:** Onboarding de cliente é manual via SQL. Inviável para SaaS.

### 3.3 Faltam módulos centrais para uma estética
- **Ordem de Serviço (OS):** o agendamento existe mas não há registro do serviço executado, com itens, valor cobrado, hora de início/fim, técnico responsável, fotos antes/depois.
- **Financeiro:** sem caixa, sem lançamentos, sem fechamento. Estética não consegue saber quanto faturou no dia.
- **Impacto:** Sistema vira "agenda online" — concorrentes diretos têm estes módulos.

### 3.4 Refresh token / expiração silenciosa
- JWT expira em 12h (`43200s`) e não há refresh. Quando o token expira, frontend só descobre na próxima request.
- **Impacto:** UX ruim — usuário é deslogado sem aviso no meio da operação.

## 4. Gaps secundários (qualidade)

- **`open-in-view: false`** está correto, mas vários `toResponse` acessam `cliente.getNome()` em entidades lazy — funciona porque tudo é EAGER por padrão em `@ManyToOne`, mas vai estourar quando crescer.
- **Sem auditoria** (`createdAt`, `updatedAt`, `createdBy`). Importante para compliance e suporte.
- **Sem paginação** nos endpoints de listagem — `findAll` puro vai derreter quando uma estética tiver 5k clientes.
- **Sem busca/filtro** no backend — frontend filtra in-memory.
- **Sem soft-delete** em `Cliente` (regra de negócio: cliente histórico nunca pode sumir, só ser inativado).
- **Senha do MySQL no `application.yml` está hardcoded** (`212420`). Risco de commit acidental.
- **Sem Flyway/Liquibase** — schema é gerenciado por `ddl-auto: update`, perigoso para produção.
- **`SeedDataConfig` mistura usuários e dados de demo** — deveria ser separado para que demo data seja opcional sem desabilitar criação de usuários.
- **Frontend não tem tela de erro 403/404 dedicada** — só redireciona para login.
- **Sem testes no frontend** além dos `.spec.ts` gerados pelo CLI.

## 5. Riscos arquiteturais

- O backend está em "service-layer architecture" clássica, não hexagonal. Quando o módulo financeiro entrar com regras de negócio mais densas (cálculo de comissão, conciliação), vai ficar acoplado ao JPA.
- A interface `NotificadorWhatsApp` é injetada direto no `AgendamentoService` — em hexagonal isso vira uma "porta de saída" (output port), e o `NoOp` vira um adapter de infraestrutura.
- Não há separação entre "casos de uso" e "controllers" — controllers chamam services que misturam orquestração + regras + persistência.

## 6. O que aproveitar como está

- DTOs (`record`) → viram view-models nos adapters de entrada.
- `GlobalExceptionHandler` → adapter de entrada permanece.
- `JwtService`, `AuthenticatedUser`, `JwtAuthenticationFilter` → adapters de infraestrutura permanecem.
- Migrations: criar o `init-mysql.sql` pode esperar; usar Flyway depois.
- Frontend: a camada `core/services` já é praticamente um anti-corruption layer entre o Angular e a API. Mantém.

## 7. Conclusão

**Para tornar o CleanifyAI vendável** é preciso, nesta ordem:

1. Implementar **Empresa + multi-tenant real + signup**.
2. Refatorar para **hexagonal** os módulos existentes (custo agora, paga ao adicionar OS/Financeiro).
3. Adicionar **Ordem de Serviço** no domínio + UI.
4. Adicionar **Financeiro básico** (lançamentos + fechamento) no domínio + UI.
5. Adicionar **paginação, auditoria, Flyway** como hardening.

A estimativa de esforço, com um único engenheiro foco-total, é de 3 a 5 sprints (6–10 semanas) para chegar à versão 1.0 vendável. Detalhes em `ROADMAP.md`.

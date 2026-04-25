# Arquitetura CleanifyAI — alvo

## 1. Princípios

- **Hexagonal (Ports & Adapters)** com fronteira clara entre domínio e infraestrutura.
- **Multi-tenant por linha** com `empresa_id` em toda entidade tenanteada, filtro automático aplicado no `Repository` via `TenantContext`.
- **Stateless**: JWT no cabeçalho, contexto de empresa derivado do token, sem sessão.
- **Pragmatismo**: hexagonal não significa over-engineering — DTOs simples, sem mappers automáticos pesados (MapStruct fica para depois).
- **Testabilidade**: domínio puro testável sem Spring; adapters testados com `@SpringBootTest` + H2.

## 2. Estrutura de pacotes (backend)

```text
com.cleanifyai.api
├── shared/                     # cross-cutting, sem dependência de outros módulos
│   ├── domain/                 # value objects compartilhados (TenantId, Cnpj, Telefone)
│   ├── exception/              # BusinessException, ResourceNotFoundException
│   └── tenant/                 # TenantContext (ThreadLocal), TenantContextFilter
│
├── auth/                       # módulo: autenticação e tenancy
│   ├── domain/                 # User, Role, Empresa (entidades de domínio puro, anêmico ok)
│   ├── application/
│   │   ├── port/in/            # UseCases (LoginUseCase, RegisterCompanyUseCase)
│   │   └── port/out/           # Repositories como interface (UserRepository, EmpresaRepository)
│   ├── infrastructure/
│   │   ├── persistence/        # entities JPA + repositórios Spring Data + adapters
│   │   ├── security/           # JwtService, JwtAuthenticationFilter, SecurityConfig
│   │   └── web/                # AuthController, RegisterCompanyController, DTOs
│   └── ...
│
├── clientes/
│   ├── domain/                 # Cliente (record/class), regras puras
│   ├── application/
│   │   ├── port/in/            # CriarCliente, AtualizarCliente, ListarClientes...
│   │   └── port/out/           # ClienteRepository (interface)
│   └── infrastructure/
│       ├── persistence/        # ClienteJpaEntity, ClienteJpaRepository, adapter
│       └── web/                # ClienteController, ClienteRequest/Response
│
├── servicos/                   # mesma estrutura
├── agendamentos/               # mesma estrutura, depende de clientes/servicos via port
├── ordens/                     # NOVO — Ordem de Serviço
│   ├── domain/                 # OrdemServico, ItemOrdem, StatusOrdem
│   ├── application/
│   └── infrastructure/
│
└── financeiro/                 # NOVO — Lançamentos e Caixa
    ├── domain/                 # Lancamento, FormaPagamento, FechamentoCaixa
    ├── application/
    └── infrastructure/
```

### Por que essa estrutura?

- **Cada módulo (`clientes`, `agendamentos`, ...) é um "vertical slice"**: contém seu próprio domain/application/infrastructure. Sem pacotes globais `controller/service/repository`. Isso reduz acoplamento e permite extrair em microsserviços no futuro sem cirurgia.
- **`port/in` = casos de uso** (interfaces que descrevem _o que_ o sistema faz).
- **`port/out` = repositórios e integrações** (interfaces que descrevem _o que o domínio precisa_).
- **`infrastructure/` = adapters concretos** (Spring Data JPA, controllers REST, JWT, WhatsApp).
- O **domínio nunca importa Spring nem JPA**. Hibernate fica restrito ao subpacote `infrastructure/persistence`.

## 3. Multi-tenant: como funciona

```text
Request → JwtAuthenticationFilter
       → TenantContextFilter (lê empresaId do JWT, popula ThreadLocal)
       → Controller
       → UseCase
       → RepositoryAdapter
            ↳ injeta TenantContext.getEmpresaId() em todo find/save
       → DB

Response → TenantContextFilter limpa ThreadLocal (finally)
```

### Decisões

1. **`empresa_id` é coluna física** em todas as tabelas tenanteadas. Sem schema-per-tenant nem database-per-tenant.
2. **Filtro automático** implementado no adapter JPA (não no controller, não no service). Quem usa `port/out` não precisa lembrar de filtrar.
3. **Hibernate `@FilterDef`** é o mecanismo escolhido — declarativo e barato. Aplicado por entidade.
4. **Endpoints públicos** (`/api/auth/login`, `/api/auth/register-company`, `/api/ping`) NÃO populam `TenantContext`.
5. **Quebra-galho administrativo**: se aparecer um endpoint cross-tenant (suporte interno), ele explicitamente desabilita o filter.

### Onboarding de empresa (signup)

```text
POST /api/auth/register-company
{
  "empresa": { "nome": "Estética X", "cnpj": "...", "telefone": "..." },
  "admin":   { "nome": "...", "email": "...", "senha": "..." }
}

Response 201:
{
  "empresa": { "id": 7, "nome": "Estética X" },
  "admin":   { "id": 12, "email": "...", "role": "ADMIN" },
  "token":   "eyJ..." // JWT já com empresaId no claim
}
```

JWT passa a carregar:
```json
{
  "sub": "admin@empresa.com",
  "role": "ADMIN",
  "empresaId": 7,
  "name": "..."
}
```

## 4. Estrutura do frontend

```text
src/app/
├── core/                # interceptors, guards, modelos compartilhados, services HTTP
├── layout/              # shell, navegação
├── features/
│   ├── auth/            # login + signup-empresa
│   ├── dashboard/
│   ├── clientes/
│   ├── servicos/
│   ├── agendamentos/
│   ├── ordens/          # NOVO
│   └── financeiro/      # NOVO
└── shared/              # componentes reutilizáveis (botões, modal, table, status pill)
```

A arquitetura Angular não muda radicalmente — só ganha módulos `ordens` e `financeiro` e uma camada `shared` para componentes que hoje se repetem.

## 5. Decisões arquiteturais (ADRs resumidas)

### ADR-001: Hexagonal por módulo (vertical slice)
**Decisão:** cada bounded context tem domain/application/infrastructure próprios.
**Alternativa rejeitada:** camadas globais (`controller/`, `service/`, `repository/`). Geram acoplamento por camada e dificultam ownership por feature.
**Trade-off:** mais pastas, mais imports. Aceito.

### ADR-002: Multi-tenant por coluna + Hibernate Filter
**Decisão:** `empresa_id` em cada tabela, filtro automático via `@Filter`.
**Alternativas rejeitadas:**
- Schema-per-tenant: caro de migrar e operar.
- Database-per-tenant: invável no MVP.
**Trade-off:** depende de disciplina (não esquecer `@Filter` em entidade nova). Mitigado por base class + teste de arquitetura.

### ADR-003: Sem MapStruct no MVP
**Decisão:** mappers feitos à mão em métodos estáticos.
**Razão:** evitar mais um build step e annotation processor; o ganho só aparece com domínios grandes.

### ADR-004: Flyway no momento de adicionar Empresa
**Decisão:** parar de depender de `ddl-auto: update` ao introduzir multi-tenant.
**Razão:** índices de `empresa_id` precisam ser determinísticos e versionados.

### ADR-005: WhatsApp e IA continuam como portas de saída
**Decisão:** `NotificadorWhatsApp` e `MotorSugestaoIa` permanecem como interfaces, com adapter `NoOp` em dev e adapter real (futuro) em produção.
**Razão:** alinhado com hexagonal — domínio não conhece provedor.

## 6. O que NÃO faz parte deste alvo

Para evitar inflar o MVP, ficam **fora** desta arquitetura:
- Event sourcing / CQRS.
- Mensageria (Kafka/RabbitMQ).
- GraphQL.
- Microsserviços.
- Cache distribuído (Redis).

São reavaliados depois da v1 vendida.

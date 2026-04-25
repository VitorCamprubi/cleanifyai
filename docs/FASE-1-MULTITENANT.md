# Fase 1 — Multi-tenant real + Empresa + Signup

> Entrega: 2026-04-25

## O que mudou

### Backend (`cleanifyai-api`)

Novos arquivos:
- `domain/entity/Empresa.java` — entidade da empresa (estética).
- `repository/EmpresaRepository.java`.
- `shared/tenant/TenantContext.java` — `ThreadLocal<Long>` populado por requisição.
- `shared/tenant/TenantEntityListener.java` — `@PrePersist` que injeta `empresaId` automaticamente.
- `dto/auth/RegisterCompanyRequest.java` — payload de signup.

Arquivos alterados:
- `domain/entity/User.java` — agora tem `empresaId` (FK).
- `domain/entity/EntidadeTenantBase.java` — `@EntityListeners(TenantEntityListener.class)` + `nullable=false`.
- `security/JwtService.java` — adiciona claim `empresaId` no token.
- `security/JwtAuthenticationFilter.java` — popula `TenantContext` a partir do JWT, limpa no `finally`.
- `service/AuthService.java` — novo método `registerCompany(...)` cria Empresa + User admin + retorna JWT.
- `controller/AuthController.java` — novo endpoint `POST /api/auth/register-company`.
- `config/SecurityConfig.java` — libera `/api/auth/register-company`.
- `dto/auth/AuthUserResponse.java` — agora retorna `empresaId`.
- `repository/{ClienteRepository,ServicoRepository,AgendamentoRepository}.java` — métodos com `empresaId`.
- `service/{ClienteService,ServicoService,AgendamentoService,DashboardService}.java` — usam `TenantContext.requireEmpresaId()`.
- `config/SeedDataConfig.java` — cria empresa demo antes dos usuários.
- `test/.../ApiSecurityAndCrudIntegrationTest.java` — atualizado para criar empresa e linkar usuários.

### Frontend (`cleanifyai-web`)

Novos arquivos:
- `features/auth/_auth-shared.scss` — partial de estilos compartilhados entre login e signup.
- `features/auth/pages/signup-page.component.{ts,html,scss}` — tela de cadastro de empresa.

Arquivos alterados:
- `core/models/auth.model.ts` — `AuthUser` ganha `empresaId`; novo `RegisterCompanyPayload`.
- `core/services/auth.service.ts` — novo método `registerCompany(payload)` e getter `empresaIdAtual`.
- `features/auth/pages/login-page.component.{ts,html,scss}` — link para `/signup`, scss agora apenas reusa o partial.
- `app.routes.ts` — nova rota `/signup`.

## Como testar manualmente

Backend:

```bash
cd cleanifyai-api
mvnw.cmd spring-boot:run
```

Frontend:

```bash
cd cleanifyai-web
npm install
npm start
```

Cenário 1 — login com seed:
1. Acesse http://localhost:4200/login.
2. Use `admin@cleanifyai.local` / `admin123`.
3. Decodifique o JWT em jwt.io — verá o claim `empresaId`.

Cenário 2 — criar empresa nova:
1. Acesse http://localhost:4200/signup.
2. Preencha empresa e admin → submeta.
3. Cai direto no dashboard.
4. Cliente/serviço/agendamento criados nesta empresa não aparecem para a empresa demo.

Cenário 3 — isolamento real:
1. Abra duas sessões em janelas anônimas, com empresas diferentes.
2. Crie clientes em cada uma.
3. Confirme que cada janela só vê os próprios clientes.

## Atenção: migração do banco de desenvolvimento

A coluna `empresa_id` em `users` passa a ser **NOT NULL**. Se você já tinha o banco `cleanifyai` no MySQL local com a versão anterior, o `ddl-auto: update` do Hibernate **não consegue** adicionar `NOT NULL` automaticamente em coluna nova com linhas existentes.

Opção 1 — derrubar o banco (mais rápido em dev):

```sql
DROP DATABASE cleanifyai;
```

Na próxima inicialização o Hibernate recria com schema novo + seed.

Opção 2 — migrar manualmente:

```sql
USE cleanifyai;

-- 1. Criar empresa default
INSERT INTO empresas (nome, ativa, criada_em) VALUES ('CleanifyAI Demo', 1, NOW());
SET @empresa_id = LAST_INSERT_ID();

-- 2. Backfill em users
ALTER TABLE users ADD COLUMN empresa_id BIGINT NULL;
UPDATE users SET empresa_id = @empresa_id WHERE empresa_id IS NULL;
ALTER TABLE users MODIFY empresa_id BIGINT NOT NULL;

-- 3. Backfill nas tabelas de domínio (se existirem)
UPDATE clientes SET empresa_id = @empresa_id WHERE empresa_id IS NULL;
UPDATE servicos SET empresa_id = @empresa_id WHERE empresa_id IS NULL;
UPDATE agendamentos SET empresa_id = @empresa_id WHERE empresa_id IS NULL;

ALTER TABLE clientes MODIFY empresa_id BIGINT NOT NULL;
ALTER TABLE servicos MODIFY empresa_id BIGINT NOT NULL;
ALTER TABLE agendamentos MODIFY empresa_id BIGINT NOT NULL;
```

Em produção isso vira migration Flyway — entra na Fase 5.

## Limites conhecidos desta fase

- **Sem refresh token.** Token expira em 12h e o usuário é deslogado. Fica para a Fase 5.
- **Hibernate `@FilterDef` não foi adotado** — o filtro está no nível do repositório (`findAllByEmpresaId`). Mais explícito, menos elegante. A migração para `@Filter` automático fica para a Fase 2 (refatoração hexagonal).
- **`empresaId` ainda não é validado em endpoints administrativos cross-tenant** — porque ainda não existem. Quando entrar suporte interno (admin global), criar role `SUPER_ADMIN` e bypass do `TenantContext`.
- **Frontend não exibe nome da empresa no header** — pequeno polimento que vai junto com a Fase 2.

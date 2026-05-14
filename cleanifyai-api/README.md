# CleanifyAI API

Backend Spring Boot do CleanifyAI para esteticas automotivas.

## Visao Geral

A API esta organizada em camadas simples:

- `controller`: endpoints REST JSON.
- `service`: regras de negocio e orquestracao.
- `repository`: acesso a dados com Spring Data JPA.
- `domain/entity`: entidades persistidas.
- `dto`: contratos de request/response.
- `security`: JWT, refresh token, auditoria HTTP e autenticacao.
- `config`: CORS, Security, seed e properties.
- `shared/tenant`: contexto e listener multi-tenant.

## Modulos Atuais

- Auth com signup de empresa, login JWT, refresh token rotativo e logout.
- Multi-tenant por `empresa_id`.
- Clientes, veiculos, servicos e agendamentos.
- Ordens de servico com itens e maquina de estados.
- Financeiro com lancamentos, categorias e resumo.
- Audit log de escritas HTTP.
- Actuator health, liveness e readiness.

## Como Rodar

Profile padrao: `dev`.

```bash
mvnw.cmd spring-boot:run
```

Defaults do profile `dev`:

- `DB_HOST=localhost`
- `DB_PORT=3306`
- `DB_NAME=cleanifyai`
- `DB_USERNAME=root`
- `DB_PASSWORD=212420`
- `APP_SEED_ENABLED=true`
- `SERVER_PORT=8080`

Para usar outro MySQL, configure as variaveis no IntelliJ em `Run/Debug Configurations > Environment variables`.

## Endpoints Publicos

- `GET /api/ping`
- `POST /api/auth/login`
- `POST /api/auth/register-company`
- `POST /api/auth/refresh`
- `POST /api/auth/logout`
- `GET /actuator/health`
- `GET /actuator/health/liveness`
- `GET /actuator/health/readiness`

## Testes

```bash
mvnw.cmd test
```

Os testes usam H2 em memoria com `ddl-auto=create-drop` e Flyway desabilitado para manter a suite rapida.

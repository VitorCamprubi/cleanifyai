# CleanifyAI API

Backend do MVP do CleanifyAI para estéticas automotivas.

## Visao geral

A API foi organizada em camadas para manter o MVP simples, legivel e facil de evoluir:

- `controller`: endpoints REST JSON.
- `service`: regras de negocio e orquestracao.
- `repository`: acesso a dados com Spring Data JPA.
- `domain/entity`: entidades persistidas no MySQL.
- `dto`: contratos de request/response.
- `config`: CORS, seed e properties da aplicacao.
- `integration`: pontos de extensao para WhatsApp e IA.
- `exception`: tratamento global com `@RestControllerAdvice`.

## Estrutura principal

```text
cleanifyai-api/
  database/
    init-mysql.sql
  src/main/java/com/cleanifyai/api/
    config/
    controller/
    domain/
    dto/
    exception/
    integration/
    repository/
    service/
  src/main/resources/
    application.yml
  src/test/resources/
    application.yml
```

## Funcionalidades do MVP

- CRUD completo de clientes.
- CRUD completo de servicos.
- CRUD operacional de agendamentos.
- Dashboard com totais e proximos agendamentos.
- Endpoint de health check em `GET /api/ping`.
- Seed opcional para desenvolvimento.

## Endpoints

- `GET /api/ping`
- `GET|POST /api/clientes`
- `GET|PUT|DELETE /api/clientes/{id}`
- `GET|POST /api/servicos`
- `GET|PUT|DELETE /api/servicos/{id}`
- `GET|POST /api/agendamentos`
- `GET|PUT /api/agendamentos/{id}`
- `PATCH /api/agendamentos/{id}/status`
- `PATCH /api/agendamentos/{id}/cancelar`
- `GET /api/dashboard`

## Como rodar localmente

### 1. Suba o MySQL

Crie o banco com o script abaixo, se necessario:

```sql
SOURCE database/init-mysql.sql;
```

### 2. Configure as variaveis de ambiente

Valores padrao definidos em `src/main/resources/application.yml`:

- `DB_HOST=localhost`
- `DB_PORT=3306`
- `DB_NAME=cleanifyai`
- `DB_USERNAME=root`
- `DB_PASSWORD=root`
- `APP_SEED_ENABLED=true`
- `SERVER_PORT=8080`

Se voce estiver rodando pelo IntelliJ, configure essas variaveis em:

- `Run/Debug Configurations`
- `Environment variables`

Exemplo:

```text
DB_HOST=localhost;DB_PORT=3306;DB_NAME=cleanifyai;DB_USERNAME=root;DB_PASSWORD=sua_senha_real
```

Se o seu MySQL local nao usa senha `root`, o backend nao vai subir com os valores padrao.

### 3. Execute a aplicacao

Windows:

```bash
mvnw.cmd spring-boot:run
```

Linux/macOS:

```bash
./mvnw spring-boot:run
```

### 4. Execute os testes

```bash
mvnw.cmd test
```

## Decisoes de arquitetura

- `empresaId` foi colocado em uma entidade base para preparar multi-tenant futuro sem acoplar isso no MVP.
- As interfaces `NotificadorWhatsApp` e `MotorSugestaoIa` deixam pontos claros para plug de integracoes futuras.
- A autenticacao nao foi implementada para acelerar a entrega do MVP. O proximo passo natural e adicionar Spring Security com JWT.
- O seed e opcional via propriedade para facilitar demos e desenvolvimento local.

## Evolucao recomendada

- Adicionar Spring Security + JWT com papeis basicos.
- Implementar tenancy por empresa com filtro por `empresaId`.
- Integrar confirmacoes automáticas por WhatsApp via webhook/provider.
- Incluir trilha de auditoria e historico de status de agendamento.

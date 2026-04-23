# CleanifyAI MVP

Implementacao inicial do CleanifyAI como base de um SaaS para estéticas automotivas.

## Arquitetura proposta

### Objetivo

Entregar um MVP vendavel com foco em operacao e agendamento, mas com estrutura preparada para:

- integracao com WhatsApp
- motor de IA para atendimento
- autenticacao JWT
- multi-tenant por empresa/estetica

### Decisao arquitetural

- Backend em `Spring Boot` com API REST JSON e camadas bem definidas.
- Frontend em `Angular` organizado por feature.
- MySQL como banco principal.
- Multi-tenant preparado via `empresaId` em entidade base, sem ativar tenancy completa ainda.
- Integracoes futuras modeladas por interfaces `NoOp` para nao inflar o MVP.

## Estrutura dos projetos

```text
cleanifyai/
  cleanifyai-api/
    README.md
    pom.xml
    database/init-mysql.sql
    src/main/java/com/cleanifyai/api/
      config/
      controller/
      domain/
      dto/
      exception/
      integration/
      repository/
      service/
  cleanifyai-web/
    README.md
    package.json
    angular.json
    src/app/
      core/
      features/
      layout/
    src/environments/
```

## Execucao rapida

### Backend

```bash
cd cleanifyai-api
mvnw.cmd spring-boot:run
```

### Frontend

```bash
cd cleanifyai-web
npm install
npm start
```

## Validacoes executadas

- Backend: `mvnw.cmd test`
- Frontend: `npm run build`

## Proximos passos sugeridos

1. Adicionar autenticacao JWT e perfis basicos de acesso.
2. Evoluir o modulo de agendamento para ordem de servico.
3. Implementar tenancy real por empresa com filtro automatico.
4. Conectar confirmacoes e lembretes via WhatsApp.
5. Adicionar camada de IA para sugestao de respostas e resumo de atendimento.

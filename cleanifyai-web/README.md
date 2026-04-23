# CleanifyAI Web

Frontend Angular do MVP do CleanifyAI.

## Visao geral

A aplicacao foi organizada por feature com componentes standalone:

- `dashboard`: indicadores e proximos agendamentos.
- `clientes`: cadastro e manutencao da base de clientes.
- `servicos`: catalogo e configuracao comercial.
- `agendamentos`: agenda operacional com status.
- `core`: models e servicos HTTP.
- `layout`: shell principal com navegacao lateral.

## Estrutura principal

```text
cleanifyai-web/
  src/app/
    core/
      models/
      services/
    features/
      dashboard/
      clientes/
      servicos/
      agendamentos/
    layout/
      app-shell/
  src/environments/
    environment.ts
```

## Requisitos

- Node.js 20+
- NPM 10+
- Backend `cleanifyai-api` rodando em `http://localhost:8080`

## Como rodar localmente

### 1. Instale as dependencias

```bash
npm install
```

### 2. Inicie o servidor de desenvolvimento

```bash
npm start
```

A aplicacao sera servida em `http://localhost:4200`.

### 3. Gere o build de producao

```bash
npm run build
```

## Configuracao da API

O endpoint base esta em `src/environments/environment.ts`:

```ts
export const environment = {
  production: false,
  apiUrl: 'http://localhost:8080/api'
};
```

## Fluxos implementados

- Dashboard com totais e proximos agendamentos.
- CRUD de clientes em tela unica com formulario e listagem.
- CRUD de servicos em tela unica com formulario e listagem.
- CRUD de agendamentos com atualizacao rapida de status e cancelamento.
- Indicador de conectividade da API via `/api/ping`.

## Evolucao recomendada

- Adicionar autenticacao JWT com guarda de rotas.
- Incluir estados globais mais robustos caso o produto cresca.
- Preparar componentes compartilhados para multi-tenant e configuracoes por empresa.
- Integrar notificacoes visuais/toasts e pagina detalhada de ordem de servico.

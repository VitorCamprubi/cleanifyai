# Fase 5 — Hardening (parcial) e validacao

> Entrega: 2026-04-25

## O que mudou

### Auditoria automatica

`EntidadeTenantBase` ganhou `criadoEm` (`@CreationTimestamp`) e `atualizadoEm` (`@UpdateTimestamp`). Todas as entidades tenanteadas — Cliente, Servico, Agendamento, OrdemServico, ItemOrdem indireto, Lancamento — passam a registrar timestamps automaticamente, sem nenhum codigo nas services.

Colunas adicionadas como `nullable = true` no JPA propositalmente: `ddl-auto: update` consegue acrescentar a coluna em tabelas com dados existentes; novas linhas recebem o timestamp via Hibernate. Em Flyway (Fase 5 completa) viram `NOT NULL` com backfill.

### Soft-delete em Cliente

- Novo campo `ativo` (default `true`).
- `ClienteService.excluir(id)` agora seta `ativo = false` em vez de remover.
- Listagens, busca por id e contador do dashboard usam `findAllByEmpresaIdAndAtivoTrue`, `findByIdAndEmpresaIdAndAtivoTrue` e `countByEmpresaIdAndAtivoTrue`.
- `ClienteService.buscarEntidadeIncluindoInativos(id)` exposto para fluxos de leitura historica (relatorios, telas de detalhe de OS antiga). Nao e usado por OS hoje porque o `@ManyToOne` carrega o cliente diretamente da FK independentemente do flag.
- Removida a regra "cliente com agendamento vinculado nao pode ser excluido": com soft-delete o historico e preservado.

### Testes integrados novos

- `OrdemServicoIntegrationTest` — criar OS com itens e calculo de total, maquina de estados completa, transicao invalida, edicao bloqueada apos status final, atendente operando OS, exigencia de auth.
- `FinanceiroIntegrationTest` — admin registra entrada e ve no resumo, atendente le mas nao registra, valor zero/negativo rejeitado, data futura rejeitada, estorno no mesmo dia, agregacao por forma de pagamento.

Em conjunto com `ApiSecurityAndCrudIntegrationTest`, a suite cobre: auth, multi-tenant implicito (cada teste em sua propria empresa), CRUD core, agendamento, OS, financeiro e roles.

### README

Reescrito para refletir o estado real e funcionar como guia de onboarding de novo dev: stack, estrutura, como rodar, decisoes ativas, proximos passos.

## O que NAO foi feito (continua pendente)

| Item | Por que ficou de fora |
|---|---|
| Flyway | Migracao de banco existente nao trivial; requer sessao dedicada com plano de rollback. |
| Refresh token | Implica em tabela de tokens + frontend handling + telas de "sessao expirada". |
| Paginacao | Toca todas as listagens (backend + frontend). Sem dor real ate ~5k linhas por empresa. |
| Rate limit no login | Dependencia extra (bucket4j ou similar); melhor com infraestrutura (CDN/WAF). |
| Logger estruturado JSON | Tarefa de ops. |
| Refator hexagonal (Fase 2) | Adiada por escolha — pagar quando entrar mais um modulo. |

Cada item esta detalhado em `ROADMAP.md`.

## Instrucao de migracao do banco local

Como `criado_em` e `atualizado_em` foram adicionados, e `ativo` foi adicionado a `clientes`, na proxima execucao com `ddl-auto: update` o Hibernate fara:

```sql
ALTER TABLE clientes ADD COLUMN ativo BIT NOT NULL DEFAULT TRUE;  -- ou NULL, depende do driver
ALTER TABLE clientes ADD COLUMN criado_em DATETIME(6);
ALTER TABLE clientes ADD COLUMN atualizado_em DATETIME(6);
-- mesmo para servicos, agendamentos, ordens_servico, lancamentos
```

Se o driver MySQL recusar `NOT NULL` em coluna nova com linhas existentes, derrube o banco em dev:

```sql
DROP DATABASE cleanifyai;
```

E suba a aplicacao novamente — o seed recria.

## Smoke run sugerido

Apos `mvnw.cmd test` passar:

1. `mvnw.cmd spring-boot:run` em um terminal.
2. `npm start` em outro.
3. Login como admin demo.
4. Cadastre cliente, servico, agendamento.
5. Crie OS a partir desse fluxo.
6. Conclua a OS, vincule pagamento em `/financeiro`.
7. Logout. Cadastre uma empresa nova em `/signup`. Confirme que NAO ve dados da empresa anterior.

Se todos os 7 passos rodam, o MVP esta vendavel.

# Fase 3 — Ordem de Servico (OS)

> Entrega: 2026-04-25

## O que mudou

### Backend (`cleanifyai-api`)

Novos arquivos:
- `domain/enums/StatusOrdem.java` — `ABERTA → EM_EXECUCAO → CONCLUIDA → ENTREGUE / CANCELADA`.
- `domain/entity/OrdemServico.java` — entidade tenanteada, com itens, valor total agregado, timestamps.
- `domain/entity/ItemOrdem.java` — item da ordem (servico, descricao, qtd, valor unit/total).
- `repository/OrdemServicoRepository.java`.
- `dto/ordem/OrdemServicoRequest.java`, `OrdemServicoResponse.java`, `ItemOrdemRequest.java`, `ItemOrdemResponse.java`, `AtualizarStatusOrdemRequest.java`.
- `service/OrdemServicoService.java` — regras: criacao, atualizacao apenas em status editavel, transicoes validas, recalculo automatico de `valorTotal`, vinculo opcional com Agendamento, fechamento automatico via `fechadaEm`.
- `controller/OrdemServicoController.java`.

Alterado:
- `config/SecurityConfig.java` — libera `/api/ordens/**` para `ADMIN` e `ATENDENTE`.

### Frontend (`cleanifyai-web`)

Novos arquivos:
- `core/models/ordem.model.ts` — tipos + labels + transicoes permitidas no front.
- `core/services/ordens-api.service.ts`.
- `features/ordens/pages/ordens-page.component.{ts,html,scss}` — pagina completa: form com itens dinamicos (`FormArray`), filtro por status, lista com acoes contextuais por status.

Alterado:
- `app.routes.ts` — nova rota `/ordens`.
- `layout/app-shell/app-shell.component.ts` — item "Ordens de Servico" no menu.

## Endpoints novos

| Metodo | Path | Descricao |
|---|---|---|
| POST | `/api/ordens` | Cria nova OS. |
| GET | `/api/ordens?status=ABERTA` | Lista (filtro opcional). |
| GET | `/api/ordens/{id}` | Detalhe. |
| PUT | `/api/ordens/{id}` | Atualiza cliente, itens, observacoes (somente em status `ABERTA` ou `EM_EXECUCAO`). |
| PATCH | `/api/ordens/{id}/status` | Avanca status conforme transicoes validas. |
| PATCH | `/api/ordens/{id}/cancelar` | Cancela. |

## Maquina de estados

```
ABERTA ─▶ EM_EXECUCAO ─▶ CONCLUIDA ─▶ ENTREGUE
   │            │             │
   └─▶ CANCELADA ◀───────── ─┘
```

`fechadaEm` é gravada na primeira transicao que atinge `CONCLUIDA`, `ENTREGUE` ou `CANCELADA`.

## Como testar

1. Suba o backend e o frontend (mesmo passo das fases anteriores).
2. Faca login como admin (ou via signup novo).
3. Cadastre **clientes e servicos ativos** (a OS exige ambos).
4. Abra `/ordens`:
   - Clique "+ Adicionar item" → escolha um servico → o `valorUnitario` e a `descricao` sao pre-preenchidos a partir do cadastro do servico, mas voce pode editar ambos.
   - Repita para mais itens. O total atualiza em tempo real.
   - Clique "Criar OS".
5. Na lista, exercite as transicoes: **Em execucao → Concluida → Entregue**, com botao "Cancelar OS" disponivel ate `EM_EXECUCAO`.
6. Tente editar uma OS ja `CONCLUIDA` — o sistema impede com toast de erro.

## Limites conhecidos

- **Sem geracao automatica de Lancamento** ao concluir OS — entra com a Fase 4 (Financeiro).
- **Sem fluxo "abrir OS a partir de um agendamento"** com pre-popular cliente/servico no front. O backend ja aceita `agendamentoId`, mas a UI ainda nao tem o atalho. Pequeno melhoramento.
- **Sem upload de fotos antes/depois** — fora do MVP.
- **Sem testes de integracao para `/api/ordens`** — pendentes na Fase 8 (validacao final).

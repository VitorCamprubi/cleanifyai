# Fase 4 — Financeiro basico

> Entrega: 2026-04-25

## O que mudou

### Backend (`cleanifyai-api`)

Novos arquivos:
- `domain/enums/TipoLancamento.java` — `ENTRADA | SAIDA`.
- `domain/enums/FormaPagamento.java` — `DINHEIRO, PIX, DEBITO, CREDITO, BOLETO, OUTROS`.
- `domain/entity/Lancamento.java` — entidade tenanteada com `tipo`, `valor`, `formaPagamento`, `dataLancamento`, `descricao`, `ordemId` (referencia frouxa, sem FK gerenciada), `registradoEm`.
- `repository/LancamentoRepository.java` — busca por periodo + por id+empresa.
- `dto/financeiro/LancamentoRequest.java`, `LancamentoResponse.java`, `TotalPorFormaResponse.java`, `ResumoFinanceiroResponse.java`.
- `service/FinanceiroService.java` — registrar, listar, resumo agregado por forma de pagamento, estorno (apenas no mesmo dia).
- `controller/FinanceiroController.java`.

Alterado:
- `config/SecurityConfig.java` — `GET /api/financeiro/**` para `ADMIN+ATENDENTE`, escrita/estorno apenas `ADMIN`.

### Frontend (`cleanifyai-web`)

Novos arquivos:
- `core/models/financeiro.model.ts` — tipos + labels + opcoes de formas de pagamento.
- `core/services/financeiro-api.service.ts`.
- `features/financeiro/pages/financeiro-page.component.{ts,html,scss}` — pagina com filtro de periodo, cards de totais, breakdown por forma de pagamento, formulario de novo lancamento (com auto-fill ao vincular a uma OS) e lista com estorno.

Alterado:
- `app.routes.ts` — nova rota `/financeiro`.
- `layout/app-shell/app-shell.component.ts` — item "Financeiro" no menu.

## Endpoints novos

| Metodo | Path | Descricao |
|---|---|---|
| POST | `/api/financeiro/lancamentos` | Registra entrada ou saida (apenas ADMIN). |
| GET | `/api/financeiro/lancamentos?inicio=&fim=` | Lista no periodo. |
| GET | `/api/financeiro/resumo?inicio=&fim=` | Totais agregados por forma de pagamento. |
| DELETE | `/api/financeiro/lancamentos/{id}` | Estorna (apenas no mesmo dia). |

## Regras de negocio

1. **Valor** > 0 obrigatorio; arredondado a 2 casas decimais.
2. **Data de lancamento** nao pode ser futura.
3. **Estorno** so eh permitido se `dataLancamento == hoje`.
4. **Vinculo com OS**: se `ordemId` informado, o servico valida que a OS pertence a empresa do usuario (via `OrdemServicoService.buscarEntidade`).
5. **Resumo por forma**: somente formas com algum movimento aparecem no response.
6. **Tenancy**: todos os lancamentos sao escopados via `TenantContext` (filtro automatico por `empresaId`).

## Como testar

1. Suba o backend e o frontend.
2. Faca login como **admin** (atendente nao consegue registrar nem estornar).
3. Crie uma OS e leve ate `CONCLUIDA` ou `EM_EXECUCAO`.
4. Va em `/financeiro`:
   - O periodo padrao eh "hoje".
   - Clique em "+ Adicionar lancamento" (na verdade o formulario ja esta aberto na tela), escolha tipo "Entrada", forma "PIX".
   - Selecione a OS no dropdown — `valor` e `descricao` sao auto-preenchidos com base na OS.
   - Clique em "Registrar".
5. O card "Entradas" aumenta, o "Saldo" reflete, o breakdown por "PIX" aparece.
6. Use "Mes atual" ou ajuste manualmente as datas para ver historico.
7. Para estornar: clique em "Estornar" ao lado do lancamento (so funciona no dia).

## Limites conhecidos

- **Sem categorias de despesa** — a `descricao` faz papel duplo. Categorias entram com hardening.
- **Sem multiplas formas de pagamento por OS** — se cliente paga metade PIX e metade dinheiro, lance dois lancamentos.
- **Estorno = delete fisico** sem trilha de auditoria. Para producao, virar soft delete + tabela de auditoria (Fase 5).
- **Gerar lancamento automatico ao concluir OS** ainda nao esta implementado — e intencional para deixar o operador escolher forma de pagamento e parcelamento.

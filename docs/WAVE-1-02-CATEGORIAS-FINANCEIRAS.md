# Wave 1 — Feature 2: Categorias financeiras

> Entrega: 2026-04-26

## O que mudou

### Backend

Novos arquivos:
- `domain/enums/TipoCategoria.java` — `RECEITA | DESPESA | AMBOS`, com helper `aceitaTipoLancamento()`.
- `domain/entity/CategoriaFinanceira.java` — entidade tenanteada, soft-delete via flag `ativo`.
- `repository/CategoriaFinanceiraRepository.java`.
- `dto/financeiro/CategoriaFinanceiraRequest.java` — valida cor em hexadecimal.
- `dto/financeiro/CategoriaFinanceiraResponse.java`.
- `service/CategoriaFinanceiraService.java` — CRUD com soft-delete + protecao contra duplicado por nome.
- `controller/CategoriaFinanceiraController.java` — endpoints sob `/api/financeiro/categorias`.

Alterado:
- `domain/entity/Lancamento.java` — campo `categoriaId` opcional.
- `dto/financeiro/LancamentoRequest.java` — campo `categoriaId` opcional.
- `dto/financeiro/LancamentoResponse.java` — campos `categoriaId`, `categoriaNome`, `categoriaCor` resolvidos via lookup.
- `service/FinanceiroService.java` — recebe `CategoriaFinanceiraService`, valida que tipo da categoria casa com tipo do lancamento (`RECEITA <-> ENTRADA`, `DESPESA <-> SAIDA`, `AMBOS` aceita ambos), resolve nome e cor na response.
- `config/SeedDataConfig.java` — cria 6 categorias padrao (Servicos, Produtos, Material de limpeza, Aluguel, Salarios, Outros) com cores distintas.

SecurityConfig ja cobria `/api/financeiro/**` — sem mudanca.

### Frontend

Novos arquivos:
- `features/financeiro/pages/categorias-page.component.{ts,html,scss}` — pagina dedicada com CRUD, color picker nativo HTML5, filtros por tipo, lista com pill colorida.

Alterado:
- `core/models/financeiro.model.ts` — `TipoCategoria`, `CategoriaFinanceira`, `CategoriaFinanceiraRequest`, labels e options. `Lancamento` ganhou `categoriaId`, `categoriaNome`, `categoriaCor`. `LancamentoRequest` ganhou `categoriaId`.
- `core/services/financeiro-api.service.ts` — `listarCategorias(tipo?)`, `criarCategoria`, `atualizarCategoria`, `excluirCategoria`.
- `features/financeiro/pages/financeiro-page.component.ts` — carrega categorias, helper `categoriasParaTipo()` filtra dinamicamente conforme tipo do lancamento, payload inclui `categoriaId`.
- `features/financeiro/pages/financeiro-page.component.html` — novo dropdown "Categoria (opcional)" filtrado pelo tipo, lista exibe pill colorida com nome da categoria.
- `features/financeiro/pages/financeiro-page.component.scss` — estilo da pill.
- `app.routes.ts` — rota `/financeiro/categorias`.
- `layout/app-shell/app-shell.component.ts` — item "Categorias" no menu.

## Regras de negocio

1. **Nome unico por empresa** — nao permite duas categorias ativas com mesmo nome.
2. **Tipo restrige uso** — categoria `RECEITA` so aceita `ENTRADA`, categoria `DESPESA` so aceita `SAIDA`, `AMBOS` aceita qualquer.
3. **Filtro automatico no front** — dropdown na pagina de Financeiro mostra apenas categorias compativeis com o tipo selecionado (mais `AMBOS`).
4. **Soft-delete** — categoria inativada some das listagens novas mas lancamentos historicos continuam mostrando o nome/cor (resolvido via `buscarEntidadeIncluindoInativos`).
5. **Cor opcional em hexadecimal** — valida pattern `#RRGGBB` ou `#RRGGBBAA`. Default ao criar pelo front: `#18E4D3` (turquesa da marca).

## Endpoints novos

| Metodo | Path | Descricao |
|---|---|---|
| POST | `/api/financeiro/categorias` | Cria (ADMIN). |
| GET | `/api/financeiro/categorias?tipo=RECEITA` | Lista, opcionalmente filtrada (RECEITA inclui AMBOS). |
| GET | `/api/financeiro/categorias/{id}` | Detalha. |
| PUT | `/api/financeiro/categorias/{id}` | Atualiza (ADMIN). |
| DELETE | `/api/financeiro/categorias/{id}` | Soft-delete (ADMIN). |

## Como testar

1. Backend e frontend rodando.
2. Login como admin.
3. Va em **Categorias** (novo item de menu):
   - Veja as 6 categorias do seed.
   - Crie uma nova categoria "Combustivel" do tipo DESPESA com cor laranja.
4. Va em **Financeiro**:
   - Selecione tipo "Saida" no formulario de lancamento.
   - Dropdown Categoria mostra apenas DESPESA + AMBOS.
   - Selecione "Combustivel", registre R$ 200.
   - Lista exibe a pill colorida laranja com o nome da categoria.
5. Tente, via Postman, enviar `tipo=ENTRADA` com `categoriaId` de uma DESPESA → backend rejeita com 400.

## Limites conhecidos

- **Nao ha relatorios agregados por categoria** ainda — proxima evolucao do dashboard.
- **Sem subcategorias**. Todas sao flat. Em concorrentes maiores ha hierarquia (Receita > Servicos > Lavagem).
- **Sem cor por padrao** ao criar via API (so via UI). Requisicoes diretas podem deixar `cor=null` e a UI mostra cinza.

## Status da Wave 1

| Feature | Status |
|---|---|
| Multi-veiculo (Veiculo + OS + Agendamento) | ✅ entregue |
| Categorias financeiras | ✅ entregue |
| Migracao de dados antigos (Cliente.veiculo -> Veiculo) | pendente |
| Agenda visual em calendario | pendente |
| Horario de funcionamento + bloqueios | pendente |
| Fotos antes/depois na OS | pendente |
| Pagina publica de agendamento | pendente |
| WhatsApp real | pendente |
| Estoque basico | pendente |
| Funcionarios + comissao | pendente |
| Dashboard com graficos | pendente |

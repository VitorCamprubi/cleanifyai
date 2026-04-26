# Wave 1 — Feature 1b: Agendamento ganha veiculoId

> Entrega: 2026-04-25 (continuacao da feature 1)

## O que mudou

### Backend

- `domain/entity/Agendamento.java` — `@ManyToOne Veiculo veiculo` (opcional, EAGER).
- `dto/agendamento/AgendamentoRequest.java` — campo `veiculoId` opcional.
- `dto/agendamento/VeiculoResumoResponse.java` (novo) — `id`, `descricao`, `placa`.
- `dto/agendamento/AgendamentoResponse.java` — agora carrega `VeiculoResumoResponse veiculo` (nullable).
- `service/AgendamentoService.java` — recebe `VeiculoService`, valida posse (veiculo precisa ser do cliente), preenche/limpa veiculo no fluxo de criacao e atualizacao, monta `VeiculoResumoResponse` na response.
- `config/SeedDataConfig.java` — agendamentos demo agora vinculam aos veiculos demo.

### Frontend

- `core/models/agendamento.model.ts` — `AgendamentoVeiculoResumo` novo tipo, `Agendamento.veiculo` nullable, `AgendamentoPayload.veiculoId` opcional.
- `features/agendamentos/pages/agendamentos-page.component.ts` — injeta `VeiculosApiService`, novo controle `veiculoId` no form, recarrega veiculos do cliente quando o controle `clienteId` muda, limpa selecao se o veiculo deixar de pertencer ao cliente.
- `features/agendamentos/pages/agendamentos-page.component.html` — dropdown "Veiculo (opcional)" depois de Servico, exibicao na lista prioriza veiculo cadastrado e cai para o legado de cliente.

## Regras de negocio

- Veiculo so pode ser vinculado se pertencer ao cliente do agendamento.
- Trocar o cliente em edicao limpa veiculo automaticamente (frontend limpa preventivamente, backend valida e recusa se inconsistente).
- Veiculo continua opcional — agendamentos antigos sem veiculo continuam funcionando, exibem o `cliente.veiculo` legado.

## Estado da migracao

| Etapa | Status |
|---|---|
| Veiculo entity + CRUD | ✅ |
| OS aceita veiculoId | ✅ |
| Agendamento aceita veiculoId | ✅ |
| Migracao de dados (extrair Cliente.veiculo+placa para Veiculo) | pendente |
| Deprecar Cliente.veiculo / Cliente.placa | pendente |

## Como testar

1. Backend e frontend rodando.
2. Login como admin (ou empresa nova).
3. Va em **Veiculos**, cadastre 2 veiculos para um cliente.
4. Va em **Agendamentos**:
   - Selecione o cliente — dropdown "Veiculo (opcional)" carrega.
   - Selecione um veiculo, salve.
   - Lista mostra "Marca Modelo · Placa".
5. Edite o agendamento, troque o cliente — veiculo eh limpo automaticamente.
6. Tente forcar um veiculo de outro cliente via API direta (Postman) — backend rejeita com `400 Veiculo nao pertence ao cliente`.

## Status da Wave 1

| Feature | Status |
|---|---|
| Multi-veiculo (Veiculo + OS + Agendamento) | ✅ entregue |
| Migracao de dados antigos (Cliente.veiculo -> Veiculo) | pendente |
| Agenda visual em calendario | pendente |
| Horario de funcionamento + bloqueios | pendente |
| Fotos antes/depois na OS | pendente |
| Pagina publica de agendamento | pendente |
| WhatsApp real | pendente |
| Estoque basico | pendente |
| Categorias financeiras | pendente |
| Funcionarios + comissao | pendente |
| Dashboard com graficos | pendente |

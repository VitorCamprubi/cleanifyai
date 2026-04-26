# Wave 1 — Feature 1: Multi-veiculo por cliente

> Entrega: 2026-04-25

## O que mudou

### Backend

Novos arquivos:
- `domain/entity/Veiculo.java` — entidade tenanteada com `clienteId` (FK), `marca`, `modelo`, `placa` (validada formato Mercosul), `cor`, `anoModelo`, `observacoes`, `ativo`.
- `repository/VeiculoRepository.java`.
- `dto/veiculo/VeiculoRequest.java`, `VeiculoResponse.java`.
- `service/VeiculoService.java` — CRUD completo, soft-delete (`ativo`), permite transferencia entre clientes (caso real: revenda).
- `controller/VeiculoController.java` — endpoints REST.

Alterado:
- `domain/entity/OrdemServico.java` — ganhou `@ManyToOne Veiculo veiculo` (opcional).
- `dto/ordem/OrdemServicoRequest.java` — campo `veiculoId` opcional.
- `dto/ordem/OrdemServicoResponse.java` — campos `veiculoId`, `veiculoDescricao`, `veiculoPlaca` (campos legados `clientePlaca` e `clienteVeiculo` mantidos para compatibilidade).
- `service/OrdemServicoService.java` — valida que veiculo pertence ao cliente, trata criacao e atualizacao.
- `config/SecurityConfig.java` — libera `/api/veiculos/**`.
- `config/SeedDataConfig.java` — cria 2 veiculos demo (Civic prata 2021 e Compass branco 2023).

### Frontend

Novos arquivos:
- `core/models/veiculo.model.ts`.
- `core/services/veiculos-api.service.ts`.
- `features/veiculos/pages/veiculos-page.component.{ts,html,scss}` — pagina dedicada com filtro por cliente, formulario com auto-foco, lista com inativacao.

Alterado:
- `core/models/ordem.model.ts` — `OrdemServico` ganhou `veiculoId`, `veiculoDescricao`, `veiculoPlaca`.
- `features/ordens/pages/ordens-page.component.ts` — form de OS ganhou dropdown de veiculo. Carrega veiculos automaticamente ao mudar cliente. Limpa selecao se cliente trocar.
- `features/ordens/pages/ordens-page.component.html` — exibe veiculo cadastrado quando vinculado, com fallback para campos legados de cliente.
- `app.routes.ts` — nova rota `/veiculos`.
- `layout/app-shell/app-shell.component.ts` — item "Veiculos" no menu.

## Decisoes de escopo

1. **Cliente.veiculo e Cliente.placa permanecem.** Sao campos legados, ainda alimentam OS antigas. Migracao formal vira na proxima sessao (extrair para Veiculo entity, depois deprecar).
2. **Agendamento NAO foi tocado** nesta passada. Continua sem `veiculoId`. Proxima sessao.
3. **Soft-delete em Veiculo** (campo `ativo`) — historico de OS preserva o veiculo mesmo apos inativacao, porque o `@ManyToOne` carrega pela FK independente do flag.
4. **Transferencia de veiculo entre clientes permitida** — quando atualizar `clienteId`, o service aceita. Caso real: cliente vende o carro pra outro cliente da casa.
5. **Validacao de placa Mercosul** identica a do Cliente (regex `[A-Z]{3}[0-9][A-Z0-9][0-9]{2}`).

## Regras de negocio

- Veiculo so pode ser vinculado a OS se pertencer ao mesmo cliente da OS.
- Filtro de listagem por `clienteId` opcional via query param.
- Inativacao via DELETE — soft-delete pelo flag `ativo`.

## Como testar

1. Suba backend e frontend.
2. Login como admin.
3. Va em **Veiculos**:
   - Cadastre 2 veiculos para o cliente "Carlos Almeida" (alem do Civic do seed).
   - Filtre por cliente — vai ver apenas os do cliente selecionado.
4. Crie uma nova OS:
   - Selecione "Carlos Almeida" no cliente.
   - Dropdown de Veiculo aparece com a frota completa do cliente.
   - Selecione um veiculo, adicione itens, salve.
5. Lista de OS exibe agora a placa+modelo do veiculo cadastrado.
6. Edite a OS, troque o cliente — o veiculo eh limpo automaticamente porque o backend valida posse.
7. Inative um veiculo — ele some da listagem mas as OS antigas continuam exibindo a placa.

## Migracao do banco local

Nova tabela `veiculos` e nova coluna `veiculo_id` em `ordens_servico`.

`ddl-auto: update` cuida disso automaticamente. Se o banco ja existir, nada quebra: a coluna `veiculo_id` e adicionada como nullable.

Se preferir comecar limpo: `DROP DATABASE cleanifyai;` antes de subir.

## Proximos passos (proxima sessao)

1. Adicionar `veiculoId` em Agendamento (mesma logica da OS).
2. Migrar dados: para cada Cliente com `veiculo`+`placa`, criar um Veiculo equivalente; vincular OS antigas ao novo Veiculo pelo match de placa.
3. Deprecar `Cliente.veiculo` e `Cliente.placa` (remover do form de cliente, manter no banco por mais 1 release).
4. Atualizar testes integrados.

## Status da Wave 1

| Feature | Status |
|---|---|
| Multi-veiculo por cliente | ✅ entregue (com pendencia de Agendamento + migracao de dados antigos) |
| Agenda visual em calendario | pendente |
| Horario de funcionamento + bloqueios | pendente |
| Fotos antes/depois na OS | pendente |
| Pagina publica de agendamento | pendente |
| WhatsApp real | pendente |
| Estoque basico | pendente |
| Categorias financeiras | pendente |
| Funcionarios + comissao | pendente |
| Dashboard com graficos | pendente |

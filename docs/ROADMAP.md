# Roadmap CleanifyAI — do MVP atual à v1 vendável

Cada fase fecha com **código que compila, testa e roda**. Nenhuma fase deixa o sistema em estado quebrado.

## Fase 0 — Fundação (✅ entregue nesta sessão)
- [x] Auditoria do código atual (`docs/AUDITORIA.md`).
- [x] Definição da arquitetura alvo (`docs/ARQUITETURA.md`).
- [x] Roadmap (este documento).

## Fase 1 — Multi-tenant real + Empresa + Signup
**Objetivo:** parar de hardcodar `empresaId = 1L`. Permitir que múltiplas estéticas usem o sistema isoladas.

Backend:
- [ ] Entidade `Empresa` (id, nome, cnpj, telefone, email, ativa, criadaEm).
- [ ] `User` ganha `empresaId` (FK).
- [ ] `TenantContext` (ThreadLocal) populado pelo `JwtAuthenticationFilter` a partir do claim `empresaId`.
- [ ] `EntidadeTenantBase` ganha listener `@PrePersist` que injeta `empresaId` do contexto.
- [ ] Hibernate `@FilterDef("empresa_filter")` aplicado em Cliente/Servico/Agendamento.
- [ ] Aspecto/interceptor que ativa o filter por request.
- [ ] Endpoint `POST /api/auth/register-company` cria Empresa + admin + retorna JWT.
- [ ] JWT passa a incluir `empresaId`.
- [ ] Migração do seed: criar 2 empresas demo (`CleanifyAI Demo` e `Estética Exemplo`).
- [ ] Atualizar testes existentes.

Frontend:
- [ ] Tela de signup `/signup` com formulário Empresa + Admin.
- [ ] Após signup → autologin (token já vem na resposta).
- [ ] Link "Criar conta" no login.

Critério de pronto: dois usuários de empresas diferentes não enxergam dados um do outro. Teste de integração comprova.

## Fase 2 — Refatoração hexagonal dos módulos existentes
**Objetivo:** preparar o terreno antes de adicionar OS e Financeiro, evitando que esses nasçam acoplados.

- [ ] Reorganizar pacotes em `clientes/`, `servicos/`, `agendamentos/`, `auth/`, `shared/`.
- [ ] Cada módulo: `domain/`, `application/port/{in,out}/`, `infrastructure/{persistence,web}/`.
- [ ] Renomear `Cliente` (entidade JPA) → `ClienteJpaEntity`. Criar `Cliente` puro no `domain/`.
- [ ] `ClienteService` vira `CriarClienteUseCase`, `AtualizarClienteUseCase`, etc.
- [ ] Adapter JPA implementa o port `out`.
- [ ] Controllers permanecem (são adapters de entrada).
- [ ] Testes existentes continuam passando, com mínimas mudanças de import.

Critério de pronto: `mvn test` verde, nenhuma classe do `domain/` importa `org.springframework` ou `jakarta.persistence`.

## Fase 3 — Ordem de Serviço
**Objetivo:** registrar o que foi efetivamente executado, com itens, valor e status.

Domínio:
- [ ] `OrdemServico` { id, empresaId, clienteId, agendamentoId?, itens[], status, valorTotal, abertaEm, fechadaEm, observacoes }.
- [ ] `ItemOrdem` { servicoId, descricao, quantidade, valorUnitario, valorTotal }.
- [ ] `StatusOrdem`: ABERTA → EM_EXECUCAO → CONCLUIDA → ENTREGUE / CANCELADA.
- [ ] Regra: criar OS direto OU a partir de um Agendamento (importa cliente + serviço).

Backend:
- [ ] Tabelas `ordens_servico` e `itens_ordem` via Flyway.
- [ ] Use cases: `AbrirOrdem`, `AdicionarItem`, `IniciarExecucao`, `Concluir`, `Entregar`, `Cancelar`, `ListarOrdens`, `BuscarOrdem`.
- [ ] `POST/PUT/PATCH/GET /api/ordens` + `/api/ordens/{id}/itens`.
- [ ] Testes de transição de status (espelha o que existe em Agendamento).

Frontend:
- [ ] Lista de OS com filtros por status e período.
- [ ] Tela de detalhe da OS (itens, ações de status, observações).
- [ ] Botão "Abrir OS" no agendamento concluído.

Critério de pronto: usuário consegue agendar → executar → fechar OS sem tocar no banco.

## Fase 4 — Financeiro básico
**Objetivo:** estética sabe quanto recebeu, em que forma, quando.

Domínio:
- [ ] `Lancamento` { id, empresaId, tipo (ENTRADA|SAIDA), valor, formaPagamento, dataLancamento, descricao, ordemId? }.
- [ ] `FormaPagamento`: DINHEIRO, PIX, DEBITO, CREDITO, BOLETO, OUTROS.
- [ ] Regra: ao concluir uma OS pode gerar `Lancamento` automaticamente (1 entrada com forma escolhida).
- [ ] Fechamento: agregação por dia/mês com totais por forma.

Backend:
- [ ] Tabela `lancamentos` via Flyway.
- [ ] Use cases: `RegistrarLancamento`, `EstornarLancamento`, `ListarLancamentos`, `ResumoDiario`, `ResumoMensal`.
- [ ] `POST/GET/DELETE /api/financeiro/lancamentos`, `GET /api/financeiro/resumo`.

Frontend:
- [ ] Tela "Caixa do dia" com lançamentos do dia + totais por forma.
- [ ] Tela mensal com gráfico simples (recharts no front Angular ou apex/chart.js).
- [ ] Botão "Registrar pagamento" no detalhe da OS gera lançamento.

Critério de pronto: ao fechar uma OS de R$ 90 com forma PIX, o caixa do dia mostra +R$ 90 PIX.

## Fase 5 — Hardening (pré-venda)
- [ ] Paginação em todos `GET` de listagem.
- [ ] Auditoria (`createdAt`, `updatedAt`, `createdBy`).
- [ ] Soft-delete em Cliente.
- [ ] Flyway substituindo `ddl-auto: update`.
- [ ] Refresh token + tela de "sessão expirada".
- [ ] Logger estruturado (JSON) + correlation id.
- [ ] Sanity check de segurança: rate limit no login, BCrypt cost calibrado.
- [ ] README com instruções de produção (variáveis, docker-compose).

## Fase 6 — Diferenciação (pós-venda inicial)
- [ ] Adapter real de WhatsApp (provider à escolha — Z-API, Twilio, etc).
- [ ] Lembrete automático D-1 e confirmação no dia.
- [ ] Adapter de IA: resumo do atendimento, sugestão de upsell baseada em histórico.
- [ ] Página pública de agendamento (cliente final marca sozinho).
- [ ] Multiusuário por empresa (Admin convida atendentes).

## Estimativa por fase

| Fase | Esforço (eng. dedicado) | Bloqueia? |
|---|---|---|
| 1 | 5–7 dias | Tudo |
| 2 | 4–6 dias | OS, Financeiro |
| 3 | 5–8 dias | Financeiro completo |
| 4 | 4–6 dias | — |
| 5 | 3–5 dias | Vender |
| 6 | 6–10 dias | — |

**Total para v1.0 vendável (Fases 1–5): ~21–32 dias úteis.**

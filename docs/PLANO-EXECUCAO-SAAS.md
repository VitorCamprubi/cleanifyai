# Plano de execucao SaaS CleanifyAI

> Objetivo: evoluir o CleanifyAI de MVP operacional para SaaS vendavel e depois competitivo contra GarageHero, Carmob, Jump Car, Forbion e similares.

## Estado atual

Base existente:
- Auth JWT, signup de empresa e multi-tenant por `empresa_id`.
- Clientes, veiculos, servicos, agendamentos, ordens de servico, financeiro e categorias financeiras.
- Dashboard operacional e financeiro basico.
- Testes backend de integracao e testes frontend headless.

Primeira onda iniciada:
- [x] Suite completa verde (`mvnw clean test`, `npm run build`, `npm run test:ci`).
- [x] Flyway adicionado para versionar schema.
- [x] `ddl-auto` saiu de `update` para `validate` por padrao.
- [x] Segredos de producao removidos do fallback versionado; profile `dev` mantem senha local para rodar no ambiente atual.
- [x] Nome da empresa vindo da sessao, sem hardcode no shell.
- [x] Financeiro e categorias expostos no menu desktop.
- [x] Audit log persistido para escritas HTTP.
- [x] Profiles `dev`/`prod` separados.
- [x] Docker Compose com MySQL, API e Web.
- [x] Testes explicitos de isolamento cross-tenant por modulo.
- [x] Health/readiness com Actuator.
- [x] Rate limit no login por email + IP.
- [x] Refresh token opaco com hash em banco, rotacao e logout.

## Onda 1 - Fundacao SaaS segura

- [x] Corrigir testes quebrados e deixar pipeline local confiavel.
- [x] Adicionar migration inicial do schema atual.
- [x] Criar migration para auditoria de acoes (`audit_logs`).
- [x] Separar configuracao local/dev/prod com profiles.
- [x] Docker Compose para API, Web e MySQL.
- [x] Health checks reais (`/actuator/health`) e readiness.
- [x] Rate limit no login.
- [x] Refresh token e UX de sessao expirada.
- [ ] Paginacao padrao em listagens.
- [x] Testes explicitos de isolamento cross-tenant para todos os modulos atuais.

## Fatia tecnica concluida - audit/profiles/docker/isolation

- `audit_logs` entrou via Flyway incremental (`V2__audit_logs.sql`) e recebe metadados de `POST`, `PUT`, `PATCH` e `DELETE` em `/api/**`.
- O filtro de auditoria roda depois da autenticacao JWT, reaproveita `TenantContext` e nao persiste payload de requisicao.
- `dev` tem defaults locais e seed opcional; `prod` exige datasource, CORS e segredo JWT por variavel de ambiente.
- `docker-compose.yml` sobe MySQL 8.4, API Spring Boot e Web Angular servida por Nginx.
- `TenantIsolationIntegrationTest` cobre leituras e escritas sensiveis em clientes, veiculos, servicos, agendamentos, ordens e financeiro.

## Fatia tecnica concluida - actuator/rate-limit/refresh-token

- Actuator exposto apenas para `health` e `info`, com `/actuator/health/liveness` e `/actuator/health/readiness` liberados publicamente.
- Dockerfile da API e Compose usam readiness como healthcheck da API antes de subir a Web.
- Login tem rate limit em memoria por email + IP, configuravel por `APP_LOGIN_RATE_LIMIT_MAX_ATTEMPTS` e `APP_LOGIN_RATE_LIMIT_WINDOW_SECONDS`.
- Login e signup retornam refresh token opaco; o banco guarda apenas `SHA-256` em `refresh_tokens` (`V3__refresh_tokens.sql`).
- `POST /api/auth/refresh` rotaciona o refresh token e revoga o anterior; `POST /api/auth/logout` revoga a sessao atual.
- Frontend guarda a sessao completa, tenta refresh uma vez ao receber `401`, repete a requisicao original com novo access token e so redireciona para login se o refresh falhar.

## Onda 2 - Operacao vendavel

- [ ] Agenda visual diaria/semanal.
- [ ] Horario de funcionamento e bloqueios.
- [ ] Reagendamento e controle de conflito mais completo.
- [ ] Checklist configuravel por tipo de servico.
- [ ] Fotos antes/depois na OS com storage isolado por tenant.
- [ ] Assinatura digital na entrega.
- [ ] Orcamentos com PDF/link e conversao em OS.
- [ ] Historico consolidado por cliente, placa e veiculo.

## Onda 3 - Aquisicao e automacao

- [ ] Pagina publica de agendamento por slug da empresa.
- [ ] QR Code/link publico da loja.
- [ ] Confirmacao e lembretes via WhatsApp D-1/D-0.
- [ ] Aviso de inicio/fim de servico.
- [ ] Aprovacao de orcamento pelo cliente.
- [ ] Link de pagamento PIX/cartao.
- [ ] Pos-venda automatico por validade do servico.

## Onda 4 - Gestao avancada

- [ ] Estoque de produtos.
- [ ] Consumo de produto por OS.
- [ ] Alertas de estoque minimo.
- [ ] Funcionarios/tecnicos.
- [ ] Comissoes por servico, funcionario e periodo.
- [ ] Contas a pagar e receber.
- [ ] Fechamento de caixa com trilha de auditoria.
- [ ] Exportacao PDF/Excel.
- [ ] Importacao CSV de clientes/veiculos.

## Onda 5 - Diferenciacao

- [ ] IA no WhatsApp para triagem e agendamento assistido.
- [ ] Resumo automatico do atendimento.
- [ ] Sugestao de upsell baseada no historico.
- [ ] Previsao de retorno por servico/cliente.
- [ ] Analise de fotos antes/depois.
- [ ] Multi-unidade.
- [ ] NFS-e/NF-e via provedor fiscal.
- [ ] Painel admin SaaS: planos, trial, cobranca, inadimplencia e suporte.

## Regras tecnicas permanentes

- Tenant sempre vem do JWT/sessao, nunca do front.
- Todo dado de negocio tenanteado precisa ter `empresa_id` e teste de isolamento.
- Arquivos devem ser armazenados em caminho/bucket com prefixo por tenant.
- Toda acao sensivel deve registrar auditoria.
- Migrations sao obrigatorias para alteracao de schema.
- Toda feature nova deve incluir API, UI, teste relevante e atualizacao de docs.

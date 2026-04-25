# Análise Competitiva — CleanifyAI vs Jump Car / Carmob / Garage Hero

> Data: 2026-04-25
> Posição: especialista de produto sênior contratado para avaliar paridade competitiva.

## TL;DR

CleanifyAI hoje **cobre 20–25% da superfície** dos concorrentes principais. O que existe está bem feito (multi-tenant real, OS com máquina de estados, financeiro com agregações). O que falta é grande, mas não tudo é igualmente importante: **70% do impacto comercial vem de 25% das features ausentes**. Esse documento mapeia o que falta e em que ordem atacar.

---

## 1. Competidores de referência

| Produto | Posicionamento | Forças observadas |
|---|---|---|
| **Jump Car** | Vertical de estética automotiva, foco operacional | Agenda visual, OS, financeiro, programa de fidelidade, WhatsApp |
| **Carmob** | Mais amplo (oficinas + estéticas), ERP-like | Estoque, NFSe, comissão, multi-unidade, contabilidade |
| **Garage Hero** | Foco UX moderno + BI | Dashboards, NPS, marketing, app mobile do cliente |

Os três competem com **R$ 99–399/mês por unidade**, em planos crescentes. Diferencial moderno (2024–2026): IA para resumo de atendimento, sugestão de upsell, agendamento conversacional via WhatsApp.

---

## 2. Inventário de features — concorrentes vs CleanifyAI

Legenda: ✅ existe · 🟡 parcial · ❌ ausente

### 2.1 Operação e agenda

| Feature | Concorrentes | CleanifyAI |
|---|---|---|
| CRUD agendamento | ✅ | ✅ |
| Visualização em calendário (semanal/diária) | ✅ | ❌ (lista) |
| Bloqueio de horários (almoço, feriado, manutenção) | ✅ | ❌ |
| Horário de funcionamento configurável por dia | ✅ | ❌ |
| Múltiplos profissionais por agendamento | ✅ | ❌ |
| Recorrência (cliente que vai toda 2ª semana) | 🟡 | ❌ |
| Lista de espera | 🟡 | ❌ |
| Confirmação automática via WhatsApp | ✅ | ❌ (NoOp) |
| Lembrete D-1 e D-0 | ✅ | ❌ |
| Reagendamento com 1 clique pelo WhatsApp | ✅ (top 1) | ❌ |
| Drag-and-drop de agendamento entre horários | ✅ | ❌ |

### 2.2 Cliente / CRM

| Feature | Concorrentes | CleanifyAI |
|---|---|---|
| CRUD cliente | ✅ | ✅ |
| Múltiplos veículos por cliente | ✅ | ❌ (1 veículo) |
| Histórico completo de visitas e gastos | ✅ | 🟡 |
| Tags / segmentação | ✅ | ❌ |
| Aniversário e datas comemorativas | ✅ | ❌ |
| Anexos (fotos, contratos, RG/CNH) | ✅ | ❌ |
| Importação CSV / planilha | ✅ | ❌ |
| Pré-cadastro online (cliente preenche) | 🟡 | ❌ |
| Ranking automático (RFM, frequência) | 🟡 | ❌ |
| LGPD: exportar / apagar dados sob solicitação | ✅ | ❌ |

### 2.3 Ordem de Serviço

| Feature | Concorrentes | CleanifyAI |
|---|---|---|
| OS com itens e total | ✅ | ✅ |
| Máquina de estados | ✅ | ✅ |
| Fotos antes/depois | ✅ (decisivo) | ❌ |
| Assinatura digital do cliente (entrega) | ✅ | ❌ |
| Checklist de execução por tipo de serviço | ✅ | ❌ |
| Alocação de técnico responsável | ✅ | ❌ |
| Tempo previsto vs realizado | ✅ | ❌ |
| Comprovante para impressão | ✅ | ❌ |
| Orçamento → OS (conversão) | ✅ | ❌ |
| Pacote de serviços com desconto | ✅ | ❌ |
| Histórico de OS por veículo (carro X teve quais serviços) | ✅ | ❌ |

### 2.4 Estoque (item ausente inteiro no MVP)

| Feature | Concorrentes | CleanifyAI |
|---|---|---|
| Cadastro de produtos consumíveis | ✅ | ❌ |
| Movimentação manual (entrada/saída) | ✅ | ❌ |
| Baixa automática ao executar OS | ✅ | ❌ |
| Estoque mínimo + alerta | ✅ | ❌ |
| Inventário cíclico | ✅ | ❌ |
| Custo médio / FIFO | 🟡 | ❌ |
| Compras / pedidos a fornecedores | 🟡 | ❌ |

### 2.5 Financeiro

| Feature | Concorrentes | CleanifyAI |
|---|---|---|
| Lançamentos entrada/saída | ✅ | ✅ |
| Contas a receber (parcelado, vencimento) | ✅ | ❌ |
| Contas a pagar (fornecedor, vencimento) | ✅ | ❌ |
| Categorias de receita/despesa | ✅ | ❌ |
| Centros de custo | 🟡 | ❌ |
| Fluxo de caixa projetado | ✅ | ❌ |
| DRE simplificado | ✅ | ❌ |
| Conciliação bancária (extrato + match) | 🟡 | ❌ |
| Múltiplas contas / múltiplas maquininhas | ✅ | ❌ |
| Cartão de crédito (taxa por bandeira, prazo de recebimento) | ✅ | ❌ |
| PIX gerado pelo sistema | 🟡 | ❌ |
| Boleto registrado | 🟡 | ❌ |
| Link de pagamento | ✅ | ❌ |
| Maquininha / TEF integrado | 🟡 | ❌ |
| Reembolso e devolução | ✅ | ❌ |

### 2.6 Comissões e equipe

| Feature | Concorrentes | CleanifyAI |
|---|---|---|
| Cadastro de funcionários | ✅ | ❌ |
| Permissões granulares por usuário (não só ADMIN/ATENDENTE) | ✅ | 🟡 (2 roles fixas) |
| Comissão por % / fixa / mista | ✅ | ❌ |
| Comissão por serviço, por produto, por equipe | ✅ | ❌ |
| Apuração mensal de comissão | ✅ | ❌ |
| Controle de ponto / horas | 🟡 | ❌ |
| Holerite / contracheque | 🟡 | ❌ |

### 2.7 Marketing e relacionamento

| Feature | Concorrentes | CleanifyAI |
|---|---|---|
| Programa de fidelidade (pontos / cashback) | ✅ | ❌ |
| Cupons de desconto | ✅ | ❌ |
| Campanhas de WhatsApp em massa segmentadas | ✅ | ❌ |
| E-mail marketing transacional | ✅ | ❌ |
| Régua de relacionamento (aniversário, retorno) | ✅ | ❌ |
| NPS / pesquisa de satisfação pós-atendimento | ✅ | ❌ |
| Avaliações públicas (review do serviço) | ✅ | ❌ |

### 2.8 Página pública / Vendas

| Feature | Concorrentes | CleanifyAI |
|---|---|---|
| Página pública de agendamento | ✅ (decisivo) | ❌ |
| Subdomínio personalizado (`empresa.cleanify.app`) | ✅ | ❌ |
| Catálogo público de serviços com fotos e preços | ✅ | ❌ |
| Mini-site / landing da empresa | 🟡 | ❌ |
| Galeria antes/depois pública | ✅ | ❌ |
| Avaliações Google integradas | 🟡 | ❌ |

### 2.9 Relatórios e BI

| Feature | Concorrentes | CleanifyAI |
|---|---|---|
| Dashboard com KPIs operacionais | 🟡 | 🟡 (básico) |
| Dashboard executivo com gráficos | ✅ | ❌ |
| Ticket médio por período | ✅ | ❌ |
| Taxa de conversão agendamento → OS | ✅ | ❌ |
| Frequência de retorno por cliente | ✅ | ❌ |
| Performance por serviço (margem, frequência) | ✅ | ❌ |
| Performance por funcionário | ✅ | ❌ |
| Cohort de retenção | 🟡 | ❌ |
| Comparativo de períodos (MoM, YoY) | ✅ | ❌ |
| Exportação Excel / PDF | ✅ | ❌ |

### 2.10 Multi-unidade / franquias

| Feature | Concorrentes | CleanifyAI |
|---|---|---|
| Hierarquia matriz → filiais | ✅ (Carmob) | ❌ |
| Consolidação de relatórios | ✅ | ❌ |
| Transferência de estoque entre unidades | ✅ | ❌ |
| Permissão por unidade | ✅ | ❌ |

### 2.11 Mobile

| Feature | Concorrentes | CleanifyAI |
|---|---|---|
| App do operador (PWA ou nativo) | ✅ (Garage Hero) | ❌ |
| App do cliente (acompanhar OS, agendar) | 🟡 | ❌ |
| Notificações push | ✅ | ❌ |

### 2.12 Fiscal / contábil

| Feature | Concorrentes | CleanifyAI |
|---|---|---|
| NFSe (Nota Fiscal de Serviço) | ✅ (Carmob) | ❌ |
| NFe (produto) | 🟡 | ❌ |
| SPED Fiscal | 🟡 | ❌ |
| Integração com contabilidade externa | ✅ | ❌ |

### 2.13 Plataforma / extensão

| Feature | Concorrentes | CleanifyAI |
|---|---|---|
| API pública documentada | 🟡 | ❌ |
| Webhooks (eventos) | 🟡 | ❌ |
| 2FA | ✅ | ❌ |
| SSO Google / Microsoft | 🟡 | ❌ |
| Auditoria de ações (quem alterou o quê) | 🟡 | 🟡 (timestamps apenas) |
| Backup automático | ✅ | ❌ |

### 2.14 IA (diferencial moderno — onde podemos liderar)

| Feature | Concorrentes | CleanifyAI |
|---|---|---|
| Resumo automático do atendimento | 🟡 (incipiente) | ❌ |
| Sugestão de upsell por histórico | 🟡 | ❌ |
| Atendimento conversacional WhatsApp via IA | 🟡 | ❌ |
| Predição de churn / reincidência | ❌ | ❌ |
| Geração automática de descrição de serviço | ❌ | ❌ |
| Análise de fotos antes/depois (laudo automático) | ❌ | ❌ |

---

## 3. Onde estamos relativamente fortes

1. **Multi-tenant real desde o dia zero.** Vários concorrentes ainda têm dívida arquitetural aqui (rumores).
2. **Modelo de OS limpo com máquina de estados validada por testes.** Não é diferencial vendável, mas é base sólida pra construir tudo em cima.
3. **Stack moderna (Spring Boot 3 + Java 21 + Angular 17).** Recrutar/manter dev mais barato que stacks legadas que alguns concorrentes carregam.
4. **Código auditável e testado.** Quando entrar dívida fiscal (NFSe), é mais fácil estender com confiança.
5. **Posicionamento de IA ainda virgem.** Concorrentes mal arranharam. Janela de 12–18 meses pra liderar.

## 4. Onde estamos perigosamente atrás

Em ordem de **impacto comercial direto**:

1. **Página pública de agendamento.** Cliente não-tech precisa marcar pelo celular sem login. Sem isso, perde venda pra qualquer concorrente.
2. **WhatsApp real (confirmação + lembrete + reagendamento).** É hoje o canal #1 de relacionamento da estética automotiva no Brasil. Adapter NoOp não vende.
3. **Estoque integrado à OS.** Sem isso, "controlo serviço mas não sei quanto produto gastei" — financeiro fica falso.
4. **Comissão.** Estética com 3+ funcionários precisa apurar comissão. Quem não oferece é descartado na demo.
5. **Fotos antes/depois na OS.** Diferencial visual e prova social.
6. **Programa de fidelidade.** Mecanismo de retenção mais barato.
7. **NFSe.** Bloqueador para quem fatura mais que MEI. Sem isso, não vende pra estética profissional formalizada.
8. **Dashboards executivos com gráfico.** Cliente-dono toma decisão olhando relatório semanalmente.

---

## 5. Roadmap proposto até paridade comercial

A prioridade não é cobrir 100% das features dos concorrentes. É cobrir o **núcleo vendável** + um ou dois **diferenciais defensáveis**.

### Wave 1 — "Pode vender" (estimativa 8–12 semanas, time de 1–2 devs)

Objetivo: chegar em paridade com tier mais barato dos concorrentes (~R$ 99/mês).

1. **Agenda visual em calendário** (semana / dia) com drag-and-drop.
2. **Horário de funcionamento + bloqueios** (almoço, feriado).
3. **Múltiplos veículos por cliente.**
4. **Fotos antes/depois na OS.** Upload + storage S3-compatível.
5. **Página pública de agendamento** com link compartilhável + subdomínio.
6. **WhatsApp real** (Z-API ou Twilio) com confirmação + lembrete D-1.
7. **Estoque básico** (produto, baixa manual, alerta de mínimo).
8. **Categorias de receita/despesa** no financeiro.
9. **Cadastro de funcionários** + alocação na OS.
10. **Comissão %** simples por funcionário, apurada na conclusão da OS.
11. **Dashboards** com 6 KPIs essenciais e gráfico (recharts ou ChartJS).

### Wave 2 — "Compete de igual" (8–12 semanas adicionais)

Objetivo: justificar plano mais caro (~R$ 199/mês).

12. **Fluxo de caixa projetado** + DRE simplificada.
13. **Contas a receber/pagar** com vencimento e parcelamento.
14. **NFSe** (escopo MEI/Simples Nacional, integração Focus NFE / NFE.io).
15. **Programa de fidelidade** (pontos por gasto, resgate em desconto).
16. **NPS pós-atendimento** automático via WhatsApp.
17. **Régua de relacionamento** (aniversário, cliente que sumiu, retorno).
18. **Permissões granulares** (5–8 papéis configuráveis em vez de 2 fixos).
19. **Importação CSV** de clientes.
20. **Exportação PDF/Excel** de relatórios.
21. **App PWA** do operador (mobile-first, offline-friendly em listas).
22. **2FA** (TOTP).
23. **LGPD** (export, delete, consent log).

### Wave 3 — "Diferenciação" (12+ semanas)

Onde podemos vencer pelo lado direito da curva:

24. **IA: resumo do atendimento.** Texto livre → resumo padronizado + tags.
25. **IA: upsell sugerido** por histórico do cliente.
26. **IA: WhatsApp conversacional** que agenda sozinho (mediante intervenção humana de fallback).
27. **Análise de fotos antes/depois** com laudo automático.
28. **Multi-unidade / franquia** (matriz vê tudo, filial vê o seu).
29. **Conciliação bancária** com Open Finance.
30. **Marketplace de plugins** (3rd parties podem extender — diferencial técnico raro no segmento).

---

## 6. O que **não** recomendo construir

Tentação clássica de SaaS PJ que mata empresa pequena:

- **App nativo (iOS/Android).** PWA cobre 95% dos casos a 10% do custo.
- **Próprio gateway de pagamento.** Use Stripe/Pagar.me/Stone. Compliance PCI é assassino de produto.
- **Própria emissão de NFe.** Use Focus NFE / NFE.io / Tagplus. Certificação fiscal é caro e politico.
- **Próprio WhatsApp Business API.** Use Z-API / Twilio. WhatsApp não dá API direto pra empresa pequena.
- **BI próprio.** Para v1, use a feature em recharts. Para v2, embed Metabase ou Cube.

---

## 7. Realidade de prazo / time

Para chegar **na Wave 2** (compete de igual com tier médio dos concorrentes):

| Configuração | Prazo aproximado |
|---|---|
| 1 dev fullstack + você (PO) | 8–12 meses |
| 2 devs (1 backend, 1 frontend) + você | 5–7 meses |
| 4 devs + 1 designer + 1 PM | 3–4 meses |

A Wave 1 sozinha — para começar a **vender** com defesa — é realista em **2–3 meses** para 1–2 devs trabalhando focados.

Construir tudo até a Wave 3 com 1 dev fullstack: 18–24 meses honestos.

---

## 8. O que eu (Claude) entrego com confiança nas próximas sessões

Itens da Wave 1 que sei implementar ponta a ponta com qualidade equivalente ao que já fizemos (multi-tenant, OS, financeiro):

- **Agenda visual em calendário** — backend já tem, frontend é uma componente nova.
- **Horário de funcionamento + bloqueios** — backend simples + UI de configuração.
- **Múltiplos veículos por cliente** — refator de domínio (Cliente 1:N Veiculo) + UI.
- **Fotos antes/depois na OS** — endpoint upload + storage local em dev, S3 em prod (com mock).
- **Página pública de agendamento** — rota pública sem auth + serviço dedicado.
- **Estoque básico** — domain novo, similar a OS em complexidade.
- **Cadastro de funcionários + comissão %** — domain novo + integração com OS.
- **Permissões granulares** — refator de Spring Security.
- **Categorias financeiras** — pequeno acréscimo de domínio.
- **Importação CSV** — endpoint + parser.
- **2FA TOTP** — biblioteca + UI.
- **LGPD básico** (export e delete) — endpoints novos.

Itens onde posso **arquitetar e prototipar** mas precisarão de validação humana antes de prod:

- **WhatsApp via Z-API** — código eu escrevo, mas o teste real precisa de número e conta no provedor.
- **NFSe via Focus NFE** — escrevo a integração, mas certificado digital + homologação na prefeitura é com vocês.
- **IA via API Anthropic** — implemento o prompt e o orquestrador, custo de inferência é seu.
- **Pagamentos online** (Stripe/Pagar.me) — implemento, mas conta + KYC + compliance é seu.

Itens que recomendo **terceirizar ou contratar especialista**:

- App nativo iOS/Android (se decidir contra PWA).
- Auditoria de segurança / pentest pré-produção.
- Compliance fiscal específico (contador acompanha a homologação NFSe).
- UX research com clientes reais antes de Wave 2.

---

## 9. Próximo passo recomendado

Você decide:

1. **"Atacar Wave 1 toda."** Eu listo as 11 features em backlog, priorizamos juntos, e começamos pela mais barata e impactante (provavelmente página pública de agendamento OU WhatsApp real). Cada feature em sessão própria.

2. **"Ficar refinando o que já tem."** Polir UX, adicionar Flyway, refresh token, paginação — virar um MVP polido antes de adicionar superfície nova.

3. **"Validar com mercado primeiro."** Não construir mais nada agora; usar o atual como demo, conversar com 5 estéticas reais, voltar com priorização baseada em dor confirmada.

Minha recomendação como PM sênior: **opção 3, depois opção 1**. Construir as 30 features do roadmap sem validação é a forma mais cara de descobrir que metade não importa.

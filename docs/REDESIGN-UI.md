# Redesign UI — light/clean SaaS

> Entrega: 2026-04-26

## Por que mudou

O design anterior era "premium dashboard 2021": dark + gradientes + glow + raios grandes. Datado e cansativo. Concorrentes diretos (Jump Car, Carmob, Garage Hero) usam light themes — expectativa do mercado.

## Direção adotada

- **Light theme**: fundo cinza muito claro (`#f7f8fa`), superfícies brancas.
- **Tipografia**: Inter (450 a 700) substituindo Manrope+Sora.
- **Bordas sutis**: `#e5e7eb` em todo lugar; sem glow, sem gradientes ornamentais.
- **Raios menores**: 6/8/12px (eram 16-25px).
- **Sombras leves**: apenas para elevação, não decoração.
- **Paleta restrita**: azul `#2563eb` como único accent; sucesso/erro/aviso só em tags semânticas.

## Tokens centrais (`styles.scss`)

```css
--color-bg: #f7f8fa;             /* fundo da página */
--color-surface: #ffffff;        /* cards, sidebar */
--color-surface-muted: #f3f4f6;  /* table header, empty state */
--color-border: #e5e7eb;
--color-text: #111827;
--color-text-muted: #6b7280;
--color-accent: #2563eb;
--color-accent-soft: #eff6ff;    /* item de menu ativo, badges */
--radius-md: 6px;                /* botões, inputs */
--radius-lg: 8px;                /* cards */
```

## Shell

- Sidebar 240px fixa, branca com borda à direita.
- Logo + nav minimal (sem captions inflados).
- Footer com status da API, avatar do usuário e botão de logout.
- Topbar 60px fixa, mostra o título da página atual derivado da rota.
- Menu hamburger no mobile (<900px) com overlay.

## Componentes globais reescritos

- `.card` / `.panel`: surface branca, borda leve, padding consistente.
- `.button`: 38px altura, primary azul sólido, ghost branco, danger vermelho outlined.
- `.button--small`: 30px para ações em listas.
- `input`/`select`/`textarea`: 38px altura, borda leve, ring azul no foco.
- `.data-table`: header cinza claro, hover sutil.
- `.status-pill`: variantes semânticas por nome do status (`--aberta`, `--em_execucao`, `--concluida`, etc.).
- `.filtro-chip`: pill clicável com estado ativo azul.
- `.alert`: tons sutis com borda colorida.

## Páginas atualizadas

Todas as páginas com SCSS próprio foram reescritas para consumir tokens em vez de cores literais escuras:
- `auth/_auth-shared.scss` (login + signup)
- `dashboard`, `clientes`, `servicos`, `agendamentos`
- `ordens` (cards de OS, item-row, status pills via global)
- `financeiro` (cards de resumo, lançamentos, formas de pagamento)
- `financeiro/categorias`
- `veiculos`
- `core/components/toast-container`

`index.html` ganhou `meta theme-color="#ffffff"`.

## Testes manuais

1. `npm start` na pasta `cleanifyai-web/`.
2. Login → confirma novo visual da auth.
3. Dashboard → cards brancos com números limpos.
4. Cada item do menu → confirma topbar atualiza título e descrição da rota.
5. Form de qualquer página → verifica focus ring azul.
6. Status pills (OS, Agendamento) → cores semânticas corretas.
7. Mobile (<900px) → hamburger funciona, sidebar abre/fecha com overlay.

## Limites e próximos passos visuais

- **Sem ícones nos itens de menu** — para v2 vale adicionar (lucide-icons via SVG inline).
- **Sem dark mode** — adiamos. Tokens já facilitam adicionar via `[data-theme="dark"]` quando precisar.
- **Tabelas sem paginação UI** — depende de paginação backend (Fase 5 do roadmap geral).
- **Sem skeleton loaders** — atualmente usa "Carregando..." simples. Trocar quando ficar perceptível.
- **Empty states sem ilustração** — basta mensagem por enquanto.

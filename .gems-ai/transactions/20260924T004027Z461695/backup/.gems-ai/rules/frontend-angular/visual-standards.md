---
stack: frontend-angular
module: gems-sdk
severity: mandatory
see-also: [gems-components.md, feedback-alerts.md]
origem: local
precedencia: local-vence
versao: 2.2.0
---

# Padrões Visuais — Angular / gems-sdk

> **TL;DR:**
> - `provideGemsTheme()` em `app.config.ts` — obrigatório, sem exceção.
> - **NUNCA** hex cravado num uso. Sempre `var(--gems-primary-500)`, `var(--gems-bg-subtle)`, etc.;
>   cor sem equivalente na SDK vira **token local `--meduc-*`** declarado uma vez em `styles.css` (§2.1).
> - Confira a tabela da §2 antes de usar uma variável: `--gems-gray-*`, `--gems-bg-page` e
>   `--gems-text-primary` **não existem** na SDK 2.0.0 — ela não tem escala de cinza.
> - Botões: `btn-save` / `btn-cancel` / `btn-novo` / `btn-back` / `btn-danger` — classes globais da SDK.
> - Ícones: **Font Awesome 6 Solid** (`fa-solid fa-{nome}`) — nunca `fa-regular`, `fa-light` ou Material Icons.
> - Layout: `list-container` / `form-container` + `animate__animated animate__fadeIn`.
> - Skeleton: `@if (isLoading()) { fa-circle-notch fa-spin }` — **nunca** spinner de terceiros.

## 1. provideGemsTheme (Obrigatório)

Configure uma única vez em `app.config.ts`:

```typescript
// app.config.ts
import { provideGemsTheme } from '@gabriel-mdias/angular-gems-sdk';

export const appConfig: ApplicationConfig = {
  providers: [
    // ...
    provideGemsTheme({
      primary:    '#1e3a5f',
      secondary:  '#3b82f6',
      tertiary:   '#6366f1',
      background: '#f8fafc'
    })
  ]
};
```

- Sobrescreve as variáveis CSS `--gems-*` globalmente com as cores do projeto.
- **Proibido** sobrescrever via `:root { --gems-*: #... }` nos estilos de componentes.

---

## 2. Variáveis CSS — Como Usar

Todo **uso** de cor é por variável: da SDK quando ela tem a cor com o mesmo papel, e **token local
do projeto** quando não tem. **Nunca** hex cravado num uso.

```css
/* CORRETO — variável da SDK */
color:            var(--gems-primary-500);
background-color: var(--gems-secondary-100);
border-color:     var(--gems-border-color);
color:            var(--gems-text-muted);
background:       var(--gems-bg-subtle);

/* CORRETO — token local do projeto, declarado UMA vez em `:root` de `styles.css` (§2.1) */
color:            var(--meduc-text-strong);
border-color:     var(--meduc-border);

/* ERRADO — hex cravado num uso */
color:            #1e293b;
background-color: #3b82f6;

/* ERRADO — variáveis que NÃO EXISTEM na SDK 2.0.0.
   Não falham: resolvem para vazio e o navegador ignora a declaração, então o
   elemento simplesmente herda a cor errada, sem nenhum aviso no console. */
color:            var(--gems-text-primary);
background:       var(--gems-bg-page);
border-color:     var(--gems-gray-200);
```

**Paleta completa disponível — SDK 2.0.0** (conferida em `gems-palette.util.ts` e
`gems-design-tokens.css`; **não há escala de cinza**):

| Grupo | Variável | Valor | Uso típico |
| :--- | :--- | :--- | :--- |
| Sete paletas | `--gems-{cor}` para `primary`, `secondary`, `tertiary`, `danger`, `success`, `warning`, `info` | o hex exato de `provideGemsTheme()` | a cor de marca |
| Escala | `--gems-{cor}-{50,100,200,300,400,500,600,700,800,900}` | **derivada em HSL** (hue e saturação da cor, lightness ancorada no 500) — nunca um hex de marca fora do 500 | headers, fundos suaves, bordas coloridas |
| Estados | `--gems-{cor}-hover` (lightness +8), `-active` (−5), `-disabled` (saturação −20, lightness +20), `-focus-ring` (alpha 0.3) | derivados | botões e campos |
| Contraste | `--gems-{cor}-contrast` | `#111827` se a cor é clara (lightness > 55), senão `#ffffff` | **texto sobre a cor** — é o único token para "branco sobre botão" |
| Fundo | `--gems-bg` (o hex de `background`), `--gems-bg-subtle` (L 97), `--gems-bg-muted` (L 93), `--gems-bg-emphasis` (L 88), `--gems-bg-inverse` (L 10) | derivados do `background` | página, card, divisória |
| Texto | `--gems-text-main` (`#1e293b`), `--gems-text-muted` (`#64748b`) | fixos em fundo claro (`#f1f5f9`/`#94a3b8` em fundo escuro) | hierarquia textual |
| Borda | `--gems-border-color` (`rgba(0,0,0,.08)`), `--gems-border-color-emphasis` (`rgba(0,0,0,.15)`) | fixos | bordas e divisórias neutras |

> Versões anteriores deste arquivo documentavam `--gems-gray-*`, `--gems-bg-page`,
> `--gems-bg-surface`, `--gems-bg-elevated`, `--gems-text-primary` e `--gems-text-secondary`, que
> **nunca existiram** — nem na 1.1.0 nem na 2.0.0. Só há dois níveis de texto e cinco de fundo.

### 2.1 Token local de projeto

A SDK não tem cinza, e um projeto tem cinzas: texto forte, borda, superfície, placeholder. Cor que
**não tem token da SDK com a mesma cor e o mesmo papel** vira **token local**:

- declarado **uma única vez**, em `:root` de `frontend/src/styles.css` — nunca em CSS de componente;
- com o **prefixo do projeto** (`--meduc-*`), nunca `--gems-*` (que é reservado à SDK e proibido de
  sobrescrever fora de `provideGemsTheme()`, §1);
- nomeado pelo **papel**, não pela cor: `--meduc-text-strong`, `--meduc-border`, `--meduc-surface`
  — não `--meduc-gray-300`;
- consumido por `var(--meduc-…)` em todo uso, como qualquer variável da SDK.

"Mesma cor e mesmo papel" é a regra de decisão: `--gems-secondary-contrast` resolve para
`#111827` com a secundária atual, mas acoplar o cinza-tinta do texto à secundária mudaria o texto
quando a marca mudar — é papel diferente, então é token local.

**Cor literal é cor literal em qualquer grafia.** O que vale para o hex vale para a cor **por nome**
(`white`, `black`, `red`, … — as cores nomeadas do CSS) e para a cor **por função** (`rgb()`, `rgba()`,
`hsl()`, `hsla()`): em CSS de componente, como **valor** de propriedade de cor (`color`, `background*`,
`border*-color`, `outline-color`, `fill`, `stroke`, `box-shadow`), nenhuma das três formas é admitida.
`transparent`, `currentColor`, `inherit` e `var(...)` passam — não são cor, são referência. Um branco
que o projeto precisa vira token: `--meduc-surface` em `styles.css`, e `var(--meduc-surface)` no uso.

```css
/* ERRADO — cor literal em CSS de componente, em qualquer grafia */
color:            white;
background:       rgba(0, 0, 0, .5);
border-color:     hsl(210 20% 90%);

/* CORRETO */
color:            var(--gems-primary-contrast);
background:       var(--meduc-overlay);
```

O gate `verify-conventions.mjs` (`hexEmCss`) reprova hex, cor nomeada e função de cor em **todo** uso
em CSS de componente, e admite cor literal **somente na declaração** de custom property em
`styles.css`. Hex em CSS de componente, mesmo numa declaração `--meduc-x: #…`, reprova — o dono do
token é o `styles.css`.

> **Registro.** Até a `versao: 2.0.0` esta seção descrevia a SDK 1.1.0 e não admitia token local:
> prescrevia tokens que a SDK não tem para cores que o projeto precisa, e por isso `--cor-fundo` e
> `--cor-terciaria` viviam no baseline de conformidade. Corrigido na rodada `009` (decisão 15 do
> product owner; R-03 do `research.md`), fechando o item A-9 da `medicao-m01.md` da `008`. Até a
> `versao: 2.1.0` a proibição alcançava só o hex — `white` e `rgba(...)` passavam pelo gate e a `009`
> os mediu fora dele (§11 dela). Estendida na rodada `010` (decisão 6, R-06) à cor por nome e por
> função, com o gate acompanhando na mesma alteração.

---

## 3. Botões

Use exclusivamente as classes globais da SDK. Não crie estilos `button` ad-hoc.

| Classe | Uso | Ícone padrão |
| :--- | :--- | :--- |
| `btn-save` | Ação principal (Salvar/Confirmar/Avançar) | `fa-floppy-disk` / `fa-arrow-right` |
| `btn-cancel` | Ação secundária (Cancelar/Voltar/Limpar) | `fa-xmark` / `fa-arrow-left` / `fa-eraser` |
| `btn-novo` | Criar registro na listagem | `fa-plus` |
| `btn-back` | Voltar para listagem (ícone só, sem texto) | `fa-arrow-left` |
| `btn-danger` | Ação destrutiva (quando fora da tabela) | `fa-trash` |
| `btn btn-info` | Ação de visualização (via `GemsTableAction`) | `fa-eye` |
| `btn btn-primary` | Ação de edição (via `GemsTableAction`) | `fa-pen` |
| `btn btn-danger` | Ação de exclusão (via `GemsTableAction`) | `fa-trash` |

**Ícone sempre à direita** em `btn-save`; **à esquerda** em `btn-cancel`.

```html
<!-- Exemplos corretos -->
<button class="btn-save" (click)="save()">
  Salvar <i class="fa-solid fa-floppy-disk"></i>
</button>

<button class="btn-cancel" routerLink="/{modulo}/list">
  <i class="fa-solid fa-arrow-left"></i> Voltar
</button>

<button class="btn-novo" routerLink="/{modulo}/form">
  <i class="fa-solid fa-plus"></i> Nova {Entidade}
</button>
```

---

## 4. Ícones

Use exclusivamente **Font Awesome 6 Solid** (`fa-solid`).

```html
<i class="fa-solid fa-plus"></i>
<i class="fa-solid fa-eye"></i>
<i class="fa-solid fa-pen"></i>
<i class="fa-solid fa-trash"></i>
<i class="fa-solid fa-floppy-disk"></i>
<i class="fa-solid fa-xmark"></i>
<i class="fa-solid fa-arrow-left"></i>
<i class="fa-solid fa-arrow-right"></i>
<i class="fa-solid fa-search"></i>
<i class="fa-solid fa-eraser"></i>
<i class="fa-solid fa-circle-notch fa-spin"></i>   <!-- loading -->
<i class="fa-solid fa-check"></i>
<i class="fa-solid fa-list"></i>
```

**Proibido:** `fa-regular`, `fa-light`, Material Icons, ngx-icons ou qualquer outra biblioteca.

---

## 5. Layout e Grid

### Containers de página:
```html
<!-- Listagem -->
<div class="list-container animate__animated animate__fadeIn">...</div>

<!-- Formulário/Stepper -->
<div class="form-container animate__animated animate__fadeIn">...</div>
```

### Grid de campos (dentro de `gems-form-card`):
```html
<div class="form-grid">
  <div class="form-group">...</div>           <!-- 50% de largura -->
  <div class="form-group full-width">...</div> <!-- 100% de largura -->
</div>
```

### Labels:
```html
<label class="fw-bold" for="campo">Campo Opcional</label>
<label class="required fw-bold" for="campo">Campo Obrigatório</label>
<!-- A classe `required` adiciona asterisco vermelho via CSS global -->
```

---

## 6. Micro-animações

- Todo wrapper de rota de componente usa `animate__animated animate__fadeIn` (da biblioteca `animate.css`, já incluída na SDK).
- **Não** adicione outras animações customizadas via `@keyframes`.
- Transições de step do stepper são gerenciadas internamente pelo orquestrador; não estilize.

---

## Anti-Patterns (NUNCA)

| Proibido | Correto |
| :--- | :--- |
| `color: #1e293b` no CSS | `color: var(--gems-text-main)` |
| `color: #374151` no CSS (cinza que a SDK não tem) | `color: var(--meduc-text-strong)`, token declarado em `:root` de `styles.css` (§2.1) |
| `--meduc-x: #…` em CSS de componente | a declaração só vive em `styles.css`; o componente consome `var(--meduc-x)` |
| `:root { --gems-primary-500: red }` em CSS de componente | `provideGemsTheme({ primary: '#...' })` em `app.config.ts` |
| `ngx-spinner` ou spinner customizado com `border + @keyframes` | `@if (isLoading()) { <i class="fa-solid fa-circle-notch fa-spin"> }` |
| `<i class="fa-regular fa-eye">` | `<i class="fa-solid fa-eye">` — apenas Font Awesome 6 **Solid** |
| Botão com estilo inline: `style="background: var(--gems-primary)"` | Classe SDK: `btn-save`, `btn-cancel`, `btn-novo`, `btn-back`, `btn-danger` |

```html
<!-- Correto: fadeIn no wrapper externo do componente -->
<div class="list-container animate__animated animate__fadeIn">
  ...
</div>
```

---

## 7. Loading / Skeleton

Sempre use o padrão signal + `@if`:

```html
@if (isLoading()) {
  <div class="loading-state">
    <i class="fa-solid fa-circle-notch fa-spin"></i> Carregando...
  </div>
} @else {
  <!-- conteúdo real -->
}
```

- **Proibido:** spinners de terceiros, `ngx-spinner`, ou CSS `::before` com `border` girando.
- O estado `isLoading` é sempre um `signal<boolean>(false)` no componente.

---

## 8. Header de Página

Estrutura obrigatória nas telas de listagem e formulário:

```html
<!-- Listagem -->
<div class="header">
  <div class="header-titles">
    <h2>{Título do Módulo}</h2>
    <p class="subtitle">Gerencie os(as) {entidades}.</p>
  </div>
  <button class="btn-novo" routerLink="/{modulo}/form">
    <i class="fa-solid fa-plus"></i> Nova {Entidade}
  </button>
</div>

<!-- Formulário/Stepper -->
<div class="header">
  <div style="display: flex; align-items: center; gap: 1rem;">
    <button class="btn-back" routerLink="/{modulo}/list">
      <i class="fa-solid fa-arrow-left"></i>
    </button>
    <div class="header-titles">
      <h2>{{ pageTitle() }}</h2>
      <p class="subtitle">{{ pageSubtitle() }}</p>
    </div>
  </div>
  @if (!isReadOnly()) {
    <button class="btn btn-danger" (click)="cancelar()">
      <i class="fa-solid fa-xmark"></i> Cancelar
    </button>
  }
</div>
```

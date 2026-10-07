<!-- gems-ai-rule: {"id":"angular.organization","version":"2.0.0","status":"candidate"} -->
---
stack: frontend-angular
module: gems-sdk
severity: mandatory
see-also: [signals-standards.md, stores.md, forms.md, lists.md, stepper.md]
origem: local
precedencia: local-vence
versao: 2.2.0
---

# Organização de Módulos & Estrutura de Feature — Angular

> **TL;DR:**
> - Estrutura por feature em `src/app/features/{pasta}/`: `models/`, `services/`, `list/`, `form/steps/`, `view/`.
> - Nome de arquivo segue o padrão da §6 **por subpasta** — o gate (regra 13) reprova o que foge, sem baseline.
> - CRUD simples: `list/` + `form/`. Stepper/Wizard: `list/` + `form/steps/` + `{modulo}-state.service.ts`.
> - `GemsNavigationService` para toda navegação programática — **nunca** `Router` diretamente nos componentes.
> - Responsividade: Bootstrap grid (`col-md-6`) dentro dos cards SDK. `gems-table` scroll horizontal automático em mobile.

## 1. Estrutura de Pastas por Feature

```
src/app/features/{pasta}/
├── {pasta}.routes.ts                   # Rotas da feature — leva o nome da PASTA
├── models/
│   ├── {modulo}.model.ts               # Interfaces TypeScript da entidade
│   └── {modulo}-filter.model.ts        # Interface de filtro (campos opcionais)
├── services/
│   └── {modulo}.store.ts               # Estende GemsBaseStore (gems-sdk)
├── list/
│   ├── {modulo}-list.component.ts
│   ├── {modulo}-list.component.html
│   └── {modulo}-list.component.css     # Geralmente vazio — estilos globais
├── view/
│   ├── {modulo}-view.component.ts      # Visualização (rota `view/:id`, constituição Princípio VI)
│   ├── {modulo}-view.component.html
│   └── {modulo}-view.component.css
└── form/
    ├── {modulo}-form.component.ts
    ├── {modulo}-form.component.html
    ├── {modulo}-form.component.css
    ├── {modulo}-state.service.ts        # Apenas em Stepper — Signal + BehaviorSubject de estado
    ├── {modulo}.config.ts               # Apenas em Stepper — lista de steps
    └── steps/                           # Apenas em Stepper
        ├── step-{nome}/
        │   ├── step-{nome}.component.ts
        │   └── step-{nome}.component.html
        └── step-resumo/
            ├── step-resumo.component.ts
            └── step-resumo.component.html
```

- **`{pasta}` e `{modulo}` são coisas diferentes.** A pasta da feature é o nome da rota — plural
  quando a rota é (`organizacoes/`); `{modulo}` é o prefixo **singular** dos arquivos da entidade
  (`organizacao.model.ts`, `organizacao-list.component.ts`), **o mesmo em toda a feature**. Só o
  `.routes.ts` leva o nome da pasta.
- **Página sem entidade** (`home/`, `acesso-negado/`) fica na raiz da feature, só com
  `{pasta}.component.{ts,html,css}` e, se houver, `{pasta}.routes.ts` — sem `models/`, `services/`,
  `list/`, `form/` ou `view/`.
- Toda prova fica **ao lado** do arquivo que prova, com o mesmo nome e `.spec.ts` (testing.md).

### Direção de import

`core/` e `shared/` são a base: **nunca** importam de `features/`. Uma feature **nunca** importa de
outra feature — o que é comum a duas sobe para `shared/` ou `core/`. O gate
`frontend/scripts/verify-conventions.mjs` (regra 12, `organization.md#1`) reprova `import … from`,
`export … from` e `import()` cujo caminho resolvido cruze essas fronteiras, nomeando arquivo e linha,
sem baseline.

---

## 2. Interfaces TypeScript

```typescript
// {modulo}.model.ts
export interface {Entidade}Model {
  id?: string;          // Sempre string no frontend (não UUID)
  dataCriacao?: string;
  campoObrigatorio: string;
  campoOpcional?: string;
}

// {modulo}-filter.model.ts
export interface {Entidade}FilterModel {
  textoLivre?: string;
  situacao?: string;
  dataMin?: string;
  dataMax?: string;
}
```

### Regras de interface:
- IDs sempre `string` (não `UUID`) — conversão ocorre no backend.
- Campos opcionais com `?` — jamais `null` explícito em interfaces.
- Interfaces de filtro com todos os campos opcionais (`?`).

---

## 3. GemsNavigationService

Para toda navegação programática, use o `GemsNavigationService` da SDK — nunca o `Router` diretamente nos componentes:

```typescript
export class {Entidade}ListComponent {
  private navigationService = inject(GemsNavigationService);

  navegarParaForm() {
    this.navigationService.navigate(['/{modulo}/form']);
  }

  navegarParaEdicao(id: string) {
    this.navigationService.navigate(['/{modulo}/form', id]);
  }

  navegarParaVisualizacao(id: string) {
    this.navigationService.navigate(['/{modulo}/view', id]);
  }
}
```

---

## 4. Padrão de Ações e Botões

| Ação | Classe CSS | Ícone | Posição do ícone |
| :--- | :--- | :--- | :--- |
| Novo registro | `.btn-novo` | `fa-plus` | **Antes** do texto |
| Salvar / Confirmar | `.btn-save` | `fa-check` / `fa-floppy-disk` | **Depois** do texto |
| Pesquisar | `.btn-save` | `fa-search` | **Depois** do texto |
| Cancelar / Limpar | `.btn-cancel` | `fa-xmark` / `fa-eraser` | **Depois** do texto |
| Voltar | `.btn-back` | `fa-arrow-left` | Sozinho (sem texto) |
| Avançar (stepper) | `.btn-save` | `fa-arrow-right` | **Depois** do texto |
| Voltar (stepper) | `.btn-cancel` | `fa-arrow-left` | **Antes** do texto |
| Excluir | `.btn-danger` | `fa-trash` | **Depois** do texto |

---

## 5. Responsividade

- Bootstrap grid (`row`, `col-md-6`, `col-md-4`) dentro dos cards SDK.
- `gems-table` com scroll horizontal automático em telas pequenas.
- `gems-form-card` ocupa largura total em mobile.
- Formulários com `form-grid` (CSS Grid customizado da SDK) ou Bootstrap rows.

---

## 6. Convenções de Nomenclatura de Arquivo

| Tipo | Onde | Padrão | Exemplo |
| :--- | :--- | :--- | :--- |
| Rotas | raiz da feature | `{pasta}.routes.ts` | `organizacoes.routes.ts` |
| Página sem entidade | raiz da feature | `{pasta}.component.{ts,html,css}` | `home.component.ts` |
| Componente de listagem | `list/` | `{modulo}-list.component.{ts,html,css}` | `produto-list.component.ts` |
| Componente de visualização | `view/` | `{modulo}-view.component.{ts,html,css}` | `produto-view.component.ts` |
| Componente de formulário | `form/` | `{modulo}-form.component.{ts,html,css}` | `produto-form.component.ts` |
| Store | `services/` | `{modulo}.store.ts` | `produto.store.ts` |
| Model | `models/` | `{modulo}.model.ts` | `produto.model.ts` |
| Filter | `models/` | `{modulo}-filter.model.ts` | `produto-filter.model.ts` |
| State Service | `form/` | `{modulo}-state.service.ts` | `produto-state.service.ts` |
| Config | `form/` | `{modulo}.config.ts` | `produto.config.ts` |
| Step | `form/steps/step-{nome}/` | `step-{nome}.component.{ts,html,css}` — a pasta tem o mesmo nome | `step-dados.component.ts` |
| Prova | ao lado do arquivo | `<mesmo nome>.spec.ts` | `produto.store.spec.ts` |

`{modulo}` é um único identificador em kebab-case, **o mesmo em toda a feature**; `{tipo}` é o nome
da subpasta (`list`, `view`, `form`). Subpasta fora da lista, arquivo fora do padrão da sua subpasta
ou dois `{modulo}` na mesma feature reprovam no gate `verify-conventions.mjs` (regra 13,
`organization.md#6`), que lê a árvore de `features/` do sistema de arquivos e nomeia arquivo e padrão
esperado — **sem baseline**: arquivo novo fora do padrão reprova no primeiro `npm test`. Caso que a
tabela não prevê é **emenda desta seção**, com a razão — nunca exceção escrita no arquivo.

> **Registro.** Até a `versao: 2.1.0` a §1 não tinha `view/` (a rota `view/:id` é do Princípio VI da
> constituição 2.5.0, e a `009` a entregou), a §6 não tinha rotas, página sem entidade nem prova, e
> nada dizia que `{modulo}` e a pasta são coisas diferentes — o código da `009` já fazia tudo isso
> (`organizacoes/` com `organizacao-*.ts`), e a regra ficou atrás. Completadas na rodada `010` (decisão
> 6, R-06) no mesmo commit em que a regra 13 nasceu, para que o gate meça a regra e não a lacuna.

---

## Anti-Patterns (NUNCA)

| Proibido | Correto |
| :--- | :--- |
| `this.router.navigate([...])` diretamente no componente | `this.navigationService.navigate([...])` |
| `id?: number` na interface de model | `id?: string` — IDs sempre `string` no frontend |
| `campo: null` em interface TypeScript | `campo?: string` — usar opcional `?`, nunca `null` explícito |
| Feature sem pasta `models/` e `services/` | Estrutura obrigatória: `models/`, `services/`, `list/`, `form/` |

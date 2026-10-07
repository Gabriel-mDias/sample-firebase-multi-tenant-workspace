<!-- gems-ai-rule: {"id":"angular.signals-standards","version":"2.0.0","status":"candidate"} -->
---
stack: frontend-angular
module: @angular/core
severity: mandatory
see-also: [forms.md, lists.md, stores.md, stepper.md]
origem: local
precedencia: local-vence
versao: 2.0.0
---

# Signals & Modern Angular — Angular 22

> **TL;DR:**
> - Componente é standalone **por padrão** no Angular 19+. Não declare `standalone: true`. **NUNCA** `NgModules`.
> - Signals: use `input()`, `output()`, `model()`, `signal()`, `computed()` — **NUNCA** `@Input`/`@Output` decorators.
> - Injeção: `inject()` no corpo da classe — **NUNCA** `constructor` com parâmetros.
> - Estado de componente é `protected readonly` + `signal()`; dependência é `private readonly`.
> - Controle de fluxo: `@if` / `@for` / `@switch` — **NUNCA** `*ngIf` / `*ngFor` / `*ngSwitch`.
> - Lazy loading: `loadComponent()` / `loadChildren()` em todas as rotas.

> **Correção da rodada `008` (bloco M).** Esta regra dizia "Angular 20+: `standalone: true`
> obrigatório" e importava símbolos sem o prefixo `Gems` de um pacote `'gems-sdk'` que não existe.
> O projeto está em **Angular 22**, onde standalone é o padrão e a flag é redundante, e o pacote é
> `@gabriel-mdias/angular-gems-sdk`.

## 1. Standalone (Padrão, não declarado)

```typescript
import { Component } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';
import { GemsFormCardComponent, GemsInputTextComponent } from '@gabriel-mdias/angular-gems-sdk';

@Component({
  selector: 'app-{entidade}-form',
  imports: [                       // importa diretamente o que usa
    ReactiveFormsModule,
    GemsFormCardComponent,
    GemsInputTextComponent,
  ],
  templateUrl: './{entidade}-form.component.html',
  styleUrl: './{entidade}-form.component.css',
})
export class {Entidade}FormComponent {}
```

**Nunca** use `NgModule`. Cada componente é auto-suficiente via `imports`.

**Não escreva `standalone: true`.** Desde o Angular 19 é o padrão, e o schematic oficial não o gera.
Declará-lo é ruído que envelhece: quem lê passa a procurar o componente "não standalone" que não
existe.

---

## 2. Signals — Declaração de Propriedades

### Signals de estado interno
```typescript
export class {Entidade}FormComponent {
  // Signals de estado
  isLoading = signal(false);
  isEdit = signal(false);

  // Computed (derivado de outros signals)
  pageTitle = computed(() => this.isEdit() ? 'Editar {Entidade}' : 'Nova {Entidade}');
  pageSubtitle = computed(() => this.isEdit() ? 'Atualize os dados.' : 'Preencha os dados abaixo.');

  // Mutação
  toggleLoading() {
    this.isLoading.set(!this.isLoading());
  }
}
```

### Inputs de componente
```typescript
export class {Entidade}CardComponent {
  // CORRETO — Angular 20+
  title = input.required<string>();
  subtitle = input<string>('');
  isReadOnly = input<boolean>(false);

  // Alias (quando o nome público difere do interno)
  entidadeId = input.required<string>({ alias: 'id' });
}
```

### Outputs de componente
```typescript
export class {Entidade}CardComponent {
  // CORRETO — Angular 20+
  selected = output<{Entidade}Model>();
  deleted = output<string>();   // Emite o ID

  onSelect(item: {Entidade}Model) {
    this.selected.emit(item);
  }
}
```

### Two-way binding (model signal)
```typescript
export class ToggleComponent {
  // Two-way binding — substitui @Input + @Output com EventEmitter
  value = model<boolean>(false);
}

// No template pai:
// <app-toggle [(value)]="meuSignal" />
```

---

## 3. Injeção de Dependências — inject()

```typescript
export class {Entidade}ListComponent {
  // Dependências: private readonly, na ordem em que a classe as usa
  private readonly fb = inject(FormBuilder);
  private readonly store = inject({Entidade}Store);
  private readonly route = inject(ActivatedRoute);
  private readonly alertService = inject(GemsAlertService);

  // Estado exposto ao template: protected readonly
  protected readonly entidades = signal<{Entidade}[]>([]);
  protected readonly isLoading = signal(false);

  // Estado interno, que o template não lê: private readonly
  private readonly hasFilters = signal(false);
}
```

**Nunca** use `constructor(private store: {Entidade}Store)` — use `inject()`.

### Visibilidade não é detalhe

| Modificador | Para quê | Por quê |
| :--- | :--- | :--- |
| `private readonly` | dependências injetadas e estado que só a classe usa | o template não deve alcançar a Store nem o `Router` |
| `protected readonly` | estado e métodos que o template lê ou chama | `public` abre a superfície para qualquer um; `private` quebra o template |

`readonly` em toda parte: o que muda é o **valor dentro do signal** (`.set()`, `.update()`), nunca a
referência. Uma reatribuição do próprio campo desconecta todos os leitores já registrados, e o
sintoma é uma tela que simplesmente para de atualizar.

---

## 4. Controle de Fluxo no Template

```html
<!-- CORRETO — Angular 17+ / 20+ -->
@if (isLoading()) {
  <div class="loading-state">
    <i class="fa-solid fa-circle-notch fa-spin"></i> Carregando...
  </div>
} @else {
  <gems-table [data]="entidades()" ... />
}

@for (item of entidades(); track item.id) {
  <div class="item">{{ item.nome }}</div>
} @empty {
  <p>Nenhum item encontrado.</p>
}

@switch (situacao()) {
  @case ('ATIVO') { <span class="badge bg-success">Ativo</span> }
  @case ('INATIVO') { <span class="badge bg-danger">Inativo</span> }
  @default { <span class="badge bg-secondary">-</span> }
}
```

**Nunca** use `*ngIf`, `*ngFor`, `*ngSwitch` — use `@if`, `@for`, `@switch`.

---

## 5. Leitura de Signal no Template

Signals **são funções** — sempre invocar com `()` no template:

```html
<!-- CORRETO -->
<h2>{{ pageTitle() }}</h2>
<gems-table [data]="entidades()" [isLoading]="isLoading()" />

@if (isEdit()) {
  <button class="btn-save">Atualizar</button>
}

<!-- ERRADO — sem () -->
<h2>{{ pageTitle }}</h2>
```

---

## 6. Rotas com Lazy Loading (Obrigatório)

```typescript
// app.routes.ts
export const routes: Routes = [
  {
    path: '{modulo}',
    children: [
      {
        path: 'list',
        loadComponent: () => import('./features/{modulo}/list/{modulo}-list.component')
          .then(c => c.{Entidade}ListComponent)
      },
      {
        path: 'form',
        loadComponent: () => import('./features/{modulo}/form/{modulo}-form.component')
          .then(c => c.{Entidade}FormComponent)
      },
      {
        path: 'form/:id',
        loadComponent: () => import('./features/{modulo}/form/{modulo}-form.component')
          .then(c => c.{Entidade}FormComponent)
      },
      { path: '', redirectTo: 'list', pathMatch: 'full' }
    ]
  }
];
```

Sempre `loadComponent` com lazy loading — nunca imports estáticos de componentes de rota.

---

## Anti-Patterns (NUNCA)

| Proibido | Correto |
| :--- | :--- |
| `NgModule` | componente standalone com `imports: [...]` |
| `standalone: true` escrito no decorator | omitir — é o padrão desde o Angular 19 |
| `import { TableComponent } from 'gems-sdk'` | `import { GemsTableComponent } from '@gabriel-mdias/angular-gems-sdk'` |
| Campo de estado `public` e sem `readonly` | `protected readonly x = signal(...)` |
| `@Input() valor: T` | `valor = input<T>()` |
| `@Output() evento = new EventEmitter<T>()` | `evento = output<T>()` |
| `constructor(private svc: Svc)` | `private svc = inject(Svc)` |
| `*ngIf="cond"` | `@if (cond) { }` |
| `*ngFor="let i of lista"` | `@for (i of lista; track i.id) { }` |
| `BehaviorSubject` para estado local do componente | `signal()` + `computed()` |

---

## 7. Resumo: O que NUNCA usar

| Padrão antigo | Substituto moderno |
| :--- | :--- |
| `@Input() valor: T` | `valor = input<T>()` |
| `@Output() evento = new EventEmitter<T>()` | `evento = output<T>()` |
| `constructor(private svc: Svc)` | `private svc = inject(Svc)` |
| `*ngIf="cond"` | `@if (cond) { }` |
| `*ngFor="let i of lista"` | `@for (i of lista; track i.id) { }` |
| `*ngSwitch` / `*ngSwitchCase` | `@switch` / `@case` |
| `NgModule` | componente standalone com `imports: [...]` |
| `standalone: true` no decorator | omitir — padrão desde o Angular 19 |
| `BehaviorSubject + async pipe` | Prefer `signal()` + `computed()` para estado local |
| Símbolo da SDK sem o prefixo `Gems` | `GemsTableComponent`, `GemsAlertService`, `GemsPageable`… |

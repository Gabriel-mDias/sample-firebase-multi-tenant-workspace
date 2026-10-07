---
stack: frontend-angular
module: gems-sdk (GemsWizardComponent, GemsBaseStepComponent)
severity: mandatory
see-also: [signals-standards.md, stores.md, gems-components.md, feedback-alerts.md]
origem: local
precedencia: local-vence
versao: 2.0.0
---

# Stepper / Wizard — Angular / gems-sdk

> **TL;DR:**
> - O stepper visual é o `<gems-wizard>` da SDK. **Nunca** monte barra de progresso, círculos ou trilha à mão.
> - Todo step estende `GemsBaseStepComponent<T>` e implementa `createForm`/`loadFromSession`/`saveToSession`/`goNext`/`goBack`.
> - `StateService` local à feature, **provido pelo orquestrador** (`providers: [...]`), não em `'root'`.
> - Estado com `signal()` + `computed()`. **Nunca** `BehaviorSubject`.
> - Steps leem e escrevem no `StateService` — **nunca** chamam a Store.
> - O orquestrador é o único que chama a Store, e limpa o estado no `ngOnDestroy`.

> **Por que esta regra foi reescrita.** Ela prescrevia montar o stepper à mão: `stepper-container`,
> `stepper-progress-bar-fill`, `step-circle` e `progressPercentage` calculado no componente. A SDK
> entrega `GemsWizardComponent` e `GemsBaseStepComponent`, que cobrem tudo isso, e o `AGENTS.md` do
> frontend manda reutilizar a composição consolidada (`gems-wizard`). Seguir a versão anterior
> significaria reescrever à mão o que a SDK já faz. Corrigida na rodada `008`, bloco M.

---

## 1. Estrutura de Arquivos

```
features/{modulo}/form/
├── {modulo}-form.component.ts          # Orquestrador
├── {modulo}-form.component.html
├── {modulo}-form-state.service.ts      # Estado do wizard (signals)
├── {modulo}-form.config.ts             # Steps + constantes de índice
└── steps/
    ├── step-{nome}/
    │   ├── step-{nome}.component.ts
    │   └── step-{nome}.component.html
    └── step-resumo/
        ├── step-resumo.component.ts
        └── step-resumo.component.html
```

---

## 2. Config dos Steps

Os steps são `GemsWizardStep[]` — `label` e `icon` (Font Awesome Solid). Os índices saem como
constantes nomeadas, para que nem o orquestrador nem os steps usem número mágico:

```typescript
// {modulo}-form.config.ts
import { GemsWizardStep } from '@gabriel-mdias/angular-gems-sdk';

export const {MODULO}_FORM_STEPS: GemsWizardStep[] = [
  { label: '{Primeiro}', icon: 'fa-solid fa-building' },
  { label: '{Segundo}',  icon: 'fa-solid fa-user-tie' },
  { label: 'Resumo',     icon: 'fa-solid fa-clipboard-check' },
];

export const STEP_{PRIMEIRO} = 1;
export const STEP_{SEGUNDO} = 2;
export const STEP_RESUMO = 3;
```

---

## 3. State Service

Estado do wizard, com `signal()` para escrita e `computed()` para leitura. **Provido pelo
orquestrador**, não em `'root'`: o estado morre com a tela, e duas abas do mesmo formulário não
compartilham rascunho.

```typescript
import { Injectable, computed, signal } from '@angular/core';

export type {Entidade}FormMode = 'create' | 'edit';

function emptyState(): {Entidade}Create {
  return { campo1: '', campo2: '', filho: { campoA: '' } };
}

@Injectable()                    // sem providedIn: 'root'
export class {Entidade}FormStateService {

  private readonly value = signal<{Entidade}Create>(emptyState());

  readonly snapshot = computed(() => this.value());
  readonly mode = signal<{Entidade}FormMode>('create');
  readonly currentStep = signal(1);
  readonly isEdit = computed(() => this.mode() === 'edit');

  update{Entidade}(change: Partial<Omit<{Entidade}Create, 'filho'>>): void {
    this.value.update((current) => ({ ...current, ...change }));
  }

  updateFilho(change: Partial<FilhoInput>): void {
    this.value.update((current) => ({ ...current, filho: { ...current.filho, ...change } }));
  }

  loadFrom(detail: {Entidade}Detail): void {
    this.update{Entidade}({ campo1: detail.campo1, campo2: detail.campo2 ?? '' });
  }

  goToStep(step: number): void {
    this.currentStep.set(step);
  }

  clear(): void {
    this.value.set(emptyState());
    this.currentStep.set(1);
  }
}
```

### Campo opcional vazio vai como `undefined`, nunca como `''`

O payload de envio é montado por método dedicado, e **campo opcional em branco sai como
`undefined`**. Mandar `''` é o defeito mais caro deste arquivo: para o backend `''` não é nulo, e a
validação de formato reprova quem deixou em branco um campo que a tela declara opcional.

```typescript
toCreatePayload(): {Entidade}Create {
  const current = structuredClone(this.value());
  return {
    ...current,
    campoOpcional: current.campoOpcional || undefined,
  };
}
```

---

## 4. Componente Orquestrador

Ele é o **único** que conhece a Store. Detecta `id` na rota, carrega, e limpa o estado ao sair.

```typescript
import { Component, OnDestroy, OnInit, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { GemsAlertService, GemsWizardComponent } from '@gabriel-mdias/angular-gems-sdk';
import { {Entidade}Store } from '../services/{entidade}.store';
import { {Entidade}FormStateService } from './{modulo}-form-state.service';
import { {MODULO}_FORM_STEPS, STEP_{PRIMEIRO}, STEP_{SEGUNDO} } from './{modulo}-form.config';
import { Step{Primeiro}Component } from './steps/step-{primeiro}/step-{primeiro}.component';
import { StepResumoComponent } from './steps/step-resumo/step-resumo.component';

@Component({
  selector: 'app-{entidade}-form',
  imports: [GemsWizardComponent, Step{Primeiro}Component, StepResumoComponent],
  providers: [{Entidade}FormStateService],        // escopo da tela, não global
  templateUrl: './{entidade}-form.component.html',
  styleUrl: './{entidade}-form.component.css',
})
export class {Entidade}FormComponent implements OnInit, OnDestroy {

  private readonly store = inject({Entidade}Store);
  private readonly route = inject(ActivatedRoute);
  private readonly alertService = inject(GemsAlertService);
  protected readonly state = inject({Entidade}FormStateService);

  protected readonly steps = {MODULO}_FORM_STEPS;
  protected readonly step{Primeiro} = STEP_{PRIMEIRO};
  protected readonly isLoading = signal(false);
  protected readonly isPageLoading = signal(false);
  private readonly id = signal<string | null>(null);

  protected readonly title = computed(() =>
    this.state.isEdit() ? 'Editar {entidade}' : 'Nova {entidade}',
  );

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    this.id.set(id);
    this.state.mode.set(id ? 'edit' : 'create');
    if (id) this.load(id);
  }

  ngOnDestroy(): void {
    this.state.clear();
  }

  protected onStepChange(step: number): void {
    this.state.goToStep(step);
  }

  protected save(): void { /* chama store.create/update, trata erro com errorFromApi */ }

  private load(id: string): void { /* chama store.detail(id), state.loadFrom(detail) */ }
}
```

---

## 5. Template do Orquestrador

O `<gems-wizard>` recebe os steps e o passo corrente, e devolve `stepChange`. O conteúdo do passo
sai de um `@switch` sobre as **constantes**, nunca sobre literais numéricos:

```html
<div class="form-container animate__animated animate__fadeIn">

  <div class="header">
    <button type="button" class="btn-back" aria-label="Voltar para a lista" (click)="exit()">
      <i class="fa-solid fa-arrow-left"></i>
    </button>
    <div class="header-titles">
      <h2>{{ title() }}</h2>
      <p class="subtitle">{{ description() }}</p>
    </div>
    <button type="button" class="btn-danger" (click)="exit()">
      <i class="fa-solid fa-xmark"></i>
      Cancelar
    </button>
  </div>

  @if (!state.isEdit()) {
    <gems-wizard
      [steps]="steps"
      [currentStep]="state.currentStep()"
      (stepChange)="onStepChange($event)"
    ></gems-wizard>
  }

  @if (isPageLoading()) {
    <div class="form-loading" role="status" aria-live="polite">
      <i class="fa-solid fa-circle-notch fa-spin"></i>
      <span>Carregando {entidade}…</span>
    </div>
  } @else {
    @switch (state.currentStep()) {
      @case (step{Primeiro}) { <app-step-{primeiro} /> }
      @case (step{Segundo})  { <app-step-{segundo} /> }
      @default               { <app-step-resumo [isLoading]="isLoading()" (confirm)="save()" /> }
    }
  }

</div>
```

**Em modo edição o wizard não aparece.** Editar não é percorrer passos: a tela mostra direto o step
de dados, com `editMode` ligado, e o botão salva em vez de avançar.

---

## 6. Step Individual — `GemsBaseStepComponent`

Todo step de formulário **estende `GemsBaseStepComponent<T>`**, onde `T` é o payload daquele passo.
A classe base cuida do ciclo (criar o form, hidratar da sessão, persistir ao sair); o step só
preenche os cinco ganchos:

```typescript
import { Component, inject, input, output } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import {
  GemsBaseStepComponent,
  GemsFormCardComponent,
  GemsInputTextComponent,
} from '@gabriel-mdias/angular-gems-sdk';
import { {Entidade}FormStateService } from '../../{modulo}-form-state.service';
import { STEP_{PRIMEIRO}, STEP_{SEGUNDO} } from '../../{modulo}-form.config';

interface {Primeiro}StepPayload {
  campo1: string;
  campo2: string;
}

@Component({
  selector: 'app-step-{primeiro}',
  imports: [ReactiveFormsModule, GemsFormCardComponent, GemsInputTextComponent],
  templateUrl: './step-{primeiro}.component.html',
  styleUrl: './step-{primeiro}.component.css',
})
export class Step{Primeiro}Component extends GemsBaseStepComponent<{Primeiro}StepPayload> {

  private readonly fb = inject(FormBuilder);
  protected readonly state = inject({Entidade}FormStateService);

  readonly editMode = input(false);
  readonly isLoading = input(false);
  readonly saveEdit = output<void>();

  override formGroup!: FormGroup;
  override stepIndex = STEP_{PRIMEIRO};

  override createForm(): void {
    this.formGroup = this.fb.group({
      campo1: ['', [Validators.required, Validators.maxLength(255)]],
      campo2: [''],
    });
  }

  override loadFromSession(): void {
    const current = this.state.snapshot();
    this.formGroup.patchValue({ campo1: current.campo1, campo2: current.campo2 ?? '' });
  }

  override saveToSession(payload: {Primeiro}StepPayload): void {
    this.state.update{Entidade}(payload);
  }

  override goNext(): void {
    this.state.goToStep(STEP_{SEGUNDO});
  }

  override goBack(): void {
    this.state.goToStep(STEP_{PRIMEIRO} - 1);
  }

  protected continue(): void {
    if (this.formGroup.invalid) {
      this.formGroup.markAllAsTouched();
      return;
    }
    this.saveToSession(this.formGroup.getRawValue() as {Primeiro}StepPayload);
    if (this.editMode()) this.saveEdit.emit();
    else this.goNext();
  }
}
```

### Os cinco ganchos, e o que cada um deve e não deve fazer

| Gancho | Faz | Nunca faz |
| :--- | :--- | :--- |
| `createForm()` | monta o `FormGroup` com os validadores do passo | chama a Store; lê a rota |
| `loadFromSession()` | hidrata o form a partir do `StateService` | chama a Store |
| `saveToSession(payload)` | grava o payload no `StateService` | chama a Store; navega |
| `goNext()` | `state.goToStep(PROXIMO)` | envia ao backend |
| `goBack()` | `state.goToStep(ANTERIOR)`, ou sai da tela no primeiro passo | descarta dados em silêncio |

Campo que não pode mudar em edição se desabilita no `createForm`, não se esconde no template:

```typescript
if (this.state.isEdit()) this.formGroup.get('alias')?.disable();
```

---

## 7. Step de Resumo

É o único step que **não** estende `GemsBaseStepComponent`: ele não tem formulário. Lê o snapshot,
exibe em `gems-summary-card`, e **emite** — quem envia ao backend é o orquestrador.

```typescript
@Component({
  selector: 'app-step-resumo-{entidade}',
  imports: [GemsFormCardComponent, GemsSummaryCardComponent],
  templateUrl: './step-resumo.component.html',
  styleUrl: './step-resumo.component.css',
})
export class StepResumoComponent {
  protected readonly state = inject({Entidade}FormStateService);
  readonly isLoading = input(false);
  readonly confirm = output<void>();
  protected readonly summary = this.state.snapshot;

  protected back(): void {
    this.state.goToStep(STEP_{SEGUNDO});
  }
}
```

```html
<gems-form-card title="Resumo" subtitle="Revise os dados antes de confirmar." icon="fa-solid fa-clipboard-check">
  <div class="summary-cards">
    <gems-summary-card title="{Seção}" icon="fa-solid fa-building" [expanded]="true">
      <span summary>{{ summary().campo1 || 'Sem {campo} informado' }}</span>
      <div details class="summary-grid">
        <div class="summary-item">
          <span class="summary-label">Campo</span>
          <span>{{ summary().campo1 || '—' }}</span>
        </div>
      </div>
    </gems-summary-card>
  </div>

  <div gems-form-card-footer class="step-actions">
    <button type="button" class="btn-secondary" (click)="back()">
      <i class="fa-solid fa-arrow-left"></i>
      Voltar
    </button>
    <button type="button" class="btn-save" [disabled]="isLoading()" (click)="confirm.emit()">
      @if (isLoading()) {
        Processando <i class="fa-solid fa-circle-notch fa-spin"></i>
      } @else {
        Confirmar <i class="fa-solid fa-check"></i>
      }
    </button>
  </div>
</gems-form-card>
```

Os slots do `gems-summary-card` são `summary` (preview recolhido) e `details` (conteúdo expandido);
o rodapé do `gems-form-card` é `gems-form-card-footer`.

---

## Anti-Patterns (NUNCA)

| Proibido | Correto |
| :--- | :--- |
| Montar `stepper-container` / barra de progresso / círculos à mão | `<gems-wizard [steps] [currentStep] (stepChange)>` |
| Step com `FormGroup` próprio sem estender a base | `extends GemsBaseStepComponent<T>` com os cinco ganchos |
| `BehaviorSubject` no `StateService` | `signal()` para escrita, `computed()` para leitura |
| `@Injectable({ providedIn: 'root' })` no `StateService` do wizard | `@Injectable()` + `providers: [...]` no orquestrador |
| Step chamando `store.getById()` ou `store.create()` | Só o orquestrador fala com a Store |
| `@case (2)` com literal no template | `@case (stepGestor)` — constante nomeada do `.config.ts` |
| Campo opcional vazio enviado como `''` | `campo \|\| undefined` no `toCreatePayload()` |
| Não limpar o estado ao sair da tela | `ngOnDestroy(): void { this.state.clear(); }` |

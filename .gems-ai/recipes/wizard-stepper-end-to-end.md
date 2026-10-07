---
origem: local
precedencia: local-vence
versao: 2.0.0
---

# Recipe: Wizard / Stepper Ponta-a-Ponta

**Objetivo:** Implementar um fluxo de cadastro em múltiplos passos com StateService baseado em signals, orquestrador, steps isolados, step de resumo e persistência no backend.

**Pré-requisitos:**
- `gems-sdk` Angular (FormCardComponent, SummaryCardComponent, AlertService, NavigationService)
- Backend CRUD completo da entidade principal (Controller + Service + Repository)

**Rules relacionadas:**
- [stepper.md](../rules/frontend-angular/stepper.md)
- [signals-standards.md](../rules/frontend-angular/signals-standards.md)
- [gems-components.md](../rules/frontend-angular/gems-components.md)
- [services.md](../rules/backend-java/services.md)
- [controllers.md](../rules/backend-java/controllers.md)

## Resultado Esperado

- `StateService` baseado em `signal()` compartilhando estado entre o orquestrador e os steps, com `clearState()` após sucesso.
- Steps isolados que leem do `StateService.snapshot` e escrevem via `updateState()` — nunca chamam a Store diretamente.
- Mesma rota `form` / `form/:id` / `view/:id` carregando o orquestrador, que detecta modo criação / edição / visualização no `ngOnInit`.

---

## Arquitetura

```
features/{modulo}/form/
├── {modulo}-form.component.ts      # Orquestrador: gerencia rota + estado global
├── {modulo}-form.component.html
├── {modulo}-state.service.ts       # Signal store local do fluxo
├── {modulo}.config.ts              # Array de steps (label + id)
└── steps/
    ├── step-dados/                 # Step 1: dados básicos
    ├── step-{outro}/              # Step N: dados complementares
    └── step-resumo/               # Último step: revisão + botão Salvar
```

Cada step é **isolado** — não conhece os outros. Comunicam-se **exclusivamente** via `StateService`.

---

## Passo 1: Config dos Steps

```typescript
// {modulo}.config.ts
export const {MODULO}_FORM_STEPS = [
  { id: 1, label: 'Dados Básicos' },
  { id: 2, label: 'Detalhes' },
  { id: 3, label: 'Resumo' }
];
```

## Passo 2: StateService

```typescript
// {modulo}-state.service.ts
export interface {Entidade}State {
  id?: string;
  // Campos dos steps:
  nome?: string;
  email?: string;
  // ...
  currentStep: number;
  isReadOnly: boolean;
}

const INITIAL_STATE: {Entidade}State = { currentStep: 1, isReadOnly: false };

@Injectable({ providedIn: 'root' })
export class {Entidade}StateService {
  private _state = signal<{Entidade}State>(INITIAL_STATE);

  state       = this._state.asReadonly();
  currentStep = computed(() => this._state().currentStep);
  isReadOnly  = computed(() => this._state().isReadOnly);

  get snapshot(): {Entidade}State { return this._state(); }

  updateState(partial: Partial<{Entidade}State>): void {
    this._state.update(s => ({ ...s, ...partial }));
  }
  setStep(step: number): void  { this.updateState({ currentStep: step }); }
  setReadOnly(v: boolean): void { this.updateState({ isReadOnly: v }); }
  clearState(): void           { this._state.set(INITIAL_STATE); }
}
```

## Passo 3: Orquestrador

```typescript
// {modulo}-form.component.ts
@Component({ standalone: true, ... })
export class {Entidade}FormComponent implements OnInit {
  private stateService = inject({Entidade}StateService);
  private store        = inject({Entidade}Store);
  private alertService = inject(AlertService);
  private nav          = inject(NavigationService);
  private route        = inject(ActivatedRoute);

  steps = {MODULO}_FORM_STEPS;
  isLoading  = signal(false);
  currentStep = this.stateService.currentStep;
  isReadOnly  = this.stateService.isReadOnly;
  progressPercentage = computed(() =>
    ((this.currentStep() - 1) / (this.steps.length - 1)) * 100
  );

  ngOnInit(): void {
    const id          = this.route.snapshot.paramMap.get('id');
    const isViewRoute = this.route.snapshot.routeConfig?.path?.includes('view');
    this.stateService.setReadOnly(isViewRoute ?? false);

    if (id) { this.loadExisting(id); }
    else     { this.stateService.clearState(); }
  }

  async loadExisting(id: string): Promise<void> {
    this.isLoading.set(true);
    try {
      const data = await firstValueFrom(this.store.getById(id));
      this.stateService.updateState({ id: data.id, nome: data.nome /* ... */ });
      if (this.isReadOnly()) {
        this.stateService.setStep(this.steps.length);  // Vai direto para resumo
      }
    } catch (err: any) {
      this.alertService.errorFromApi(err);
      this.nav.navigate(['/{modulo}/list']);
    } finally {
      this.isLoading.set(false);
    }
  }

  async cancelar(): Promise<void> {
    const c = await this.alertService.confirm('Cancelar', 'Os dados não salvos serão perdidos.');
    if (c.isConfirmed) {
      this.stateService.clearState();
      this.nav.navigate(['/{modulo}/list']);
    }
  }
}
```

## Passo 4: Template do Orquestrador

Ver [stepper.md §5](../rules/frontend-angular/stepper.md) para o template completo.

Ponto crítico: stepper visual usa `@for` e `@if`:
```html
@for (step of steps; track step.id) {
  <div class="stepper-item"
       [class.active]="currentStep() === step.id"
       [class.completed]="currentStep() > step.id">
    <div class="step-circle">
      @if (currentStep() > step.id) {
        <i class="fa-solid fa-check"></i>
      } @else {
        <span>{{ step.id }}</span>
      }
    </div>
    <div class="step-label">{{ step.label }}</div>
  </div>
}
```

## Passo 5: Step Individual

```typescript
@Component({ standalone: true, ... })
export class Step{Nome}Component implements OnInit {
  private stateService = inject({Entidade}StateService);
  private alertService = inject(AlertService);

  nome   = signal('');
  email  = signal('');

  ngOnInit(): void {
    const s = this.stateService.snapshot;
    this.nome.set(s.nome ?? '');
    this.email.set(s.email ?? '');
  }

  next(): void {
    if (!this.nome()) {
      this.alertService.warning('Atenção', 'Preencha o nome.');
      return;
    }
    this.stateService.updateState({ nome: this.nome(), email: this.email() });
    this.stateService.setStep(2);
  }

  goBack(): void { this.stateService.setStep(0); } // ou cancelar
}
```

```html
<gems-form-card title="Dados Básicos">
  <p form-card-subtitle>Informe os dados principais.</p>
  <div class="form-grid">
    <div class="form-group full-width">
      <label class="required fw-bold">Nome</label>
      <input class="form-control" [(ngModel)]="nome" placeholder="Nome completo">
    </div>
  </div>
  <div form-card-footer class="footer-step">
    <button class="btn-cancel" (click)="goBack()" style="visibility: hidden;">
      <i class="fa-solid fa-arrow-left"></i> Voltar
    </button>
    <button class="btn-save" (click)="next()">
      Avançar <i class="fa-solid fa-arrow-right"></i>
    </button>
  </div>
</gems-form-card>
```

## Passo 6: Step de Resumo + Persistência no Backend

```typescript
@Component({ standalone: true, ... })
export class StepResumoComponent {
  private stateService = inject({Entidade}StateService);
  private store        = inject({Entidade}Store);
  private alertService = inject(AlertService);
  private nav          = inject(NavigationService);

  state      = this.stateService.state;
  isReadOnly = this.stateService.isReadOnly;
  isLoading  = signal(false);

  goBack(): void {
    if (this.isReadOnly()) { this.nav.navigate(['/{modulo}/list']); return; }
    this.stateService.setStep(2);
  }

  async save(): Promise<void> {
    this.isLoading.set(true);
    const s = this.stateService.snapshot;
    const dto: {Entidade}Model = { nome: s.nome!, email: s.email! };
    const call$ = s.id ? this.store.update(s.id, dto) : this.store.create(dto);

    call$.subscribe({
      next: () => {
        this.alertService.success('Sucesso', '{Entidade} salvo(a) com sucesso!');
        this.stateService.clearState();
        this.nav.navigate(['/{modulo}/list']);
      },
      error: (err) => {
        this.alertService.errorFromApi(err);
        this.isLoading.set(false);
      }
    });
  }
}
```

**Delegação de sub-entidades:** quando o fluxo salva múltiplas entidades relacionadas, faça isso no **Service do backend** usando Spring Events ou chamadas sequenciais — nunca chame múltiplos endpoints do frontend no step de resumo:

```java
// {Entidade}Service.java — persiste entidade principal + sub-entidades
@Transactional(rollbackFor = Exception.class)
public {Entidade} save({Entidade}DTO dto) {
    {Entidade} entidade = mapper.toEntity(dto);
    repository.save(entidade);
    applicationEventPublisher.publishEvent(new {Entidade}CriadoEvent(entidade.getId()));
    return entidade;
}
```

## Passo 7: Rotas

```typescript
export const routes: Routes = [
  { path: 'list',     loadComponent: () => import('./list/{modulo}-list.component').then(c => c.{Entidade}ListComponent) },
  { path: 'form',     loadComponent: () => import('./form/{modulo}-form.component').then(c => c.{Entidade}FormComponent) },
  { path: 'form/:id', loadComponent: () => import('./form/{modulo}-form.component').then(c => c.{Entidade}FormComponent) },
  { path: 'view/:id', loadComponent: () => import('./form/{modulo}-form.component').then(c => c.{Entidade}FormComponent) },
  { path: '',         redirectTo: 'list', pathMatch: 'full' }
];
```

Form e view carregam **o mesmo componente** — o `isReadOnly` é derivado da URL no `ngOnInit`.

---

## Checklist de Conformidade

- [ ] StateService usa `signal()` — nunca `BehaviorSubject`.
- [ ] `state.asReadonly()` expõe o state publicamente.
- [ ] Steps comunicam-se **somente** via StateService (sem `@Input/@Output` entre eles).
- [ ] Orquestrador carrega dados com `firstValueFrom()` (async/await) no `ngOnInit`.
- [ ] Step de resumo chama **um único endpoint** do backend.
- [ ] Backend persiste sub-entidades no Service (nunca múltiplas chamadas do frontend).
- [ ] `clearState()` é chamado após save com sucesso.
- [ ] Rotas `form`, `form/:id` e `view/:id` apontam para o mesmo componente.
- [ ] `isReadOnly()` em view: stepper oculto, ir direto para o resumo.
- [ ] CSS `.resumo-details-grid` e `.detail-item` nos estilos do step-resumo.

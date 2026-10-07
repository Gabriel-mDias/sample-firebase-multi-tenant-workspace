<!-- gems-ai-rule: {"id":"angular.forms","version":"2.0.0","status":"candidate"} -->
---
stack: frontend-angular
module: gems-sdk, @angular/forms
severity: mandatory
see-also: [signals-standards.md, gems-components.md, feedback-alerts.md, stepper.md]
origem: local
precedencia: local-vence
versao: 2.1.0
---

# Formulários Simples — Angular / gems-sdk

> **TL;DR:**
> - Componente standalone (padrão, não declarado). `FormGroup` com `FormBuilder`, **nunca** template-driven.
> - `inject()` para dependências — **nunca** `constructor` com parâmetros.
> - `ngOnInit`: detecta `id` na rota para modo edição (`isEdit.set(true)`).
> - Template: header (`btn-back`) + `[formGroup]` + `gems-form-card` + `gems-form-card-footer` (Cancelar/Salvar).
> - `@if (isLoading())` para skeleton — **nunca** `*ngIf`.
> - **Todo campo usa componente da SDK.** `gems-input-text` para texto; `gems-input-mask`,
>   `gems-input-data`, `gems-input-documento` para os formatados. **NUNCA** `<input>` nativo.

> **Correção da rodada `008` (bloco M).** Esta regra mostrava `<input type="text" class="form-control">`
> para campo comum, enquanto proibia input nativo para campo formatado — e o `AGENTS.md` do frontend
> manda usar `gems-input-text` inclusive onde há máscara aplicada por diretiva (RG usa
> `gems-input-text` + `appRgMask`; `maskType="rg"` é **proibido**). A contradição foi resolvida a
> favor do componente da SDK: **nenhum `<input>` nativo em formulário de feature.**

## 1. Estrutura de Arquivos

```
features/{modulo}/form/
├── {modulo}-form.component.ts
├── {modulo}-form.component.html
└── {modulo}-form.component.css    # Geralmente vazio
```

---

## 2. Componente TypeScript

```typescript
import { Component, OnInit, signal, computed, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { GemsFormCardComponent, GemsAlertService, GemsNavigationService, GemsInputMaskComponent } from '@gabriel-mdias/angular-gems-sdk';
import { {Entidade}Store } from '../services/{entidade}.store';
import { {Entidade}Model } from '../../../models/{entidade}.model';

@Component({
  selector: 'app-{entidade}-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule, GemsFormCardComponent, GemsInputMaskComponent],
  templateUrl: './{entidade}-form.component.html'
})
export class {Entidade}FormComponent implements OnInit {

  private store = inject({Entidade}Store);
  private route = inject(ActivatedRoute);
  private alertService = inject(GemsAlertService);
  private navigationService = inject(GemsNavigationService);
  private fb = inject(FormBuilder);

  // Signals de estado
  isLoading = signal(false);
  isEdit = signal(false);

  // Computed
  pageTitle = computed(() => this.isEdit() ? 'Editar {Entidade}' : 'Nova {Entidade}');
  pageSubtitle = computed(() => this.isEdit() ? 'Atualize os dados.' : 'Preencha os dados abaixo.');

  // FormGroup — construído no construtor, nunca no ngOnInit
  {entidade}Form: FormGroup;

  constructor() {
    this.{entidade}Form = this.fb.group({
      id: [null],
      dataCriacao: [null],
      campoObrigatorio: ['', Validators.required],
      campoOpcional: ['']
    });
  }

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.isEdit.set(true);
      this.load{Entidade}(id);
    }
  }

  load{Entidade}(id: string): void {
    this.isLoading.set(true);
    this.store.getById(id).subscribe({
      next: (data: {Entidade}Model) => {
        this.{entidade}Form.patchValue(data);
        this.isLoading.set(false);
      },
      error: (err) => {
        this.alertService.errorFromApi(err);
        this.isLoading.set(false);
      }
    });
  }

  save(): void {
    if (this.{entidade}Form.invalid) return;
    this.isLoading.set(true);
    const model: {Entidade}Model = this.{entidade}Form.value;

    const call$ = this.isEdit() && model.id
      ? this.store.update(model.id, model)
      : this.store.create(model);

    call$.subscribe({
      next: () => {
        this.alertService.success('Sucesso', `{Entidade} ${this.isEdit() ? 'atualizado(a)' : 'criado(a)'} com sucesso.`);
        this.navigationService.navigate(['/{modulo}/list']);
      },
      error: (err) => {
        this.alertService.errorFromApi(err);
        this.isLoading.set(false);
      }
    });
  }
}
```

---

## 3. Template HTML (Estrutura Obrigatória)

```html
<div class="form-container">

  <!-- HEADER -->
  <div class="header">
    <button class="btn-back" routerLink="/{modulo}/list">
      <i class="fa-solid fa-arrow-left"></i>
    </button>
    <div class="header-titles">
      <h2>{{ pageTitle() }}</h2>
      <p class="subtitle">{{ pageSubtitle() }}</p>
    </div>
  </div>

  <!-- LOADING -->
  @if (isLoading()) {
    <div class="loading-state">
      <i class="fa-solid fa-circle-notch fa-spin"></i> Carregando dados...
    </div>
  } @else {
    <!-- FORMULÁRIO -->
    <form [formGroup]="{entidade}Form" (ngSubmit)="save()">
      <gems-form-card
        title="Dados Gerais"
        subtitle="Informações básicas de identificação."
        icon="fa-solid fa-file-lines">

        <div class="form-grid">

          <div class="form-group full-width">
            <label class="required fw-bold">Campo Obrigatório</label>
            <gems-input-text
              formControlName="campoObrigatorio"
              [required]="true"
              icon="fa-solid fa-pen"
              placeholder="Digite...">
            </gems-input-text>
          </div>

          <div class="form-group">
            <gems-input-mask
              formControlName="telefone"
              label="Telefone"
              maskType="telefone"
              icon="fa-solid fa-phone"
              placeholder="(00) 00000-0000">
            </gems-input-mask>
          </div>

          <div class="form-group">
            <gems-input-data
              formControlName="dataNascimento"
              label="Data de Nascimento"
              inputFormat="diaMesAno"
              placeholder="DD/MM/AAAA">
            </gems-input-data>
          </div>

        </div>

        <div gems-form-card-footer>
          <button type="button" class="btn-cancel" routerLink="/{modulo}/list">
            Cancelar <i class="fa-solid fa-xmark"></i>
          </button>
          <button type="submit" class="btn-save" [disabled]="{entidade}Form.invalid">
            @if (isEdit()) { Atualizar } @else { Salvar }
            <i class="fa-solid fa-floppy-disk"></i>
          </button>
        </div>
      </gems-form-card>
    </form>
  }

</div>
```

---

## 4. Regras do Template

### Header:
- `btn-back` com `routerLink` para a listagem.
- `<h2>` dinâmico via signal computed.

### Form:
- `[formGroup]` no `<form>`, nunca em elementos individuais.
- `(ngSubmit)` para o evento de submit.

### Grid de campos:
- `<div class="form-grid">` como container.
- `<div class="form-group">` = 50% da grid.
- `<div class="form-group full-width">` = 100%.
- `<label class="fw-bold">` em campos padrão.
- `<label class="required fw-bold">` em campos obrigatórios (asterisco via CSS global).

> O `for` do `<label>` fica de fora quando o campo é um componente da SDK: o `id` do `<input>`
> interno é gerado por ele, e um `for` apontando para um `id` inexistente é pior que nenhum — quebra
> o clique no rótulo sem nenhum aviso.

### Footer:
- Dentro de `<div gems-form-card-footer>`.
- `btn-cancel` com `routerLink` para listagem.
- `btn-save` com `[disabled]` no form invalid.
- Texto dinâmico via `@if (isEdit())`.

---

## 5. Componentes de Campo da SDK

**Nenhum `<input>` nativo em formulário de feature** — nem para texto simples. Todo campo é um
componente da SDK, que já traz rótulo, ícone, estado de erro e `ControlValueAccessor`:

```html
<!-- Texto simples — o caso mais comum -->
<gems-input-text formControlName="nome" label="Nome" [required]="true"
  icon="fa-solid fa-user" placeholder="Digite o nome">
</gems-input-text>

<!-- Texto longo -->
<gems-textarea formControlName="observacao" label="Observação" [rows]="4">
</gems-textarea>

<!-- Seleção -->
<gems-select formControlName="status" placeholder="Todos" [options]="statusOptions">
</gems-select>

<!-- Senha: gems-input-password da SDK (>= 2.0.0); a confirmação leva [matchControl] -->
<gems-input-password formControlName="senha" [required]="true"></gems-input-password>
<gems-field-error [control]="form.get('senha')" [messages]="passwordMessages"></gems-field-error>
```

E, para campos formatados:

```html
<!-- E-mail -->
<gems-input-mask formControlName="email" label="E-mail" [required]="true"
  maskType="email" icon="fa-solid fa-envelope" placeholder="contato@email.com">
</gems-input-mask>

<!-- Telefone -->
<gems-input-mask formControlName="telefone" label="Telefone"
  maskType="telefone" icon="fa-solid fa-phone" placeholder="(00) 00000-0000">
</gems-input-mask>

<!-- CEP -->
<gems-input-mask formControlName="cep" label="CEP"
  maskType="cep" icon="fa-solid fa-location-dot" placeholder="00000-000">
</gems-input-mask>

<!-- Moeda -->
<gems-input-mask formControlName="valor" label="Valor"
  maskType="moeda" placeholder="R$ 0,00">
</gems-input-mask>

<!-- CPF / CNPJ (alterna automaticamente) -->
<gems-input-documento
  [numeroDocumento]="{entidade}Form.get('numeroDocumento')?.value"
  (numeroDocumentoChange)="{entidade}Form.get('numeroDocumento')?.setValue($event)"
  [tipoDocumento]="{entidade}Form.get('tipoDocumento')?.value"
  (tipoDocumentoChange)="{entidade}Form.get('tipoDocumento')?.setValue($event)">
</gems-input-documento>

<!-- Data -->
<gems-input-data formControlName="dataNascimento" label="Data de Nascimento"
  inputFormat="diaMesAno" placeholder="DD/MM/AAAA">
</gems-input-data>
```

---

## Anti-Patterns (NUNCA)

| Proibido | Correto |
| :--- | :--- |
| `this.{entidade}Form = this.fb.group({...})` no `ngOnInit` | No construtor, ou como inicializador de campo `protected readonly form = this.fb.group({...})` |
| `constructor(private fb: FormBuilder, private store: ...)` | `private readonly fb = inject(FormBuilder)` + `private readonly store = inject(...)` |
| `*ngIf="isLoading"` | `@if (isLoading())` — signals são funções |
| `<input type="text" class="form-control">` para texto simples | `<gems-input-text>` — nenhum input nativo em formulário de feature |
| `<input type="text" placeholder="000.000.000-00">` para CPF | `<gems-input-documento>` |
| `<input type="text" placeholder="(00) 00000-0000">` para telefone | `<gems-input-mask maskType="telefone">` |
| `<input type="password">` ou cópia local de senha | `<gems-input-password>` da SDK ≥ 2.0.0 — política, critérios e `matchControl` vêm dele |
| `maskType="rg"` | `<gems-input-text>` + `appRgMask` — o `maskType` de RG é proibido |
| `<div form-card-footer>` | `<div gems-form-card-footer>` |
| `<p form-card-subtitle>…</p>` | `subtitle="…"` como input do `gems-form-card` |

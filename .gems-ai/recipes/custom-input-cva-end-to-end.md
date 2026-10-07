---
origem: local
precedencia: local-vence
versao: 2.0.0
---

# Recipe: Input Customizado via ControlValueAccessor

**Objetivo:** Criar um input customizado compatível com `formControlName` e `[(ngModel)]` usando `ControlValueAccessor`, integrado com a SDK.

**Pré-requisitos:**
- Angular 20+ (standalone components, signals)
- `gems-sdk` Angular (gems-field-error para exibir erros)

**Rules relacionadas:**
- [signals-standards.md](../rules/frontend-angular/signals-standards.md)
- [gems-components.md](../rules/frontend-angular/gems-components.md)

---

## Quando Criar um Input Customizado

Crie quando:
- A SDK não oferece o componente (ex: seletor de cor, input de coordenadas).
- Você precisa de lógica de máscara específica do negócio.
- Você quer encapsular um grupo de campos como um único controle de formulário.

**Antes de criar**, verificar: `gems-input-mask`, `gems-input-data`, `gems-input-documento`, `gems-input-checkbox` já cobrem a maioria dos casos.

Senha já está coberta por `gems-input-password` da SDK (≥ 2.0.0): política, critérios pendentes e
`matchControl` (ver `rules/frontend-angular/gems-components.md`). Esta receita não autoriza recriar
input de senha, política ou `passwordMismatch` por feature.

---

## Passo 1: Estrutura do Componente

```typescript
// features/shared/components/{nome-input}/{nome-input}.component.ts
import {
  Component, input, forwardRef, signal, ChangeDetectionStrategy
} from '@angular/core';
import {
  ControlValueAccessor, NG_VALUE_ACCESSOR,
  NG_VALIDATORS, AbstractControl, Validator, ValidationErrors
} from '@angular/forms';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-{nome-input}',
  standalone: true,
  imports: [CommonModule, FormsModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => {NomeInput}Component),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => {NomeInput}Component),
      multi: true   // Remover se o componente não valida internamente
    }
  ],
  template: `
    <div class="form-group">
      @if (label()) {
        <label [class.required]="required()" class="fw-bold">{{ label() }}</label>
      }
      <input
        class="form-control"
        [class.is-invalid]="hasError()"
        [disabled]="disabled()"
        [placeholder]="placeholder()"
        [(ngModel)]="internalValue"
        (ngModelChange)="onInternalChange($event)">
      @if (hasError()) {
        <div class="invalid-feedback">{{ errorMessage() }}</div>
      }
    </div>
  `
})
export class {NomeInput}Component implements ControlValueAccessor, Validator {

  // Inputs declarativos (Angular 20+)
  label       = input<string>('');
  placeholder = input<string>('');
  required    = input<boolean>(false);

  // Estado interno
  internalValue = signal<string>('');
  disabled      = signal<boolean>(false);
  hasError      = signal<boolean>(false);
  errorMessage  = signal<string>('');

  // Callbacks do CVA
  private onChange: (value: string) => void = () => {};
  private onTouched: () => void = () => {};

  // ControlValueAccessor
  writeValue(value: string): void {
    this.internalValue.set(value ?? '');
  }

  registerOnChange(fn: (value: string) => void): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: () => void): void {
    this.onTouched = fn;
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled.set(isDisabled);
  }

  // Validator (implementar somente se o componente valida internamente)
  validate(control: AbstractControl): ValidationErrors | null {
    if (this.required() && !this.internalValue()) {
      this.hasError.set(true);
      this.errorMessage.set(`${this.label() || 'Campo'} é obrigatório.`);
      return { required: true };
    }
    this.hasError.set(false);
    return null;
  }

  onInternalChange(value: string): void {
    const processed = this.processValue(value);    // Aplicar máscara/formatação
    this.internalValue.set(processed);
    this.onChange(processed);
    this.onTouched();
  }

  private processValue(value: string): string {
    // Implementar lógica de máscara aqui
    return value;
  }
}
```

## Passo 2: Usar o Input Customizado

```typescript
// No componente pai:
@Component({
  standalone: true,
  imports: [{NomeInput}Component, ReactiveFormsModule],
  template: `
    <form [formGroup]="form">
      <app-{nome-input}
        formControlName="campo"
        label="Meu Campo"
        placeholder="Digite..."
        [required]="true">
      </app-{nome-input>
    </form>
  `
})
export class FormComponent {
  form = inject(FormBuilder).group({
    campo: ['', Validators.required]
  });
}
```

## Passo 3: Exibir Erros com gems-field-error

Quando o componente **não** implementa `NG_VALIDATORS` internamente:

```html
<app-{nome-input} formControlName="campo" label="Campo"></app-{nome-input}>
<gems-field-error [control]="form.get('campo')"></gems-field-error>
```

---

## Exemplo Completo: Input de Porcentagem

```typescript
@Component({
  selector: 'app-input-percentual',
  standalone: true,
  imports: [CommonModule, FormsModule],
  providers: [{ provide: NG_VALUE_ACCESSOR, useExisting: forwardRef(() => InputPercentualComponent), multi: true }],
  template: `
    <div class="form-group">
      <label class="fw-bold">{{ label() }}</label>
      <div class="input-group">
        <input class="form-control" type="number" min="0" max="100" step="0.01"
               [disabled]="disabled()"
               [value]="internalValue()"
               (input)="onInternalChange(+($event.target as HTMLInputElement).value)">
        <span class="input-group-text">%</span>
      </div>
    </div>
  `
})
export class InputPercentualComponent implements ControlValueAccessor {
  label    = input<string>('Percentual');
  disabled = signal(false);
  internalValue = signal<number>(0);

  private onChange: (v: number) => void = () => {};
  private onTouched: () => void = () => {};

  writeValue(v: number): void     { this.internalValue.set(v ?? 0); }
  registerOnChange(fn: any): void { this.onChange = fn; }
  registerOnTouched(fn: any): void { this.onTouched = fn; }
  setDisabledState(d: boolean): void { this.disabled.set(d); }

  onInternalChange(value: number): void {
    const clamped = Math.min(100, Math.max(0, value));
    this.internalValue.set(clamped);
    this.onChange(clamped);
    this.onTouched();
  }
}
```

---

## Checklist de Conformidade

- [ ] `standalone: true`.
- [ ] `NG_VALUE_ACCESSOR` com `forwardRef` + `multi: true`.
- [ ] Implementa os 4 métodos: `writeValue`, `registerOnChange`, `registerOnTouched`, `setDisabledState`.
- [ ] Estado interno com `signal()`, não propriedades mutáveis simples.
- [ ] `input()` para inputs declarativos (não `@Input()`).
- [ ] `ChangeDetectionStrategy.OnPush` para performance.
- [ ] `NG_VALIDATORS` **somente** se o componente valida internamente (não duplicar com `Validators.required` no FormGroup).
- [ ] `gems-field-error` no componente pai quando erros vêm do FormGroup.

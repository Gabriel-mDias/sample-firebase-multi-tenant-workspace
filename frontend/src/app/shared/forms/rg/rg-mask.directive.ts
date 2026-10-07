import { DestroyRef, Directive, OnInit, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { AbstractControl, NgControl } from '@angular/forms';
import { formatRg } from './rg-format';

/**
 * Aplica a máscara de RG do projeto sobre qualquer componente que implemente
 * `ControlValueAccessor` — na prática, o `gems-input-text`.
 *
 * <p>Não se usa o `gems-input-mask` da SDK para RG: o `maskType="rg"` dela remove letras e
 * trunca em nove dígitos, o que destrói siglas de UF e dígitos verificadores alfabéticos.
 * Ver {@link formatRg}.
 *
 * <p>A reescrita acontece com o evento normal de `valueChanges` (e não silenciada), para que
 * os assinantes do formulário — os `StateService` dos wizards — enxerguem o valor já
 * formatado. Como {@link formatRg} é idempotente, a segunda passagem não altera nada e a
 * recursão termina na primeira comparação.
 */
@Directive({
  selector: '[appRgMask]',
})
export class RgMaskDirective implements OnInit {
  private readonly ngControl = inject(NgControl, { self: true });
  private readonly destroyRef = inject(DestroyRef);

  ngOnInit(): void {
    const control = this.ngControl.control;
    if (!control) {
      return;
    }
    // Formata o que já veio carregado (modo edição) antes de o operador digitar.
    this.aplicar(control);
    control.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.aplicar(control));
  }

  private aplicar(control: AbstractControl): void {
    const atual = control.value as string | null;
    const formatado = formatRg(atual);
    // Campo vazio permanece como está: trocar `null` por `''` só geraria ruído no formulário.
    if (formatado === atual || (!formatado && !atual)) {
      return;
    }
    control.setValue(formatado);
  }
}

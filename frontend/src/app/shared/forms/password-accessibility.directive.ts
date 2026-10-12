import { afterRenderEffect, Directive, ElementRef, inject, input } from '@angular/core';

/**
 * Lacuna observada no core 3.0.0: o botão de visibilidade tem tabindex=-1 e
 * não acompanha disabled do CVA. A diretiva acrescenta acesso por teclado,
 * mantendo o componente, sua política e seu controle de visibilidade publicados.
 */
@Directive({ selector: 'gems-input-password[appPasswordA11y]' })
export class PasswordAccessibilityDirective {
  readonly appPasswordA11y = input(false);
  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef);

  constructor() {
    afterRenderEffect(() => {
      const disabled = this.appPasswordA11y();
      const button = this.host.nativeElement.querySelector<HTMLButtonElement>('button');
      if (button) {
        button.tabIndex = disabled ? -1 : 0;
        button.disabled = disabled;
      }
    });
  }
}

import { Directive, output } from '@angular/core';

/**
 * Dispara a busca quando o operador pressiona <kbd>Enter</kbd> dentro de um formulário de filtro.
 *
 * <p><b>Por que uma diretiva, e não a submissão nativa do formulário.</b> Nas telas do projeto o
 * botão "Buscar" mora no slot <code>[gems-form-card-footer]</code>, que o
 * <code>gems-form-card</code> projeta <em>fora</em> do elemento <code>&lt;form&gt;</code>. Sem
 * botão de <code>submit</code> dentro do formulário e com mais de um campo, o navegador
 * simplesmente não faz a submissão implícita — foi por isso que o
 * <code>(ngSubmit)</code> da listagem de Famílias nunca chegou a rodar. Ouvir o
 * <code>keydown</code> no próprio formulário não depende de onde o botão está.
 *
 * <p>Uso:
 * <pre>
 * &lt;form [formGroup]="filtroForm" appEnterBusca (buscar)="aplicarFiltro()"&gt;
 * </pre>
 *
 * <p>Reservada a <b>filtros e consultas</b>. Formulário que grava entidade continua exigindo o
 * clique no botão: ali o Enter atalha para um efeito que o operador não pediu.
 */
@Directive({
  selector: '[appEnterBusca]',
  host: {
    '(keydown.enter)': 'aoEnter($event)',
  },
})
export class EnterBuscaDirective {
  readonly buscar = output<void>();

  // O parâmetro é `Event`, e não `KeyboardEvent`, porque é assim que o compilador tipa o `$event`
  // de um binding de host. O binding `keydown.enter` só entrega teclado, daí a conversão abaixo.
  protected aoEnter(event: Event): void {
    // Enter em textarea é quebra de linha, não confirmação. Nenhum filtro de hoje tem um, mas a
    // diretiva é compartilhada e o próximo pode ter.
    if (event.target instanceof HTMLTextAreaElement) {
      return;
    }

    // Durante a composição de um IME o Enter confirma o candidato que está sendo digitado; buscar
    // aqui engoliria a tecla que o operador dedicou a terminar a palavra.
    if ((event as KeyboardEvent).isComposing) {
      return;
    }

    // Impede a submissão implícita do navegador — hoje inexistente por falta de botão `submit`
    // dentro do form, mas um botão acrescentado amanhã recarregaria a página.
    event.preventDefault();
    this.buscar.emit();
  }
}

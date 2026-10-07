import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  ElementRef,
  inject,
  input,
  signal,
  viewChild,
} from '@angular/core';

let tooltipSequence = 0;

/** Folga entre o botão de ajuda e a bolha, e margem mínima até a borda da janela. */
const FOLGA = 8;

/**
 * Balão de ajuda de campo.
 *
 * <p>O conteúdo é posicionado com `position: fixed` e coordenadas calculadas em tempo de
 * abertura — e não com `position: absolute` ancorado no host. O motivo é concreto: o
 * `gems-form-card` da SDK aplica `overflow: hidden` no wrapper dele, e todo uso deste
 * componente está dentro de um card. Com posicionamento absoluto o balão é recortado pela
 * borda do card, e `z-index` não resolve — `overflow` recorta antes de qualquer empilhamento.
 */
@Component({
  selector: 'app-field-tooltip',
  templateUrl: './field-tooltip.component.html',
  styleUrl: './field-tooltip.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class FieldTooltipComponent {
  readonly label = input.required<string>();
  readonly text = input.required<string>();
  private readonly botao = viewChild.required<ElementRef<HTMLButtonElement>>('botao');
  private readonly conteudo = viewChild.required<ElementRef<HTMLElement>>('conteudo');
  /** Aberto por clique ou teclado. O ponteiro exibe o balão pelo `:hover` do CSS. */
  protected readonly open = signal(false);
  protected readonly posicao = signal({ top: 0, left: 0 });
  protected readonly tooltipId = `field-tooltip-${++tooltipSequence}`;
  private ouvindoViewport = false;

  constructor() {
    inject(DestroyRef).onDestroy(() => this.pararDeOuvirViewport());
  }

  protected toggle(): void {
    const aberto = !this.open();
    this.open.set(aberto);
    if (aberto) this.posicionar();
    else this.pararDeOuvirViewport();
  }

  protected close(): void {
    this.open.set(false);
    this.pararDeOuvirViewport();
  }

  /**
   * Recalcula as coordenadas. Chamado a cada abertura porque o balão é `fixed`: a posição
   * depende de onde o botão está na janela **agora**, não de onde estava ao renderizar.
   */
  protected posicionar(): void {
    const alvo = this.botao().nativeElement.getBoundingClientRect();
    const caixa = this.conteudo().nativeElement.getBoundingClientRect();

    // Abre para cima; se não couber acima do botão, vira para baixo.
    const cabeAcima = alvo.top >= caixa.height + FOLGA;
    const top = cabeAcima ? alvo.top - caixa.height - FOLGA : alvo.bottom + FOLGA;

    // Centraliza no botão e mantém o balão inteiro dentro da janela.
    const centralizado = alvo.left + alvo.width / 2 - caixa.width / 2;
    const limite = Math.max(window.innerWidth - caixa.width - FOLGA, FOLGA);
    const left = Math.min(Math.max(centralizado, FOLGA), limite);

    this.posicao.set({ top, left });
    this.ouvirViewport();
  }

  private readonly reposicionar = (): void => {
    this.posicionar();
  };

  private ouvirViewport(): void {
    if (this.ouvindoViewport) return;
    this.ouvindoViewport = true;
    // `capture` para alcançar o `.app-content`, que é o contêiner de rolagem real da tela.
    window.addEventListener('scroll', this.reposicionar, { capture: true, passive: true });
    window.addEventListener('resize', this.reposicionar);
  }

  private pararDeOuvirViewport(): void {
    if (!this.ouvindoViewport) return;
    this.ouvindoViewport = false;
    window.removeEventListener('scroll', this.reposicionar, { capture: true });
    window.removeEventListener('resize', this.reposicionar);
  }
}

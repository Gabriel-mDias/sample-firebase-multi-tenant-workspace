import { Component, effect, inject, input, output, signal } from '@angular/core';
import { GemsAlertService, GemsModalComponent } from '@gabriel-mdias/angular-gems-sdk';
import { dataBrOu } from '../../utils/data-br';
import {
  CampoAlteradoModel,
  EntidadeAuditada,
  OPERACAO_ICONE,
  OPERACAO_LABEL,
  ORIGEM_LABEL,
  OperacaoAuditoria,
  RegistroAuditoriaModel,
} from '../auditoria.model';
import { AuditoriaStore } from '../auditoria.store';

/**
 * Quem alterou este registro, o quê, e quando.
 *
 * ## Não é feature, é um modal
 * Não tem rota, não entra no menu, e é chamado das telas de visualização que já existem. A trilha é
 * transversal a todos os domínios — dar-lhe uma tela própria obrigaria o operador a sair do cadastro
 * que ele está olhando para procurar o histórico dele em outro lugar.
 *
 * ## O campo sigiloso mostra "valor não registrado", nunca vazio
 * Quando `sigiloso` é verdadeiro, os dois valores vêm nulos do backend porque **nunca foram
 * gravados** — é a regra do sigilo, e vale para a comissão. Exibir em branco faria parecer que o campo
 * ficou vazio, que é uma informação diferente e falsa. A trilha registra que o campo mudou; o valor
 * é o que ela deliberadamente não sabe.
 */
@Component({
  selector: 'app-historico-registro',
  imports: [GemsModalComponent],
  templateUrl: './historico-registro.component.html',
  styleUrl: './historico-registro.component.css',
})
export class HistoricoRegistroComponent {
  private readonly store = inject(AuditoriaStore);
  private readonly alertService = inject(GemsAlertService);

  readonly open = input(false);
  readonly entidade = input<EntidadeAuditada | null>(null);
  readonly registroId = input<string | null>(null);

  readonly fechar = output<void>();

  protected readonly isLoading = signal(false);
  protected readonly registros = signal<RegistroAuditoriaModel[]>([]);

  constructor() {
    effect(() => {
      const entidade = this.entidade();
      const id = this.registroId();

      if (this.open() && entidade && id) {
        this.carregar(entidade, id);
      }
    });
  }

  protected operacaoLabel(registro: RegistroAuditoriaModel): string {
    return registro.operacaoDescricao || OPERACAO_LABEL[registro.operacao];
  }

  protected operacaoIcone(operacao: OperacaoAuditoria): string {
    return OPERACAO_ICONE[operacao];
  }

  /**
   * O backend devolve ISO (`2026-08-30T10:00:00`). Exibir isso ao operador faria desta a única tela
   * do sistema a fazê-lo.
   */
  protected data(valor: string | null | undefined): string {
    return dataBrOu(valor);
  }

  /**
   * De qual parte do cadastro veio o registro — vazio quando é a própria raiz.
   *
   * Na tela de Parceiro, marcar um registro como "Parceiro" não informa nada; marcar como "Contato"
   * informa tudo. O rótulo só aparece quando acrescenta.
   */
  protected origem(registro: RegistroAuditoriaModel): string {
    if (registro.entidade === this.entidade()) {
      return '';
    }

    return ORIGEM_LABEL[registro.entidade] ?? registro.entidade;
  }

  /** Vazio e sigiloso são coisas diferentes, e a tela precisa distingui-las. */
  protected valor(campo: CampoAlteradoModel, valor: string | null): string {
    if (campo.sigiloso) {
      return 'valor não registrado';
    }

    return valor === null || valor === '' ? '—' : valor;
  }

  protected cancelar(): void {
    this.fechar.emit();
  }

  private carregar(entidade: EntidadeAuditada, id: string): void {
    this.isLoading.set(true);
    // Limpa antes de buscar: sem isto, a abertura seguinte mostraria o histórico do registro
    // anterior enquanto a requisição não volta.
    this.registros.set([]);

    this.store.historico(entidade, id).subscribe({
      next: (pagina) => {
        this.registros.set(pagina.content);
        this.isLoading.set(false);
      },
      error: (err) => {
        this.alertService.errorFromApi(err);
        this.isLoading.set(false);
      },
    });
  }
}

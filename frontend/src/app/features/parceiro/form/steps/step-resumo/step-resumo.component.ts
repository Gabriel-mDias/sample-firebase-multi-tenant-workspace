import { Component, computed, inject, input, output } from '@angular/core';
import { GemsFormCardComponent } from '@gabriel-mdias/angular-gems-sdk';
import { sexoLabel } from '../../../../../shared/forms/dados-pessoais/dados-pessoais-form';
import { ParceiroFormStateService } from '../../parceiro-state.service';
import { STEP_ENDERECO } from '../../parceiro.config';

/**
 * Passo final — confere o que será gravado e confirma.
 *
 * <p>Lê **só** o estado do wizard; não faz requisição nenhuma. É por isso que os passos anteriores
 * guardam o rótulo ao lado do id: buscar o nome de um país aqui seria uma consulta que o estado já
 * podia ter carregado.
 */
@Component({
  selector: 'app-step-resumo',
  imports: [GemsFormCardComponent],
  templateUrl: './step-resumo.component.html',
  styleUrl: './step-resumo.component.css',
})
export class StepResumoComponent {
  protected readonly state = inject(ParceiroFormStateService);

  readonly isLoading = input(false);
  readonly confirmar = output<void>();
  readonly sair = output<void>();

  protected readonly isReadOnly = this.state.isReadOnly;
  protected readonly dados = computed(() => this.state.snapshot());

  protected readonly sexoDescricao = computed(() => sexoLabel(this.dados().pessoa?.sexo as never));

  protected readonly enderecoLinha = computed(() => {
    const e = this.dados().endereco;
    if (!e) {
      return '';
    }
    const numero = e.numero ? `, ${e.numero}` : '';
    const uf = e.estadoNome ? ` — ${e.estadoNome}` : '';
    return `${e.logradouro ?? ''}${numero}, ${e.bairro ?? ''}, ${e.cidade ?? ''}${uf}`;
  });

  protected voltar(): void {
    this.state.goToStep(STEP_ENDERECO);
  }
}

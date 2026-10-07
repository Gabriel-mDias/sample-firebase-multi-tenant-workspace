import { Component, OnDestroy, OnInit, computed, inject, signal } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import {
  GemsAlertService,
  GemsNavigationService,
  GemsWizardComponent,
} from '@gabriel-mdias/angular-gems-sdk';
import { Acoes, globalAuthorization } from '../../../core/auth/acoes';
import { AuthService } from '../../../core/auth/auth.service';
import { BotaoHistoricoComponent } from '../../../shared/auditoria/botao-historico/botao-historico.component';
import { ParceiroFormMode, ParceiroFormStateService } from './parceiro-state.service';
import { ParceiroStore } from '../services/parceiro.store';
import { PARCEIRO_FORM_STEPS, STEP_CONTATO, STEP_DADOS, STEP_ENDERECO } from './parceiro.config';
import { StepContatoComponent } from './steps/step-contato/step-contato.component';
import { StepDadosComponent } from './steps/step-dados/step-dados.component';
import { StepEnderecoComponent } from './steps/step-endereco/step-endereco.component';
import { StepResumoComponent } from './steps/step-resumo/step-resumo.component';

/**
 * Orquestrador do wizard de parceiro. Atende os três modos — criar, editar e visualizar — porque
 * os campos são exatamente os mesmos; o que muda é se os formulários estão habilitados e se o
 * passo de resumo oferece confirmação.
 *
 * `gems-wizard` é só a barra de progresso: quem renderiza o passo ativo é o `@switch` do template
 * daqui. O `ParceiroFormStateService` é fornecido neste nível (não em `root`) para que cada
 * abertura do formulário comece com estado limpo.
 */
@Component({
  selector: 'app-parceiro-form',
  imports: [
    BotaoHistoricoComponent,
    GemsWizardComponent,
    StepDadosComponent,
    StepContatoComponent,
    StepEnderecoComponent,
    StepResumoComponent,
  ],
  providers: [ParceiroFormStateService],
  templateUrl: './parceiro-form.component.html',
  styleUrl: './parceiro-form.component.css',
})
export class ParceiroFormComponent implements OnInit, OnDestroy {
  private readonly route = inject(ActivatedRoute);
  private readonly navigationService = inject(GemsNavigationService);
  private readonly store = inject(ParceiroStore);
  private readonly auth = inject(AuthService);
  private readonly alertService = inject(GemsAlertService);
  protected readonly state = inject(ParceiroFormStateService);

  protected readonly steps = PARCEIRO_FORM_STEPS;
  protected readonly stepDados = STEP_DADOS;
  protected readonly stepContato = STEP_CONTATO;
  protected readonly stepEndereco = STEP_ENDERECO;

  /** Alimenta o botão de histórico da trilha de auditoria. */
  protected readonly registroId = signal<string | null>(null);

  protected readonly isLoading = signal(false);
  protected readonly isCarregando = signal(false);

  protected readonly titulo = computed(() => {
    switch (this.state.mode()) {
      case 'edit':
        return 'Editar parceiro';
      case 'view':
        return 'Visualizar parceiro';
      default:
        return 'Novo parceiro';
    }
  });

  protected readonly descricao = computed(() => {
    switch (this.state.mode()) {
      case 'edit':
        return 'Altere os dados e confirme no resumo para gravar.';
      case 'view':
        return 'Consulta somente leitura — nenhum campo é editável.';
      default:
        return 'Siga os passos para cadastrar um novo parceiro.';
    }
  });

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    this.registroId.set(id);
    const modo = (this.route.snapshot.data['mode'] as ParceiroFormMode | undefined) ?? 'create';

    this.state.mode.set(modo);

    if (id) {
      this.carregar(id);
    } else {
      // Numa criação não há o que carregar, mas o estado precisa saber desde já se a comissão é
      // visível — o passo de resumo e o toPayload leem essa marca, não a consultam.
      this.state.updateState({ podeVerComissao: this.podeVerComissao() });
    }
  }

  ngOnDestroy(): void {
    this.state.clearState();
  }

  /** O wizard só emite ao clicar num passo já concluído — voltar é sempre permitido. */
  protected onStepChange(step: number): void {
    this.state.goToStep(step);
  }

  /** Seta de retorno do cabeçalho e botão Cancelar levam ao mesmo lugar: a listagem. */
  protected sair(): void {
    this.navigationService.navigate(['/parceiros/list']);
  }

  protected confirmar(): void {
    const payload = this.state.toPayload();
    const id = payload.id;

    this.isLoading.set(true);

    const requisicao = id ? this.store.update(id, payload) : this.store.create(payload);

    requisicao.subscribe({
      next: () => {
        this.isLoading.set(false);
        this.alertService.success(
          id ? 'Parceiro atualizado' : 'Parceiro cadastrado',
          'Os dados foram gravados com sucesso.',
        );
        this.state.clearState();
        this.navigationService.navigate(['/parceiros/list']);
      },
      error: (err) => {
        this.alertService.errorFromApi(err);
        this.isLoading.set(false);
      },
    });
  }

  private podeVerComissao(): boolean {
    return this.auth.isAuthorized(globalAuthorization(Acoes.VISUALIZAR_COMISSAO));
  }

  private carregar(id: string): void {
    this.isCarregando.set(true);

    this.store.getById(id).subscribe({
      next: (parceiro) => {
        this.state.loadFrom(parceiro, this.podeVerComissao());
        this.isCarregando.set(false);
      },
      error: (err) => {
        this.alertService.errorFromApi(err);
        this.isCarregando.set(false);
        this.navigationService.navigate(['/parceiros/list']);
      },
    });
  }
}

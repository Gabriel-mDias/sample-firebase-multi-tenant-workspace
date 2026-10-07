import { Component, inject } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import {
  GemsBaseStepComponent,
  GemsFieldErrorComponent,
  GemsFormCardComponent,
  GemsInputTextComponent,
  GemsNavigationService,
  GemsTextareaComponent,
} from '@gabriel-mdias/angular-gems-sdk';
import { Acoes, globalAuthorization } from '../../../../../core/auth/acoes';
import { AuthService } from '../../../../../core/auth/auth.service';
import { DadosPessoaisFieldsComponent } from '../../../../../shared/forms/dados-pessoais/dados-pessoais-fields.component';
import { createDadosPessoaisForm } from '../../../../../shared/forms/dados-pessoais/dados-pessoais-form';
import { ParceiroFormStateService } from '../../parceiro-state.service';
import { STEP_CONTATO, STEP_DADOS } from '../../parceiro.config';

interface StepDadosPayload {
  nome: string;
  descricao: string;
  comissao: number | null;
  primeiroNome: string;
  sobrenome: string;
  cpf: string;
  rg: string;
  dataNascimento: string;
  sexo: string;
}

/** Passo 1 — identificação do parceiro e identidade civil. */
@Component({
  selector: 'app-step-dados',
  imports: [
    ReactiveFormsModule,
    GemsFormCardComponent,
    GemsInputTextComponent,
    GemsTextareaComponent,
    DadosPessoaisFieldsComponent,
    GemsFieldErrorComponent,
  ],
  templateUrl: './step-dados.component.html',
  styleUrl: './step-dados.component.css',
})
export class StepDadosComponent extends GemsBaseStepComponent<StepDadosPayload> {
  private readonly fb = inject(FormBuilder);
  private readonly navigationService = inject(GemsNavigationService);
  private readonly auth = inject(AuthService);
  protected readonly state = inject(ParceiroFormStateService);

  protected readonly isReadOnly = this.state.isReadOnly;

  /**
   * Espelha a ação concreta do backend, e é a **única** coisa que decide se o campo aparece.
   *
   * Esconder o campo é conveniência de tela, não segurança: a API já devolve `null` para quem não
   * tem a ação, e ignora o campo na gravação. Se esta linha sumisse, o pior que aconteceria seria
   * exibir um campo permanentemente vazio — nunca vazamento.
   */
  protected readonly podeVerComissao = this.auth.isAuthorized(
    globalAuthorization(Acoes.VISUALIZAR_COMISSAO),
  );

  protected readonly erroNome = { required: 'O nome do parceiro é obrigatório.' };
  protected readonly erroComissao = { min: 'A comissão não pode ser negativa.' };

  /** Grupo aninhado da identidade civil, entregue ao componente compartilhado. */
  protected get dadosPessoais(): FormGroup {
    return this.formGroup.get('pessoa') as FormGroup;
  }

  override formGroup!: FormGroup;
  override stepIndex = STEP_DADOS;

  override createForm(): void {
    this.formGroup = this.fb.group({
      nome: ['', [Validators.required, Validators.maxLength(255)]],
      descricao: ['', [Validators.maxLength(4000)]],
      comissao: [null as number | null, [Validators.min(0)]],
      // A estrutura da identidade civil vem do shared: um dono só, para que os cadastros que
      // gravam a MESMA Pessoa no backend não divirjam entre si.
      pessoa: createDadosPessoaisForm(this.fb, { cpfObrigatorio: true }),
    });

    if (this.isReadOnly()) {
      this.formGroup.disable();
    }
  }

  override loadFromSession(): void {
    const atual = this.state.snapshot();
    this.formGroup.patchValue({
      nome: atual.nome ?? '',
      descricao: atual.descricao ?? '',
      comissao: atual.comissao ?? null,
      pessoa: {
        primeiroNome: atual.pessoa?.primeiroNome ?? '',
        sobrenome: atual.pessoa?.sobrenome ?? '',
        cpf: atual.pessoa?.cpf ?? '',
        rg: atual.pessoa?.rg ?? '',
        dataNascimento: atual.pessoa?.dataNascimento ?? '',
        sexo: atual.pessoa?.sexo ?? null,
      },
    });
  }

  override saveToSession(): void {
    const valor = this.formGroup.getRawValue();

    this.state.updateState({
      nome: valor.nome,
      descricao: valor.descricao,
      comissao: valor.comissao,
      // Guardado aqui, e não recalculado a cada leitura: o resumo e o `toPayload` precisam decidir
      // pelo mesmo valor, ou o payload sairia diferente do que a tela mostrou.
      podeVerComissao: this.podeVerComissao,
      pessoa: {
        id: this.state.snapshot().pessoa?.id,
        primeiroNome: valor.pessoa?.primeiroNome,
        sobrenome: valor.pessoa?.sobrenome,
        cpf: valor.pessoa?.cpf,
        rg: valor.pessoa?.rg,
        dataNascimento: valor.pessoa?.dataNascimento,
        sexo: valor.pessoa?.sexo,
      },
    });
  }

  override goNext(): void {
    this.state.goToStep(STEP_CONTATO);
  }

  /** Primeiro passo: "voltar" é sair do formulário. */
  override goBack(): void {
    this.navigationService.navigate(['/parceiros/list']);
  }

  /** Em leitura não há o que validar nem salvar — apenas navega. */
  protected avancar(): void {
    if (this.isReadOnly()) {
      this.goNext();
      return;
    }
    this.saveAndNext();
  }
}

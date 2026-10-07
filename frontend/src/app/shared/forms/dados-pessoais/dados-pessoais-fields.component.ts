import { Component, input, output } from '@angular/core';
import { FormGroup, ReactiveFormsModule } from '@angular/forms';
import {
  GemsInputDateComponent,
  GemsInputDocumentComponent,
  GemsInputTextComponent,
  GemsFieldErrorComponent,
  GemsSelectComponent,
  GemsSelectOption,
} from '@gabriel-mdias/angular-gems-sdk';
import {
  ERRO_CPF,
  ERRO_DATA_NASCIMENTO,
  ERRO_PRIMEIRO_NOME,
  ERRO_SEXO,
  ERRO_SOBRENOME,
  SEXO_OPTIONS,
  Sexo,
} from './dados-pessoais-form';
import { EnterBuscaDirective } from '../enter-busca/enter-busca.directive';
import { RgMaskDirective } from '../rg/rg-mask.directive';

/** Campos civis comuns. Regras de busca e de bloqueio permanecem no adaptador de cada feature. */
@Component({
  selector: 'app-dados-pessoais-fields',
  imports: [
    ReactiveFormsModule,
    GemsInputTextComponent,
    GemsInputDocumentComponent,
    GemsInputDateComponent,
    GemsSelectComponent,
    RgMaskDirective,
    EnterBuscaDirective,

    GemsFieldErrorComponent,
  ],
  templateUrl: './dados-pessoais-fields.component.html',
  styleUrl: './dados-pessoais-fields.component.css',
})
export class DadosPessoaisFieldsComponent {
  readonly form = input.required<FormGroup>();
  readonly isReadOnly = input(false);
  readonly isSearching = input(false);
  readonly cpfErrors = input<Record<string, string>>(ERRO_CPF);

  /** Ver `DadosPessoaisFormOpcoes`: os dois acompanham o que a factory montou no `FormGroup`. */
  readonly cpfObrigatorio = input(true);
  readonly comDataNascimento = input(true);

  /** Texto auxiliar sob o CPF. O `gems-input-document` não tem `hint`, então vai como parágrafo. */
  readonly cpfHint = input('');

  readonly buscarCpf = output<void>();

  protected readonly erroPrimeiroNome = ERRO_PRIMEIRO_NOME;
  protected readonly erroSobrenome = ERRO_SOBRENOME;
  protected readonly erroDataNascimento = ERRO_DATA_NASCIMENTO;
  protected readonly erroSexo = ERRO_SEXO;
  protected readonly sexoOptions = SEXO_OPTIONS as GemsSelectOption<Sexo>[];

  /**
   * Enter no campo de CPF equivale ao clique em "Buscar" — a consulta apenas preenche o
   * formulário com a Pessoa já cadastrada, não grava nada.
   *
   * <p>As condições repetem as do botão de propósito: ele some em modo leitura e fica desabilitado
   * durante a consulta. Um atalho que dispare onde o botão não dispararia é pior que atalho nenhum.
   */
  protected buscarPorEnter(): void {
    if (this.isReadOnly() || this.isSearching()) {
      return;
    }
    this.buscarCpf.emit();
  }
}

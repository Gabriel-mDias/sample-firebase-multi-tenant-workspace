import { Component, input } from '@angular/core';
import { FormGroup, ReactiveFormsModule } from '@angular/forms';
import { GemsFieldErrorComponent, GemsInputTextComponent } from '@gabriel-mdias/angular-gems-sdk';
import { ERRO_EMAIL, ERRO_TELEFONE } from './contato-form';

/**
 * Campos de contato comuns, independentes do papel exercido pela Pessoa.
 *
 * <p>Os três textos de apoio são parametrizáveis porque o exemplo útil muda com o titular — o
 * telefone de um parceiro no exterior e o WhatsApp de um parceiro não se parecem. A **estrutura**,
 * os validadores e as mensagens de erro não mudam, e é por isso que o componente é um só: até a
 * o domínio de referência é justamente quem
 * redefinia interface, `FormGroup` e mensagens que já existiam aqui.
 */
@Component({
  selector: 'app-contato-fields',
  imports: [ReactiveFormsModule, GemsInputTextComponent, GemsFieldErrorComponent],
  templateUrl: './contato-fields.component.html',
})
export class ContatoFieldsComponent {
  readonly form = input.required<FormGroup>();

  readonly placeholderEmail = input('contato@exemplo.com');
  readonly placeholderTelefone = input('+55 27 99999-8888');
  readonly hintTelefone = input('Aceita código internacional (DDI).');

  protected readonly erroEmail = ERRO_EMAIL;
  protected readonly erroTelefone = ERRO_TELEFONE;
}

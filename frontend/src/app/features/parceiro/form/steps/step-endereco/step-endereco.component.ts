import { Component, inject, signal } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule } from '@angular/forms';
import {
  GemsAlertService,
  GemsBaseStepComponent,
  GemsFieldErrorComponent,
  GemsFormCardComponent,
  GemsInputMaskComponent,
  GemsInputTextComponent,
  GemsSelectComponent,
  GemsSelectOption,
} from '@gabriel-mdias/angular-gems-sdk';
import { LocalidadeStore } from '../../../../../core/localidade/localidade.store';
import {
  ERRO_BAIRRO,
  ERRO_CEP,
  ERRO_CIDADE,
  ERRO_ESTADO,
  ERRO_LOGRADOURO,
  ERRO_PAIS,
  aplicarObrigatoriedadeDeUfECep,
  createEnderecoForm,
} from '../../../../../shared/forms/endereco/endereco-form';
import { ParceiroFormStateService } from '../../parceiro-state.service';
import { STEP_CONTATO, STEP_ENDERECO, STEP_RESUMO } from '../../parceiro.config';

/**
 * Passo 3 — endereço do parceiro.
 *
 * <p>O país é escolha do operador: um parceiro pode residir fora do Brasil, e é por isso que
 * `estado` e `cep` são condicionais em vez de obrigatórios. Um cadastro que derive o país do estado
 * torna impossível o endereço estrangeiro sem que ninguém perceba — o campo simplesmente não tem
 * como ser preenchido.
 */
@Component({
  selector: 'app-step-endereco',
  imports: [
    ReactiveFormsModule,
    GemsFormCardComponent,
    GemsInputTextComponent,
    GemsInputMaskComponent,
    GemsSelectComponent,
    GemsFieldErrorComponent,
  ],
  templateUrl: './step-endereco.component.html',
  styleUrl: './step-endereco.component.css',
})
export class StepEnderecoComponent extends GemsBaseStepComponent<Record<string, string>> {
  private readonly fb = inject(FormBuilder);
  private readonly localidadeStore = inject(LocalidadeStore);
  private readonly alertService = inject(GemsAlertService);
  protected readonly state = inject(ParceiroFormStateService);

  protected readonly isReadOnly = this.state.isReadOnly;
  protected readonly paises = signal<GemsSelectOption<string>[]>([]);
  protected readonly estados = signal<GemsSelectOption<string>[]>([]);

  /** `true` quando o país escolhido exige UF e CEP. Controla o que o template mostra. */
  protected readonly exigeUfECep = signal(true);

  protected readonly erroPais = ERRO_PAIS;
  protected readonly erroEstado = ERRO_ESTADO;
  protected readonly erroCep = ERRO_CEP;
  protected readonly erroCidade = ERRO_CIDADE;
  protected readonly erroBairro = ERRO_BAIRRO;
  protected readonly erroLogradouro = ERRO_LOGRADOURO;

  override formGroup!: FormGroup;
  override stepIndex = STEP_ENDERECO;

  override createForm(): void {
    this.formGroup = createEnderecoForm(this.fb, { comPais: true });

    if (this.isReadOnly()) {
      this.formGroup.disable();
    }

    this.carregarPaises();

    this.formGroup.get('paisId')?.valueChanges.subscribe((paisId: string | null) => {
      const nome = this.paises().find((p) => p.value === paisId)?.label;
      this.exigeUfECep.set(aplicarObrigatoriedadeDeUfECep(this.formGroup, nome));
      this.carregarEstados(paisId);
    });
  }

  override loadFromSession(): void {
    const endereco = this.state.snapshot().endereco;

    this.formGroup.patchValue({
      paisId: endereco?.paisId ?? null,
      estadoId: endereco?.estadoId ?? null,
      cep: endereco?.cep ?? '',
      cidade: endereco?.cidade ?? '',
      bairro: endereco?.bairro ?? '',
      logradouro: endereco?.logradouro ?? '',
      numero: endereco?.numero ?? '',
      complemento: endereco?.complemento ?? '',
    });
  }

  override saveToSession(): void {
    const valor = this.formGroup.getRawValue();

    this.state.updateState({
      endereco: {
        id: this.state.snapshot().endereco?.id,
        paisId: valor.paisId,
        paisNome: this.paises().find((p) => p.value === valor.paisId)?.label,
        estadoId: valor.estadoId || undefined,
        estadoNome: this.estados().find((e) => e.value === valor.estadoId)?.label,
        cep: valor.cep,
        cidade: valor.cidade,
        bairro: valor.bairro,
        logradouro: valor.logradouro,
        numero: valor.numero,
        complemento: valor.complemento,
      },
    });
  }

  override goNext(): void {
    this.state.goToStep(STEP_RESUMO);
  }

  override goBack(): void {
    this.state.goToStep(STEP_CONTATO);
  }

  protected avancar(): void {
    if (this.isReadOnly()) {
      this.goNext();
      return;
    }
    this.saveAndNext();
  }

  private carregarPaises(): void {
    this.localidadeStore.listarPaises().subscribe({
      next: (paises) => this.paises.set(paises.map((p) => ({ label: p.nome ?? '', value: p.id }))),
      error: (err) => this.alertService.errorFromApi(err),
    });
  }

  /** Estados são do país escolhido: trocar o país zera a lista antes de buscar a nova. */
  private carregarEstados(paisId: string | null): void {
    this.estados.set([]);

    if (!paisId) {
      return;
    }

    this.localidadeStore.listarEstados(paisId).subscribe({
      next: (estados) =>
        this.estados.set(
          estados.map((e) => ({
            label: e.siglaEstado ? `${e.nome} (${e.siglaEstado})` : (e.nome ?? ''),
            value: e.id,
          })),
        ),
      error: (err) => this.alertService.errorFromApi(err),
    });
  }
}

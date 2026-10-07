import { Component, inject } from '@angular/core';
import { FormBuilder, FormGroup } from '@angular/forms';
import { GemsBaseStepComponent, GemsFormCardComponent } from '@gabriel-mdias/angular-gems-sdk';
import { ContatoFieldsComponent } from '../../../../../shared/forms/contato/contato-fields.component';
import { createContatoForm } from '../../../../../shared/forms/contato/contato-form';
import { ParceiroFormStateService } from '../../parceiro-state.service';
import { STEP_CONTATO, STEP_DADOS, STEP_ENDERECO } from '../../parceiro.config';

interface StepContatoPayload {
  email: string;
  telefone: string;
}

/** Passo 2 — canal de contato do parceiro. */
@Component({
  selector: 'app-step-contato',
  imports: [GemsFormCardComponent, ContatoFieldsComponent],
  templateUrl: './step-contato.component.html',
  styleUrl: './step-contato.component.css',
})
export class StepContatoComponent extends GemsBaseStepComponent<StepContatoPayload> {
  private readonly fb = inject(FormBuilder);
  protected readonly state = inject(ParceiroFormStateService);

  protected readonly isReadOnly = this.state.isReadOnly;

  override formGroup!: FormGroup;
  override stepIndex = STEP_CONTATO;

  override createForm(): void {
    // Estrutura e validadores vêm do shared. O telefone é texto livre, sem máscara: a máscara
    // `phone` da SDK trunca em 11 dígitos e descarta o `+`, o que impediria cadastrar o telefone
    // de um parceiro no exterior — contra o requisito de aceitar DDI (FR-003).
    this.formGroup = createContatoForm(this.fb);

    if (this.isReadOnly()) {
      this.formGroup.disable();
    }
  }

  override loadFromSession(): void {
    const contato = this.state.snapshot().contato;
    this.formGroup.patchValue({
      email: contato?.email ?? '',
      telefone: contato?.telefone ?? '',
    });
  }

  override saveToSession(payload: StepContatoPayload): void {
    this.state.updateState({
      contato: {
        id: this.state.snapshot().contato?.id,
        email: payload.email,
        telefone: payload.telefone,
      },
    });
  }

  override goNext(): void {
    this.state.goToStep(STEP_ENDERECO);
  }

  override goBack(): void {
    this.state.goToStep(STEP_DADOS);
  }

  protected avancar(): void {
    if (this.isReadOnly()) {
      this.goNext();
      return;
    }
    this.saveAndNext();
  }
}

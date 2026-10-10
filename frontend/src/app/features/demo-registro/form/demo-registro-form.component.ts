import { Component, computed, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import {
  GemsAlertService,
  GemsFieldErrorComponent,
  GemsFormCardComponent,
  GemsInputTextComponent,
  GemsTextareaComponent,
  GemsNavigationService,
} from '@gabriel-mdias/angular-gems-sdk';
import { finalize } from 'rxjs';
import { AuthService } from '../../../core/auth/auth.service';
import { Acoes } from '../../../core/auth/acoes';
import { DemoRegistroStore } from '../services/demo-registro.store';
import { DemoRegistro } from '../models/demo-registro.model';

@Component({
  selector: 'app-demo-registro-form',
  imports: [
    ReactiveFormsModule,
    GemsFieldErrorComponent,
    GemsFormCardComponent,
    GemsInputTextComponent,
    GemsTextareaComponent,
  ],
  templateUrl: './demo-registro-form.component.html',
  styleUrl: './demo-registro-form.component.css',
})
export class DemoRegistroFormComponent {
  private readonly store = inject(DemoRegistroStore);
  private readonly route = inject(ActivatedRoute);
  private readonly navigation = inject(GemsNavigationService);
  private readonly alert = inject(GemsAlertService);
  private readonly destroy = inject(DestroyRef);
  private readonly auth = inject(AuthService);
  protected readonly id = this.route.snapshot.paramMap.get('id');
  readonly form = inject(FormBuilder).nonNullable.group({
    nome: ['', [Validators.required, Validators.maxLength(150)]],
    descricao: ['', Validators.maxLength(1000)],
    valorRestrito: [''],
  });
  protected readonly loading = signal(false);
  protected readonly restricted = computed(() =>
    this.auth.isAuthorized([Acoes.VISUALIZAR_VALOR_RESTRITO], 'tenant'),
  );
  protected readonly readonly = this.route.snapshot.url[0]?.path === 'view';
  protected readonly canSave = computed(
    () =>
      !this.readonly &&
      this.auth.isAuthorized([this.id ? Acoes.ALTERAR_DEMO : Acoes.INSERIR_DEMO], 'tenant'),
  );

  constructor() {
    this.auth.invalidated.pipe(takeUntilDestroyed(this.destroy)).subscribe(() => {
      this.form.reset();
      this.loading.set(false);
    });
    if (this.readonly) this.form.disable();
    if (this.id) {
      const snapshot = this.auth.capture();
      this.loading.set(true);
      this.store
        .getById(this.id)
        .pipe(
          takeUntilDestroyed(this.destroy),
          finalize(() => {
            if (this.auth.isCurrent(snapshot)) this.loading.set(false);
          }),
        )
        .subscribe({
          next: (item) => {
            if (this.auth.isCurrent(snapshot))
              this.form.patchValue({
                nome: item.nome,
                descricao: item.descricao ?? '',
                valorRestrito:
                  this.restricted() && item.valorRestrito != null ? item.valorRestrito : '',
              });
          },
          error: () => {
            if (this.auth.isCurrent(snapshot))
              void this.alert.error('Não foi possível abrir o registro.');
          },
        });
    }
  }

  protected save(): void {
    if (this.loading() || !this.canSave()) return;
    this.form.markAllAsTouched();
    if (this.form.invalid) return;
    const snapshot = this.auth.capture();
    const value = this.form.getRawValue();
    let decimal: string | undefined;
    if (this.restricted() && value.valorRestrito.trim()) {
      const match = /^([+-]?)(\d+)(?:\.(\d{1,2}))?$/.exec(value.valorRestrito.trim());
      const integer = match?.[2].replace(/^0+(?=\d)/, '');
      if (!match || !integer || integer.length > 17) {
        this.form.controls.valorRestrito.setErrors({ decimal: true });
        return;
      }
      const fraction = (match[3] ?? '').padEnd(2, '0');
      const sign = match[1] === '-' && !/^0+$/.test(integer + fraction) ? '-' : '';
      decimal = `${sign}${integer}.${fraction}`;
    }
    const body: DemoRegistro = {
      ...(this.id ? { id: this.id } : {}),
      nome: value.nome.trim(),
      descricao: value.descricao,
      ...(this.restricted() ? { valorRestrito: decimal } : {}),
    };
    this.loading.set(true);
    const operation = this.id ? this.store.update(this.id, body) : this.store.create(body);
    operation
      .pipe(
        takeUntilDestroyed(this.destroy),
        finalize(() => {
          if (this.auth.isCurrent(snapshot)) this.loading.set(false);
        }),
      )
      .subscribe({
        next: () => {
          if (this.auth.isCurrent(snapshot)) this.back();
        },
        error: () => {
          if (this.auth.isCurrent(snapshot))
            void this.alert.error(
              'Não foi possível salvar o registro.',
              'Confira os dados e tente novamente.',
            );
        },
      });
  }

  protected back(): void {
    this.navigation.navigate(['/demo-registro/list']);
  }
}

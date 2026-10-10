import { Component, computed, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import {
  GemsAlertService,
  GemsFormCardComponent,
  GemsInputTextComponent,
  GemsNavigationService,
  GemsTableAction,
  GemsTableColumn,
  GemsTableComponent,
  GemsTableRow,
} from '@gabriel-mdias/angular-gems-sdk';
import { finalize } from 'rxjs';
import { AuthService } from '../../../core/auth/auth.service';
import { Acoes } from '../../../core/auth/acoes';
import { DemoRegistroStore } from '../services/demo-registro.store';
import { DemoRegistro } from '../models/demo-registro.model';

@Component({
  selector: 'app-demo-registro-list',
  imports: [ReactiveFormsModule, GemsInputTextComponent, GemsTableComponent, GemsFormCardComponent],
  templateUrl: './demo-registro-list.component.html',
  styleUrl: './demo-registro-list.component.css',
})
export class DemoRegistroListComponent {
  private readonly store = inject(DemoRegistroStore);
  protected readonly auth = inject(AuthService);
  private readonly alert = inject(GemsAlertService);
  private readonly navigation = inject(GemsNavigationService);
  private readonly destroy = inject(DestroyRef);
  protected readonly filter = inject(FormBuilder).nonNullable.group({ nome: '' });
  protected readonly rows = signal<GemsTableRow[]>([]);
  protected readonly loading = signal(false);
  protected readonly total = signal(0);
  protected readonly page = signal(0);
  protected readonly canCreate = computed(() =>
    this.auth.isAuthorized([Acoes.INSERIR_DEMO], 'tenant'),
  );
  protected readonly columns = computed<GemsTableColumn[]>(() => [
    { field: 'nome', header: 'Nome', sortable: false },
    { field: 'descricao', header: 'Descrição', sortable: false },
    ...(this.auth.isAuthorized([Acoes.VISUALIZAR_VALOR_RESTRITO], 'tenant')
      ? [{ field: 'valorRestrito', header: 'Valor restrito', sortable: false }]
      : []),
    { field: 'acoes', header: 'Ações', type: 'actions' },
  ]);
  protected readonly actions = computed<GemsTableAction[]>(() => [
    { icon: 'fa-solid fa-eye', tooltip: 'Visualizar', actionName: 'view' },
    ...(this.auth.isAuthorized([Acoes.ALTERAR_DEMO], 'tenant')
      ? [{ icon: 'fa-solid fa-pen', tooltip: 'Editar', actionName: 'edit' }]
      : []),
    ...(this.auth.isAuthorized([Acoes.EXCLUIR_DEMO], 'tenant')
      ? [{ icon: 'fa-solid fa-trash', tooltip: 'Excluir', actionName: 'delete' }]
      : []),
  ]);

  constructor() {
    this.auth.invalidated.pipe(takeUntilDestroyed(this.destroy)).subscribe(() => {
      this.rows.set([]);
      this.filter.reset();
      this.total.set(0);
    });
    this.search();
  }

  protected search(page = 0): void {
    if (this.loading() || !this.auth.isAuthorized([Acoes.CONSULTAR_DEMO], 'tenant')) return;
    const snapshot = this.auth.capture();
    this.loading.set(true);
    this.store
      .search(this.filter.getRawValue(), { page, size: 10, sort: ['nome,asc'] })
      .pipe(
        takeUntilDestroyed(this.destroy),
        finalize(() => {
          if (this.auth.isCurrent(snapshot)) this.loading.set(false);
        }),
      )
      .subscribe({
        next: (result) => {
          if (this.auth.isCurrent(snapshot)) {
            this.rows.set(result.content.map((item) => ({ ...item })));
            this.total.set(result.totalElements);
            this.page.set(result.number);
          }
        },
        error: () => {
          if (this.auth.isCurrent(snapshot))
            void this.alert.error('Não foi possível carregar os registros.', 'Tente novamente.');
        },
      });
  }

  protected create(): void {
    this.navigation.navigate(['/demo-registro/form']);
  }

  protected onAction(event: { action: string; row: GemsTableRow }): void {
    const id = String(event.row['id']);
    if (event.action === 'view' && this.auth.isAuthorized([Acoes.CONSULTAR_DEMO], 'tenant'))
      this.navigation.navigate(['/demo-registro/view', id]);
    if (event.action === 'edit' && this.auth.isAuthorized([Acoes.ALTERAR_DEMO], 'tenant'))
      this.navigation.navigate(['/demo-registro/form', id]);
    if (event.action === 'delete') void this.remove(id);
  }

  private async remove(id: string): Promise<void> {
    const snapshot = this.auth.capture();
    if (!this.auth.isAuthorized([Acoes.EXCLUIR_DEMO], 'tenant')) return;
    const confirmation = await this.alert.confirm(
      'Excluir registro?',
      'Esta ação remove o registro da empresa selecionada.',
    );
    if (!confirmation.isConfirmed || !this.auth.isCurrent(snapshot)) return;
    this.store
      .deleteRegistro(id)
      .pipe(takeUntilDestroyed(this.destroy))
      .subscribe({
        next: () => {
          if (this.auth.isCurrent(snapshot)) this.search(this.page());
        },
        error: () => {
          if (this.auth.isCurrent(snapshot))
            void this.alert.error('Não foi possível excluir o registro.');
        },
      });
  }
}

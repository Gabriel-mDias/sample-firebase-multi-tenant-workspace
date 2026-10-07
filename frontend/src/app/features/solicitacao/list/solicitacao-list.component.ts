import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import {
  GemsAlertService,
  GemsFormCardComponent,
  GemsInputTextComponent,
  GemsNavigationService,
  GemsPageable,
  GemsSelectComponent,
  GemsSelectOption,
  GemsTableAction,
  GemsTableColumn,
  GemsTableComponent,
  GemsTableRow,
} from '@gabriel-mdias/angular-gems-sdk';
import { GEMS_TABLE_LABELS_PT_BR } from '../../../shared/models/gems-table-labels';
import { parseTableSort } from '../../../shared/utils/table-sort';
import { Acoes, globalAuthorization } from '../../../core/auth/acoes';
import { AuthService } from '../../../core/auth/auth.service';
import { EnterBuscaDirective } from '../../../shared/forms/enter-busca/enter-busca.directive';
import {
  SITUACAO_LABELS,
  SituacaoSolicitacao,
  SolicitacaoFilterModel,
  SolicitacaoResponseModel,
} from '../models/solicitacao.model';
import { SolicitacaoStore } from '../services/solicitacao.store';

/** Mais recentes primeiro: numa fila de trabalho, o topo é o que acabou de chegar. */
const SORT_PADRAO = { field: 'dataCriacao', direction: 'desc' as const };

@Component({
  selector: 'app-solicitacao-list',
  imports: [
    ReactiveFormsModule,
    EnterBuscaDirective,
    GemsFormCardComponent,
    GemsTableComponent,
    GemsInputTextComponent,
    GemsSelectComponent,
  ],
  templateUrl: './solicitacao-list.component.html',
  styleUrl: './solicitacao-list.component.css',
})
export class SolicitacaoListComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly navigationService = inject(GemsNavigationService);
  private readonly store = inject(SolicitacaoStore);
  private readonly alertService = inject(GemsAlertService);
  private readonly auth = inject(AuthService);

  protected readonly labels = GEMS_TABLE_LABELS_PT_BR;
  protected readonly isLoading = signal(false);
  protected readonly rows = signal<GemsTableRow[]>([]);
  protected readonly totalRecords = signal(0);

  protected readonly sortField = signal<string | undefined>(SORT_PADRAO.field);
  protected readonly sortDirection = signal<'asc' | 'desc'>(SORT_PADRAO.direction);

  protected readonly podeInserir = signal(
    this.auth.isAuthorized(globalAuthorization(Acoes.INSERIR_SOLICITACAO)),
  );
  protected readonly podeAlterar = signal(
    this.auth.isAuthorized(globalAuthorization(Acoes.ALTERAR_SOLICITACAO)),
  );
  protected readonly podeExcluir = signal(
    this.auth.isAuthorized(globalAuthorization(Acoes.EXCLUIR_SOLICITACAO)),
  );

  /** As situações vêm da lista local, não de um endpoint: são enum, não cadastro. */
  protected readonly situacoes: GemsSelectOption<string>[] = Object.entries(SITUACAO_LABELS).map(
    ([value, label]) => ({ value, label }),
  );

  private readonly comFiltro = signal(false);

  protected readonly emptyMessage = computed(() =>
    this.comFiltro()
      ? 'Nenhuma solicitação corresponde aos filtros informados.'
      : 'Nenhuma solicitação cadastrada.',
  );

  protected readonly filtroForm = this.fb.group({
    titulo: [''],
    // `null`, nunca `''`: o `gems-select` só exibe o placeholder com `null`.
    situacao: [null as string | null],
  });

  protected readonly pageable = signal<GemsPageable>({
    page: 0,
    size: 10,
    sort: [`${SORT_PADRAO.field},${SORT_PADRAO.direction}`],
  });

  protected readonly columns: GemsTableColumn[] = [
    { field: 'titulo', header: 'Título', sortable: true, sortParam: 'titulo' },
    { field: 'parceiroNome', header: 'Parceiro', sortable: true, sortParam: 'parceiroNome' },
    { field: 'situacao', header: 'Situação', sortable: true, sortParam: 'situacao' },
    { field: 'dataCriacao', header: 'Aberta em', sortable: true, sortParam: 'dataCriacao' },
    { field: 'acoes', header: 'Ações', type: 'actions', sortable: false },
  ];

  protected readonly actions: GemsTableAction[] = [
    { actionName: 'view', icon: 'fa-solid fa-eye', tooltip: 'Visualizar', colorClass: 'btn-info' },
    {
      actionName: 'edit',
      icon: 'fa-solid fa-pen',
      tooltip: 'Editar',
      colorClass: 'btn-warning',
      visible: () => this.podeAlterar(),
    },
    {
      actionName: 'delete',
      icon: 'fa-solid fa-trash',
      tooltip: 'Excluir',
      colorClass: 'btn-danger',
      visible: () => this.podeExcluir(),
    },
  ];

  ngOnInit(): void {
    this.buscar();
  }

  protected buscar(): void {
    this.isLoading.set(true);
    const filtro = this.filtroAtual();
    this.comFiltro.set(Object.keys(filtro).length > 0);

    this.store.search(filtro, this.pageable()).subscribe({
      next: (pagina) => {
        this.rows.set(pagina.content.map((s) => this.toRow(s)));
        this.totalRecords.set(pagina.totalElements);
        this.isLoading.set(false);
      },
      error: (err) => {
        this.alertService.errorFromApi(err);
        this.isLoading.set(false);
      },
    });
  }

  protected limpar(): void {
    this.filtroForm.reset({ titulo: '', situacao: null });
    this.aplicarOrdenacao(SORT_PADRAO.field, SORT_PADRAO.direction, 0, 10);
    this.buscar();
  }

  protected aplicarFiltro(): void {
    this.pageable.update((p) => ({ ...p, page: 0 }));
    this.buscar();
  }

  protected onPageChange(pageable: GemsPageable): void {
    const { field, direction } = parseTableSort(pageable.sort, SORT_PADRAO);
    this.aplicarOrdenacao(field, direction, pageable.page ?? 0, pageable.size ?? 10);
    this.buscar();
  }

  protected novo(): void {
    this.navigationService.navigate(['/solicitacoes/form']);
  }

  protected onAction(event: { action: string; row: GemsTableRow }): void {
    const id = String(event.row['id']);

    switch (event.action) {
      case 'view':
        this.navigationService.navigate(['/solicitacoes/view', id]);
        break;
      case 'edit':
        this.navigationService.navigate(['/solicitacoes/form', id]);
        break;
      case 'delete':
        this.confirmarExclusao(id, String(event.row['titulo']));
        break;
    }
  }

  private aplicarOrdenacao(
    field: string | undefined,
    direction: 'asc' | 'desc',
    page: number,
    size: number,
  ): void {
    this.sortField.set(field);
    this.sortDirection.set(direction);
    this.pageable.set({ page, size, sort: field ? [`${field},${direction}`] : undefined });
  }

  private confirmarExclusao(id: string, titulo: string): void {
    this.alertService
      .confirm(
        'Excluir solicitação',
        `Confirma a exclusão de "${titulo}"? Só rascunho e solicitação cancelada podem ser excluídos.`,
        'Excluir',
        'Cancelar',
      )
      .then((resultado) => {
        if (!resultado.isConfirmed) {
          return;
        }

        this.isLoading.set(true);
        this.store.deleteSolicitacao(id).subscribe({
          next: () => {
            this.alertService.success(
              'Solicitação excluída',
              'O registro não aparece mais na lista.',
            );
            this.buscar();
          },
          error: (err) => {
            this.alertService.errorFromApi(err);
            this.isLoading.set(false);
          },
        });
      });
  }

  private toRow(solicitacao: SolicitacaoResponseModel): GemsTableRow {
    return {
      ...solicitacao,
      situacao: SITUACAO_LABELS[solicitacao.situacao] ?? solicitacao.situacao,
    };
  }

  private filtroAtual(): SolicitacaoFilterModel {
    const { titulo, situacao } = this.filtroForm.getRawValue();
    const filtro: SolicitacaoFilterModel = {};

    if (titulo) {
      filtro.titulo = titulo;
    }
    if (situacao) {
      filtro.situacao = situacao as SituacaoSolicitacao;
    }

    return filtro;
  }
}

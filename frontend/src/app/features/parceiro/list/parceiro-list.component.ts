import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import {
  GemsAlertService,
  GemsFormCardComponent,
  GemsInputTextComponent,
  GemsNavigationService,
  GemsPageable,
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
import { ParceiroFilterModel } from '../models/parceiro-filter.model';
import { ParceiroResponseModel } from '../models/parceiro.model';
import { ParceiroStore } from '../services/parceiro.store';

/** Ordenação inicial da listagem. */
const SORT_PADRAO = { field: 'nome', direction: 'asc' as const };

/**
 * Listagem de parceiros — o padrão de tela do projeto: **cabeçalho global + card de filtros +
 * tabela solta**. A `gems-table` é elemento próprio da tela, sem card em volta.
 */
@Component({
  selector: 'app-parceiro-list',
  imports: [
    ReactiveFormsModule,
    EnterBuscaDirective,
    GemsFormCardComponent,
    GemsTableComponent,
    GemsInputTextComponent,
  ],
  templateUrl: './parceiro-list.component.html',
  styleUrl: './parceiro-list.component.css',
})
export class ParceiroListComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly navigationService = inject(GemsNavigationService);
  private readonly store = inject(ParceiroStore);
  private readonly alertService = inject(GemsAlertService);
  private readonly auth = inject(AuthService);

  protected readonly labels = GEMS_TABLE_LABELS_PT_BR;
  protected readonly isLoading = signal(false);
  protected readonly rows = signal<GemsTableRow[]>([]);
  protected readonly totalRecords = signal(0);

  /**
   * Estado de ordenação. Precisa viver aqui porque a `gems-table` é **controlada**: ela lê
   * `sortField`/`sortDirection` como inputs para decidir a próxima direção e só emite o resultado.
   * Sem devolver esses dois valores, a seta não muda e o asc↔desc não alterna.
   */
  protected readonly sortField = signal<string | undefined>(SORT_PADRAO.field);
  protected readonly sortDirection = signal<'asc' | 'desc'>(SORT_PADRAO.direction);

  // A UI não decide autorização — o backend recusa de qualquer forma. Ela evita oferecer ao
  // operador um caminho que termina em 403. Cada sinal espelha a AÇÃO CONCRETA do endpoint.
  protected readonly podeInserir = signal(
    this.auth.isAuthorized(globalAuthorization(Acoes.INSERIR_PARCEIRO)),
  );
  protected readonly podeAlterar = signal(
    this.auth.isAuthorized(globalAuthorization(Acoes.ALTERAR_PARCEIRO)),
  );
  protected readonly podeExcluir = signal(
    this.auth.isAuthorized(globalAuthorization(Acoes.EXCLUIR_PARCEIRO)),
  );
  protected readonly podeVerComissao = signal(
    this.auth.isAuthorized(globalAuthorization(Acoes.VISUALIZAR_COMISSAO)),
  );

  /** Verdadeiro quando a busca corrente tem algum filtro aplicado — muda a mensagem de vazio. */
  private readonly comFiltro = signal(false);

  protected readonly emptyMessage = computed(() =>
    this.comFiltro()
      ? 'Nenhum parceiro corresponde aos filtros informados.'
      : 'Nenhum parceiro cadastrado.',
  );

  protected readonly filtroForm = this.fb.group({
    nome: [''],
    cpf: [''],
  });

  protected readonly pageable = signal<GemsPageable>({
    page: 0,
    size: 10,
    sort: [`${SORT_PADRAO.field},${SORT_PADRAO.direction}`],
  });

  /**
   * A coluna de comissão só existe para quem tem a ação.
   *
   * <p>Ela é conveniência: o backend já apaga o valor linha a linha antes de responder. Sem esta
   * condição, quem não tem a ação veria uma coluna permanentemente vazia — feia, não insegura.
   */
  protected readonly columns = computed<GemsTableColumn[]>(() => {
    const colunas: GemsTableColumn[] = [
      { field: 'nome', header: 'Nome', sortable: true, sortParam: 'nome' },
      { field: 'cpf', header: 'CPF', sortable: true, sortParam: 'cpf' },
      { field: 'email', header: 'E-mail', sortable: true, sortParam: 'email' },
      { field: 'estadoSigla', header: 'UF', sortable: true, sortParam: 'estadoSigla' },
      {
        field: 'solicitacoesConcluidas',
        header: 'Concluídas',
        sortable: true,
        sortParam: 'solicitacoesConcluidas',
      },
    ];

    if (this.podeVerComissao()) {
      colunas.push({ field: 'comissao', header: 'Comissão', sortable: false });
    }

    // Sem esta coluna a `gems-table` ignora o input `actions` por completo: os botões só são
    // renderizados dentro do case `type: 'actions'` do template dela.
    colunas.push({ field: 'acoes', header: 'Ações', type: 'actions', sortable: false });
    return colunas;
  });

  // `colorClass` precisa ser uma das classes que a própria `gems-table` define no seu CSS
  // encapsulado — btn-view/btn-edit/btn-delete não existem em lugar nenhum e sairiam sem cor.
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
        this.rows.set(pagina.content.map((p) => this.toRow(p)));
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
    this.filtroForm.reset({ nome: '', cpf: '' });
    this.aplicarOrdenacao(SORT_PADRAO.field, SORT_PADRAO.direction, 0, 10);
    this.buscar();
  }

  /** Nova busca sempre volta à primeira página. */
  protected aplicarFiltro(): void {
    this.pageable.update((p) => ({ ...p, page: 0 }));
    this.buscar();
  }

  /** Recebe tanto a troca de página/tamanho quanto o clique de ordenação no cabeçalho. */
  protected onPageChange(pageable: GemsPageable): void {
    const { field, direction } = parseTableSort(pageable.sort, SORT_PADRAO);
    this.aplicarOrdenacao(field, direction, pageable.page ?? 0, pageable.size ?? 10);
    this.buscar();
  }

  protected novo(): void {
    this.navigationService.navigate(['/parceiros/form']);
  }

  protected onAction(event: { action: string; row: GemsTableRow }): void {
    const id = String(event.row['id']);

    switch (event.action) {
      case 'view':
        this.navigationService.navigate(['/parceiros/view', id]);
        break;
      case 'edit':
        this.navigationService.navigate(['/parceiros/form', id]);
        break;
      case 'delete':
        this.confirmarExclusao(id, String(event.row['nome']));
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

  private confirmarExclusao(id: string, nome: string): void {
    this.alertService
      .confirm(
        'Excluir parceiro',
        `Confirma a exclusão de "${nome}"? A pessoa, o contato e o endereço também serão excluídos.`,
        'Excluir',
        'Cancelar',
      )
      .then((resultado) => {
        // `confirm` devolve SweetAlertResult, não boolean: sem checar `.isConfirmed` o cancelar
        // também excluiria.
        if (!resultado.isConfirmed) {
          return;
        }

        this.isLoading.set(true);
        this.store.deleteParceiro(id).subscribe({
          next: () => {
            this.alertService.success('Parceiro excluído', 'O registro não aparece mais na lista.');
            this.buscar();
          },
          error: (err) => {
            this.alertService.errorFromApi(err);
            this.isLoading.set(false);
          },
        });
      });
  }

  /** A `gems-table` não projeta template por célula: a formatação chega pronta na linha. */
  private toRow(parceiro: ParceiroResponseModel): GemsTableRow {
    return {
      ...parceiro,
      comissao:
        parceiro.comissao === null || parceiro.comissao === undefined
          ? '—'
          : `${parceiro.comissao}%`,
    };
  }

  /** Só envia o que foi de fato preenchido — campo em branco não vira filtro. */
  private filtroAtual(): ParceiroFilterModel {
    const { nome, cpf } = this.filtroForm.getRawValue();
    const filtro: ParceiroFilterModel = {};

    if (nome) {
      filtro.nome = nome;
    }
    if (cpf) {
      filtro.cpf = cpf;
    }

    return filtro;
  }
}

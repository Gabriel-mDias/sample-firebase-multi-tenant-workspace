// origem: local; precedencia: local-vence; versao: 2.0.0
import { Component, OnInit, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import {
  TableComponent, TableColumn, TableAction,
  FormCardComponent, Pageable,
  AlertService, NavigationService
} from 'gems-sdk';
import { {Entidade}Store } from '../services/{entidade}.store';
import { {Entidade}FilterModel } from '../models/{entidade}-filter.model';

@Component({
  selector: 'app-{entidade}-list',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule, TableComponent, FormCardComponent],
  templateUrl: './{entidade}-list.component.html'
})
export class {Entidade}ListComponent implements OnInit {

  private store             = inject({Entidade}Store);
  private alertService      = inject(AlertService);
  private navigationService = inject(NavigationService);

  entidades    = signal<any[]>([]);
  isLoading    = signal(false);
  totalRecords = signal(0);

  filter: {Entidade}FilterModel = {};
  pageable: Pageable = { page: 0, size: 10, sort: [] };

  get currentSortField(): string | undefined {
    return this.pageable.sort?.length ? this.pageable.sort[0].split(',')[0] : undefined;
  }

  get currentSortDirection(): 'asc' | 'desc' {
    return this.pageable.sort?.length && this.pageable.sort[0].split(',')[1] === 'desc' ? 'desc' : 'asc';
  }

  columns: TableColumn[] = [
    { field: 'nome',    header: 'Nome',    sortable: true },
    { field: 'actions', header: '',        type: 'actions', sortable: false }
  ];

  actions: TableAction[] = [
    { actionName: 'view',   icon: 'fa-solid fa-eye',   tooltip: 'Visualizar', colorClass: 'btn-info' },
    { actionName: 'edit',   icon: 'fa-solid fa-pen',   tooltip: 'Editar',     colorClass: 'btn-primary' },
    { actionName: 'delete', icon: 'fa-solid fa-trash', tooltip: 'Excluir',    colorClass: 'btn-danger' }
  ];

  ngOnInit(): void {
    this.search();
  }

  search(): void {
    this.pageable.page = 0;
    this.load{Entidade}s();
  }

  clearFilters(): void {
    this.filter = {};
    this.search();
  }

  load{Entidade}s(): void {
    this.isLoading.set(true);
    this.store.search(this.filter, this.pageable).subscribe({
      next: (res) => {
        this.entidades.set(res.content);
        this.totalRecords.set(res.totalElements);
        this.isLoading.set(false);
      },
      error: (err) => {
        this.alertService.errorFromApi(err);
        this.isLoading.set(false);
      }
    });
  }

  onPageChange(newPageable: Pageable): void {
    this.pageable = newPageable;
    this.load{Entidade}s();
  }

  handleAction(event: { action: string; row: any }): void {
    if (event.action === 'view') {
      this.navigationService.navigate(['/{modulo}/view', event.row.id]);
    } else if (event.action === 'edit') {
      this.navigationService.navigate(['/{modulo}/form', event.row.id]);
    } else if (event.action === 'delete') {
      this.remove(event.row.id);
    }
  }

  async remove(id: string): Promise<void> {
    const result = await this.alertService.confirm('Excluir', 'Tem certeza que deseja excluir este(a) {entidade}?');
    if (result.isConfirmed) {
      this.store.delete{Entidade}(id).subscribe({
        next: () => {
          this.alertService.success('Sucesso', '{Entidade} excluído(a) com sucesso.');
          this.load{Entidade}s();
        },
        error: (err) => this.alertService.errorFromApi(err)
      });
    }
  }
}

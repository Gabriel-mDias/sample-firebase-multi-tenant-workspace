<!-- gems-ai-rule: {"id":"angular.lists","version":"2.0.0","status":"candidate"} -->
---
stack: frontend-angular
module: gems-sdk (GemsTableComponent, GemsFormCardComponent)
severity: mandatory
see-also: [signals-standards.md, stores.md, gems-components.md, feedback-alerts.md]
origem: local
precedencia: local-vence
versao: 2.0.0
---

# Listagens & Pesquisa Paginada — Angular / gems-sdk

> **TL;DR:**
> - `standalone: true`. Imports: `GemsTableComponent` + `GemsFormCardComponent` da gems-sdk + `FormsModule`.
> - Signals de estado: `entidades = signal([])`, `isLoading = signal(false)`, `totalRecords = signal(0)`.
> - Ordem dos métodos: `ngOnInit` > `search()` > `clearFilters()` > `loadEntidades()` > `onPageChange()` > `handleAction()` > `remove()`.
> - Template: header (`h2` + `btn-novo`) > `gems-form-card` (filtros) > `@if (isLoading())` > `gems-table`.
> - `gems-table` inputs obrigatórios: `columns`, `data`, `actions`, `totalRecords`, `page`, `size`, `sortField`, `sortDirection`.

## 1. Estrutura de Arquivos

```
features/{modulo}/list/
├── {modulo}-list.component.ts
├── {modulo}-list.component.html
└── {modulo}-list.component.css    # Geralmente vazio
```

## 2. Declaração do Componente

```typescript
import { Component, OnInit, signal, computed, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import {
  GemsTableComponent, GemsTableColumn, GemsTableAction,
  GemsFormCardComponent, GemsPageable,
  GemsAlertService, GemsNavigationService
} from '@gabriel-mdias/angular-gems-sdk';
import { {Entidade}Store } from '../services/{entidade}.store';
import { {Entidade}FilterModel } from '../../../models/{entidade}-filter.model';

@Component({
  selector: 'app-{entidade}-list',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule, GemsTableComponent, GemsFormCardComponent],
  templateUrl: './{entidade}-list.component.html'
})
export class {Entidade}ListComponent implements OnInit {

  private store = inject({Entidade}Store);
  private alertService = inject(GemsAlertService);
  private navigationService = inject(GemsNavigationService);
```

---

## 3. Propriedades (Nesta Ordem)

```typescript
  // 1. Estado de dados (signals)
  entidades = signal<any[]>([]);
  isLoading = signal(false);
  totalRecords = signal(0);

  // 2. Filtro e paginação
  filter: {Entidade}FilterModel = {};
  pageable: GemsPageable = { page: 0, size: 10, sort: [] };

  // 3. Getters de sort
  get currentSortField(): string | undefined {
    return this.pageable.sort?.length ? this.pageable.sort[0].split(',')[0] : undefined;
  }
  get currentSortDirection(): 'asc' | 'desc' {
    return this.pageable.sort?.length && this.pageable.sort[0].split(',')[1] === 'desc' ? 'desc' : 'asc';
  }

  // 4. Colunas da tabela
  columns: GemsTableColumn[] = [
    { field: 'campo1', header: 'Label 1', sortable: true },
    { field: 'campo2', header: 'Label 2', sortable: true },
    { field: 'actions', header: '', type: 'actions', sortable: false }
  ];

  // 5. Ações da tabela
  actions: GemsTableAction[] = [
    { actionName: 'view', icon: 'fa-solid fa-eye', tooltip: 'Visualizar', colorClass: 'btn-info' },
    { actionName: 'edit', icon: 'fa-solid fa-pen', tooltip: 'Editar', colorClass: 'btn-primary' },
    { actionName: 'delete', icon: 'fa-solid fa-trash', tooltip: 'Excluir', colorClass: 'btn-danger' }
  ];
```

### 3.1 Tipos especiais de GemsTableColumn:
```typescript
{ field: 'numeroDocumento', header: 'Documento', type: 'document', docTypeField: 'tipoDocumento' }
{ field: 'valorFormatado',  header: 'Valor',      sortable: true,   sortParam: 'valor' }
```

---

## 4. Métodos (Nesta Ordem)

```typescript
  ngOnInit(): void {
    this.search();
  }

  search(): void {
    this.pageable.page = 0;
    this.loadEntidades();
  }

  clearFilters(): void {
    this.filter = {};
    this.search();
  }

  loadEntidades(): void {
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

  onPageChange(newPageable: GemsPageable): void {
    this.pageable = newPageable;
    this.loadEntidades();
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
          this.loadEntidades();
        },
        error: (err) => this.alertService.errorFromApi(err)
      });
    }
  }
```

---

## 5. Template HTML (Estrutura Obrigatória)

```html
<div class="list-container animate__animated animate__fadeIn">

  <!-- HEADER -->
  <div class="header">
    <div class="header-titles">
      <h2>{Título do Módulo}</h2>
      <p class="subtitle">Gerencie os(as) {entidades} cadastrados(as).</p>
    </div>
    <button class="btn-novo" routerLink="/{modulo}/form">
      <i class="fa-solid fa-plus"></i> Nova {Entidade}
    </button>
  </div>

  <!-- FILTROS -->
  <gems-form-card
    title="Filtros de Pesquisa"
    subtitle="Utilize os campos para refinar a lista."
    icon="fa-solid fa-filter">
    <div class="form-grid">
      <div class="form-group">
        <label>Campo</label>
        <input type="text" class="form-control" [(ngModel)]="filter.textoLivre" placeholder="Digite...">
      </div>
    </div>
    <div gems-form-card-footer>
      <button type="button" class="btn-cancel" (click)="clearFilters()">
        Limpar <i class="fa-solid fa-eraser"></i>
      </button>
      <button type="button" class="btn-save" (click)="search()">
        Pesquisar <i class="fa-solid fa-search"></i>
      </button>
    </div>
  </gems-form-card>

  <!-- TABELA -->
  <div class="content-card">
    @if (isLoading()) {
      <div class="loading-state">
        <i class="fa-solid fa-circle-notch fa-spin"></i> Carregando {entidades}...
      </div>
    } @else {
      <gems-table
        [columns]="columns"
        [data]="entidades()"
        [actions]="actions"
        [totalRecords]="totalRecords()"
        [page]="pageable.page ?? 0"
        [size]="pageable.size ?? 10"
        [sortField]="currentSortField"
        [sortDirection]="currentSortDirection"
        emptyMessage="Nenhum(a) {entidade} encontrado(a)."
        (actionClick)="handleAction($event)"
        (pageChange)="onPageChange($event)">
      </gems-table>
    }
  </div>

</div>
```

---

## 6. Formatação de Dados no Load (Quando Necessário)

```typescript
loadEntidades(): void {
  this.isLoading.set(true);
  this.store.search(this.filter, this.pageable).subscribe({
    next: (res) => {
      this.entidades.set(res.content.map(item => ({
        ...item,
        valorFormatado: new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(item.valor)
      })));
      this.totalRecords.set(res.totalElements);
      this.isLoading.set(false);
    },
    error: (err) => {
      this.alertService.errorFromApi(err);
      this.isLoading.set(false);
    }
  });
}
```

---

## Anti-Patterns (NUNCA)

| Proibido | Correto |
| :--- | :--- |
| `entidades: any[] = []` (propriedade comum) | `entidades = signal<any[]>([])` |
| `isLoading: boolean = false` | `isLoading = signal(false)` |
| `*ngFor="let e of entidades"` | `@for (e of entidades(); track e.id)` — signal com `()` |
| `HttpClient` para busca paginada | `store.search(filter, pageable)` via `GemsBaseStore` |
| `GET /api/{recurso}?nome=x&status=y` | `POST /api/{recurso}/search` com `@RequestBody FilterParams` |

// origem: local; precedencia: local-vence; versao: 2.0.0
import { Component, OnInit, signal, computed, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { FormCardComponent, AlertService, NavigationService } from 'gems-sdk';
import { {Entidade}Store } from '../services/{entidade}.store';
import { {Entidade}Model } from '../models/{entidade}.model';

@Component({
  selector: 'app-{entidade}-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule, FormCardComponent],
  templateUrl: './{entidade}-form.component.html'
})
export class {Entidade}FormComponent implements OnInit {

  private store             = inject({Entidade}Store);
  private route             = inject(ActivatedRoute);
  private alertService      = inject(AlertService);
  private navigationService = inject(NavigationService);
  private fb                = inject(FormBuilder);

  isLoading = signal(false);
  isEdit    = signal(false);

  pageTitle    = computed(() => this.isEdit() ? 'Editar {Entidade}' : 'Nova {Entidade}');
  pageSubtitle = computed(() => this.isEdit() ? 'Atualize os dados.' : 'Preencha os dados abaixo.');

  {entidade}Form: FormGroup;

  constructor() {
    this.{entidade}Form = this.fb.group({
      id:               [null],
      nome:             ['', Validators.required],
      // TODO: adicionar campos específicos da entidade
    });
  }

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.isEdit.set(true);
      this.load{Entidade}(id);
    }
  }

  load{Entidade}(id: string): void {
    this.isLoading.set(true);
    this.store.getById(id).subscribe({
      next: (data: {Entidade}Model) => {
        this.{entidade}Form.patchValue(data);
        this.isLoading.set(false);
      },
      error: (err) => {
        this.alertService.errorFromApi(err);
        this.isLoading.set(false);
      }
    });
  }

  save(): void {
    if (this.{entidade}Form.invalid) return;
    this.isLoading.set(true);
    const model: {Entidade}Model = this.{entidade}Form.value;

    const call$ = this.isEdit() && model.id
      ? this.store.update(model.id, model)
      : this.store.create(model);

    call$.subscribe({
      next: () => {
        this.alertService.success('Sucesso', `{Entidade} ${this.isEdit() ? 'atualizado(a)' : 'criado(a)'} com sucesso.`);
        this.navigationService.navigate(['/{modulo}/list']);
      },
      error: (err) => {
        this.alertService.errorFromApi(err);
        this.isLoading.set(false);
      }
    });
  }
}

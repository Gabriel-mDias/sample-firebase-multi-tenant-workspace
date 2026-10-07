import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import {
  GemsAlertService,
  GemsFieldErrorComponent,
  GemsFormCardComponent,
  GemsInputTextComponent,
  GemsNavigationService,
  GemsSelectComponent,
  GemsSelectOption,
  GemsTextareaComponent,
} from '@gabriel-mdias/angular-gems-sdk';
import { Observable } from 'rxjs';
import { Acoes, globalAuthorization } from '../../../core/auth/acoes';
import { AuthService } from '../../../core/auth/auth.service';
import { BotaoHistoricoComponent } from '../../../shared/auditoria/botao-historico/botao-historico.component';
import { ParceiroLookupStore } from '../../../shared/parceiro/parceiro-lookup.store';
import { SITUACAO_CLASSES, SITUACAO_LABELS, SolicitacaoModel } from '../models/solicitacao.model';
import { SolicitacaoStore } from '../services/solicitacao.store';

type Modo = 'create' | 'edit' | 'view';

/**
 * Cadastro e fluxo da solicitação, na mesma tela.
 *
 * <h2>Por que não há um select de situação</h2>
 * Porque a situação **não entra pelo corpo da requisição**. Cada transição é um botão próprio, que
 * chama o endpoint próprio dela, protegido pela ação própria dela. Um select gravado num PUT daria
 * a quem pode editar o poder de concluir — e a autorização por transição viraria decoração.
 *
 * <p>Os botões aparecem pela conjunção de duas coisas: a transição ser **possível** a partir da
 * situação atual, e o usuário **ter a ação**. Nenhuma das duas substitui a outra: o backend recusa
 * as duas de qualquer forma, e esconder é só não oferecer um caminho que termina em erro.
 */
@Component({
  selector: 'app-solicitacao-form',
  imports: [
    ReactiveFormsModule,
    BotaoHistoricoComponent,
    GemsFormCardComponent,
    GemsInputTextComponent,
    GemsTextareaComponent,
    GemsSelectComponent,
    GemsFieldErrorComponent,
  ],
  templateUrl: './solicitacao-form.component.html',
  styleUrl: './solicitacao-form.component.css',
})
export class SolicitacaoFormComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly navigationService = inject(GemsNavigationService);
  private readonly store = inject(SolicitacaoStore);
  private readonly parceiroLookupStore = inject(ParceiroLookupStore);
  private readonly auth = inject(AuthService);
  private readonly alertService = inject(GemsAlertService);

  protected readonly modo = signal<Modo>('create');
  protected readonly isReadOnly = computed(() => this.modo() === 'view');
  protected readonly isLoading = signal(false);
  protected readonly isCarregando = signal(false);

  protected readonly solicitacao = signal<SolicitacaoModel | null>(null);
  protected readonly parceiros = signal<GemsSelectOption<string>[]>([]);

  protected readonly registroId = computed(() => this.solicitacao()?.id ?? null);

  protected readonly situacaoLabel = computed(() => {
    const situacao = this.solicitacao()?.situacao;
    return situacao ? SITUACAO_LABELS[situacao] : '';
  });

  protected readonly situacaoClasse = computed(() => {
    const situacao = this.solicitacao()?.situacao;
    return situacao ? SITUACAO_CLASSES[situacao] : '';
  });

  // Cada botão de transição: possível a partir da situação atual E permitido para este usuário.
  protected readonly podeAbrir = computed(
    () =>
      this.solicitacao()?.situacao === 'RASCUNHO' &&
      this.auth.isAuthorized(globalAuthorization(Acoes.ABRIR_SOLICITACAO)),
  );
  protected readonly podeIniciar = computed(
    () =>
      this.solicitacao()?.situacao === 'ABERTA' &&
      this.auth.isAuthorized(globalAuthorization(Acoes.INICIAR_SOLICITACAO)),
  );
  protected readonly podeConcluir = computed(
    () =>
      this.solicitacao()?.situacao === 'EM_ANDAMENTO' &&
      this.auth.isAuthorized(globalAuthorization(Acoes.CONCLUIR_SOLICITACAO)),
  );
  protected readonly podeCancelar = computed(() => {
    const situacao = this.solicitacao()?.situacao;
    const terminal = situacao === 'CONCLUIDA' || situacao === 'CANCELADA';
    return (
      !!situacao &&
      !terminal &&
      this.auth.isAuthorized(globalAuthorization(Acoes.CANCELAR_SOLICITACAO))
    );
  });

  protected readonly erroTitulo = { required: 'O título é obrigatório.' };
  protected readonly erroParceiro = { required: 'O parceiro é obrigatório.' };

  protected readonly form = this.fb.group({
    titulo: ['', [Validators.required, Validators.maxLength(255)]],
    descricao: ['', [Validators.maxLength(4000)]],
    // `null`, nunca `''`: o `gems-select` só exibe o placeholder com `null`.
    parceiroId: [null as string | null, [Validators.required]],
  });

  protected readonly titulo = computed(() => {
    switch (this.modo()) {
      case 'edit':
        return 'Editar solicitação';
      case 'view':
        return 'Visualizar solicitação';
      default:
        return 'Nova solicitação';
    }
  });

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    this.modo.set((this.route.snapshot.data['mode'] as Modo | undefined) ?? 'create');

    this.carregarParceiros();

    if (id) {
      this.carregar(id);
    }

    if (this.isReadOnly()) {
      this.form.disable();
    }
  }

  protected sair(): void {
    this.navigationService.navigate(['/solicitacoes/list']);
  }

  protected salvar(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const id = this.solicitacao()?.id;
    const valor = this.form.getRawValue();

    // A situação NÃO vai no payload. Nem quando ela é conhecida: o backend a ignoraria, e
    // enviá-la sugeriria que este caminho pode alterá-la.
    const dto: SolicitacaoModel = {
      titulo: valor.titulo ?? '',
      descricao: valor.descricao ?? '',
      parceiroId: valor.parceiroId ?? undefined,
    };

    this.isLoading.set(true);
    const requisicao = id ? this.store.update(id, dto) : this.store.create(dto);

    requisicao.subscribe({
      next: () => {
        this.isLoading.set(false);
        this.alertService.success(
          id ? 'Solicitação atualizada' : 'Solicitação aberta',
          'Os dados foram gravados com sucesso.',
        );
        this.navigationService.navigate(['/solicitacoes/list']);
      },
      error: (err) => {
        this.alertService.errorFromApi(err);
        this.isLoading.set(false);
      },
    });
  }

  protected abrir(): void {
    this.transitar((id) => this.store.abrir(id), 'Solicitação publicada.');
  }

  protected iniciar(): void {
    this.transitar((id) => this.store.iniciar(id), 'Execução iniciada.');
  }

  protected concluir(): void {
    this.transitar((id) => this.store.concluir(id), 'Solicitação concluída.');
  }

  protected cancelar(): void {
    const id = this.solicitacao()?.id;
    if (!id) {
      return;
    }

    this.alertService
      .confirm(
        'Cancelar solicitação',
        'O motivo do cancelamento é obrigatório e fica registrado. Deseja continuar?',
        'Cancelar solicitação',
        'Voltar',
      )
      .then((resultado) => {
        if (!resultado.isConfirmed) {
          return;
        }

        // Motivo fixo aqui é placeholder do molde: numa rodada real ele vem de um campo, e o
        // backend recusa a chamada sem ele de qualquer forma.
        this.executar(
          this.store.cancelar(id, 'Cancelada pelo operador.'),
          'Solicitação cancelada.',
        );
      });
  }

  private transitar(acao: (id: string) => Observable<SolicitacaoModel>, mensagem: string): void {
    const id = this.solicitacao()?.id;
    if (!id) {
      return;
    }
    this.executar(acao(id), mensagem);
  }

  private executar(requisicao: Observable<SolicitacaoModel>, mensagem: string): void {
    this.isLoading.set(true);

    requisicao.subscribe({
      next: (atualizada) => {
        // Reflete a resposta do servidor, e não uma suposição da tela: a situação de destino é
        // decisão do backend, e um otimismo local esconderia uma transição recusada.
        this.solicitacao.set(atualizada);
        this.isLoading.set(false);
        this.alertService.success('Pronto', mensagem);
      },
      error: (err) => {
        this.alertService.errorFromApi(err);
        this.isLoading.set(false);
      },
    });
  }

  private carregar(id: string): void {
    this.isCarregando.set(true);

    this.store.getById(id).subscribe({
      next: (solicitacao) => {
        this.solicitacao.set(solicitacao);
        this.form.patchValue({
          titulo: solicitacao.titulo,
          descricao: solicitacao.descricao ?? '',
          parceiroId: solicitacao.parceiroId ?? null,
        });
        this.isCarregando.set(false);
      },
      error: (err) => {
        this.alertService.errorFromApi(err);
        this.isCarregando.set(false);
        this.navigationService.navigate(['/solicitacoes/list']);
      },
    });
  }

  private carregarParceiros(): void {
    this.parceiroLookupStore.search({}, { page: 0, size: 100, sort: ['nome,asc'] }).subscribe({
      next: (pagina) =>
        this.parceiros.set(pagina.content.map((p) => ({ label: p.nome, value: p.id }))),
      error: (err) => this.alertService.errorFromApi(err),
    });
  }
}

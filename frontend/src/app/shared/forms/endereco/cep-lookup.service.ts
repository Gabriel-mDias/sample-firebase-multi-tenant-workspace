import { Injectable, inject, signal } from '@angular/core';
import { FormGroup } from '@angular/forms';
import { GemsAlertService } from '@gabriel-mdias/angular-gems-sdk';
import { CepStore } from '../../../core/localidade/cep.store';
import { TAMANHO_CEP } from './endereco-form';

const AVISO_ESTADO_AUSENTE =
  'CEP encontrado, mas o estado não está no cadastro. Selecione-o manualmente.';
const AVISO_CEP_INEXISTENTE =
  'CEP não encontrado. Confira o número ou preencha o endereço manualmente.';
const AVISO_PROVEDOR_FORA =
  'Não foi possível consultar o CEP agora. Preencha o endereço manualmente.';

/**
 * Consulta de CEP dos passos de endereço — o mesmo bloco que existia copiado em Parceiro,
 * Usuário e Parceiro.
 *
 * <h2>Por que é serviço e não função</h2>
 * A consulta carrega dois estados de tela (`consultando`, `aviso`) e um contador de corrida, e os
 * três precisam viver junto do formulário que está sendo preenchido. Por isso **não** é
 * `providedIn: 'root'`: cada passo o declara no próprio `providers`, e cada abertura do wizard
 * começa limpa.
 *
 * <h2>A corrida que só uma das três cópias tratava</h2>
 * O operador digita um CEP, a consulta sai, ele corrige o CEP antes de a resposta voltar. Sem
 * guarda, a resposta antiga chega depois e sobrescreve cidade, bairro e logradouro com os dados do
 * CEP errado — em silêncio, e sem nada na tela que denuncie. Só o passo do Parceiro tinha o
 * contador; ao concentrar aqui, Usuário e Parceiro passam a ter também.
 *
 * <h2>Erro previsto não é erro inesperado</h2>
 * 400 (CEP inexistente) e 502 (ViaCEP fora do ar) são respostas previstas pelo contrato do
 * `CepService`: viram aviso não bloqueante, para que o preenchimento manual continue possível.
 * Qualquer outro status vai para `errorFromApi`, como em todo o resto do projeto.
 */
@Injectable()
export class CepLookupService {
  private readonly cepStore = inject(CepStore);
  private readonly alertService = inject(GemsAlertService);

  private consultaAtual = 0;

  readonly consultando = signal(false);
  readonly aviso = signal<string | null>(null);

  /**
   * Descarta a resposta de qualquer consulta em voo e limpa o estado de tela.
   *
   * <p>Chamada quando o CEP é editado ou o país trocado: o resultado que estiver a caminho já não
   * corresponde ao que o operador está vendo.
   */
  invalidar(): void {
    this.consultaAtual += 1;
    this.consultando.set(false);
    this.aviso.set(null);
  }

  /** Preenche cidade, bairro, logradouro e estado do `form` a partir do CEP digitado nele. */
  consultar(form: FormGroup): void {
    const consulta = ++this.consultaAtual;
    const cep = String(form.get('cep')?.value ?? '').replace(/\D/g, '');

    this.consultando.set(false);
    this.aviso.set(null);

    if (cep.length !== TAMANHO_CEP) {
      return;
    }

    this.consultando.set(true);

    this.cepStore.buscarPorCep(cep).subscribe({
      next: (endereco) => {
        if (consulta !== this.consultaAtual) {
          return;
        }

        this.consultando.set(false);

        form.patchValue(
          {
            cidade: endereco.cidade ?? '',
            bairro: endereco.bairro ?? '',
            logradouro: endereco.logradouro ?? '',
          },
          { emitEvent: false },
        );

        if (endereco.estado?.id) {
          form.patchValue({ estadoId: endereco.estado.id }, { emitEvent: false });
        } else {
          this.aviso.set(AVISO_ESTADO_AUSENTE);
        }
      },
      error: (err) => {
        if (consulta !== this.consultaAtual) {
          return;
        }

        this.consultando.set(false);

        if (err?.status === 400) {
          this.aviso.set(AVISO_CEP_INEXISTENTE);
          return;
        }

        if (err?.status === 502) {
          this.aviso.set(AVISO_PROVEDOR_FORA);
          return;
        }

        this.aviso.set(null);
        this.alertService.errorFromApi(err);
      },
    });
  }
}

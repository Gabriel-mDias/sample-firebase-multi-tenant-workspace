import { Injectable, computed, signal } from '@angular/core';
import { ParceiroModel } from '../models/parceiro.model';

/** Modo de operação do wizard. A rota decide qual é. */
export type ParceiroFormMode = 'create' | 'edit' | 'view';

/**
 * Estado acumulado do wizard.
 *
 * Guarda **ids** para as referências (`paisId`, `estadoId`), não objetos: é o que os selects
 * manipulam e o que o backend precisa receber.
 */
export interface ParceiroFormState {
  id?: string;
  nome?: string;
  descricao?: string;
  comissao?: number | null;
  /**
   * `true` quando o usuário tem `VISUALIZAR_COMISSAO`. Guardado no estado, e não consultado a cada
   * render, para que o passo de resumo e o `toPayload` decidam pelo **mesmo** valor.
   */
  podeVerComissao?: boolean;
  pessoa?: {
    id?: string;
    primeiroNome?: string;
    sobrenome?: string;
    cpf?: string;
    rg?: string;
    dataNascimento?: string;
    sexo?: string;
  };
  contato?: {
    id?: string;
    email?: string;
    telefone?: string;
  };
  endereco?: {
    id?: string;
    paisId?: string;
    paisNome?: string;
    estadoId?: string;
    estadoNome?: string;
    cep?: string;
    cidade?: string;
    bairro?: string;
    logradouro?: string;
    numero?: string;
    complemento?: string;
  };
}

/**
 * Fonte única da verdade do wizard de parceiro.
 *
 * Os passos não conversam entre si nem chamam a store: leem e escrevem só aqui. O contrato do
 * `GemsBaseStepComponent` fala em "sessão" (`loadFromSession`/`saveToSession`), mas quem faz esse
 * papel é este serviço — usar `sessionStorage` em paralelo criaria dois estados divergentes.
 *
 * Fornecido no componente orquestrador (não em `root`), para que cada abertura do formulário comece
 * limpa.
 */
@Injectable()
export class ParceiroFormStateService {
  private readonly state = signal<ParceiroFormState>({});

  /** Leitura do estado acumulado. */
  readonly snapshot = computed(() => this.state());

  readonly mode = signal<ParceiroFormMode>('create');

  /** Passo ativo. 1-based, como o `gems-wizard` espera. */
  readonly currentStep = signal(1);

  readonly isReadOnly = computed(() => this.mode() === 'view');

  /** Mescla superficialmente; cada passo escreve só a sua fatia. */
  updateState(partial: ParceiroFormState): void {
    this.state.update((current) => ({ ...current, ...partial }));
  }

  clearState(): void {
    this.state.set({});
    this.currentStep.set(1);
  }

  goToStep(step: number): void {
    this.currentStep.set(step);
  }

  /** Hidrata o wizard a partir do que veio do backend (modos editar e visualizar). */
  loadFrom(parceiro: ParceiroModel, podeVerComissao: boolean): void {
    this.state.set({
      id: parceiro.id,
      nome: parceiro.nome,
      descricao: parceiro.descricao,
      comissao: parceiro.comissao,
      podeVerComissao,
      pessoa: {
        id: parceiro.pessoa?.id,
        primeiroNome: parceiro.pessoa?.primeiroNome,
        sobrenome: parceiro.pessoa?.sobrenome,
        cpf: parceiro.pessoa?.cpf,
        rg: parceiro.pessoa?.rg,
        dataNascimento: parceiro.pessoa?.dataNascimento,
        sexo: parceiro.pessoa?.sexo,
      },
      contato: {
        id: parceiro.contato?.id,
        email: parceiro.contato?.email,
        telefone: parceiro.contato?.telefone,
      },
      endereco: {
        id: parceiro.endereco?.id,
        paisId: parceiro.endereco?.pais?.id,
        paisNome: parceiro.endereco?.pais?.nome,
        estadoId: parceiro.endereco?.estado?.id,
        estadoNome: parceiro.endereco?.estado?.nome,
        cep: parceiro.endereco?.cep,
        cidade: parceiro.endereco?.cidade,
        bairro: parceiro.endereco?.bairro,
        logradouro: parceiro.endereco?.logradouro,
        numero: parceiro.endereco?.numero,
        complemento: parceiro.endereco?.complemento,
      },
    });
  }

  /**
   * Monta o payload único enviado ao backend no passo de resumo. As referências trafegam só com o
   * `id`; nome e sigla são responsabilidade do servidor.
   *
   * <p><b>A comissão só é incluída quando o usuário pode vê-la.</b> Quem não tem a ação recebeu o
   * campo `null` na leitura; reenviá-lo apagaria o valor gravado. O backend também ignora o campo
   * nesse caso — as duas guardas são deliberadas, porque a da tela protege o payload e a do
   * servidor protege a API de quem não usa a tela.
   */
  toPayload(): ParceiroModel {
    const current = this.state();

    const payload: ParceiroModel = {
      id: current.id,
      nome: current.nome ?? '',
      descricao: current.descricao,
      pessoa: {
        id: current.pessoa?.id,
        primeiroNome: current.pessoa?.primeiroNome ?? '',
        sobrenome: current.pessoa?.sobrenome ?? '',
        cpf: current.pessoa?.cpf,
        rg: current.pessoa?.rg,
        dataNascimento: current.pessoa?.dataNascimento,
        sexo: current.pessoa?.sexo,
      },
      contato: {
        id: current.contato?.id,
        email: current.contato?.email ?? '',
        telefone: current.contato?.telefone ?? '',
      },
      endereco: {
        id: current.endereco?.id,
        pais: { id: current.endereco?.paisId ?? '' },
        estado: current.endereco?.estadoId ? { id: current.endereco.estadoId } : undefined,
        cep: current.endereco?.cep ?? '',
        cidade: current.endereco?.cidade ?? '',
        bairro: current.endereco?.bairro ?? '',
        logradouro: current.endereco?.logradouro ?? '',
        numero: current.endereco?.numero,
        complemento: current.endereco?.complemento,
      },
    };

    if (current.podeVerComissao) {
      payload.comissao = current.comissao ?? null;
    }

    return payload;
  }
}

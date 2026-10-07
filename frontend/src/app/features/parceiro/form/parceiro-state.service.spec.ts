import { beforeEach, describe, expect, it } from 'vitest';
import { ParceiroFormStateService } from './parceiro-state.service';

/**
 * O estado do wizard é função pura sobre signals: testa-se sem TestBed, sem HTTP e sem Keycloak.
 *
 * O caso que mais importa aqui é o da **comissão**. Ele parece detalhe de serialização e não é: é o
 * ponto em que uma autorização de leitura vira, por descuido, uma autorização de escrita ao
 * contrário.
 */
describe('ParceiroFormStateService', () => {
  let state: ParceiroFormStateService;

  beforeEach(() => {
    state = new ParceiroFormStateService();
  });

  it('deveComecarVazioNoPrimeiroPasso_quandoCriarServico', () => {
    expect(state.snapshot()).toEqual({});
    expect(state.currentStep()).toBe(1);
    expect(state.mode()).toBe('create');
    expect(state.isReadOnly()).toBe(false);
  });

  it('devePreservarDemaisFatias_quandoAtualizarUmPasso', () => {
    state.updateState({ nome: 'Parceiro A' });
    state.updateState({ contato: { email: 'a@b.c', telefone: '11999990000' } });

    expect(state.snapshot().nome).toBe('Parceiro A');
    expect(state.snapshot().contato?.email).toBe('a@b.c');
  });

  it('deveSerSomenteLeitura_quandoModoForView', () => {
    state.mode.set('view');
    expect(state.isReadOnly()).toBe(true);
  });

  it('deveZerarEstadoEVoltarAoPrimeiroPasso_quandoLimpar', () => {
    state.updateState({ nome: 'Parceiro A' });
    state.goToStep(3);

    state.clearState();

    expect(state.snapshot()).toEqual({});
    expect(state.currentStep()).toBe(1);
  });

  describe('a comissão no payload', () => {
    it('deveIncluirComissao_quandoUsuarioPuderVisualizar', () => {
      state.updateState({ nome: 'Parceiro A', comissao: 12.5, podeVerComissao: true });

      expect(state.toPayload().comissao).toBe(12.5);
    });

    it('naoDeveIncluirComissao_quandoUsuarioNaoPuderVisualizar', () => {
      // Quem não tem VISUALIZAR_COMISSAO recebeu `null` na leitura. Reenviar esse `null`
      // sobrescreveria o valor gravado, e a tela apagaria em silêncio um dado que ela nem exibe.
      state.updateState({ nome: 'Parceiro A', comissao: null, podeVerComissao: false });

      expect('comissao' in state.toPayload()).toBe(false);
    });

    it('deveEnviarComissaoNula_quandoUsuarioPuderVisualizarELimparCampo', () => {
      state.updateState({ nome: 'Parceiro A', comissao: null, podeVerComissao: true });

      const payload = state.toPayload();
      expect('comissao' in payload).toBe(true);
      expect(payload.comissao).toBeNull();
    });
  });

  it('naoDeveEnviarEstadoVazio_quandoPaisNaoTiverUf', () => {
    state.updateState({ nome: 'Parceiro A', endereco: { paisId: 'p1', cidade: 'Lisboa' } });

    expect(state.toPayload().endereco.estado).toBeUndefined();
  });
});

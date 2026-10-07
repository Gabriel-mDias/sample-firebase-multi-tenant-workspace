import { TestBed } from '@angular/core/testing';
import { FormBuilder, FormGroup } from '@angular/forms';
import { GemsAlertService } from '@gabriel-mdias/angular-gems-sdk';
import { of, throwError } from 'rxjs';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { CepStore } from '../../../core/localidade/cep.store';
import { CepEnderecoModel } from '../../../core/localidade/models/cep-endereco.model';
import { CepLookupService } from './cep-lookup.service';
import { createEnderecoForm } from './endereco-form';

/**
 * O que este teste protege: o comportamento que, espalhado em cópias por tela, é
 * testado em **uma**. Os dois avisos não bloqueantes — CEP inexistente e provedor fora do ar —
 * nunca tiveram teste em lugar nenhum; a corrida entre consultas só tinha no passo do Parceiro.
 *
 * A distinção que o serviço precisa manter é: 400 e 502 são respostas **previstas** pelo contrato
 * do `CepService` e viram aviso, para não impedir o preenchimento manual. Qualquer outro status é
 * falha inesperada e vai para `errorFromApi`, como no resto do projeto.
 */
const BRASIL = { id: 'pais-brasil', nome: 'Brasil' };

function resposta(cidade: string, estadoId?: string): CepEnderecoModel {
  return {
    pais: BRASIL,
    estado: estadoId
      ? { id: estadoId, nome: 'Espírito Santo', siglaEstado: 'ES', pais: BRASIL }
      : null,
    cep: '29010000',
    cidade,
    bairro: 'Centro',
    logradouro: 'Rua Sete de Setembro',
  };
}

describe('CepLookupService', () => {
  let service: CepLookupService;
  let form: FormGroup;
  let buscarPorCep: ReturnType<typeof vi.fn>;
  let errorFromApi: ReturnType<typeof vi.fn>;

  beforeEach(() => {
    buscarPorCep = vi.fn();
    errorFromApi = vi.fn();

    TestBed.configureTestingModule({
      providers: [
        CepLookupService,
        { provide: CepStore, useValue: { buscarPorCep } },
        { provide: GemsAlertService, useValue: { errorFromApi } },
      ],
    });

    service = TestBed.inject(CepLookupService);
    form = createEnderecoForm(TestBed.inject(FormBuilder), { comPais: true });
  });

  function comCep(valor: string) {
    form.get('cep')?.setValue(valor);
  }

  it('naoDeveConsultar_quandoCepEstiverIncompleto', () => {
    comCep('2901');
    service.consultar(form);

    expect(buscarPorCep).not.toHaveBeenCalled();
    expect(service.consultando()).toBe(false);
  });

  it('devePreencherEndereco_quandoConsultaRetornarDados', () => {
    buscarPorCep.mockReturnValue(of(resposta('Vitória', 'estado-es')));
    comCep('29010000');

    service.consultar(form);

    expect(form.get('cidade')?.value).toBe('Vitória');
    expect(form.get('bairro')?.value).toBe('Centro');
    expect(form.get('logradouro')?.value).toBe('Rua Sete de Setembro');
    expect(form.get('estadoId')?.value).toBe('estado-es');
    expect(service.aviso()).toBeNull();
    expect(service.consultando()).toBe(false);
  });

  it('deveAvisar_quandoEstadoNaoEstiverCadastrado', () => {
    buscarPorCep.mockReturnValue(of(resposta('Vitória')));
    comCep('29010000');

    service.consultar(form);

    expect(service.aviso()).toContain('estado não está no cadastro');
    expect(form.get('cidade')?.value).toBe('Vitória');
  });

  it('devePreservarPreenchimentoManual_quandoApiRetornar400', () => {
    buscarPorCep.mockReturnValue(throwError(() => ({ status: 400 })));
    comCep('29010000');

    service.consultar(form);

    expect(service.aviso()).toContain('CEP não encontrado');
    expect(errorFromApi).not.toHaveBeenCalled();
  });

  it('deveAvisarSemBloquear_quandoApiRetornar502', () => {
    buscarPorCep.mockReturnValue(throwError(() => ({ status: 502 })));
    comCep('29010000');

    service.consultar(form);

    expect(service.aviso()).toContain('Não foi possível consultar o CEP agora');
    expect(errorFromApi).not.toHaveBeenCalled();
  });

  it('deveEncaminharErro_quandoStatusForInesperado', () => {
    buscarPorCep.mockReturnValue(throwError(() => ({ status: 500 })));
    comCep('29010000');

    service.consultar(form);

    expect(errorFromApi).toHaveBeenCalledTimes(1);
    expect(service.aviso()).toBeNull();
  });

  it('deveDescartarResposta_quandoConsultaForInvalidada', () => {
    // A consulta sai, o operador corrige o CEP, e só então a resposta antiga chega.
    let emitir: ((valor: CepEnderecoModel) => void) | undefined;
    buscarPorCep.mockReturnValue({
      subscribe: (obs: { next: (v: CepEnderecoModel) => void }) => {
        emitir = obs.next;
        return { unsubscribe: () => undefined };
      },
    });

    comCep('29010000');
    service.consultar(form);

    service.invalidar();
    emitir?.(resposta('Cidade Obsoleta', 'estado-antigo'));

    expect(form.get('cidade')?.value).toBe('');
    expect(service.consultando()).toBe(false);
  });
});

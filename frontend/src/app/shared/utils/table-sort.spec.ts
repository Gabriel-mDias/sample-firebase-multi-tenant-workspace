// Prova do utilitário compartilhado em `shared/utils`.
import { describe, expect, it } from 'vitest';
import { parseTableSort } from './table-sort';

describe('parseTableSort', () => {
  it('deveLerCampoEDirecao_quandoReceberFormatoSpringData', () => {
    expect(parseTableSort(['nome,asc'])).toEqual({ field: 'nome', direction: 'asc' });
    expect(parseTableSort(['paisRepresentadoNome,desc'])).toEqual({
      field: 'paisRepresentadoNome',
      direction: 'desc',
    });
  });

  it('deveRetornarPadrao_quandoOrdenacaoEstiverAusente', () => {
    const padrao = { field: 'nome', direction: 'asc' as const };

    expect(parseTableSort(undefined, padrao)).toEqual(padrao);
    expect(parseTableSort([], padrao)).toEqual(padrao);
    expect(parseTableSort([''], padrao)).toEqual(padrao);
  });

  it('deveUsarAscendente_quandoDirecaoNaoForDescendente', () => {
    expect(parseTableSort(['nome'])).toEqual({ field: 'nome', direction: 'asc' });
    expect(parseTableSort(['nome,'])).toEqual({ field: 'nome', direction: 'asc' });
    expect(parseTableSort(['nome,qualquer'])).toEqual({ field: 'nome', direction: 'asc' });
  });

  it('deveRetornarCampoIndefinido_quandoPadraoNaoForInformado', () => {
    expect(parseTableSort(undefined)).toEqual({ field: undefined, direction: 'asc' });
  });
});

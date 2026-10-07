// Prova do utilitário compartilhado em `shared/utils`.
import { describe, expect, it } from 'vitest';
import { dataBr, dataBrOu } from './data-br';

describe('dataBr', () => {
  it('deveConverterDataIso_quandoReceberDataSimples', () => {
    expect(dataBr('2026-08-27')).toBe('27/08/2026');
  });

  it('deveIgnorarAHora_quandoReceberDataHoraIso', () => {
    expect(dataBr('2026-08-27T10:30:00')).toBe('27/08/2026');
  });

  /**
   * ⚠️ A razão de a função cortar a string em vez de usar `new Date(valor)`.
   * `new Date('2026-01-01')` é interpretado como **UTC**, e em qualquer fuso negativo — o Brasil
   * inteiro — `getDate()` devolveria 31/12 do ano anterior. O defeito só apareceria na virada.
   */
  it('naoDeveDeslocarODia_quandoDataNaoTiverFuso', () => {
    expect(dataBr('2026-01-01')).toBe('01/01/2026');
    expect(dataBr('2026-01-01T00:00:00')).toBe('01/01/2026');
  });

  it('deveRetornarNull_quandoDataEstiverAusente', () => {
    expect(dataBr(null)).toBeNull();
    expect(dataBr(undefined)).toBeNull();
    expect(dataBr('')).toBeNull();
  });

  it('deveRetornarNull_quandoEntradaNaoForDataIso', () => {
    expect(dataBr('ontem')).toBeNull();
  });
});

describe('dataBrOu', () => {
  it('deveUsarTravessao_quandoDataEstiverAusente', () => {
    expect(dataBrOu(null)).toBe('—');
  });

  it('deveUsarTextoInformado_quandoDataEstiverAusente', () => {
    expect(dataBrOu(null, 'Não vence')).toBe('Não vence');
  });

  it('deveFormatarData_quandoValorEstiverPresente', () => {
    expect(dataBrOu('2026-12-31')).toBe('31/12/2026');
  });
});

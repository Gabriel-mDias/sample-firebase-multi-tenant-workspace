// Prova do utilitário compartilhado em `shared/utils`.
import { describe, expect, it } from 'vitest';
import { formatarMoedaBr, parseMoedaBr } from './moeda-br';

/**
 * O que este teste protege: um valor monetário exibido ao usuário. Um erro de
 * separador aqui não quebra nada visivelmente — grava um número plausível e errado, e o operador
 * só descobre no orçamento.
 */
describe('parseMoedaBr', () => {
  it('deveLerFormatoBrasileiro_quandoHouverMilharEDecimal', () => {
    expect(parseMoedaBr('1.234,56')).toBe(1234.56);
    expect(parseMoedaBr('12.345.678,90')).toBe(12345678.9);
  });

  it('deveLerPontoDecimal_quandoDigitadoNoTecladoNumerico', () => {
    expect(parseMoedaBr('1234.56')).toBe(1234.56);
    expect(parseMoedaBr('1.5')).toBe(1.5);
  });

  it('deveLerFormatoEnUs_quandoHouverMilharEDecimal', () => {
    expect(parseMoedaBr('1,234.56')).toBe(1234.56);
  });

  it('deveTratarSeparadorComoMilhar_quandoRepetido', () => {
    expect(parseMoedaBr('12.345.678')).toBe(12345678);
    expect(parseMoedaBr('1,234,567')).toBe(1234567);
  });

  it('deveLerInteiro_quandoValorNaoTiverDecimal', () => {
    expect(parseMoedaBr('2500')).toBe(2500);
    expect(parseMoedaBr('2.500')).toBe(2500);
  });

  it('deveIgnorarEspacos_quandoValorForInformado', () => {
    expect(parseMoedaBr('  1 234,56 ')).toBe(1234.56);
  });

  it('deveRetornarNull_quandoValorEstiverVazio', () => {
    expect(parseMoedaBr('')).toBeNull();
    expect(parseMoedaBr('   ')).toBeNull();
    expect(parseMoedaBr(null)).toBeNull();
    expect(parseMoedaBr(undefined)).toBeNull();
  });

  it('deveRetornarNull_quandoTextoNaoForNumero', () => {
    expect(parseMoedaBr('grátis')).toBeNull();
    expect(parseMoedaBr('R$')).toBeNull();
  });
});

describe('formatarMoedaBr', () => {
  it('deveFormatarDuasCasas_quandoValorForNumero', () => {
    expect(formatarMoedaBr(1234.5)).toBe('1.234,50');
    expect(formatarMoedaBr(2500)).toBe('2.500,00');
  });

  it('deveRetornarTextoVazio_quandoValorEstiverAusente', () => {
    expect(formatarMoedaBr(null)).toBe('');
    expect(formatarMoedaBr(undefined)).toBe('');
  });

  it('devePreservarValor_quandoFormatarEInterpretar', () => {
    expect(parseMoedaBr(formatarMoedaBr(1234.56))).toBe(1234.56);
  });
});

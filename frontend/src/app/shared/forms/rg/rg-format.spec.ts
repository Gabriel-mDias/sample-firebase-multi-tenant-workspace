import { formatRg, sanitizeRg } from './rg-format';

describe('rg-format', () => {
  it('deveFormatarComDigitoNumerico_quandoRgForPaulista', () => {
    expect(formatRg('123456789')).toBe('12.345.678-9');
  });

  it('devePreservarDigitoAlfabetico_quandoRgForPaulista', () => {
    expect(formatRg('12345678x')).toBe('12.345.678-X');
  });

  it('naoDeveInventarDigito_quandoRgTiverSiglaDaUf', () => {
    expect(formatRg('mg12345678')).toBe('MG-12.345.678');
  });

  it('naoDeveSepararDigito_quandoRgForCurto', () => {
    expect(formatRg('1234567')).toBe('1.234.567');
  });

  it('deveAcomodarEmissorLongo_quandoRgTiverMaisDeNoveDigitos', () => {
    expect(formatRg('RS1234567890')).toBe('RS-1.234.567.890');
  });

  it('deveManterValor_quandoRgJaEstiverFormatado', () => {
    for (const valor of ['12.345.678-9', 'MG-12.345.678', '12.345.678-X', '1.234.567']) {
      expect(formatRg(valor)).toBe(valor);
    }
  });

  it('naoDeveDescartarCaractere_quandoEstiverDentroDoLimite', () => {
    const bruto = 'MG12345678';
    expect(sanitizeRg(formatRg(bruto))).toBe(bruto);
  });

  it('deveManterValorLegivel_quandoSiglaEstiverIncompleta', () => {
    expect(formatRg('m')).toBe('M');
    expect(formatRg('mg')).toBe('MG');
    expect(formatRg('mg1')).toBe('MG-1');
  });

  it('naoDeveUltrapassarVinteCaracteres_quandoFormatarRg', () => {
    expect(formatRg('12345678901234567890'.repeat(2)).length).toBeLessThanOrEqual(20);
    expect(formatRg('RS12345678901234567890').length).toBeLessThanOrEqual(20);
  });

  it('deveRetornarTextoVazio_quandoValorEstiverAusente', () => {
    expect(formatRg('')).toBe('');
    expect(formatRg(null)).toBe('');
    expect(formatRg(undefined)).toBe('');
    expect(sanitizeRg(null)).toBe('');
  });
});

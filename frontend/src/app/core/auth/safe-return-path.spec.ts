import { safeReturnPath } from './safe-return-path';
describe('safeReturnPath', () => {
  it.each([
    '//evil.example',
    'https://evil.example',
    '/login?returnUrl=/login',
    'javascript:',
    '\\\\evil.example',
  ])('G14 deveDescartarRetornoInseguro_quandoValor_%s', (value) => {
    expect(safeReturnPath(value)).toBe('/');
  });
  it.each([
    '/',
    '/tenants',
    '/demo-registro/list',
    '/demo-registro/form',
    '/demo-registro/form/123e4567-e89b-12d3-a456-426614174000',
    '/demo-registro/view/123e4567-e89b-12d3-a456-426614174000',
  ])('G14 devePreservarRetornoInterno_quandoRotaCanonica_%s', (value) => {
    expect(safeReturnPath(value)).toBe(value);
  });
  it('naoDeveAceitarRedirecionamento_quandoExternoLoopOuEscape', () => {
    for (const value of [
      'https://evil.test',
      '//evil.test',
      '/login',
      '/login?returnTo=/login',
      '/demo-registro/%2f%2fevil.test',
      '/demo-registro/../login',
      '/\\evil.test',
      '/demo-registro?returnTo=//evil.test',
    ])
      expect(safeReturnPath(value)).toBe('/');
    expect(safeReturnPath('/demo-registro/list')).toBe('/demo-registro/list');
  });
});

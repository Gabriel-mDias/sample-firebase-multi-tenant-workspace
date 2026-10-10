import { safeReturnPath } from './safe-return-path';
describe('safeReturnPath', () => {
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

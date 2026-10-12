import { buildMenu } from './menu.builder';
import { Acoes } from '../auth/acoes';
describe('buildMenu', () => {
  it('naoDeveMostrarNegocio_quandoContextoSemAcao', () => {
    expect(buildMenu({ isAuthorized: () => false })).toEqual([]);
  });
  it('deveMostrarDemo_quandoAcaoTenantConcedida', () => {
    const check = vi.fn(() => true);
    const result = buildMenu({ isAuthorized: check });
    expect(result[0].route).toBe('/demo-registro/list');
    expect(check).toHaveBeenCalledWith([Acoes.CONSULTAR_DEMO], 'tenant');
  });
});

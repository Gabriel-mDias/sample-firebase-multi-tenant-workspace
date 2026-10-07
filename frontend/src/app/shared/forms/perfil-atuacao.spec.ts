import { describe, expect, it } from 'vitest';
import { PERFIL_ATUACAO_OPTIONS, perfilAtuacaoLabel } from './perfil-atuacao';

/**
 * Trava a lista local contra o enum do backend.
 *
 * Os valores abaixo são transcritos de `PerfilAtuacao.java`. A duplicação é deliberada: é ela que
 * faz este teste quebrar quando um perfil entra num lugar só.
 */
describe('PERFIL_ATUACAO_OPTIONS', () => {
  const DO_BACKEND = ['ADMINISTRADOR', 'OPERADOR', 'CONSULTA'];

  it('deveEspelharPerfisDoBackend_quandoListarOpcoes', () => {
    expect(PERFIL_ATUACAO_OPTIONS.map((o) => o.value)).toEqual(DO_BACKEND);
  });

  it('naoDeveTerRotuloVazio_quandoListarOpcoes', () => {
    PERFIL_ATUACAO_OPTIONS.forEach((o) => expect(o.label.trim()).not.toBe(''));
  });

  it('deveRetornarRotulo_quandoPerfilForConhecido', () => {
    expect(perfilAtuacaoLabel('OPERADOR')).toBe('Operador');
  });

  it('deveRetornarTextoVazio_quandoPerfilForAusenteOuDesconhecido', () => {
    expect(perfilAtuacaoLabel(null)).toBe('');
    expect(perfilAtuacaoLabel(undefined)).toBe('');
    expect(perfilAtuacaoLabel('PERFIL_QUE_NAO_EXISTE')).toBe('');
  });
});

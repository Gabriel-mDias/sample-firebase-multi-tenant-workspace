// Prova do validador compartilhado em `shared/validators`.
import { describe, expect, it } from 'vitest';
import { PASSWORD_PATTERN, passwordValidator } from './password.validator';

const mockControl = (value: unknown) => ({ value }) as any;

describe('passwordValidator', () => {
  it('devePermitirValorAusente_quandoObrigatoriedadeForExterna', () => {
    expect(passwordValidator(mockControl(''))).toBeNull();
    expect(passwordValidator(mockControl(null))).toBeNull();
    expect(passwordValidator(mockControl(undefined))).toBeNull();
  });

  it('deveValidar_quandoSenhaAtenderTodosOsCriterios', () => {
    expect(passwordValidator(mockControl('Senha@Forte123'))).toBeNull();
    expect(passwordValidator(mockControl('Admin#2026abc'))).toBeNull();
    expect(passwordValidator(mockControl('P@sswordLonga!'))).toBeNull();
    expect(passwordValidator(mockControl('aB1!aaaaaaaa'))).toBeNull();
  });

  it('deveRejeitar_quandoSenhaTiverMenosDeDozeCaracteres', () => {
    expect(passwordValidator(mockControl('Aa@1234'))).toEqual({ passwordPattern: true });
    // 11 caracteres, forte em todo o resto: aqui o que recusa é só o comprimento mínimo.
    expect(passwordValidator(mockControl('Aa@1234567b'))).toEqual({ passwordPattern: true });
  });

  it('deveRejeitar_quandoSenhaNaoTiverMaiuscula', () => {
    expect(passwordValidator(mockControl('senha@forte123'))).toEqual({ passwordPattern: true });
  });

  it('deveRejeitar_quandoSenhaNaoTiverMinuscula', () => {
    expect(passwordValidator(mockControl('SENHA@FORTE123'))).toEqual({ passwordPattern: true });
  });

  it('deveRejeitar_quandoSenhaNaoTiverCaractereEspecial', () => {
    expect(passwordValidator(mockControl('SenhaSemEspecial'))).toEqual({ passwordPattern: true });
  });

  it('deveRejeitar_quandoSenhaTiverEspaco', () => {
    expect(passwordValidator(mockControl('Senha @Forte123'))).toEqual({ passwordPattern: true });
  });

  it('deveCriarValidator_quandoChamadoComoFactory', () => {
    const fn = passwordValidator();
    expect(fn(mockControl('Senha@Forte123'))).toBeNull();
    expect(fn(mockControl('fraca'))).toEqual({ passwordPattern: true });
  });

  it('deveEspelharBackend_quandoUsarPasswordPattern', () => {
    expect(PASSWORD_PATTERN.test('Senha@Forte123')).toBe(true);
    expect(PASSWORD_PATTERN.test('invalida')).toBe(false);
  });
});

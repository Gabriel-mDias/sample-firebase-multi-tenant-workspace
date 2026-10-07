// Validador compartilhado entre formulários; não pertence a uma feature.
import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

/**
 * Padrão de senha exigido pelo backend (PasswordUtil.java):
 * - Pelo menos uma letra minúscula: (?=.*[a-z])
 * - Pelo menos uma letra maiúscula: (?=.*[A-Z])
 * - Pelo menos um caractere especial: (?=.*[@#$%^&+=!])
 * - Sem espaços em branco: (?=\S+$)
 * - No mínimo 12 caracteres: .{12,}$
 *
 * A POLÍTICA DE SENHA VIVE EM TRÊS LUGARES, E OS TRÊS MUDAM JUNTOS:
 *   1. este arquivo — a primeira barreira, no formulário;
 *   2. `PasswordUtil.java` (app-security) — a validação do backend;
 *   3. `passwordPolicy` em `environments/shared/keycloak/app-realm.yaml` — a última palavra.
 *
 * Divergência entre eles não aparece como erro de campo: o formulário aceita, o backend chama o
 * Keycloak, o Keycloak recusa, e o `IdentityProviderExceptionHandler` devolve **502** no meio do
 * wizard de cadastro de usuário. O operador vê "erro do servidor" onde deveria ver "a senha
 * precisa de 12 caracteres".
 *
 * O mínimo é 12, e não os 8 habituais: é o que a `passwordPolicy` do realm exige.
 */
export const PASSWORD_PATTERN = /^(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!])(?=\S+$).{12,}$/;

/**
 * Validador Angular para senhas fortes compatível com a validação do backend.
 * Pode ser utilizado diretamente como validador (`passwordValidator`) ou como fábrica (`passwordValidator()`).
 */
export function passwordValidator(control: AbstractControl): ValidationErrors | null;
export function passwordValidator(): ValidatorFn;
export function passwordValidator(
  control?: AbstractControl,
): ValidationErrors | null | ValidatorFn {
  const validate = (c: AbstractControl): ValidationErrors | null => {
    const value = c.value;
    if (!value) {
      return null;
    }
    return PASSWORD_PATTERN.test(value) ? null : { passwordPattern: true };
  };

  if (control) {
    return validate(control);
  }
  return validate;
}

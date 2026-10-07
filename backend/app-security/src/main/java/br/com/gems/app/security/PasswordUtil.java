package br.com.gems.app.security;

import br.com.gems.utils.ObjectUtil;
import lombok.experimental.UtilityClass;

/**
 * Validação de senha da aplicação — a <b>primeira</b> linha, e a mais estrita.
 *
 * <h2>Este arquivo e a política do realm mudam JUNTOS</h2>
 * O {@code passwordPolicy} de {@code environments/shared/keycloak/app-realm.yaml} espelha estas
 * regras. Se um subir o mínimo e o outro não, o cadastro de usuário aceita uma senha que o
 * Keycloak depois recusa — e a falha chega ao operador como <b>502</b> no meio do wizard (pelo
 * {@code IdentityProviderExceptionHandler}), não como validação de campo. O 502 é o
 * comportamento correto para "o provedor de identidade recusou"; o que não pode existir é a
 * divergência que o provoca.
 *
 * <p>
 * A validação daqui continua sendo a mais restritiva das duas: o {@code specialChars} do
 * Keycloak aceita qualquer não-alfanumérico, enquanto o padrão abaixo restringe a um conjunto.
 */
@UtilityClass
public class PasswordUtil {

    private static final String PASSWORD_PATTERN = "(?=.*[a-z])" + // Pelo menos uma letra minúscula
            "(?=.*[A-Z])" + // Pelo menos uma letra maiúscula
            "(?=.*[@#$%^&+=!])" + // Pelo menos um caractere especial
            "(?=\\S+$)" + // Sem espaços em branco
            ".{12,}$"; // No mínimo 12 caracteres

    /**
     * É validado se a string informada é uma senha válida. Para isso, é necessário:
     * <ul>
     * <li>Pelo menos uma letra minúscula;</li>
     * <li>Pelo menos uma letra maiúscula;</li>
     * <li>Pelo menos um caractere especial;</li>
     * <li>Sem espaços em branco;</li>
     * <li>No <strong>mínimo</strong> 12 caracteres.</li>
     * </ul>
     *
     * @param password
     *            a senha de texto limpo que será submetida à validação.
     * @return {@code true} se a senha atender a todos os critérios de segurança descritos;<br/>
     *         {@code false} se a string for nula, vazia ou falhe em alguma regra.
     */
    public static boolean isValid( String password ) {
        if ( ObjectUtil.isNullOrEmpty( password ) ) {
            return false;
        }

        return password.matches( PASSWORD_PATTERN );
    }

}

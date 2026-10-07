package br.com.gems.app.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Trava a política de senha da aplicação.
 *
 * <h2>Por que este teste existe</h2>
 * A política vive em <b>três</b> lugares, e os três precisam concordar:
 *
 * <ol>
 * <li>{@code frontend/src/app/commons/password.validator.ts} — a primeira barreira, no formulário;</li>
 * <li>{@link PasswordUtil} — a validação do backend, e a mais estrita das três;</li>
 * <li>{@code passwordPolicy} em {@code environments/shared/keycloak/app-realm.yaml} — a última
 * palavra, porque é o Keycloak quem grava a credencial.</li>
 * </ol>
 *
 * <p>
 * Divergência entre eles <b>não</b> aparece como erro de campo. O formulário aceita, o backend
 * chama o Keycloak, o Keycloak recusa, e o {@code IdentityProviderExceptionHandler} devolve
 * <b>502</b> no meio do wizard de cadastro de usuário — o operador vê "erro do servidor" onde
 * deveria ver "a senha precisa de 12 caracteres". Nenhum dos três pontos falha sozinho de forma
 * visível; por isso o mínimo é fixado aqui, num teste, e não só em comentário.
 *
 * <p>
 * O mínimo é 12, e subiu de 8 antes da primeira exposição pública.
 */
class PasswordUtilTest {

    private static final String MINIMA_VALIDA = "Senha@Forte1";

    @Test
    @DisplayName("a senha mínima válida tem exatamente 12 caracteres — é o contrato com o realm")
    void deveComprovarMinimoSaoDozeCaracteres_quandoExecutaOCenario() {
        assertThat( MINIMA_VALIDA ).hasSize( 12 );
        assertThat( PasswordUtil.isValid( MINIMA_VALIDA ) ).isTrue();
    }

    @Test
    @DisplayName("11 caracteres é recusado, mesmo satisfazendo todo o resto")
    void deveComprovarOnzeCaracteresEhRecusado_quandoExecutaOCenario() {
        var onze = "Senha@Fort1";

        assertThat( onze ).hasSize( 11 );
        assertThat( PasswordUtil.isValid( onze ) ).isFalse();
    }

    /**
     * Guarda o motivo pelo qual essa mudança não foi indolor: estas senhas eram válidas sob a
     * política de 8 caracteres e deixaram de ser. Elas continuam funcionando para quem <b>já</b> tem
     * conta — a política do Keycloak só se aplica a senha nova —, mas nenhum cadastro novo as aceita.
     */
    @Test
    @DisplayName("as senhas que passavam com o mínimo de 8 agora são recusadas")
    void deveComprovarSenhasDoRegimeAnteriorSaoRecusadas_quandoExecutaOCenario() {
        assertThat( PasswordUtil.isValid( "Senha@123" ) ).isFalse();
        assertThat( PasswordUtil.isValid( "Admin#2026" ) ).isFalse();
        assertThat( PasswordUtil.isValid( "aB1!aaaa" ) ).isFalse();
    }

    @Nested
    @DisplayName("cada exigência recusa sozinha")
    class CadaExigencia {

        @Test
        @DisplayName("sem minúscula")
        void deveComprovarSemMinuscula_quandoExecutaOCenario() {
            assertThat( PasswordUtil.isValid( "SENHA@FORTE1" ) ).isFalse();
        }

        @Test
        @DisplayName("sem maiúscula")
        void deveComprovarSemMaiuscula_quandoExecutaOCenario() {
            assertThat( PasswordUtil.isValid( "senha@forte1" ) ).isFalse();
        }

        /**
         * O conjunto de especiais do {@link PasswordUtil} é {@code [@#$%^&+=!]} — mais restrito que
         * o {@code specialChars} do Keycloak, que aceita qualquer não-alfanumérico. A assimetria é
         * deliberada e segura nesta direção: tudo que passa aqui passa lá. O inverso não vale, e é
         * por isso que a validação da aplicação precisa ser a primeira a rodar.
         */
        @Test
        @DisplayName("sem caractere especial do conjunto aceito")
        void deveComprovarSemEspecial_quandoExecutaOCenario() {
            assertThat( PasswordUtil.isValid( "SenhaForte12" ) ).isFalse();
            assertThat( PasswordUtil.isValid( "SenhaForte1~" ) )
                    .as( "'~' é especial para o Keycloak, mas não para o PasswordUtil" )
                    .isFalse();
        }

        @Test
        @DisplayName("com espaço em branco")
        void deveComprovarComEspaco_quandoExecutaOCenario() {
            assertThat( PasswordUtil.isValid( "Senha @Forte1" ) ).isFalse();
        }
    }

    @Test
    @DisplayName("nula e vazia são recusadas sem estourar")
    void deveComprovarNulaEVaziaSaoRecusadas_quandoExecutaOCenario() {
        assertThat( PasswordUtil.isValid( null ) ).isFalse();
        assertThat( PasswordUtil.isValid( "" ) ).isFalse();
        assertThat( PasswordUtil.isValid( "   " ) ).isFalse();
    }
}

package br.com.gems.app.model.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Perfil de atuação de um usuário do sistema.
 *
 * <p>
 * Cada perfil corresponde a um <b>grupo</b> no realm do Keycloak, e é o grupo que carrega a role
 * composta — {@code ADMIN} para o administrador, {@code PERFIL_<NOME>} para os demais. O sistema
 * nunca atribui role a usuário: ele apenas coloca o usuário no grupo certo, e o Keycloak resolve a
 * herança ao emitir o token. Por isso {@link #administrador} vive aqui, no enum, e não espalhado em
 * {@code if} pelo serviço.
 *
 * <p>
 * Três perfis bastam para exercitar o modelo: um que tem tudo, um que opera, e um que só lê. Um
 * projeto derivado troca esta lista e a matriz do {@code MapaPerfilAcoes} juntas — são as duas
 * metades da mesma decisão.
 */
@Getter
@RequiredArgsConstructor
public enum PerfilAtuacao {

    ADMINISTRADOR("Administrador", "ADMINISTRADOR", true), OPERADOR("Operador", "OPERADOR", false), CONSULTA("Consulta",
            "CONSULTA", false);

    private final String descricao;

    /** Nome do grupo correspondente no realm do Keycloak. */
    private final String nomeGrupo;

    /** {@code true} quando o grupo do perfil carrega a role composta ADMIN. */
    private final boolean administrador;

}

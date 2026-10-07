package br.com.gems.app.security;

/**
 * Roles do realm que <b>não</b> são ações.
 *
 * <p>
 * O vocabulário de autorização deste projeto é o {@link AcaoSistema}: role é ação, e grupo é
 * perfil de atuação. Restou aqui uma única constante, e ela existe por um motivo específico.
 *
 * <h2>Por que ADMIN existe</h2>
 * {@code ADMIN} não é um <i>nível</i>: é a <b>role composta que contém todas as ações</b>. É o que
 * faz o perfil administrador receber uma ação nova sem que ninguém precise lembrar de concedê-la, e
 * sem nenhum caso especial no código: {@code hasRole('EXCLUIR_PARCEIRO')} é verdadeiro para ele
 * porque o Keycloak expande a composta dentro de {@code realm_access.roles}.
 *
 * <p>
 * É também a razão de {@code hasAnyRole(..., 'ADMIN')} ser <b>proibido</b> (Princípio V):
 * a composta já expande, e incluí-la à mão sugere uma exceção que o modelo não tem. Quem se pegar
 * escrevendo isso deve corrigir a composta do perfil, não a anotação.
 *
 * <h2>Por que não existe uma role genérica de "usuário logado"</h2>
 * Uma role assim não significa nada depois que as ações existem — ela é justamente o que deixaria
 * dois perfis diferentes indistinguíveis para o backend. Dado referencial sem regra de negócio usa
 * {@code isAuthenticated()}, que não precisa de role nenhuma.
 *
 * <p>
 * Se um projeto derivado herdar uma role legada desse tipo, aposente-a com um
 * <b>desmapeamento explícito</b> no bootstrap: {@code mapearRoleNoGrupo} nunca remove mapeamento,
 * então sem isso todo grupo continuaria carregando a role para sempre, e a aposentadoria seria
 * apenas nominal.
 *
 * <p>
 * Os nomes são exatamente como cadastrados no realm (case-sensitive). O {@code SecurityConfig}
 * prefixa cada realm role com {@code ROLE_} e a converte para MAIÚSCULAS ao montar as authorities,
 * então {@code hasRole('X')} casa com a authority {@code ROLE_X}; as constantes {@code AUTHORITY_*}
 * já trazem o prefixo aplicado, para uso direto em {@code hasAuthority(...)}.
 *
 * @see AcaoSistema
 */
public final class SecurityRoles {

    private SecurityRoles() {
    }

    /** Composta que contém <b>todas</b> as ações do {@link AcaoSistema}. Mapeada só no grupo do administrador. */
    public static final String ADMIN = "ADMIN";

    /** Prefixo aplicado pelo {@code SecurityConfig} ao converter uma realm role em authority. */
    public static final String PREFIXO_AUTHORITY = "ROLE_";

    // Authority já com o prefixo ROLE_ (como o SecurityConfig a expõe).
    public static final String AUTHORITY_ADMIN = PREFIXO_AUTHORITY + ADMIN;
}

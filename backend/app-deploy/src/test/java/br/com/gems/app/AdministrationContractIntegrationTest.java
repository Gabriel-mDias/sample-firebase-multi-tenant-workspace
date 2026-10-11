package br.com.gems.app;

import br.com.gems.app.support.FirebaseContractSupport;
import br.com.gems.firebase.auth.GrantAdministration;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(FirebaseBackendIntegrationTest.Ports.class)
class AdministrationContractIntegrationTest extends FirebaseContractSupport {
    private static final String CENTRAL = "/api/admin/central";
    private static final String TENANT = "/api/admin/tenants/{tenant}";
    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    RequestMappingHandlerMapping mapping;

    @Test
    void deveCriarPrimeiroAdminComAcaoGlobalDistinta_quandoCentralNaoTemMembership() throws Exception {
        var central = bind( "bootstrapcentral" );
        grants.grantGlobal( central.id(), "CRIAR_ADMINISTRADOR_TENANT" );
        UUID tenant = tenants.create( unique( "primeiro" ), "Primeiro administrador" );
        var body = administratorBody( subject.id(), catalog.tenantActions() );

        var response = mvc.perform( firstAdministrator( tenant, body, central.uid() ) )
                .andExpect( status().isCreated() ).andReturn().getResponse();

        UUID membership = responseId( response );
        assertFirstAdministrator( tenant, subject.id(), membership );
        assertThat( jdbc.sql( "SELECT count(*) FROM security.usuario_tenant WHERE id_usuario=:id" )
                .param( "id", central.id() ).query( Long.class ).single() ).isZero();
        assertThat( jdbc.sql( "SELECT count(*) FROM security.evento_seguranca "
                + "WHERE id_tenant=:tenant AND id_ator=:actor" ).param( "tenant", tenant )
                .param( "actor", central.id() ).query( Long.class ).single() ).isPositive();
    }

    @Test
    void deveRecusarSemAcaoGlobalSemParciais_quandoCentralOuTenantAdminTentaBootstrap() throws Exception {
        UUID tenant = tenants.create( unique( "negado" ), "Sem administrador" );
        revokeAction( "CRIAR_ADMINISTRADOR_TENANT", false );
        String body = administratorBody( subject.id(), catalog.tenantActions() );
        var before = securityState();

        mvc.perform( firstAdministrator( tenant, body, operator.uid() ) ).andExpect( status().isForbidden() );
        mvc.perform( firstAdministrator( tenant, body, tenantAdmin.uid() ).header( "X-Tenant-Alias", alpha ) )
                .andExpect( status().isForbidden() );

        assertThat( securityState() ).as( "Negação não cria membership, grant, evento nem outbox parcial." )
                .isEqualTo( before );
    }

    @Test
    void deveRollbackIntegral_quandoBootstrapIncluiAcaoGlobalDepoisDeAcaoTenantValida() throws Exception {
        UUID tenant = tenants.create( unique( "rollback" ), "Bootstrap inválido" );
        var before = securityState();
        String body = "{\"userId\":\"" + subject.id()
                + "\",\"actions\":[\"CONSULTAR_DEMO\",\"CONSULTAR_TENANT\"]}";

        var response = mvc.perform( firstAdministrator( tenant, body, operator.uid() ) )
                .andReturn().getResponse();

        assertThat( response.getStatus() ).isIn( 400, 403, 409 );
        assertThat( securityState() ).as( "Bootstrap inválido deve desfazer todas as escritas." ).isEqualTo( before );
    }

    @Test
    void devePreservarPrimeiroAdminEHistorico_quandoComandoERepetido() throws Exception {
        UUID tenant = tenants.create( unique( "repetido" ), "Idempotência" );
        String body = administratorBody( subject.id(), catalog.tenantActions() );
        var first = mvc.perform( firstAdministrator( tenant, body, operator.uid() ) )
                .andExpect( status().isCreated() ).andReturn().getResponse();
        UUID membership = responseId( first );
        assertFirstAdministrator( tenant, subject.id(), membership );
        assertThat( bootstrapEvents( tenant ) ).as( "Um único evento corresponde à eleição inicial." ).isOne();
        var before = firstAdministratorState( tenant );

        var repeated = mvc.perform( firstAdministrator( tenant, body, operator.uid() ) ).andReturn().getResponse();

        assertThat( repeated.getStatus() ).isEqualTo( 201 );
        assertThat( responseId( repeated ) ).isEqualTo( membership );
        assertThat( firstAdministratorState( tenant ) )
                .as( "Repetição preserva membership, grants, identidade e eventos do comando." ).isEqualTo( before );
        var different = mvc.perform( firstAdministrator( tenant,
                administratorBody( tenantAdmin.id(), catalog.tenantActions() ), operator.uid() ) )
                .andReturn().getResponse();
        assertThat( different.getStatus() ).isEqualTo( 403 );
        assertThat( firstAdministratorState( tenant ) ).isEqualTo( before );
        assertFirstAdministrator( tenant, subject.id(), membership );
    }

    @Test
    void deveElegerUmPrimeiroAdminSemParciais_quandoDoisUsuariosDisputamBootstrap() throws Exception {
        UUID tenant = tenants.create( unique( "disputa" ), "Concorrência" );
        var started = new CountDownLatch( 2 );
        var release = new CountDownLatch( 1 );
        var beforeEvents = securityEvents();
        try ( var pool = Executors.newVirtualThreadPerTaskExecutor() ) {
            var first = pool.submit( () -> concurrentBootstrap( tenant, subject.id(), started, release ) );
            var second = pool.submit( () -> concurrentBootstrap( tenant, tenantAdmin.id(), started, release ) );
            try {
                assertThat( started.await( 5, TimeUnit.SECONDS ) ).isTrue();
            } finally {
                release.countDown();
            }
            var responses = List.of( first.get( 10, TimeUnit.SECONDS ), second.get( 10, TimeUnit.SECONDS ) );
            assertThat( jdbc.sql( "SELECT count(*) FROM security.usuario_tenant WHERE id_tenant=:tenant" )
                    .param( "tenant", tenant ).query( Long.class ).single() )
                    .as( "Tenant recém-criado deve ter um único primeiro administrador, inclusive no histórico." )
                    .isOne();
            assertThat( responses.stream().filter( response -> response.getStatus() == 201 ) ).hasSize( 1 );
            assertThat( responses.stream().filter( response -> response.getStatus() != 201 ) )
                    .allSatisfy( response -> assertThat( response.getStatus() ).isEqualTo( 403 ) );
            UUID membership = responseId( responses.stream().filter( response -> response.getStatus() == 201 )
                    .findFirst().orElseThrow() );
            UUID winner = jdbc.sql( "SELECT id_usuario FROM security.usuario_tenant WHERE id_usuario_tenant=:id" )
                    .param( "id", membership ).query( UUID.class ).single();
            assertFirstAdministrator( tenant, winner, membership );
            assertThat( bootstrapEvents( tenant ) ).as( "Disputa emite um único evento de eleição." ).isOne();
            assertThat( securityEvents().stream().filter( event -> !beforeEvents.contains( event ) )
                    .filter( event -> tenant.equals( event.get( "id_tenant" ) ) ) )
                    .isNotEmpty().allSatisfy( event -> {
                        assertThat( event.get( "id_ator" ) ).isEqualTo( operator.id() );
                        if ( event.get( "id_usuario" ) instanceof UUID user ) {
                            assertThat( user ).isEqualTo( winner );
                        }
                    } );
        }
    }

    @Test
    void deveCobrirTodasAsRotasAdministrativas_quandoMappingRealEComparadoComMatriz() {
        var actual = new HashSet<String>();
        mapping.getHandlerMethods().forEach( ( route, handler ) -> {
            if ( handler.getBeanType().getPackageName().equals( "br.com.gems.app.administracao.controller" ) ) {
                route.getMethodsCondition().getMethods().forEach( method -> route.getPatternValues()
                        .forEach( path -> actual.add( method.name() + " " + path ) ) );
            }
        } );
        var expected = new HashSet<String>();
        routes().forEach( route -> expected.add( route.method() + " " + route.pattern() ) );

        assertThat( actual ).containsExactlyInAnyOrderElementsOf( expected );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("routes")
    void deveAutorizarSoAcaoEspecificaEProprioTenant_quandoRotaAdministrativaEExecutada( Route route )
            throws Exception {
        var prepared = prepareRoute( route );
        revokeAction( route.action(), route.tenant() );
        var deniedState = securityState();

        mvc.perform( routeRequest( route, prepared, prepared.path() ) ).andExpect( status().isForbidden() );

        assertThat( securityState() ).as( "Sem mutação na ausência de %s.", route.action() ).isEqualTo( deniedState );
        if ( route.tenant() ) {
            grants.grant( GrantAdministration.Target.MEMBER, alphaId, operatorMember, route.action() );
            var foreignState = securityState();
            String foreignPath = prepared.path().replace( alphaId.toString(), betaId.toString() );
            mvc.perform( routeRequest( route, prepared, foreignPath ) )
                    .andExpect( status().isForbidden() );
            assertThat( securityState() ).as( "Tenant da rota diverge do snapshot autenticado." )
                    .isEqualTo( foreignState );
        } else {
            grants.grantGlobal( operator.id(), route.action() );
        }
        var before = securityState();
        var beforeEvents = securityEvents();
        var response = mvc.perform( routeRequest( route, prepared, prepared.path() ) )
                .andExpect( status().is( route.status() ) ).andReturn().getResponse();

        assertRouteResult( route, prepared, response, before, beforeEvents );
    }

    private MockHttpServletResponse concurrentBootstrap( UUID tenant, UUID user, CountDownLatch started,
            CountDownLatch release ) throws Exception {
        started.countDown();
        if ( !release.await( 5, TimeUnit.SECONDS ) ) {
            throw new IllegalStateException( "O teste excedeu o limite de concorrência." );
        }
        return mvc.perform( firstAdministrator( tenant, administratorBody( user, catalog.tenantActions() ),
                operator.uid() ) ).andReturn().getResponse();
    }

    /** Estado durável do comando; agendamento de entrega e comandos de outros testes ficam fora da comparação. */
    private Map<String, String> firstAdministratorState( UUID tenant ) {
        var state = new TreeMap<String, String>();
        for ( String table : List.of( "usuario_tenant", "usuario_tenant_role", "evento_seguranca" ) ) {
            state.put( table, jdbc.sql( "SELECT coalesce(jsonb_agg(to_jsonb(row) "
                    + "ORDER BY to_jsonb(row)::text),'[]'::jsonb)::text FROM security." + table
                    + " row WHERE id_tenant=:tenant" ).param( "tenant", tenant ).query( String.class ).single() );
        }
        state.put( "usuario_identidade", jdbc.sql( "SELECT coalesce(jsonb_agg(to_jsonb(row) "
                + "ORDER BY to_jsonb(row)::text),'[]'::jsonb)::text FROM security.usuario_identidade row "
                + "WHERE id_usuario IN (:users)" ).param( "users", List.of( subject.id(), tenantAdmin.id() ) )
                .query( String.class ).single() );
        return state;
    }

    private long bootstrapEvents( UUID tenant ) {
        return jdbc.sql( "SELECT count(*) FROM security.evento_seguranca "
                + "WHERE id_tenant=:tenant AND cd_evento='TENANT_ADMIN_BOOTSTRAPPED'" )
                .param( "tenant", tenant ).query( Long.class ).single();
    }

    private MockHttpServletRequestBuilder firstAdministrator( UUID tenant, String body, String uid ) {
        return post( CENTRAL + "/tenants/" + tenant + "/first-administrator" )
                .header( "Authorization", "Bearer " + uid ).contentType( "application/json" ).content( body );
    }

    private String administratorBody( UUID user, Set<String> actions ) {
        String array = actions.stream().sorted().map( action -> "\"" + action + "\"" )
                .collect( java.util.stream.Collectors.joining( "," ) );
        return "{\"userId\":\"" + user + "\",\"actions\":[" + array + "]}";
    }

    private void assertFirstAdministrator( UUID tenant, UUID user, UUID membership ) {
        var members = jdbc.sql( "SELECT id_usuario_tenant,id_usuario FROM security.usuario_tenant "
                + "WHERE id_tenant=:id" ).param( "id", tenant ).query().listOfRows();
        assertThat( members ).containsExactly( Map.of( "id_usuario_tenant", membership, "id_usuario", user ) );
        assertThat( jdbc.sql( "SELECT r.cd_acao FROM security.usuario_tenant_role g "
                + "JOIN security.role_seguranca r ON r.id_role=g.id_role "
                + "WHERE g.id_usuario_tenant=:id AND g.dt_fim IS NULL" ).param( "id", membership )
                .query( String.class ).list() ).containsExactlyInAnyOrderElementsOf( catalog.tenantActions() );
    }

    private UUID responseId( MockHttpServletResponse response ) throws Exception {
        return UUID.fromString( com.jayway.jsonpath.JsonPath.read( response.getContentAsString(), "$.id" ) );
    }

    private MockHttpServletRequestBuilder routeRequest( Route route, Prepared prepared, String path ) {
        var builder = request( HttpMethod.valueOf( route.method() ), path )
                .header( "Authorization", "Bearer " + operator.uid() )
                .contentType( "application/json" ).content( prepared.body() );
        if ( route.tenant() ) {
            builder.header( "X-Tenant-Alias", alpha );
        }
        return builder;
    }

    private Prepared prepareRoute( Route route ) {
        String path = route.pattern().replace( "{tenant}", alphaId.toString() );
        if ( route.pattern().endsWith( "/search" ) ) {
            if ( route.pattern().endsWith( "/profiles/search" ) ) {
                grants.createProfile( alphaId, unique( "consulta" ), "Perfil consultável" );
            } else if ( route.pattern().endsWith( "/groups/search" ) ) {
                grants.createGroup( alphaId, unique( "consulta" ), "Grupo consultável" );
            }
            return new Prepared( path.replace( "{id}", subject.id().toString() ), queryBody(), "", null, Map.of() );
        }
        return switch ( route.kind() ) {
            case "USER_CREATE" -> new Prepared( path, "{\"name\":\"Novo\",\"email\":\""
                    + unique( "novo" ) + "@example.test\",\"method\":\"GOOGLE\"}", "usuario", null,
                    Map.of( "nm_usuario", "Novo" ) );
            case "USER_UPDATE" -> new Prepared( path.replace( "{id}", subject.id().toString() ),
                    "{\"name\":\"Renomeado\",\"email\":\"" + subject.email() + "\"}", "usuario", subject.id(),
                    Map.of( "nm_usuario", "Renomeado", "cd_email", subject.email() ) );
            case "USER_SUSPEND" -> new Prepared( path.replace( "{id}", subject.id().toString() ), "{}",
                    "usuario", subject.id(), Map.of() );
            case "USER_REACTIVATE" -> prepareReactivation( path );
            case "TENANT_CREATE" -> prepareTenantCreation( path );
            case "TENANT_RENAME" -> new Prepared( path.replace( "{id}", alphaId.toString() ),
                    "{\"name\":\"Renomeado\"}", "tenant", alphaId,
                    Map.of( "nm_tenant", "Renomeado", "cd_alias", alpha ) );
            case "TENANT_SUSPEND" -> new Prepared( path.replace( "{id}", alphaId.toString() ), "{}", "tenant",
                    alphaId, Map.of() );
            case "FIRST" -> prepareFirst( path );
            case "GLOBAL_GRANT", "GLOBAL_REVOKE" -> prepareGlobalGrant( route, path );
            case "MEMBER_CREATE" -> {
                var account = bind( "semvinculo" );
                yield new Prepared( path, "{\"userId\":\"" + account.id() + "\"}", "usuario_tenant", null,
                        Map.of( "id_usuario", account.id(), "id_tenant", alphaId ) );
            }
            case "MEMBER_END" -> new Prepared( path.replace( "{id}", subjectMember.toString() ), "{}",
                    "usuario_tenant", subjectMember, Map.of( "id_usuario", subject.id(), "id_tenant", alphaId ) );
            case "PROFILE_CREATE", "GROUP_CREATE" -> prepareContainer( route, path );
            case "GRANT", "REVOKE" -> prepareTenantGrant( route, path );
            case "LINK", "UNLINK" -> prepareLink( route, path );
            default -> throw new IllegalArgumentException( "Rota fora do contrato." );
        };
    }

    private Prepared prepareReactivation( String path ) {
        users.suspend( subject.id() );
        postponeCommands();
        return new Prepared( path.replace( "{id}", subject.id().toString() ), "{}", "usuario", subject.id(), Map.of() );
    }

    private Prepared prepareTenantCreation( String path ) {
        String alias = unique( "criado" );
        return new Prepared( path, "{\"alias\":\"" + alias + "\",\"name\":\"Criado\"}", "tenant", null,
                Map.of( "cd_alias", alias, "nm_tenant", "Criado", "cd_provisionamento", "PENDING" ) );
    }

    private Prepared prepareFirst( String path ) {
        UUID tenant = tenants.create( unique( "matriz" ), "Primeiro administrador" );
        return new Prepared( path.replace( "{id}", tenant.toString() ),
                administratorBody( subject.id(), catalog.tenantActions() ), "usuario_tenant", null,
                Map.of( "id_usuario", subject.id(), "id_tenant", tenant ) );
    }

    private Prepared prepareGlobalGrant( Route route, String path ) {
        UUID grant = null;
        if ( route.kind().equals( "GLOBAL_REVOKE" ) ) {
            grant = grants.grantGlobal( subject.id(), "CONSULTAR_TENANT" );
            path = path.replace( "{grant}", grant.toString() );
        }
        return new Prepared( path.replace( "{id}", subject.id().toString() ),
                "{\"action\":\"CONSULTAR_TENANT\"}", "usuario_role_global", grant,
                Map.of( "id_usuario", subject.id() ) );
    }

    private Prepared prepareContainer( Route route, String path ) {
        String code = unique( "container" );
        String table = route.kind().equals( "PROFILE_CREATE" ) ? "perfil" : "grupo";
        return new Prepared( path, "{\"code\":\"" + code + "\",\"name\":\"Novo container\"}", table, null,
                Map.of( "id_tenant", alphaId, "cd_" + table, code ) );
    }

    private Prepared prepareTenantGrant( Route route, String path ) {
        String target = route.variant();
        UUID owner = switch ( target ) {
            case "MEMBER" -> subjectMember;
            case "PROFILE" -> grants.createProfile( alphaId, unique( "perfil" ), "Perfil" );
            case "GROUP" -> grants.createGroup( alphaId, unique( "grupo" ), "Grupo" );
            default -> throw new IllegalArgumentException( "Destino fora do contrato." );
        };
        String table = switch ( target ) {
            case "MEMBER" -> "usuario_tenant_role";
            case "PROFILE" -> "perfil_role";
            case "GROUP" -> "grupo_role";
            default -> throw new IllegalArgumentException( "Destino fora do contrato." );
        };
        UUID grant = null;
        if ( route.kind().equals( "REVOKE" ) ) {
            grant = grants.grant( GrantAdministration.Target.valueOf( target ), alphaId, owner, "CONSULTAR_DEMO" );
            path = path.replace( "{id}", grant.toString() );
        }
        path = path.replace( "{target}", target );
        return new Prepared( path, "{\"ownerId\":\"" + owner + "\",\"action\":\"CONSULTAR_DEMO\"}", table, grant,
                Map.of( "id_tenant", alphaId, "id_role", role( "CONSULTAR_DEMO" ) ) );
    }

    private UUID role( String action ) {
        return jdbc.sql( "SELECT id_role FROM security.role_seguranca WHERE cd_acao=:action" )
                .param( "action", action ).query( UUID.class ).single();
    }

    private Prepared prepareLink( Route route, String path ) {
        var link = GrantAdministration.Link.valueOf( route.variant() );
        UUID profile = grants.createProfile( alphaId, unique( "perfil" ), "Perfil" );
        UUID group = grants.createGroup( alphaId, unique( "grupo" ), "Grupo" );
        UUID left = link == GrantAdministration.Link.MEMBER_PROFILE ? subjectMember : group;
        UUID right = link == GrantAdministration.Link.GROUP_MEMBER ? subjectMember : profile;
        String table = switch ( link ) {
            case MEMBER_PROFILE -> "usuario_tenant_perfil";
            case GROUP_MEMBER -> "grupo_usuario";
            case GROUP_PROFILE -> "grupo_perfil";
            default -> throw new IllegalArgumentException( "Relacionamento fora do contrato." );
        };
        UUID id = null;
        if ( route.kind().equals( "UNLINK" ) ) {
            id = grants.link( link, alphaId, left, right );
            path = path.replace( "{id}", id.toString() );
        }
        return new Prepared( path.replace( "{link}", route.variant() ),
                "{\"leftId\":\"" + left + "\",\"rightId\":\"" + right + "\"}", table, id,
                Map.of( "id_tenant", alphaId ) );
    }

    private void assertRouteResult( Route route, Prepared prepared, MockHttpServletResponse response,
            Map<String, String> before, List<Map<String, Object>> beforeEvents ) throws Exception {
        if ( route.pattern().endsWith( "/search" ) ) {
            Map<String, Object> page = com.jayway.jsonpath.JsonPath.read( response.getContentAsString(), "$" );
            var lists = page.values().stream().filter( List.class::isInstance )
                    .map( value -> (List<?>) value ).toList();
            assertThat( lists ).hasSize( 1 );
            assertThat( lists.getFirst() ).as( "Consulta retorna os registros SQL preparados." ).isNotEmpty();
            assertThat( securityState() ).as( "Consulta não altera estado nem eventos." ).isEqualTo( before );
            return;
        }
        UUID id = route.status() == 201 ? responseId( response ) : prepared.id();
        var row = jdbc.sql( "SELECT * FROM security." + prepared.table() + " WHERE id_" + prepared.table() + "=:id" )
                .param( "id", id ).query().singleRow();
        assertThat( row ).containsAllEntriesOf( prepared.expected() );
        if ( route.method().equals( "DELETE" ) || route.kind().endsWith( "SUSPEND" ) ) {
            assertThat( row.get( "dt_fim" ) ).isNotNull();
        } else if ( route.kind().equals( "USER_REACTIVATE" ) ) {
            assertThat( row.get( "dt_fim" ) ).as( "ENABLE pendente mantém bloqueio local." ).isNotNull();
        } else {
            assertThat( row.get( "dt_fim" ) ).isNull();
        }
        if ( route.kind().equals( "USER_CREATE" ) ) {
            assertThat( identities( id ) ).isZero();
            assertThat( jdbc.sql( "SELECT count(*) FROM security.usuario_role_global WHERE id_usuario=:id" )
                    .param( "id", id ).query( Long.class ).single() ).isOne();
        }
        if ( route.kind().equals( "USER_SUSPEND" ) || route.kind().equals( "USER_REACTIVATE" ) ) {
            String type = route.kind().equals( "USER_SUSPEND" ) ? "DISABLE" : "ENABLE";
            assertThat( jdbc.sql( "SELECT count(*) FROM security.comando_identidade "
                    + "WHERE id_usuario=:id AND cd_tipo=:type AND cd_estado='PENDING'" )
                    .param( "id", id ).param( "type", type ).query( Long.class ).single() ).isPositive();
        }
        if ( route.kind().equals( "FIRST" ) ) {
            assertFirstAdministrator( (UUID) prepared.expected().get( "id_tenant" ), subject.id(), id );
        }
        assertThat( securityEvents().stream().filter( event -> !beforeEvents.contains( event ) ) )
                .as( "Evento durável correspondente à rota %s.", route.pattern() )
                .isNotEmpty().allSatisfy( event -> assertThat( event.get( "id_ator" ) ).isEqualTo( operator.id() ) );
    }

    private static Stream<Route> routes() {
        var central = Stream.of(
                new Route( "POST", CENTRAL + "/users/search", "CONSULTAR_USUARIO", 200, "QUERY", "" ),
                new Route( "POST", CENTRAL + "/users", "INSERIR_USUARIO", 201, "USER_CREATE", "" ),
                new Route( "PUT", CENTRAL + "/users/{id}", "ALTERAR_USUARIO", 200, "USER_UPDATE", "" ),
                new Route( "POST", CENTRAL + "/users/{id}/suspend", "SUSPENDER_USUARIO", 200, "USER_SUSPEND", "" ),
                new Route( "POST", CENTRAL + "/users/{id}/reactivate", "REATIVAR_USUARIO", 200, "USER_REACTIVATE", "" ),
                new Route( "POST", CENTRAL + "/tenants/search", "CONSULTAR_TENANT", 200, "QUERY", "" ),
                new Route( "POST", CENTRAL + "/tenants", "INSERIR_TENANT", 201, "TENANT_CREATE", "" ),
                new Route( "PUT", CENTRAL + "/tenants/{id}", "ALTERAR_TENANT", 200, "TENANT_RENAME", "" ),
                new Route( "POST", CENTRAL + "/tenants/{id}/suspend", "SUSPENDER_TENANT", 200, "TENANT_SUSPEND", "" ),
                new Route( "POST", CENTRAL + "/users/{id}/global-grants/search", "CONSULTAR_ACAO_GLOBAL", 200,
                        "QUERY", "" ),
                new Route( "POST", CENTRAL + "/users/{id}/global-grants", "CONCEDER_ACAO_GLOBAL", 201,
                        "GLOBAL_GRANT", "" ),
                new Route( "DELETE", CENTRAL + "/users/{id}/global-grants/{grant}", "REVOGAR_ACAO_GLOBAL", 204,
                        "GLOBAL_REVOKE", "" ),
                new Route( "POST", CENTRAL + "/tenants/{id}/first-administrator", "CRIAR_ADMINISTRADOR_TENANT", 201,
                        "FIRST", "" ) );
        var tenant = Stream.of(
                new Route( "POST", TENANT + "/memberships/search", "CONSULTAR_MEMBERSHIP", 200, "QUERY", "" ),
                new Route( "POST", TENANT + "/grants/search", "CONSULTAR_GRANT", 200, "QUERY", "" ),
                new Route( "POST", TENANT + "/memberships", "INSERIR_MEMBERSHIP", 201, "MEMBER_CREATE", "" ),
                new Route( "DELETE", TENANT + "/memberships/{id}", "ENCERRAR_MEMBERSHIP", 204, "MEMBER_END", "" ),
                new Route( "POST", TENANT + "/grants", "CONCEDER_GRANT", 201, "GRANT", "MEMBER" ),
                new Route( "DELETE", TENANT + "/grants/{id}", "REVOGAR_GRANT", 204, "REVOKE", "MEMBER" ),
                new Route( "POST", TENANT + "/profiles/search", "CONSULTAR_GRANT", 200, "QUERY", "" ),
                new Route( "POST", TENANT + "/groups/search", "CONSULTAR_GRANT", 200, "QUERY", "" ),
                new Route( "POST", TENANT + "/profiles", "CONCEDER_GRANT", 201, "PROFILE_CREATE", "" ),
                new Route( "POST", TENANT + "/groups", "CONCEDER_GRANT", 201, "GROUP_CREATE", "" ) );
        var targets = Arrays.stream( GrantAdministration.Target.values() ).flatMap( target -> Stream.of(
                new Route( "POST", TENANT + "/grants/targets/{target}", "CONCEDER_GRANT", 201, "GRANT", target.name() ),
                new Route( "DELETE", TENANT + "/grants/targets/{target}/{id}", "REVOGAR_GRANT", 204,
                        "REVOKE", target.name() ) ) );
        var links = Arrays.stream( GrantAdministration.Link.values() ).flatMap( link -> Stream.of(
                new Route( "POST", TENANT + "/grants/links/{link}", "CONCEDER_GRANT", 201, "LINK", link.name() ),
                new Route( "DELETE", TENANT + "/grants/links/{link}/{id}", "REVOGAR_GRANT", 204,
                        "UNLINK", link.name() ) ) );
        return Stream.of( central, tenant, targets, links ).flatMap( stream -> stream );
    }

    private record Route(String method, String pattern, String action, int status, String kind, String variant) {
        boolean tenant() {
            return pattern.startsWith( "/api/admin/tenants/" );
        }
    }

    private record Prepared(String path, String body, String table, UUID id, Map<String, Object> expected) {
    }
}

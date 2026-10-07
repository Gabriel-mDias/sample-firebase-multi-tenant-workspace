---
stack: backend-java
module: gems-exception, gems-security-authorization, spring-security
severity: mandatory
see-also: [rest-common.md, controllers.md, openapi.md]
origem: local
precedencia: local-vence
versao: 2.2.1
---

# Segurança & Autorização — Spring Security + gems-security-authorization + gems-exception

> **TL;DR:**
> - Backend como OAuth2 Resource Server: valida JWT gerado pelo IdP (Keycloak ou similar) — não gerencia sessões.
> - **Autorização é por ação, nunca por nível.** O catálogo é **um enum** que implementa `AuthorizationAction`
>   com escopo; todo endpoint de negócio leva `@PreAuthorize("hasRole('<ACAO>')")` com uma ação desse enum.
> - `hasAnyRole('ADMIN')`, `hasRole('ADMINISTRADOR')` ou qualquer nome que não seja ação do catálogo é **proibido**.
> - O JSON de ações do frontend é **gerado** do enum e verificado no build — nunca editado à mão.
> - `gems-exception` responde **403** com envelope a `@PreAuthorize` negado; **401** sem token é do entry point, sem corpo.
> - **NUNCA** armazene credenciais ou segredos no código — use variáveis de ambiente ou Vault.
> - Frontend usa PKCE + Authorization Code — backend apenas valida o token.

## 1. Configuração do Resource Server

O backend valida tokens JWT emitidos pelo IdP (ex.: Keycloak) — não faz login nem gerencia sessões:

```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain( HttpSecurity http ) throws Exception {
        http
            .csrf( AbstractHttpConfigurer::disable )
            .sessionManagement( session -> session.sessionCreationPolicy( SessionCreationPolicy.STATELESS ) )
            .authorizeHttpRequests( auth -> auth
                .requestMatchers( "/actuator/health", "/v3/api-docs/**", "/swagger-ui/**" ).permitAll()
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer( oauth2 -> oauth2.jwt( Customizer.withDefaults() ) );
        return http.build();
    }

    /** O catálogo é derivado do enum — uma fonte, nenhuma lista à mão. */
    @Bean
    public AuthorizationCatalog authorizationCatalog() {
        return AuthorizationCatalog.of( AcaoSistema.class );
    }
}
```

```yaml
# application.yml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: https://{idp-host}/realms/{realm}
          jwk-set-uri: https://{idp-host}/realms/{realm}/protocol/openid-connect/certs
```

---

## 2. Autorização por ação

Autorização é **por ação concreta**, nunca por nível ou perfil (`ADMIN`, `GESTOR`, `SUPERVISOR`). O
perfil existe no IdP, como agrupamento de ações; o backend só conhece **ações**.

### 2.1 O catálogo: um enum, fonte única

```java
/** O catálogo de ações do projeto. Fonte única: o backend deriva o catálogo daqui e o frontend recebe o JSON gerado daqui. */
public enum AcaoSistema implements AuthorizationAction {

    /** Ler o cadastro de uma organização. */
    CONSULTAR_ORGANIZACAO(AuthorizationScope.GLOBAL),

    /** Cadastrar uma organização. */
    INSERIR_ORGANIZACAO(AuthorizationScope.GLOBAL),

    /** Ler o contexto da organização selecionada, de dentro dela. */
    CONSULTAR_CONTEXTO_ORGANIZACAO(AuthorizationScope.TENANT);

    private final AuthorizationScope scope;

    AcaoSistema( AuthorizationScope scope ) {
        this.scope = scope;
    }

    @Override
    public AuthorizationScope scope() {
        return scope;
    }
}
```

- Nome no padrão `VERBO_SUBSTANTIVO` (`CONSULTAR_ORGANIZACAO`, `GERENCIAR_GESTOR_ORGANIZACAO`), com
  Javadoc de uma linha dizendo **o que a ação permite**.
- **Escopo não é detalhe**: `GLOBAL` é ação sobre dado que não pertence a organização alguma; `TENANT` é
  ação dentro da organização selecionada. `AuthorizationCatalog` recusa sobreposição entre os dois.
- Ação nova é **linha no enum** — e só ali. O JSON do frontend (`authorization.actions.json`) é
  **gerado** desse enum no build (`FrontendActionCatalogGenerator`) e verificado por teste
  (`FrontendActionCatalogGateTest`) e pelo `verify-authorization-actions.mjs` do frontend: JSON editado
  à mão reprova o build. Ação escrita errada **não compila**.

### 2.2 No controller: uma ação por endpoint

```java
@RestController
@RequestMapping("/api/contexto-organizacao")
public class ContextoOrganizacaoController extends BaseController {

    @Operation(summary = "Consultar o contexto da organização selecionada", description = "...")
    @ApiResponse(responseCode = "403", description = "Sem organização única no token, ou sem a ação CONSULTAR_CONTEXTO_ORGANIZACAO")
    @GetMapping
    @TenantEndpoint
    @PreAuthorize("hasRole('CONSULTAR_CONTEXTO_ORGANIZACAO')")
    public ResponseEntity<ContextoOrganizacaoResponse> context() { ... }
}
```

- `@PreAuthorize("hasRole('<ACAO>')")` com o **nome exato** de uma constante do enum. A primitiva
  `hasRole` fica — é o que o `gems-security-authorization` mapeia a partir do claim; o que **não** existe
  é semântica por nível.
- O endpoint é classificado por escopo — `@GlobalEndpoint` ou `@TenantEndpoint` — e a ação exigida
  precisa ter o **mesmo escopo**; a varredura da SDK (`EndpointAuthorizationScan`) reprova ação de escopo
  errado, ação fora do catálogo e endpoint de negócio sem `@PreAuthorize`.
- Endpoint de escopo `TENANT` **não recebe identificador de organização**: a organização vem do token,
  resolvida pelo `TenantContextResolver`. Aceitar um identificador permitiria pedir o contexto de outra.
- A autorização é declarada no **controller**, uma vez, na borda. Service não verifica autorização —
  regra de negócio que depende de "quem é" recebe o dado (id do usuário, organização) como parâmetro.
- **Toda transição de situação de fluxo é um endpoint de ação próprio**, com a própria ação no
  catálogo — situação nunca entra pelo corpo da requisição.

### 2.3 A rede que prova a regra

Um teste de integração sobre o contexto **real** varre todos os controllers do projeto com o catálogo
real e reprova qualquer endpoint sem ação concreta do catálogo — a varredura é
`EndpointAuthorizationScan.assertProtected(handlers, catalog)` da SDK, não uma lista montada à mão. O
número de endpoints publicados fica afirmado no mesmo teste, para que um endpoint que desapareça
não passe numa varredura que só procura violação.

```java
class EndpointAuthorizationApplicationCoverageTest extends AbstractIntegrationTest {

    @Test
    void deveAprovarAVarredura_quandoTodoControllerProprioExigeAcaoDoCatalogo() {
        assertThatCode( () -> EndpointAuthorizationScan.assertProtected( ownControllers(), catalog ) )
                .doesNotThrowAnyException();
    }
}
```

Controller de terceiro no classpath (ex.: `S3Controller` do `gems-aws-web`) não recebe anotação: é
bloqueado por `denyAll()` no `SecurityFilterChain` — nunca por `hasRole('ADMIN')`.

### 2.4 Lendo o usuário autenticado

O usuário vem do `Authentication` — no projeto, o `TenantJwtAuthenticationToken` da SDK, que já carrega
o `AuthorizationContext` (ações efetivas e alias da organização selecionada):

```java
var authentication = (TenantJwtAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
var alias = authentication.authorizationContext().tenantAlias().orElseThrow();
```

Verificação programática de role no service (`getAuthorities().anyMatch(...)`) **não** é forma admitida:
é a autorização por nível voltando pela porta dos fundos, invisível à varredura.

---

## 3. Tratamento de erros de segurança (gems-exception 3.1.0)

Com `gems.exception.security.enabled: true`, os advices do `gems-exception` respondem — e o projeto
**não** escreve handler próprio para nenhum destes casos:

| Situação | Status | Quem responde | Corpo |
| :--- | :--- | :--- | :--- |
| Sem token, token inválido ou expirado | **401** | `HttpStatusEntryPoint` do `SecurityFilterChain` — antes de qualquer advice | **vazio** — nada se revela a quem não se autenticou; a descrição da OpenAPI declara a ressalva |
| Token válido, `@PreAuthorize` negado (`AuthorizationDeniedException`) | **403** `ACESSO_NEGADO` | `AuthorizationExceptionHandler` | envelope `ExceptionResponseDTO` |
| `AccessDeniedException` de qualquer outra origem | **403** | idem | envelope |
| Endereço inexistente | **404** `RECURSO_NAO_ENCONTRADO` | `GlobalExceptionHandler` | envelope, **sem ecoar o caminho** |
| Parâmetro de rota/consulta malformado | **400** `PARAMETRO_INVALIDO` | `GlobalExceptionHandler` | envelope, nomeia o parâmetro, **sem ecoar o valor** |

```yaml
gems:
  exception:
    security:
      enabled: true
```

> Na **3.0.0**, o `SecurityExceptionHandler` declarava 401 para negação de `@PreAuthorize`, mas,
> com os três advices presentes, o 403 já prevalecia pela ordem do `AuthorizationExceptionHandler`.
> A mutação 10 da rodada 010 provou essa distinção. A **3.1.0** remove a declaração concorrente e
> cobre 404/400 de requisição e 409 de conflito. Os handlers locais **saem** — mantê-los é
> reimplementar mecanismo que a SDK entrega (constituição, Adoção de Módulo).

---

## 4. Regras

- **Nunca** armazene `client_secret`, senhas ou chaves no código — use variáveis de ambiente.
- **Stateless**: sem `HttpSession`, sem cookies de sessão — apenas JWT.
- `@EnableMethodSecurity` é necessário para `@PreAuthorize` funcionar.
- Para a receita completa com frontend Angular (PKCE + guard de rota), veja `recipes/secured-resource-end-to-end.md`.
- Em testes de integração, o token de teste carrega **ações**, não perfis:
  `jwt().authorities(new SimpleGrantedAuthority("ROLE_<ACAO>"))` do Spring Security Test para escopo
  `GLOBAL`, e `authentication(<TenantJwtAuthenticationToken>)` com a organização para escopo `TENANT`.

---

## Anti-Patterns (NUNCA)

| Proibido | Correto |
| :--- | :--- |
| `@PreAuthorize("hasAnyRole('ADMIN', 'GESTOR')")` ou `hasRole('ADMINISTRADOR')` — autorização por nível | `@PreAuthorize("hasRole('CONSULTAR_ORGANIZACAO')")` — uma ação concreta do enum |
| Ação declarada em JSON, `application.yml` ou constante solta | linha no enum que implementa `AuthorizationAction`; o JSON do frontend é gerado |
| Editar `authorization.actions.json` à mão | regenerar pelo build; o gate reprova a edição |
| `hasRole` programático no service (`getAuthorities().anyMatch(...)`) | autorização na borda, no controller; a varredura da SDK é a rede |
| Endpoint `TENANT` recebendo `organizacaoId` na rota ou no corpo | organização do token, resolvida pela SDK |
| Handler local para 401/403/404/400 | `gems-exception` 3.1.0 responde os quatro; handler local só com `GEMS-EXCEPTION` escrita e data |
| `client_secret` ou senha hardcoded no `application.yml` | Variáveis de ambiente ou Vault |
| `HttpSession` ou cookie de sessão | JWT stateless (`SessionCreationPolicy.STATELESS`) |
| `@PreAuthorize` sem `@EnableMethodSecurity` na `SecurityConfig` | Adicionar `@EnableMethodSecurity` — sem ela, `@PreAuthorize` é ignorado |

> **Registro.** Até a `versao: 2.1.0` a §2 desta regra prescrevia **autorização por role** —
> `hasRole('ADMINISTRADOR')`, `hasAnyRole('ADMINISTRADOR', 'SUPERVISOR')` e verificação programática
> no service —, o oposto do que a constituição (Princípio V) e o código consolidado fazem desde a
> rodada `006` (catálogo derivado de enum, A2). A `008` não a alcançou; a `009` a mediu no sample
> (S-09). Reescrita na rodada `010` (A-04, R-07), **antes** de a pasta ser propagada ao sample, para a
> regra errada não viajar uma segunda vez. A §3 antiga ("Acessando o Token na Service") saiu: a forma
> que ela ensinava é o anti-pattern da última linha da §2.4. A §3 atual descreve o `gems-exception`
> **3.1.0**, publicado nesta mesma rodada (G6).

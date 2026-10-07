---
origem: local
precedencia: local-vence
versao: 2.1.0
---

# Recipe: Recurso Protegido Ponta-a-Ponta

**Objetivo:** Proteger endpoints do backend com JWT (Resource Server) e rotas do frontend com AuthGuard + SessionService, com validação por roles.

**Pré-requisitos:**
- Spring Security + `gems-exception` (formata 401/403 no envelope uniforme)
- `angular-oauth2-oidc` (PKCE)
- IdP com suporte a OIDC (Keycloak, Auth0, etc.)

**Rules relacionadas:**
- [security.md](../rules/backend-java/security.md)
- [auth.md](../rules/frontend-angular/auth.md)
- [http-interceptors.md](../rules/frontend-angular/http-interceptors.md)
- [feedback-alerts.md](../rules/frontend-angular/feedback-alerts.md)

---

## Camada Backend: Resource Server JWT

### Dependências (pom.xml)
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-oauth2-resource-server</artifactId>
</dependency>
```

### SecurityFilterChain
```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));
        return http.build();
    }
}
```

### application.yml
```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: https://your-idp.example.com/realms/{realm}
          # jwk-set-uri: alternativa quando issuer-uri não resolve automaticamente
```

### Autorização por ação nos endpoints
```java
// Controller — cada endpoint nomeia UMA ação concreta do enum AcaoSistema ({app}-security), nunca um nível.
@PreAuthorize("hasRole('INSERIR_{ENTIDADE}')")
@PostMapping
public ResponseEntity<{Entidade}DTO> insert( @RequestBody {Entidade}DTO dto, HttpServletRequest request ) {
    // ...
}

@PreAuthorize("hasRole('ALTERAR_{ENTIDADE}')")
@PutMapping("/{id}")
public ResponseEntity<{Entidade}DTO> save( @PathVariable UUID id, @RequestBody {Entidade}DTO dto,
        HttpServletRequest request ) {
    // ...
}

// Leitura também é ação — não existe endpoint "liberado para todos os autenticados":
@PreAuthorize("hasRole('CONSULTAR_{ENTIDADE}')")
@GetMapping
public ResponseEntity<List<{Entidade}DTO>> findAll() {
    // ...
}
```

A ação entra no enum antes de aparecer aqui; o catálogo do frontend é gerado dele no build e ação
escrita errada não compila. Autorização negada é **403**. `hasAnyRole('ADMIN')` é o anti-padrão.
### Acessar Usuário Logado no Service
```java
public {Entidade} insert({Entidade}DTO dto) {
    String userId = SecurityContextHolder.getContext()
        .getAuthentication().getName();   // Subject do JWT
    // ...
}
```

### Respostas 401/403

O `gems-exception` intercepta automaticamente e retorna:
```json
// 401 Unauthorized:
{ "success": false, "errors": ["Autenticação necessária."] }

// 403 Forbidden:
{ "success": false, "errors": ["Acesso não autorizado."] }
```

---

## Camada Frontend: OIDC/PKCE

### Configuração (app.config.ts)
```typescript
import { provideOAuthClient } from 'angular-oauth2-oidc';

export const appConfig: ApplicationConfig = {
  providers: [
    provideOAuthClient({
      resourceServer: {
        allowedUrls: [environment.apiBaseUrl],
        sendAccessToken: true    // Bearer token automático em todas as chamadas à API
      }
    })
  ]
};
```

### AuthGuard
```typescript
// src/app/core/auth/auth.guard.ts
export const authGuard: CanActivateFn = () => {
  const session = inject(SessionService);
  if (session.isAuthenticated) return true;
  session.login();    // Redireciona para IdP via PKCE
  return false;
};
```

### Proteger rotas
```typescript
export const routes: Routes = [
  {
    path: '',
    canActivate: [authGuard],
    children: [
      { path: '{modulo}', loadChildren: () => import('./features/{modulo}/{modulo}.routes') }
    ]
  },
  { path: 'login', component: LoginComponent },
  { path: 'callback', component: CallbackComponent }   // redirectUri do PKCE
];
```

### Restringir por ação no template
```html
<!-- por ação concreta, nunca por nível — auth.md, anti-patterns -->
@if (auth.isAuthorized({ scope: 'global', actions: ['INSERIR_{ENTIDADE}'] })) {
  <button class="btn-novo" routerLink="/{modulo}/form">
    <i class="fa-solid fa-plus"></i> Nova {Entidade}
  </button>
}
```

### Tratar erro 403 do backend
```typescript
error: (err: HttpErrorResponse) => {
  if (err.status === 403) {
    this.alertService.error('Acesso negado', 'Você não tem permissão para realizar esta ação.');
  } else {
    this.alertService.errorFromApi(err);
  }
  this.isLoading.set(false);
}
```

---

## Checklist de Conformidade

**Backend:**
- [ ] `SessionCreationPolicy.STATELESS` (sem sessão HTTP).
- [ ] `issuer-uri` configurado no `application.yml`.
- [ ] `/actuator/health` + Swagger liberados sem autenticação.
- [ ] `@PreAuthorize("hasRole('...')")` nos endpoints mutantes.
- [ ] `@EnableMethodSecurity` na configuração.
- [ ] Swagger desabilitado em produção (ou protegido).

**Frontend:**
- [ ] `sendAccessToken: true` + `allowedUrls` configurados no `provideOAuthClient`.
- [ ] `authGuard` nas rotas protegidas.
- [ ] Botões de criação/edição condicionais a `hasRole()`.
- [ ] 403 tratado com `alertService.error()` (não `errorFromApi` — a mensagem já é clara).
- [ ] Rota `/callback` registrada como `redirectUri` no IdP.

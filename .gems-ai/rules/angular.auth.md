<!-- gems-ai-rule: {"id":"angular.auth","version":"3.0.0","status":"candidate"} -->
---
stack: frontend-angular
module: keycloak-angular, gems-sdk (provideGemsKeycloak)
severity: mandatory
see-also: [http-interceptors.md, stores.md, organization.md]
origem: sdk-publicada
precedencia: contrato-sdk-e-constituicao-local
versao: 3.0.0
---

# Autenticação e autorização — Angular GEMS SDK 2.3.4

A SDK publicada expõe `provideGemsKeycloak`, `KeycloakService`, `gemsRoleGuard` e `gemsHasRequiredRoles`. O guard da SDK lê `route.data['roles']` e, opcionalmente, `roleMode: 'any' | 'all'` (`any` por padrão). Lista ausente ou vazia **nega**. Usuário anônimo inicia `login` com URL de retorno e a navegação aguarda autenticação. Não há `AuthorizationRequirement`, `data.authorization`, parser de escopo `global|tenant` ou catálogo de ações gerado na API publicada.

## Configuração e rotas

```typescript
import { provideGemsKeycloak } from '@gabriel-mdias/angular-gems-sdk/auth';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';

export const appConfig: ApplicationConfig = {
  providers: [
    provideHttpClient(withInterceptorsFromDi()),
    provideGemsKeycloak(keycloakOptions()),
  ],
};
```

`keycloakOptions()` vem da aplicação. Preserve sua configuração de check-sso, PKCE S256 e silent-check-sso quando já existir. O provider da SDK registra `KeycloakService` e o interceptor bearer por DI; o consumidor deve testar login, callback, refresh, logout e quais URLs recebem o token. Falha de inicialização não autoriza rota protegida.

```typescript
{
  path: 'consulados',
  canActivate: [gemsRoleGuard],
  data: { roles: [Acoes.CONSULTAR_CONSULADO] },
  loadChildren: () => import('./features/consulado/consulado.routes'),
}
```

Para exigir todos os papéis declarados, use `data: { roles: [...], roleMode: 'all' }`. Sem `roles`, o guard deve negar. `gemsHasRequiredRoles` segue a mesma semântica `any`/`all`. A UI usa a mesma ação concreta que o endpoint, e o backend repete a autorização com `@PreAuthorize`. Ocultar botão sozinho não protege dados. Ações compostas como `ADMIN` não são bypass manual de rota.

## Extensão local de autorização por ação

Uma aplicação pode manter um guard/fachada próprios para traduzir uma ação de domínio em papel do token e definir seu destino de negação. O ADACI usa ações concretas, `data.roles`, guard local que nega lista vazia e `/acesso-negado`. Isso é um contrato do consumidor, não uma API adicional da SDK. Um teste de cobertura deve percorrer as rotas guardadas e falhar se `data.roles` faltar.

**Extensão opcional de produto, fora da SDK:** um sistema multi tenant pode definir `global|tenant`, parser de claims, modelos e backend próprios. Isso exige spec, contrato, isolamento e testes de autorização dedicados. Não se ativa por esta regra nem por instalar a SDK. ADACI é single tenant e não recebe tal extensão.

## Casos e contraexemplos

- Positivo: `data: { roles: [Acoes.INSERIR_CLIENTE] }`, guard local ou da SDK nega anônimo/sem papel e o endpoint exige a ação.
- Negativo: rota guardada com `data.roles` ausente ou `[]` é negada; não virar acesso público por omissão.
- Negativo: exigir `data.authorization` ou `AuthorizationRequirement` como API da SDK 2.3.4 inventa contrato ausente; o bundle deve recusar conflito.
- Negativo: bearer em URL de terceiro, fallback `ADMIN` ou afirmação de tenant implícito violam o contrato de segurança.

# Pacote de execução — rodada 002, C11

> Lista `READ` fechada. Sem delegação: o próprio Codex conduz o checkpoint aprovado. Não iniciar
> C12, auditoria, gates completos ou homologação. Não commitar nem fazer push.

Checkpoint T013–T024. Âncora: `098a9f5`. Sessão: `nova`. Gate: comando literal de T024.

## 1. Fronteira fechada do lote

C11 adota `gems-security-authorization` e `gems-keycloak-admin` 3.3.0, preservando as 21 ações e
todo o ciclo Keycloak provado por FR-020. Não toca auditoria, frontend nem realm declarativo.

### READ

- `AGENTS.md`, `backend/AGENTS.md`, `specs/AGENTS.md`
- `.specify/memory/constitution.md`
- `.gems-ai/rules/backend-java/{security.md,testing.md,events.md,architecture.md}`
- `specs/002-conformidade-gems/{spec.md,plan.md,tasks.md,roteiro.md,contracts/backend.md}`
- `.specify/templates/estado-lote-template.md`
- `.specify/scripts/powershell/verificar-lote.ps1`
- `backend/{pom.xml,app-security/pom.xml,app-core/pom.xml,app-deploy/pom.xml}`
- `backend/app-security/src/main/java/br/com/gems/app/security/{AcaoSistema.java,config/SecurityConfig.java,config/KeycloakConfig.java,config/AppKeycloakProperties.java,annotation/PublicEndpoint.java,dto/PublicRoute.java,dto/CreateUserDTO.java,service/KeycloakUserService.java,service/KeycloakRoleService.java,service/KeycloakGroupService.java}`
- `backend/app-core/src/main/java/br/com/gems/app/{autorizacao/MapaPerfilAcoes.java,autorizacao/AutorizacaoBootstrap.java,usuario/listener/UsuarioKeycloakListener.java,usuario/service/UsuarioService.java}`
- `backend/app-core/src/main/java/br/com/gems/app/{auditoria/controller/AuditoriaController.java,localidade/controller/LocalidadeController.java,normalizacao/controller/CepController.java,parceiro/controller/ParceiroController.java,solicitacao/controller/SolicitacaoController.java}`
- `backend/app-core/src/test/java/br/com/gems/app/{autorizacao/AutorizacaoBootstrapTest.java,autorizacao/MapaPerfilAcoesTest.java,usuario/listener/UsuarioKeycloakListenerTest.java,usuario/service/UsuarioServiceTest.java}`
- `backend/app-deploy/src/test/java/br/com/gems/app/autorizacao/{CoberturaPreAuthorizeTest.java,AutorizacaoPorAcaoIntegrationTest.java}`
- `backend/app-deploy/src/{main/resources/application.yml,test/resources/application.yml}`
- `D:/repos/GEMS SDK/Java-GEMS-SDK/gems-keycloak-admin/src/main/java/br/com/gems/keycloak/admin/{KeycloakAdminGateway.java,KeycloakUserLifecycleGateway.java,KeycloakRealmRoleGateway.java,KeycloakRealmGroupGateway.java,KeycloakUserSnapshot.java,KeycloakAdminRestClient.java,KeycloakAdminProperties.java}`
- `D:/repos/GEMS SDK/Java-GEMS-SDK/gems-security-authorization/src/main/java/br/com/gems/security/authorization/{AuthorizationAction.java,AuthorizationCatalog.java,AuthorizationScope.java,EndpointAuthorizationScan.java,GlobalEndpoint.java,TenantEndpoint.java,PublicEndpoint.java,FrontendActionCatalogGenerator.java}`

**Nada fora de READ pode ser pesquisado ou lido.** Se faltar um tipo ou contrato, parar e registrar.

### EDIT / CREATE

| # | Ação | Arquivo |
| :-- | :--- | :--- |
| C11 | EDIT | `backend/pom.xml` `backend/app-security/pom.xml` — consumir 3.3.0 e declarar os dois módulos SDK |
| T014 | EDIT | `backend/app-core/src/test/java/br/com/gems/app/autorizacao/AutorizacaoBootstrapTest.java` `backend/app-core/src/test/java/br/com/gems/app/autorizacao/MapaPerfilAcoesTest.java` `backend/app-core/src/test/java/br/com/gems/app/usuario/listener/UsuarioKeycloakListenerTest.java` `backend/app-core/src/test/java/br/com/gems/app/usuario/service/UsuarioServiceTest.java` |
| T015 | CREATE | `backend/app-security/src/test/java/br/com/gems/app/security/AcaoSistemaCatalogTest.java` `backend/app-deploy/src/test/java/br/com/gems/app/autorizacao/CatalogoAutorizacaoGeradoTest.java` |
| T016 | EDIT | `backend/app-security/src/main/java/br/com/gems/app/security/AcaoSistema.java` `backend/app-core/src/main/java/br/com/gems/app/autorizacao/MapaPerfilAcoes.java` `backend/app-core/src/main/java/br/com/gems/app/autorizacao/AutorizacaoBootstrap.java` |
| T017 | EDIT | `backend/app-deploy/src/test/java/br/com/gems/app/autorizacao/CoberturaPreAuthorizeTest.java` |
| T018 | EDIT | `backend/app-core/src/main/java/br/com/gems/app/auditoria/controller/AuditoriaController.java` `backend/app-core/src/main/java/br/com/gems/app/localidade/controller/LocalidadeController.java` `backend/app-core/src/main/java/br/com/gems/app/normalizacao/controller/CepController.java` `backend/app-core/src/main/java/br/com/gems/app/parceiro/controller/ParceiroController.java` `backend/app-core/src/main/java/br/com/gems/app/solicitacao/controller/SolicitacaoController.java` |
| T019 | EDIT | `backend/app-security/src/main/java/br/com/gems/app/security/config/SecurityConfig.java` |
| T019 | DELETE | `backend/app-security/src/main/java/br/com/gems/app/security/annotation/PublicEndpoint.java` `backend/app-security/src/main/java/br/com/gems/app/security/dto/PublicRoute.java` |
| T020 | EDIT | `backend/app-security/src/main/java/br/com/gems/app/security/config/KeycloakConfig.java` `backend/app-deploy/src/main/resources/application.yml` `backend/app-deploy/src/test/resources/application.yml` |
| T021 | EDIT | `backend/app-core/src/main/java/br/com/gems/app/usuario/listener/UsuarioKeycloakListener.java` `backend/app-core/src/main/java/br/com/gems/app/usuario/service/UsuarioService.java` |
| T022 | DELETE | `backend/app-security/src/main/java/br/com/gems/app/security/config/AppKeycloakProperties.java` `backend/app-security/src/main/java/br/com/gems/app/security/service/KeycloakUserService.java` `backend/app-security/src/main/java/br/com/gems/app/security/service/KeycloakRoleService.java` `backend/app-security/src/main/java/br/com/gems/app/security/service/KeycloakGroupService.java` `backend/app-security/src/main/java/br/com/gems/app/security/dto/CreateUserDTO.java` |
| C11 | EDIT | `specs/002-conformidade-gems/tasks.md` `specs/002-conformidade-gems/roteiro.md` — somente depois do gate verde |
| C11 | CREATE | `specs/002-conformidade-gems/pacote-de-execucao-C11.md` `specs/002-conformidade-gems/estado-lote-C11.md` |

## 2. Contratos publicados consumidos

`AcaoSistema` implementa `AuthorizationAction`; todas as 21 ações são `GLOBAL` neste sample.
`AuthorizationCatalog.of(AcaoSistema.class)` e `EndpointAuthorizationScan` substituem listas e scan
locais. `KeycloakAdminRestClient` implementa `KeycloakAdminGateway`, `KeycloakUserLifecycleGateway` e
`KeycloakRealmRoleGateway`; `KeycloakUserSnapshot` preserva escalares, atributos, enable e grupos.

## 3. Referências

| Para escrever | Copie de |
| :--- | :--- |
| Catálogo/scan | fontes publicadas `gems-security-authorization` listadas em READ |
| Ciclo Keycloak | interfaces e `KeycloakAdminRestClient` 3.3.0 listados em READ |
| Forma dos testes | quatro testes T014 existentes e `.gems-ai/rules/backend-java/testing.md` |

## 4. Regras deste lote

1. Testes T014/T015 ficam vermelhos antes de a implementação ser trocada.
2. As 21 ações continuam concretas; todas têm escopo `GLOBAL` no sample single-tenant.
3. Controller de negócio recebe exatamente uma classificação SDK e ação existente.
4. A saída do catálogo é determinística e derivada do enum; não criar lista paralela.
5. FR-020 inteiro é preservado; falha em qualquer operação interrompe a adoção.
6. As classes locais saem na mesma alteração em que a SDK assume suas garantias.
7. Nenhum segredo ou default de ambiente entra em código; configuração vem de propriedades existentes.
8. D-P03 não foi escolhido: não tocar `environments/`.
9. Mutação de classificação deve ficar vermelha e ser restaurada por edição inversa.
10. Sem alteração de API REST, payload, status ou comportamento observável do ciclo de usuário.

## 5. O portão

Em `backend/`:

```powershell
.\mvnw.cmd -pl app-deploy -am -Dtest=AutorizacaoBootstrapTest,MapaPerfilAcoesTest,UsuarioKeycloakListenerTest,UsuarioServiceTest,AcaoSistemaCatalogTest,CatalogoAutorizacaoGeradoTest,CoberturaPreAuthorizeTest,AutorizacaoPorAcaoIntegrationTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Exigir exit 0 e todos os testes selecionados executados. Rodar também `git diff --check` e o
verificador de extensão antes de fechar.

## 6. Relatório e rollback

Registrar tarefas, arquivos, vermelho test-first, mutação, gate e divergências no estado C11.
Rollback é edição inversa somente dos caminhos da tabela contra `098a9f5`; remover nomeadamente
os dois documentos C11. Nunca usar reset, clean ou checkout amplo.

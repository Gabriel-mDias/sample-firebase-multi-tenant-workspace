# Estado — C11, rodada 002

Data: 2026-09-23. Estado inicial: **aberto**. Estado atual: **verde** com a release 3.3.0.
Âncora: `098a9f5`. Sessão: `nova`.

## 1. Rollback deste lote

Reverter por edição inversa somente os caminhos da tabela do pacote C11 contra `098a9f5` e remover
nomeadamente apenas `pacote-de-execucao-C11.md` e este estado. Nunca usar reset, clean ou checkout amplo.

## 2. Concluído

| Tarefa | O que provou |
| :-- | :--- |
| Preparação | Ledger selecionou C11; SDK 3.2.0 publicada e resolvida; pacote fechado criado antes do código. |
| T013 (primeira aferição) | A release 3.2.0 não expunha as operações de grupo de realm exigidas pelo sample. FR-021 determinou a parada antes de editar. |
| T013 (retomada) | A release 3.3.0 publicada resolveu pelo Maven. `KeycloakRealmGroupGateway` expõe busca exata, criação idempotente de grupo de primeiro nível e associação idempotente de realm role; com lifecycle, snapshot/restauração, memberships e roles compostas já publicados, FR-020/FR-034 ficam cobertos. Tag `v3.3.0` em `7c343208ea35562afb1e3da9520b33bb2db7b045`. |
| T014–T015 | Testes migrados/criados antes da produção. O primeiro gate ficou vermelho na compilação porque `AcaoSistema` ainda não implementava `AuthorizationAction` nem `scope()`. |
| T016–T018 | 21 ações `GLOBAL`, catálogo/bootstrap e scan SDK adotados. Cinco controllers classificados. A mutação temporária de `ParceiroController` para `TENANT` reprovou nominalmente suas cinco operações e foi restaurada. |
| T019–T022 | Segurança e ciclo Keycloak migrados para a SDK; timeouts explícitos; compensação e memberships preservados; sete tipos locais removidos. Busca nominal residual sem ocorrências. |
| T023 | Não aplicável por D-P03; `environments/` permaneceu intocado. |
| T024 | Comando literal repetido após restauração: exit 0/BUILD SUCCESS; 52 testes, zero falhas, zero erros e zero ignorados (1 security, 42 core, 9 deploy). |
| Dependências | `dependency:tree` exit 0: `gems-security-authorization` e `gems-keycloak-admin` 3.3.0 em compile nos consumidores. |

## 3. Em curso

Nenhuma tarefa do C11.

## 4. Não iniciado

C12 e posteriores permanecem fora do lote, conforme a fronteira fechada.

## 5. Arquivos tocados

`git status --porcelain --untracked-files=all`, menos este próprio estado:

```text
 M backend/app-core/src/main/java/br/com/gems/app/auditoria/controller/AuditoriaController.java
 M backend/app-core/src/main/java/br/com/gems/app/autorizacao/AutorizacaoBootstrap.java
 M backend/app-core/src/main/java/br/com/gems/app/localidade/controller/LocalidadeController.java
 M backend/app-core/src/main/java/br/com/gems/app/normalizacao/controller/CepController.java
 M backend/app-core/src/main/java/br/com/gems/app/parceiro/controller/ParceiroController.java
 M backend/app-core/src/main/java/br/com/gems/app/solicitacao/controller/SolicitacaoController.java
 M backend/app-core/src/main/java/br/com/gems/app/usuario/listener/UsuarioKeycloakListener.java
 M backend/app-core/src/test/java/br/com/gems/app/autorizacao/AutorizacaoBootstrapTest.java
 M backend/app-core/src/test/java/br/com/gems/app/autorizacao/MapaPerfilAcoesTest.java
 M backend/app-core/src/test/java/br/com/gems/app/usuario/listener/UsuarioKeycloakListenerTest.java
 M backend/app-deploy/src/main/resources/application.yml
 M backend/app-deploy/src/test/java/br/com/gems/app/autorizacao/CoberturaPreAuthorizeTest.java
 M backend/app-deploy/src/test/resources/application.yml
 M backend/app-security/pom.xml
 M backend/app-security/src/main/java/br/com/gems/app/security/AcaoSistema.java
 D backend/app-security/src/main/java/br/com/gems/app/security/annotation/PublicEndpoint.java
 D backend/app-security/src/main/java/br/com/gems/app/security/config/AppKeycloakProperties.java
 M backend/app-security/src/main/java/br/com/gems/app/security/config/KeycloakConfig.java
 M backend/app-security/src/main/java/br/com/gems/app/security/config/SecurityConfig.java
 D backend/app-security/src/main/java/br/com/gems/app/security/dto/CreateUserDTO.java
 D backend/app-security/src/main/java/br/com/gems/app/security/dto/PublicRoute.java
 D backend/app-security/src/main/java/br/com/gems/app/security/service/KeycloakGroupService.java
 D backend/app-security/src/main/java/br/com/gems/app/security/service/KeycloakRoleService.java
 D backend/app-security/src/main/java/br/com/gems/app/security/service/KeycloakUserService.java
 M backend/pom.xml
 M specs/002-conformidade-gems/roteiro.md
 M specs/002-conformidade-gems/tasks.md
?? backend/app-deploy/src/test/java/br/com/gems/app/autorizacao/CatalogoAutorizacaoGeradoTest.java
?? backend/app-security/src/test/java/br/com/gems/app/security/AcaoSistemaCatalogTest.java
?? specs/002-conformidade-gems/pacote-de-execucao-C11.md
```

## 6. Decisões fora do pacote

Os POMs raiz e `app-security` foram incluídos nominalmente no pacote: consumir a release 3.2.0 e
declarar seus dois módulos é condição técnica inevitável do C11 aprovado, embora as tasks originais
não tenham nomeado esses manifests. Isso não altera comportamento nem amplia o checkpoint.

A SDK 3.2.0 não satisfazia FR-020 para este consumidor. A 3.3.0 acrescentou
`KeycloakRealmGroupGateway`, separando grupos de primeiro nível do realm dos grupos de organização e
publicando as três operações que faltavam. A adoção foi atômica depois do vermelho T014/T015.

O `EndpointAuthorizationScan` considera ação obrigatória para todo endpoint classificado. As leituras
referenciais autenticadas `CepController#buscarPorCep`, `LocalidadeController#listarPaises` e
`LocalidadeController#listarEstadosPorPais` são a exceção constitucional existente: ficaram numa
lista nominal fechada no teste e foram excluídas somente da verificação de ação, sem se tornarem
públicas. Todo o restante passa pelo scan SDK.

## 7. O que está quebrado agora

Nada conhecido. O gate T024, a árvore de dependências, a busca nominal residual e
`git diff --check` estão verdes. A âncora de rollback continua `098a9f5`.

## 8. Retomada

- **Executor:** Codex
- **Política:** C11 encerrado sem commit nem push.
- **Próximo passo:** C12 é o próximo checkpoint elegível, mas não foi iniciado por estar fora deste
  pacote fechado.

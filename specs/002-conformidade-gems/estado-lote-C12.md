# Estado — C12, rodada 002

Data: 2026-09-23. Estado inicial: **aberto**. Estado atual: **verde local, sem commit/push**.
Âncora: `098a9f5`. Sessão: retomada.

## 1. Rollback deste lote

Reverter por edição inversa somente os caminhos C12 da tabela do pacote, preservando integralmente
as mudanças C11 já existentes. Remover nomeadamente apenas os CREATE de C12 e estes dois documentos
C12. Nunca usar reset, clean ou checkout amplo.

## 2. Concluído

| Tarefa | O que provou |
| :-- | :--- |
| Preparação | Branch e upstream permanecem em `098a9f5`, ahead/behind `0/0`; dirty state coincide com C11 preservado. Pacote C12 criado antes de produção. |
| T025 | `dependency:get` resolveu `br.com.gems:gems-auditing:3.3.0`. O `-sources.jar` publicado expõe `AuditActorProvider`, `AuditContext(actorId, correlationId)` e `AuditContextProvider`; a autoconfiguração aceita providers consumidores e o writer opt-in grava `ID_ACTOR`/`CD_CORRELATION` na mesma conexão. FR-033 está materialmente presente. |
| T026–T027 | Primeiro vermelho por API/provider ausentes. Migração/schema, integração, sigilo, cobertura e ausência de escrita transferidos. |
| T028–T031 | Migração preservadora com backfill/rollback; JPA lê tabelas SDK; enum conserva o contrato REST; `@Auditable`/`@SensitiveField` substituem locais. |
| T032–T034 | Provider preserva autor humano/técnico, UUID e correlação; ativação atômica; writer/interceptor/testes locais removidos. |
| T035 | Gate literal exit 0: 20 testes selecionados (2 security, 2 core e 16 deploy), zero falhas/erros/skips; Liquibase e `ddl-auto: validate` verdes. |
| T036 | Writer, config, schema, filtro, identidade e correlação reprovaram por mutação e foram restaurados; rollback real e gate final verdes. |

## 3. Em curso

Nenhuma tarefa do C12.

## 4. Não iniciado

C13 e posteriores permanecem fora do lote.

## 5. Arquivos tocados

`git status --porcelain --untracked-files=all`, menos este próprio estado:

```text
 M backend/app-core/src/main/java/br/com/gems/app/auditoria/controller/AuditoriaController.java
 D backend/app-core/src/main/java/br/com/gems/app/auditoria/interceptor/AuditoriaGravador.java
 D backend/app-core/src/main/java/br/com/gems/app/auditoria/interceptor/AuditoriaInterceptor.java
 D backend/app-core/src/main/java/br/com/gems/app/auditoria/interceptor/RegistroPendente.java
 M backend/app-core/src/main/java/br/com/gems/app/autorizacao/AutorizacaoBootstrap.java
 M backend/app-core/src/main/java/br/com/gems/app/localidade/controller/LocalidadeController.java
 M backend/app-core/src/main/java/br/com/gems/app/normalizacao/controller/CepController.java
 M backend/app-core/src/main/java/br/com/gems/app/parceiro/controller/ParceiroController.java
 M backend/app-core/src/main/java/br/com/gems/app/solicitacao/controller/SolicitacaoController.java
 M backend/app-core/src/main/java/br/com/gems/app/usuario/listener/UsuarioKeycloakListener.java
 D backend/app-core/src/test/java/br/com/gems/app/auditoria/interceptor/AuditoriaInterceptorTest.java
 D backend/app-core/src/test/java/br/com/gems/app/auditoria/interceptor/AuditoriaIsolamentoTest.java
 M backend/app-core/src/test/java/br/com/gems/app/auditoria/controller/AuditoriaControllerSemEscritaTest.java
 M backend/app-core/src/test/java/br/com/gems/app/autorizacao/AutorizacaoBootstrapTest.java
 M backend/app-core/src/test/java/br/com/gems/app/autorizacao/MapaPerfilAcoesTest.java
 M backend/app-core/src/test/java/br/com/gems/app/usuario/listener/UsuarioKeycloakListenerTest.java
 M backend/app-deploy/src/main/resources/application.yml
 M backend/app-deploy/src/main/resources/db/changelog/changelog-auditoria.xml
 M backend/app-deploy/src/test/java/br/com/gems/app/auditoria/AuditoriaInterceptorIntegrationTest.java
 M backend/app-deploy/src/test/java/br/com/gems/app/auditoria/AuditoriaSigiloIntegrationTest.java
 M backend/app-deploy/src/test/java/br/com/gems/app/auditoria/interceptor/AuditoriaCoberturaIntegrationTest.java
 M backend/app-deploy/src/test/java/br/com/gems/app/autorizacao/CoberturaPreAuthorizeTest.java
 M backend/app-deploy/src/test/resources/application.yml
 M backend/app-model/pom.xml
 M backend/app-model/src/main/java/br/com/gems/app/model/entity/auditoria/CampoAlterado.java
 M backend/app-model/src/main/java/br/com/gems/app/model/entity/auditoria/RegistroAuditoria.java
 D backend/app-model/src/main/java/br/com/gems/app/model/entity/base/CampoSigiloso.java
 M backend/app-model/src/main/java/br/com/gems/app/model/entity/base/EntidadeAuditavel.java
 M backend/app-model/src/main/java/br/com/gems/app/model/entity/parceiro/Parceiro.java
 M backend/app-model/src/main/java/br/com/gems/app/model/enums/OperacaoAuditoria.java
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
?? backend/app-deploy/src/test/java/br/com/gems/app/auditoria/AuditoriaSchemaMigrationIntegrationTest.java
?? backend/app-deploy/src/test/java/br/com/gems/app/autorizacao/CatalogoAutorizacaoGeradoTest.java
?? backend/app-security/src/main/java/br/com/gems/app/security/auditoria/UsuarioAuditActorProvider.java
?? backend/app-security/src/test/java/br/com/gems/app/security/AcaoSistemaCatalogTest.java
?? backend/app-security/src/test/java/br/com/gems/app/security/auditoria/UsuarioAuditActorProviderTest.java
?? specs/002-conformidade-gems/estado-lote-C11.md
?? specs/002-conformidade-gems/pacote-de-execucao-C11.md
?? specs/002-conformidade-gems/pacote-de-execucao-C12.md
```

## 6. Decisões fora do pacote

O writer SDK registra campos também em INSERT; o teste de sigilo seleciona UPDATE para preservar a
intenção original. O alvo de mutação do rollback é package-private no artefato; a prova real passou
aqui e a mutação correspondente já integra a certificação da release produtora.

As ocorrências residuais de `CampoSigiloso` no comentário do changeset histórico e de
`AuditoriaInterceptorIntegrationTest` nos nomes/Javadocs dos testes são deliberadas: o changeset já
aplicado é imutável, e os nomes dos testes preservam o gate existente. Não há referência produtora
aos tipos locais removidos.

## 7. O que está quebrado agora

Nada conhecido. Gate T035 repetido com 20/20 testes, dependências e `git diff --check` estão
verdes. O verificador fechou com 28 caminhos previstos, 26 declarados/reais, zero extensão indevida
e somente os dois documentos de protocolo como ressalva de não entrega por serem descontados pela
própria ferramenta. A busca residual contém apenas o comentário histórico imutável e nomes de teste
registrados na seção 6.

## 8. Retomada

- **Executor:** Codex
- **Política:** C12 encerrado sem commit nem push; próximo lote C13.

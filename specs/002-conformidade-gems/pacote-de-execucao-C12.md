# Pacote de execução — rodada 002, C12

> Lista `READ` fechada. Sem delegação: o próprio Codex conduz o checkpoint aprovado. C11 permanece
> intacto na árvore de trabalho. Não iniciar C13, gates completos ou homologação. Não commitar nem
> fazer push.

Checkpoint T025–T036. Âncora: `098a9f5`. Sessão: retomada. Gate: comando literal de T035.

## 1. Fronteira fechada do lote

C12 substitui atomicamente a escrita local de auditoria por `gems-auditing` 3.3.0. Preserva
identidade estável, nome/tipo do autor, correlação, commit/rollback, sigilo, operação, agregados,
consulta e data inclusiva. Schema, leitura, providers, writer e remoções formam um único bloco.

### READ

- `AGENTS.md`, `backend/AGENTS.md`, `specs/AGENTS.md`
- `.specify/memory/constitution.md`
- `.gems-ai/rules/backend-java/{observability.md,persistence-db.md,repositories.md,security.md,testing.md,architecture.md}`
- `specs/002-conformidade-gems/{spec.md,plan.md,tasks.md,roteiro.md,contracts/backend.md,estado-lote-C11.md}`
- `.specify/templates/estado-lote-template.md`
- `.specify/scripts/powershell/verificar-lote.ps1`
- `backend/{pom.xml,app-model/pom.xml,app-security/pom.xml,app-core/pom.xml,app-deploy/pom.xml}`
- `backend/app-deploy/src/{main/resources/application.yml,test/resources/application.yml}`
- `backend/app-deploy/src/main/resources/db/changelog/{changelog-auditoria.xml,changelog-master.xml}`
- `backend/app-model/src/main/java/br/com/gems/app/model/entity/{base/EntidadeAuditavel.java,base/CampoSigiloso.java,auditoria/RegistroAuditoria.java,auditoria/CampoAlterado.java,localidades/Estado.java,localidades/Pais.java,normalizacao/Contato.java,normalizacao/Endereco.java,normalizacao/Pessoa.java,parceiro/Parceiro.java,seguranca/Usuario.java,solicitacao/EtapaSolicitacao.java,solicitacao/Solicitacao.java}`
- `backend/app-model/src/main/java/br/com/gems/app/model/{enums/OperacaoAuditoria.java,params/AuditoriaFilterParams.java,response/RegistroAuditoriaResponseDTO.java,dto/RegistroAuditoriaDTO.java}`
- `backend/app-core/src/main/java/br/com/gems/app/auditoria/{controller/AuditoriaController.java,repository/RegistroAuditoriaRepository.java,repository/CampoAlteradoRepository.java,service/AuditoriaService.java,interceptor/AuditoriaGravador.java,interceptor/AuditoriaInterceptor.java,interceptor/RegistroPendente.java}`
- `backend/app-security/src/main/java/br/com/gems/app/security/service/UsuarioAutenticadoService.java`
- `backend/app-core/src/test/java/br/com/gems/app/auditoria/{controller/AuditoriaControllerSemEscritaTest.java,interceptor/AuditoriaInterceptorTest.java,interceptor/AuditoriaIsolamentoTest.java}`
- `backend/app-deploy/src/test/java/br/com/gems/app/auditoria/{AuditoriaInterceptorIntegrationTest.java,AuditoriaSigiloIntegrationTest.java,interceptor/AuditoriaCoberturaIntegrationTest.java}`
- `backend/app-deploy/src/test/java/br/com/gems/app/{AppApplicationTests.java,parceiro/ParceiroRollbackIntegrationTest.java,solicitacao/SolicitacaoRollbackIntegrationTest.java}`
- `backend/app-security/src/test/java/br/com/gems/app/security/service/UsuarioAutenticadoServiceTest.java`
- `D:/repos/GEMS SDK/Java-GEMS-SDK/{CLAUDE.md,AI-CONSUMER-GUIDE.md}`
- artefato publicado `br.com.gems:gems-auditing:3.3.0` e seu `-sources.jar`, limitado às APIs `Auditable`, `SensitiveField`, `AuditActorProvider`, `AuditContext`, `AuditContextProvider`, `AuditOperation` e à autoconfiguração

**Nada fora de READ pode ser pesquisado ou lido.** Se faltar um tipo ou contrato, parar e registrar.

### EDIT / CREATE / DELETE

| # | Ação | Arquivo |
| :-- | :--- | :--- |
| C12 | EDIT | `backend/pom.xml` `backend/app-model/pom.xml` `backend/app-security/pom.xml` |
| T026 | CREATE | `backend/app-deploy/src/test/java/br/com/gems/app/auditoria/AuditoriaSchemaMigrationIntegrationTest.java` |
| T026 | EDIT | `backend/app-deploy/src/test/java/br/com/gems/app/auditoria/AuditoriaInterceptorIntegrationTest.java` `backend/app-deploy/src/test/java/br/com/gems/app/auditoria/AuditoriaSigiloIntegrationTest.java` `backend/app-deploy/src/test/java/br/com/gems/app/auditoria/interceptor/AuditoriaCoberturaIntegrationTest.java` |
| T027 | EDIT | `backend/app-core/src/test/java/br/com/gems/app/auditoria/controller/AuditoriaControllerSemEscritaTest.java` |
| T028 | EDIT | `backend/app-deploy/src/main/resources/db/changelog/changelog-auditoria.xml` |
| T029 | EDIT | `backend/app-model/src/main/java/br/com/gems/app/model/entity/auditoria/RegistroAuditoria.java` `backend/app-model/src/main/java/br/com/gems/app/model/entity/auditoria/CampoAlterado.java` `backend/app-model/src/main/java/br/com/gems/app/model/enums/OperacaoAuditoria.java` |
| T031 | EDIT | `backend/app-model/src/main/java/br/com/gems/app/model/entity/base/EntidadeAuditavel.java` `backend/app-model/src/main/java/br/com/gems/app/model/entity/parceiro/Parceiro.java` |
| T031 | DELETE | `backend/app-model/src/main/java/br/com/gems/app/model/entity/base/CampoSigiloso.java` |
| T032 | CREATE | `backend/app-security/src/main/java/br/com/gems/app/security/auditoria/UsuarioAuditActorProvider.java` `backend/app-security/src/test/java/br/com/gems/app/security/auditoria/UsuarioAuditActorProviderTest.java` |
| T033 | EDIT | `backend/app-deploy/src/main/resources/application.yml` `backend/app-deploy/src/test/resources/application.yml` |
| T034 | DELETE | `backend/app-core/src/main/java/br/com/gems/app/auditoria/interceptor/AuditoriaGravador.java` `backend/app-core/src/main/java/br/com/gems/app/auditoria/interceptor/AuditoriaInterceptor.java` `backend/app-core/src/main/java/br/com/gems/app/auditoria/interceptor/RegistroPendente.java` `backend/app-core/src/test/java/br/com/gems/app/auditoria/interceptor/AuditoriaInterceptorTest.java` `backend/app-core/src/test/java/br/com/gems/app/auditoria/interceptor/AuditoriaIsolamentoTest.java` |
| C12 | EDIT | `specs/002-conformidade-gems/tasks.md` `specs/002-conformidade-gems/roteiro.md` — somente depois do gate verde |
| C12 | CREATE | `specs/002-conformidade-gems/pacote-de-execucao-C12.md` `specs/002-conformidade-gems/estado-lote-C12.md` |

## 2. Contrato publicado aferido

O artefato publicado 3.3.0 resolve do registry e expõe `AuditActorProvider` e
`AuditContextProvider`. `AuditContext(actorId, correlationId)` normaliza brancos para `null`; a
autoconfiguração mantém os providers como `@ConditionalOnMissingBean`. As colunas de contexto são
opt-in por `gems.auditing.context-columns-enabled=true`; o writer usa `ID_ACTOR` e
`CD_CORRELATION` na mesma conexão somente depois da migração.

## 3. Regras deste lote

1. Testes T026/T027 ficam vermelhos antes da substituição.
2. Dados históricos são convertidos por backfill antes de constraints; valor incompatível aborta.
3. Changeset aplicado não é editado; a evolução entra em changeset novo com rollback nominal.
4. Autor humano preserva nome e id estável do JWT; ausência ou falha segura vira autor técnico.
5. Correlação vem do contexto já estabelecido e não é inventada.
6. Entidades auditáveis e campos sigilosos usam somente as anotações publicadas.
7. A leitura consulta diretamente o novo schema; não resolve id por login.
8. A escrita local sai na mesma alteração em que a SDK e suas provas entram; não há coexistência.
9. Writer só é ativado depois de schema, leitura e providers estarem presentes no mesmo diff.
10. Sem alteração de API REST, payload ou status.

## 4. Portão e mutações

Em `backend/`:

```powershell
.\mvnw.cmd -pl app-deploy -am -Dtest=AuditoriaSchemaMigrationIntegrationTest,AuditoriaInterceptorIntegrationTest,AuditoriaSigiloIntegrationTest,AuditoriaCoberturaIntegrationTest,AuditoriaControllerSemEscritaTest,UsuarioAuditActorProviderTest,AppApplicationTests -Dsurefire.failIfNoSpecifiedTests=false test
```

Exigir exit 0, upgrade e `ddl-auto: validate` verdes. Depois mutar, um alvo por vez,
writer/config/schema/filtro/identidade/correlação/rollback real; exigir vermelho, restaurar por
edição inversa e repetir verde. Rodar `git diff --check` e o verificador de extensão no fechamento.

## 5. Relatório e rollback

Registrar T025–T036, arquivos, vermelho test-first, cada mutação e gate no estado C12. Rollback é
edição inversa somente dos caminhos C12 contra o estado C11 preservado; remover nomeadamente apenas
os dois documentos C12 e os arquivos CREATE de T026/T032. Nunca usar reset, clean ou checkout amplo.

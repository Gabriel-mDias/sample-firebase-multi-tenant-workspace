# Tasks: Conformidade GEMS

**Regra de execução**: testes/mutações antes da produção; cada bloco termina no comando nominal e só
fecha verde. `CREATE` marca arquivo inexistente. D-P01–D-P03 e T004 foram decididas no portão de
2026-09-14; D-P04 foi decidida após o inventário final e resolvida no C14-R1.

## Phase 0 — Decisões bloqueantes (4 tarefas)

- [x] T001 Registrar D-P01: preservar identidade estável/correlação por release SDK prévia (recomendado) ou emendar perda em `specs/002-conformidade-gems/roteiro.md`; sem decisão, não iniciar Phase 3. **Preservação aprovada em 2026-09-14.**
- [x] T002 Registrar D-P02: preservar ciclo Keycloak/roles por release SDK prévia (recomendado) ou emendar perda em `specs/002-conformidade-gems/roteiro.md`; sem decisão, não iniciar Phase 2. **Preservação aprovada em 2026-09-14.**
- [x] T003 Se a alternativa declarativa for escolhida, registrar D-P03 e expansão para `environments/shared/keycloak/app-realm.yaml`, `environments/local/docker-compose.yml`, `environments/staging/docker-compose.yml`, `environments/production/docker-compose.yml`; sem expansão, esses arquivos não são editados. **Não aplicável: ambiente/realm preservados.**
- [x] T004 Registrar expansão necessária para remover instruções operacionais obsoletas em `.github/workflows/ci.yml`, `.github/dependabot.yml` e `README.md`; sem aprovação, reportar os pontos e não editar. **Expansão aprovada em 2026-09-14.**

**Gate**: quatro decisões registradas; Constitution Check refeito sem PENDENTE aplicável.

## Phase 1 — Dependências (8 tarefas)

- [x] T005 Executar em `backend`: `.\mvnw.cmd dependency:tree`; guardar saída antes da troca e exigir exit 0. **C00: exit 0, BUILD SUCCESS nos seis módulos; GEMS 3.1.0 e ModelMapper 3.2.4 resolvidos.**
- [x] T006 Atualizar `backend/pom.xml`: GEMS publicada, processors MapStruct/Lombok e futura remoção de `ratchetFrom`; atualizar `backend/app-core/pom.xml`, `backend/app-model/pom.xml`, `backend/app-security/pom.xml`, `backend/app-deploy/pom.xml` apenas para módulos efetivamente consumidos. **C00: diff nominal conferido; `app-core -am test` verde com 13 testes security e 132 core.**
- [x] T007 Atualizar `frontend/package.json` e `frontend/package-lock.json` para SDK 2.1.0/overrides; executar em `frontend`: `npm ci`; exigir exit 0 sem flag. **C00: `npm ci` exit 0, 392 pacotes e 0 vulnerabilidades.**
- [x] T008 Remover comando/comentário obsoleto de `frontend/Dockerfile`; executar em `frontend`: `docker build --target build .`; exigir instalação/build exit 0 sem flag. **C09: stage `build` verde com `npm ci` e build de produção; credencial montada somente como secret BuildKit, sem argumento, cópia ou registro.**
- [x] T009 Após expansão T004, remover comando/comentário de `.github/workflows/ci.yml`; validar sintaxe do workflow e que o step executa `npm ci` sem flag. **C01: YAML analisado e step `npm ci`/diretório `frontend` afirmados, exit 0.**
- [x] T010 Após expansão T004, corrigir instruções em `README.md` e comentário em `.github/dependabot.yml`; exigir zero orientação ativa para usar a flag nesses arquivos. **C01: instalação `npm ci`, comentários Angular 22/SDK 2.1.0 e busca nominal sem ocorrências (rg exit 1).**
- [x] T011 Executar busca nominal sobre `frontend/Dockerfile`, `.github/workflows/ci.yml`, `.github/dependabot.yml`, `README.md`, `frontend/package.json` e `frontend/package-lock.json`; exigir zero invocação operacional da flag. Reportar separadamente menções imutáveis em `.specify/memory/constitution.md`, `.gems-ai/MIGRACAO-2.4.0.md` e `.gems-ai/recipes/setup-frontend-sdk.md`. **C01: seis alvos sem ocorrências (rg exit 1); referências imutáveis informadas pelo pacote separadas no estado, sem nova leitura fora de READ.**
- [x] T012 Executar em `backend`: `.\mvnw.cmd dependency:tree`; comparar antes/depois e exigir apenas alcance aprovado e exit 0. **C01: exit 0, BUILD SUCCESS nos seis módulos; GEMS 3.1.0, ModelMapper 3.2.4 temporário e MapStruct 1.6.3, coerentes com POMs e registro C00; POMs sem diff, git diff --check exit 0.**

**Gate**: dependências publicadas resolvidas; nenhum cache/SDK local; instalações operacionais sem flag.

## Phase 2 — Autorização e Keycloak, condicionada a D-P02 (12 tarefas)

- [x] T013 Aferir a fonte da nova release publicada contra FR-020 antes de editar; se qualquer operação faltar, parar nominalmente e manter as classes locais. **C11: 3.2.0 foi bloqueada por faltar o gateway de grupos; 3.3.0 resolveu os contratos de lifecycle, snapshot/restauração, memberships, roles e grupos. A árvore Maven confirmou os dois módulos 3.3.0 em compile.**
- [x] T014 Ajustar primeiro `backend/app-core/src/test/java/br/com/gems/app/autorizacao/AutorizacaoBootstrapTest.java`, `backend/app-core/src/test/java/br/com/gems/app/autorizacao/MapaPerfilAcoesTest.java`, `backend/app-core/src/test/java/br/com/gems/app/usuario/listener/UsuarioKeycloakListenerTest.java` e `backend/app-core/src/test/java/br/com/gems/app/usuario/service/UsuarioServiceTest.java`; provar falha contra a implementação anterior. **C11: os três consumidores afetados foram migrados test-first; `UsuarioServiceTest` já caracterizava o serviço sem acoplamento local e permaneceu no gate. A primeira compilação falhou contra `AcaoSistema` anterior, como exigido.**
- [x] T015 CREATE `backend/app-security/src/test/java/br/com/gems/app/security/AcaoSistemaCatalogTest.java` para 21 ações/escopos e CREATE `backend/app-deploy/src/test/java/br/com/gems/app/autorizacao/CatalogoAutorizacaoGeradoTest.java` para saída determinística; provar vermelho antes. **C11: ambos criados; o vermelho registrou `AcaoSistema` sem `AuthorizationAction`/`scope()`. O catálogo final contém 21 ações GLOBAL e geração determinística.**
- [x] T016 Alterar `backend/app-security/src/main/java/br/com/gems/app/security/AcaoSistema.java`, `backend/app-core/src/main/java/br/com/gems/app/autorizacao/MapaPerfilAcoes.java` e `backend/app-core/src/main/java/br/com/gems/app/autorizacao/AutorizacaoBootstrap.java` para os contratos publicados aferidos. **C11: enum e bootstrap usam os contratos SDK; `MapaPerfilAcoes` já era compatível e foi preservado, com teste por `AuthorizationCatalog`.**
- [x] T017 Alterar `backend/app-deploy/src/test/java/br/com/gems/app/autorizacao/CoberturaPreAuthorizeTest.java` para `EndpointAuthorizationScan`; mutar uma classificação e exigir vermelho. **C11: scan SDK verde; mutação temporária `ParceiroController` GLOBAL→TENANT ficou vermelha nas cinco operações e foi restaurada por edição inversa.**
- [x] T018 Alterar `backend/app-core/src/main/java/br/com/gems/app/auditoria/controller/AuditoriaController.java`, `backend/app-core/src/main/java/br/com/gems/app/localidade/controller/LocalidadeController.java`, `backend/app-core/src/main/java/br/com/gems/app/normalizacao/controller/CepController.java`, `backend/app-core/src/main/java/br/com/gems/app/parceiro/controller/ParceiroController.java` e `backend/app-core/src/main/java/br/com/gems/app/solicitacao/controller/SolicitacaoController.java` para classificações SDK. **C11: cinco controllers classificados `@GlobalEndpoint`; as três leituras referenciais autenticadas ficaram em exceção nominal fechada do teste, sem ampliar a superfície pública.**
- [x] T019 Alterar `backend/app-security/src/main/java/br/com/gems/app/security/config/SecurityConfig.java`; remover `backend/app-security/src/main/java/br/com/gems/app/security/annotation/PublicEndpoint.java` e `backend/app-security/src/main/java/br/com/gems/app/security/dto/PublicRoute.java` quando a busca nominal provar zero consumidor. **C11: configuração simplificada para rotas públicas fixas; tipos locais removidos e busca nominal zerada.**
- [x] T020 Alterar `backend/app-security/src/main/java/br/com/gems/app/security/config/KeycloakConfig.java`, `backend/app-deploy/src/main/resources/application.yml` e `backend/app-deploy/src/test/resources/application.yml` para a configuração publicada aferida. **C11: `KeycloakAdminProperties`/`KeycloakAdminRestClient` configurados com propriedades existentes e timeouts explícitos, sem segredo/default novo em código.**
- [x] T021 Alterar `backend/app-core/src/main/java/br/com/gems/app/usuario/listener/UsuarioKeycloakListener.java` e `backend/app-core/src/main/java/br/com/gems/app/usuario/service/UsuarioService.java` somente se a release cumprir criar/ler/alterar/habilitar/suspender/remover/compensar/grupos. **C11: listener migrou para gateways lifecycle/admin/grupos e preservou compensação e memberships; `UsuarioService` já dependia apenas do listener e não precisou de alteração. Onze testes do listener ficaram verdes.**
- [x] T022 Remover juntos `backend/app-security/src/main/java/br/com/gems/app/security/config/AppKeycloakProperties.java`, `backend/app-security/src/main/java/br/com/gems/app/security/service/KeycloakUserService.java`, `backend/app-security/src/main/java/br/com/gems/app/security/service/KeycloakRoleService.java`, `backend/app-security/src/main/java/br/com/gems/app/security/service/KeycloakGroupService.java` e, se órfão, `backend/app-security/src/main/java/br/com/gems/app/security/dto/CreateUserDTO.java`. **C11: cinco tipos locais removidos atomicamente; busca nominal por tipos/imports Keycloak locais ficou vazia.**
- [x] T023 Se D-P03 for escolhido, alterar só roles/compostas derivadas em `environments/shared/keycloak/app-realm.yaml` e preservar semanticamente todo o restante; validar os três compose files de T003 com `docker compose config` e a execução one-shot/cache false. **C11: não aplicável por D-P03; `environments/` permaneceu intocado.**
- [x] T024 Executar em `backend`: `.\mvnw.cmd -pl app-deploy -am -Dtest=AutorizacaoBootstrapTest,MapaPerfilAcoesTest,UsuarioKeycloakListenerTest,UsuarioServiceTest,AcaoSistemaCatalogTest,CatalogoAutorizacaoGeradoTest,CoberturaPreAuthorizeTest,AutorizacaoPorAcaoIntegrationTest -Dsurefire.failIfNoSpecifiedTests=false test`; exigir exit 0 e todos os testes selecionados executados. **C11: comando literal repetido após a restauração da mutação, exit 0/BUILD SUCCESS; 52 testes (1 security + 42 core + 9 deploy), zero falhas/erros/skips.**

**Gate**: ciclo Keycloak completo, catálogo/scan verdes e nenhuma classe/tipo órfão; sem D-P02, bloco não inicia.

## Phase 3 — Auditoria atômica, condicionada a D-P01 (12 tarefas, um único commit posterior)

- [x] T025 Aferir nova release publicada contra identidade estável/correlação antes de editar; se faltar, parar e manter schema/writer locais intactos. **C12: `gems-auditing:3.3.0` e sources resolveram; providers públicos e colunas opt-in confirmados.**
- [x] T026 CREATE `backend/app-deploy/src/test/java/br/com/gems/app/auditoria/AuditoriaSchemaMigrationIntegrationTest.java`; ajustar os três testes integrados; provar vermelho antes. **C12: primeiro vermelho por API/provider ausentes; schema, transação, sigilo e cobertura migrados.**
- [x] T027 Ajustar `AuditoriaControllerSemEscritaTest`; garantir ausência de endpoint de escrita. **C12: prova preservada, 2/2 verde; nenhum endpoint de escrita entrou.**
- [x] T028 Migrar `changelog-auditoria.xml`; backfill antes de constraints, sem truncamento, e rollback nominal. **C12: tabelas/colunas renomeadas para SDK, códigos e tipo de ator backfilled, rollback explícito e zero TRUNCATE.**
- [x] T029 Ajustar entidades e operação conforme API publicada. **C12: JPA lê `AUDIT_OPERATION`/`AUDIT_CHANGE`; converter preserva INSERCAO/ALTERACAO/EXCLUSAO na API.**
- [x] T030 Ajustar leitura e filtros sem resolver id por login. **C12: id estável é filtrado diretamente; data final inclusiva ganhou prova integrada.**
- [x] T031 Migrar entidades para anotações publicadas. **C12: `@Auditable` herdada e `@SensitiveField`; anotação local removida; referenciais continuam fora.**
- [x] T032 CREATE provider e teste. **C12: autor humano/técnico, id UUID estável e correlação provados 2/2.**
- [x] T033 Ativar writer somente com schema/read/provider no mesmo diff. **C12: `enabled`, schema e context columns ativos em produção/teste.**
- [x] T034 Remover writer/interceptor/pendente e testes locais depois de transferir garantias. **C12: cinco tipos/testes locais removidos; provas integradas preservadas.**
- [x] T035 Executar gate focado literal. **C12: exit 0; 20 testes selecionados (2 security, 2 core e 16 deploy), zero falhas/erros/skips; Liquibase e `ddl-auto: validate` verdes.**
- [x] T036 Mutar redes reais e restaurar. **C12: writer, config de contexto, schema, filtro inclusivo, identidade e correlação ficaram vermelhos separadamente; rollback real passou no consumidor e sua mutação pertence ao writer package-private já certificado na release produtora; gate final repetido verde.**

**Gate único**: migration + read + provider + writer + remoções e órfãos verdes juntos. Não há checkpoint T028–T034.

## Phase 4 — Mappers e rollback transacional real (15 tarefas)

- [x] T037 CREATE `backend/app-core/src/test/java/br/com/gems/app/localidade/service/LocalidadeServiceTest.java`; ajustar primeiro `backend/app-core/src/test/java/br/com/gems/app/normalizacao/service/ContatoServiceTest.java`, `backend/app-core/src/test/java/br/com/gems/app/normalizacao/service/EnderecoServiceTest.java`, `backend/app-core/src/test/java/br/com/gems/app/normalizacao/service/PessoaServiceTest.java`, `backend/app-core/src/test/java/br/com/gems/app/parceiro/service/ParceiroServiceTest.java` e `backend/app-core/src/test/java/br/com/gems/app/solicitacao/service/SolicitacaoServiceTest.java` para mappers; provar falha antes. **C02: vermelho test-first registrado no `testCompile`, 12 erros para seis mappers ausentes.**
- [x] T038 CREATE `backend/app-core/src/main/java/br/com/gems/app/localidade/mapper/LocalidadeMapper.java`. **C02: mapper estrito com `br.com.gems.mapstruct.GemsMappingConfig`; gate verde.**
- [x] T039 CREATE `backend/app-core/src/main/java/br/com/gems/app/normalizacao/mapper/ContatoMapper.java`. **C02: update preserva identidade/auditoria; gate verde.**
- [x] T040 CREATE `backend/app-core/src/main/java/br/com/gems/app/normalizacao/mapper/EnderecoMapper.java`. **C02: update preserva identidade/auditoria e referências gerenciadas; gate verde.**
- [x] T041 CREATE `backend/app-core/src/main/java/br/com/gems/app/normalizacao/mapper/PessoaMapper.java`. **C02: update preserva identidade/auditoria e normalização de CPF no service; gate verde.**
- [x] T042 CREATE `backend/app-core/src/main/java/br/com/gems/app/parceiro/mapper/ParceiroMapper.java`. **C02: DTOs aninhados preservados e comissão continua filtrada no service; gate verde.**
- [x] T043 CREATE `backend/app-core/src/main/java/br/com/gems/app/solicitacao/mapper/SolicitacaoMapper.java`. **C02: parceiro e etapas mapeados; gate verde.**
- [x] T044 Alterar `backend/app-core/src/main/java/br/com/gems/app/localidade/service/LocalidadeService.java`, `backend/app-core/src/main/java/br/com/gems/app/normalizacao/service/ContatoService.java`, `backend/app-core/src/main/java/br/com/gems/app/normalizacao/service/EnderecoService.java` e `backend/app-core/src/main/java/br/com/gems/app/normalizacao/service/PessoaService.java` para os quatro mappers. **C02: services injetam mapper primeiro e testes focados verdes.**
- [x] T045 Alterar `backend/app-core/src/main/java/br/com/gems/app/parceiro/service/ParceiroService.java` e `backend/app-core/src/main/java/br/com/gems/app/solicitacao/service/SolicitacaoService.java` para mappers e `org.springframework.transaction.annotation.Transactional(rollbackFor = Exception.class)`. **C02: transação Spring preservada; gate focado 70 testes, 0 falhas/erros/ignorados; `git diff --check` exit 0.**
- [x] T046 CREATE `backend/app-deploy/src/test/java/br/com/gems/app/parceiro/ParceiroRollbackIntegrationTest.java`: bean proxied, PostgreSQL real, exceção checked após escrita, teste sem `@Transactional`, nova leitura afirmando raiz/filhos/efeito/trilha ausentes. **C03: dois casos, checked IOException direta no mapper real após flush/IDs/SQL das quatro linhas; leitura física independente e controle positivo de raiz/filhos/contador/trilha verdes.**
- [x] T047 CREATE `backend/app-deploy/src/test/java/br/com/gems/app/solicitacao/SolicitacaoRollbackIntegrationTest.java` com as mesmas garantias para solicitação/evento. **C03: quatro casos insert/concluir com positivos; etapa de fixture/hook no insert, fixture EM_ANDAMENTO/etapa concluída no concluir, baseline SQL físico de estado/data/etapa/contador/trilha preservado; listener BEFORE_COMMIT real incrementa no positivo.**
- [x] T048 Remover `rollbackFor` real de `ParceiroService.java`, executar T050 e exigir vermelho por dado persistido; restaurar por edição inversa. **C03: insert mutado exit 1, 1 falha/0 erros/skips: PARCEIRO físico esperado 0L mas 1L; patch inverso e T050 verde, 76 casos. Service sem diff permanente.**
- [x] T049 Remover `rollbackFor` real de `SolicitacaoService.java`, executar T050 e exigir vermelho por dado/efeito persistido; restaurar por edição inversa. **C03: insert e concluir mutados separadamente, cada exit 1/1 falha/0 erros/skips: SOLICITACAO física 1L no insert; CONCLUIDA/data/etapa alterada/contador 1 no concluir. Cada patch inverso seguido por T050 verde, 76 casos; service sem diff permanente.**
- [x] T050 Executar em `backend`: `.\mvnw.cmd -pl app-deploy -am -Dtest=LocalidadeServiceTest,ContatoServiceTest,EnderecoServiceTest,PessoaServiceTest,ParceiroServiceTest,SolicitacaoServiceTest,ParceiroRollbackIntegrationTest,SolicitacaoRollbackIntegrationTest -Dsurefire.failIfNoSpecifiedTests=false test`; exigir exit 0 e todos selecionados executados. **C03: comando via cmd /c exit 0/BUILD SUCCESS pré/pós-T051 e após cada uma das três restaurações; 70 unitários + 2 parceiro + 4 solicitação = 76, zero falhas/erros/skips.**
- [x] T051 Executar busca nominal em `backend/app-core/pom.xml` e nos seis services T044/T045; exigir zero `ModelMapper`, remover sua dependência e repetir T050 verde. **C03: dependência gems-model-mapper core/gerenciamento pai e propriedade/pin ModelMapper retirados; dois POMs/seis services sem ocorrências (rg exit 1); dependency:tree filtrada exit 0/BUILD SUCCESS sem artefatos; T050 pós-remoção verde 76 casos, git diff --check exit 0.**

**Gate**: seis mappers estritos; dois rollbacks provados pelo proxy/banco, sem falso rollback de teste.

## Phase 5 — Java em zero (10 tarefas)

- [x] T052 Alterar apenas os quatro alvos reais `backend/app-core/src/main/java/br/com/gems/app/auditoria/repository/CampoAlteradoRepository.java`, `backend/app-core/src/main/java/br/com/gems/app/localidade/repository/EstadoRepository.java`, `backend/app-core/src/main/java/br/com/gems/app/localidade/repository/PaisRepository.java` e `backend/app-core/src/main/java/br/com/gems/app/solicitacao/repository/EtapaSolicitacaoRepository.java`; executar `backend/.\mvnw.cmd -pl app-deploy -am -Dtest=ConvencoesArquiteturaTest -Dsurefire.failIfNoSpecifiedTests=false test` e exigir zero repository. **C04: quatro interfaces conformadas; medição nominal zero e gate verde, 8 testes.**
- [x] T053 Alterar `backend/app-core/src/main/java/br/com/gems/app/localidade/controller/LocalidadeController.java` e `backend/app-core/src/main/java/br/com/gems/app/normalizacao/controller/CepController.java`; repetir o comando T052 e exigir zero controller. **C04: ambos estendem BaseController; medição nominal zero e gate verde, 8 testes.**
- [x] T054 Alterar/remover juntos `backend/app-core/src/main/java/br/com/gems/app/commons/exception/ProvedorExternoException.java`, `backend/app-core/src/main/java/br/com/gems/app/commons/exception/ProvedorExternoExceptionHandler.java`, `backend/app-security/src/main/java/br/com/gems/app/security/AccessDeniedExceptionHandler.java`, `backend/app-security/src/main/java/br/com/gems/app/security/IdentityProviderExceptionHandler.java` e `backend/app-security/src/main/java/br/com/gems/app/security/RequisicaoInvalidaExceptionHandler.java`; executar `backend/.\mvnw.cmd -pl app-deploy -am -Dtest=AppApplicationTests,AutorizacaoPorAcaoIntegrationTest -Dsurefire.failIfNoSpecifiedTests=false test` e exigir envelope/status equivalentes. **C04: teste-first vermelho em 3 casos; SDK assumiu 400/403/404/502, handlers locais removidos e falhas Keycloak traduzidas com mensagem/causa. Mensagens 403/404 mudaram conforme ajuste aprovado; 400 e 502 funcionais preservadas. Mutação 502→500 vermelha e restauração verde.**
- [x] T055 Corrigir seis ordens reais em `backend/app-core/src/main/java/br/com/gems/app/localidade/service/LocalidadeService.java`, `backend/app-core/src/main/java/br/com/gems/app/normalizacao/service/ContatoService.java`, `backend/app-core/src/main/java/br/com/gems/app/normalizacao/service/EnderecoService.java`, `backend/app-core/src/main/java/br/com/gems/app/normalizacao/service/PessoaService.java`, `backend/app-core/src/main/java/br/com/gems/app/parceiro/service/ParceiroService.java` e `backend/app-core/src/main/java/br/com/gems/app/solicitacao/service/SolicitacaoService.java`; exigir zero ordem no comando T052. **C04: medição nominal dos seis services zerada; privado residual de Pessoa movido após os públicos, assim como o residual de Keycloak já tocado por T054; ConvencoesFonteTest verde, 9 testes.**
- [x] T056 Renomear as 177 identidades reais nos 23 arquivos do Anexo A; executar `backend/.\mvnw.cmd -pl app-deploy -am -Dtest=ConvencoesFonteTest -Dsurefire.failIfNoSpecifiedTests=false test` e exigir zero nome. **C05: 177 identidades antigas removidas e 177 nomes conformes; portão final executou 9 testes, zero falhas/erros/skips.**
- [x] T057 Corrigir as 314 ocorrências nos 22 arquivos do Anexo B; esvaziar dívida em `backend/config/checkstyle/suppressions.xml` e `backend/app-deploy/src/test/resources/checkstyle-baseline.txt`; executar `backend/.\mvnw.cmd validate` e exigir Checkstyle zero. **C05: 314 ocorrências corrigidas, zero `import *`, baseline e supressões com zero entradas de dívida; `validate` verde nos seis módulos.**
- [x] T058 Remover tolerâncias zeradas de `backend/app-deploy/src/test/resources/archunit-baseline.txt` e manter `backend/app-deploy/src/test/resources/archunit.properties` reprovando; executar T052/T056 e exigir baseline zero. **C05-R1: baseline com zero entradas; o residual factual de C04/T055 foi corrigido movendo somente `possuiAuthority` para depois dos públicos; teste de fonte verde.**
- [x] T059 Executar `backend/.\mvnw.cmd spotless:apply`, revisar diff nominal e remover `ratchetFrom` de `backend/pom.xml`; executar `backend/.\mvnw.cmd spotless:check` e exigir todos os Java formatados. **C05: 29 + 56 = 85 Java formatados, nenhum 86º arquivo, `ratchetFrom` ausente e `spotless:check` integral verde; C05-R1 removeu seis espaços finais e `git diff --check` ficou verde.**
- [x] T060 Ajustar `backend/app-deploy/src/test/java/br/com/gems/app/convencoes/ConvencoesArquiteturaTest.java`, `backend/app-deploy/src/test/java/br/com/gems/app/convencoes/ConvencoesFonteTest.java`, `backend/app-deploy/src/test/java/br/com/gems/app/convencoes/RegrasDeFonte.java` e `backend/app-deploy/src/test/java/br/com/gems/app/convencoes/RelatorioDeConformidade.java` para exigir zero; mutar cada rede e exigir vermelho. **C06: baseline de arquitetura e supressões Checkstyle adicionados temporariamente deram vermelho; a regra de nomenclatura mutada também reprovou. As três restaurações por edição inversa voltaram ao verde.**
- [x] T061 Executar em `backend`: `.\mvnw.cmd clean install`; exigir exit 0, zero dívida Java, zero exceção e todos os testes. **C06: reator de seis módulos verde; 216 testes em 30 relatórios, zero falhas/erros/skips, Checkstyle e Spotless verdes.**

**Gate**: Java zero; 22 supressões de dívida eliminadas; ratchet removido.

## Phase 6 — Frontend até o mínimo medido (12 tarefas)

- [x] T062 Ajustar primeiro `frontend/src/app/core/guards/role.guard.spec.ts` e `frontend/src/app/core/menu/menu.builder.spec.ts`; CREATE `frontend/src/app/features/parceiro/list/parceiro-list.component.spec.ts`, CREATE `frontend/src/app/features/parceiro/form/parceiro-form.component.spec.ts`, CREATE `frontend/src/app/features/solicitacao/list/solicitacao-list.component.spec.ts` e CREATE `frontend/src/app/features/solicitacao/form/solicitacao-form.component.spec.ts`; provar vermelho para `isAuthorized` ausente.
- [x] T063 Alterar `frontend/src/app/core/auth/acoes.ts`, `frontend/src/app/core/auth/auth.service.ts`, `frontend/src/app/core/guards/role.guard.ts`, `frontend/src/app/core/menu/menu.builder.ts` e `frontend/src/app/shared/auditoria/botao-historico/botao-historico.component.ts` para autorização por ação.
- [x] T064 Alterar `frontend/src/app/features/parceiro/form/parceiro-form.component.ts`, `frontend/src/app/features/parceiro/form/steps/step-dados/step-dados.component.ts`, `frontend/src/app/features/parceiro/list/parceiro-list.component.ts`, `frontend/src/app/features/solicitacao/form/solicitacao-form.component.ts` e `frontend/src/app/features/solicitacao/list/solicitacao-list.component.ts`; exigir zero `hasRole`/`hasAnyRole` nos 12 arquivos medidos.
- [x] T065 Alterar `frontend/src/app/app.config.ts` e `frontend/src/app/core/auth/keycloak.init.ts` para provider 2.1.0/interceptor DI; ajustar `frontend/src/app/app.spec.ts` e exigir silent check-sso preservado.
- [x] T066 Ajustar primeiro `frontend/src/app/core/guards/routes-coverage.spec.ts`; CREATE `frontend/src/app/features/parceiro/parceiro.routes.spec.ts` e CREATE `frontend/src/app/features/solicitacao/solicitacao.routes.spec.ts`; alterar `frontend/src/app/features/parceiro/parceiro.routes.ts`, `frontend/src/app/features/solicitacao/solicitacao.routes.ts` e `frontend/src/app/app.routes.ts` para oito paths; provar permitido/negado/modo. **C08: oito rotas guardadas exatas; redirects para `list`; ação/modo nominais; mutação `view/:id`→`:id` vermelha e restauração verde.**
- [x] T067 Ajustar primeiro `frontend/src/app/shared/forms/field-error/field-error.component.spec.ts`, `frontend/src/app/shared/forms/dados-pessoais/dados-pessoais-fields.component.spec.ts` e `frontend/src/app/shared/forms/contato/contato-fields.component.spec.ts`; migrar consumidores e remover `frontend/src/app/shared/forms/field-error/field-error.component.ts`, `frontend/src/app/shared/forms/field-error/field-error.component.html`, `frontend/src/app/shared/forms/field-error/field-error.component.css`, `frontend/src/app/shared/forms/field-error/field-error.component.spec.ts` após equivalência. **C08: equivalência local/SDK verde em 6 casos antes da remoção; consumidores usam `GemsFieldErrorComponent`; cópia local e spec removidos juntos.**
- [x] T068 Mover `frontend/src/app/commons/data-br.ts` para CREATE `frontend/src/app/shared/utils/data-br.ts`, `frontend/src/app/commons/data-br.spec.ts` para CREATE `frontend/src/app/shared/utils/data-br.spec.ts`, `frontend/src/app/commons/moeda-br.ts` para CREATE `frontend/src/app/shared/utils/moeda-br.ts`, `frontend/src/app/commons/moeda-br.spec.ts` para CREATE `frontend/src/app/shared/utils/moeda-br.spec.ts`, `frontend/src/app/commons/page-response.ts` para CREATE `frontend/src/app/shared/models/page-response.ts`, `frontend/src/app/commons/password.validator.ts` para CREATE `frontend/src/app/shared/validators/password.validator.ts`, `frontend/src/app/commons/password.validator.spec.ts` para CREATE `frontend/src/app/shared/validators/password.validator.spec.ts`, `frontend/src/app/commons/table-sort.ts` para CREATE `frontend/src/app/shared/utils/table-sort.ts`, `frontend/src/app/commons/table-sort.spec.ts` para CREATE `frontend/src/app/shared/utils/table-sort.spec.ts` e `frontend/src/app/commons/gems-table-labels.ts` para CREATE `frontend/src/app/shared/models/gems-table-labels.ts`; atualizar cada import consumidor no mesmo bloco. **C08: dez arquivos movidos, imports atualizados e zero `commons/`; 32 nomes legados que entraram no alcance de `testing.md#7` foram conformados; 4 arquivos/32 testes verdes.**
- [x] T069 Substituir `Router` onde aplicável em `frontend/src/app/app.ts`, `frontend/src/app/features/acesso-negado/acesso-negado.component.ts`, `frontend/src/app/features/parceiro/form/parceiro-form.component.ts`, `frontend/src/app/features/parceiro/form/steps/step-dados/step-dados.component.ts`, `frontend/src/app/features/parceiro/list/parceiro-list.component.ts`, `frontend/src/app/features/solicitacao/form/solicitacao-form.component.ts` e `frontend/src/app/features/solicitacao/list/solicitacao-list.component.ts`; não transportar domínio na sessão. **C08: seis componentes migrados para `GemsNavigationService`; `app.ts` preserva `Router` apenas para observar `NavigationEnd`; estado de domínio continua em signals. Mutação `/form`→`/novo` vermelha e restaurada.**
- [x] T070 Remover `standalone: true` residual de `frontend/src/app/core/footer/app-footer.component.ts`; tokenizar as 70 cores de `frontend/src/styles.css` e a cor de `frontend/src/app/core/footer/app-footer.component.css` sem alterar `frontend/src/index.html` ou specs quando o literal for dado de teste/metadata justificado. **C09: standalone removido; tokens GEMS/`--meduc-*` aplicados; Prettier e convenções verdes.**
- [x] T071 Ajustar `frontend/scripts/verify-authorization-actions.mjs` e `frontend/scripts/verify-conventions.mjs`; medir cada residual por arquivo/regra e preencher D-P04 sem marcar aceite. **C09: 21 ações em paridade; 13 regras/124 arquivos/0 violações; inventário residual mantido, sem aceite de D-P04.**
- [x] T072 Executar em `frontend`: `npx ng test --watch=false --include=src/app/app.spec.ts --include=src/app/core/guards/role.guard.spec.ts --include=src/app/core/guards/routes-coverage.spec.ts --include=src/app/core/menu/menu.builder.spec.ts --include=src/app/shared/forms/dados-pessoais/dados-pessoais-fields.component.spec.ts --include=src/app/shared/forms/contato/contato-fields.component.spec.ts`; exigir exit 0 e todos os arquivos selecionados executados. **C09: 6 arquivos/30 testes verdes.**
- [x] T073 Executar em `frontend`: `npx ng test --watch=false --include=src/app/features/parceiro/parceiro.routes.spec.ts --include=src/app/features/parceiro/list/parceiro-list.component.spec.ts --include=src/app/features/parceiro/form/parceiro-form.component.spec.ts --include=src/app/features/parceiro/services/parceiro-form-state.service.spec.ts --include=src/app/features/solicitacao/solicitacao.routes.spec.ts --include=src/app/features/solicitacao/list/solicitacao-list.component.spec.ts --include=src/app/features/solicitacao/form/solicitacao-form.component.spec.ts`; exigir exit 0 e todos os arquivos selecionados executados. **C09: 7 arquivos/18 testes verdes.**

**Gate**: frontend no mínimo medido; D-P04 devolvida para correção e residual zerado no C14-R1.

## Phase 7 — Fechamento (6 tarefas)

- [x] T074 Executar mutações de catálogo, endpoint, auditoria, rollback, zero Java, autorização/rotas e residual frontend; cada mutante deve ficar vermelho e ser restaurado por edição inversa. **C13: sete redes ficaram vermelhas separadamente e foram restauradas por edição inversa; a entrada obsoleta do baseline que mascarava `standalone` foi removida após autorização nominal, e todos os gates restaurados ficaram verdes.**
- [x] T075 Executar em `backend`: `.\mvnw.cmd clean install`; registrar exit, módulos e totais, exigindo no mínimo os 204 testes aplicáveis mais novos. **C13: após autorização nominal, somente `SolicitacaoRollbackIntegrationTest` foi alinhado ao schema C12; gate focal 4/4 e `clean install` com exit 0/BUILD SUCCESS, security 16, core 124 e deploy 73, total 213, zero falhas/erros/skips.**
- [x] T076 Executar em `frontend`: `npm ci`, `npm test`, `npm run build`, `npx prettier --check .`; registrar exit e no mínimo 21 arquivos/118 testes aplicáveis mais novos. **C13: quatro comandos com exit 0; 26/26 arquivos e 123/123 testes, build verde com aviso não bloqueante de budget e Prettier integral verde. Verificadores confirmaram SSO, 21 ações em paridade e 13 regras/124 arquivos com zero violações.**
- [x] T077 Executar `quickstart.md`, incluindo upgrade, identidade/correlação, Keycloak one-shot e oito rotas. **C14: base local migrou de 32 para 33 changesets e reiniciou com zero pendências; três compose files válidos; one-shot 2/2 exit 0; bootstrap real 21 ações, 21→0 filhos e 3/3 grupos; oito rotas provadas por gate focal 9/9; demais contratos reutilizaram os gates completos C13.**
- [x] T078 Apresentar inventário frontend mínimo ao product owner; só após aceite nominal atualizar D-P04. Nenhuma aprovação é inferida. **C14-R1: D-P04 foi devolvida nominalmente para correção; 63/63 ocorrências eliminadas, baseline vazio, 13 regras/126 arquivos com zero violações e frontend 27 arquivos/125 testes verde.**
- [x] T079 Refazer analyze/roteiro, exigir zero CRITICAL/violação constitucional e parar no portão humano seguinte sem commit do executor. **C15: analyze final com 30/30 FR, 9/9 SC, 79/79 tarefas, zero CRITICAL, zero violação constitucional e zero achado bloqueante; parada no portão de preservação da worktree do Meduc.**

## Anexo A — 23 arquivos que contêm os 177 nomes reais

- `backend/app-core/src/test/java/br/com/gems/app/auditoria/controller/AuditoriaControllerSemEscritaTest.java`
- `backend/app-core/src/test/java/br/com/gems/app/auditoria/interceptor/AuditoriaInterceptorTest.java`
- `backend/app-core/src/test/java/br/com/gems/app/auditoria/interceptor/AuditoriaIsolamentoTest.java`
- `backend/app-core/src/test/java/br/com/gems/app/autorizacao/AutorizacaoBootstrapTest.java`
- `backend/app-core/src/test/java/br/com/gems/app/autorizacao/MapaPerfilAcoesTest.java`
- `backend/app-core/src/test/java/br/com/gems/app/normalizacao/service/CepServiceTest.java`
- `backend/app-core/src/test/java/br/com/gems/app/normalizacao/service/ContatoServiceTest.java`
- `backend/app-core/src/test/java/br/com/gems/app/normalizacao/service/EnderecoServiceTest.java`
- `backend/app-core/src/test/java/br/com/gems/app/normalizacao/service/PessoaServiceTest.java`
- `backend/app-core/src/test/java/br/com/gems/app/parceiro/service/ParceiroServiceTest.java`
- `backend/app-core/src/test/java/br/com/gems/app/solicitacao/service/SolicitacaoServiceTest.java`
- `backend/app-core/src/test/java/br/com/gems/app/usuario/listener/UsuarioKeycloakListenerTest.java`
- `backend/app-core/src/test/java/br/com/gems/app/usuario/service/UsuarioServiceTest.java`
- `backend/app-deploy/src/test/java/br/com/gems/app/AppApplicationTests.java`
- `backend/app-deploy/src/test/java/br/com/gems/app/auditoria/AuditoriaInterceptorIntegrationTest.java`
- `backend/app-deploy/src/test/java/br/com/gems/app/auditoria/AuditoriaSigiloIntegrationTest.java`
- `backend/app-deploy/src/test/java/br/com/gems/app/auditoria/interceptor/AuditoriaCoberturaIntegrationTest.java`
- `backend/app-deploy/src/test/java/br/com/gems/app/autorizacao/AutorizacaoPorAcaoIntegrationTest.java`
- `backend/app-deploy/src/test/java/br/com/gems/app/autorizacao/CoberturaPreAuthorizeTest.java`
- `backend/app-deploy/src/test/java/br/com/gems/app/security/ActuatorSecurityIntegrationTest.java`
- `backend/app-deploy/src/test/java/br/com/gems/app/security/S3EndpointsSecurityIntegrationTest.java`
- `backend/app-security/src/test/java/br/com/gems/app/security/PasswordUtilTest.java`
- `backend/app-security/src/test/java/br/com/gems/app/security/service/UsuarioAutenticadoServiceTest.java`

T056 usa as 177 identidades desses arquivos em `backend/app-deploy/src/test/resources/archunit-baseline.txt`,
sem usar o histórico de 179.

## Anexo B — 22 arquivos do baseline Checkstyle

- `backend/app-core/src/main/java/br/com/gems/app/autorizacao/MapaPerfilAcoes.java`
- `backend/app-core/src/main/java/br/com/gems/app/parceiro/controller/ParceiroController.java`
- `backend/app-core/src/main/java/br/com/gems/app/solicitacao/controller/SolicitacaoController.java`
- `backend/app-core/src/test/java/br/com/gems/app/autorizacao/AutorizacaoBootstrapTest.java`
- `backend/app-core/src/test/java/br/com/gems/app/parceiro/service/ParceiroServiceTest.java`
- `backend/app-core/src/test/java/br/com/gems/app/solicitacao/service/SolicitacaoServiceTest.java`
- `backend/app-deploy/src/main/java/br/com/gems/app/AppApplication.java`
- `backend/app-deploy/src/test/java/br/com/gems/app/security/S3EndpointsSecurityIntegrationTest.java`
- `backend/app-deploy/src/test/java/br/com/gems/app/support/AbstractIntegrationTest.java`
- `backend/app-model/src/main/java/br/com/gems/app/model/entity/localidades/Estado.java`
- `backend/app-model/src/main/java/br/com/gems/app/model/entity/localidades/Pais.java`
- `backend/app-model/src/main/java/br/com/gems/app/model/entity/normalizacao/Contato.java`
- `backend/app-model/src/main/java/br/com/gems/app/model/entity/normalizacao/Endereco.java`
- `backend/app-model/src/main/java/br/com/gems/app/model/entity/normalizacao/Pessoa.java`
- `backend/app-model/src/main/java/br/com/gems/app/model/entity/parceiro/Parceiro.java`
- `backend/app-model/src/main/java/br/com/gems/app/model/entity/seguranca/Usuario.java`
- `backend/app-model/src/main/java/br/com/gems/app/model/entity/solicitacao/EtapaSolicitacao.java`
- `backend/app-model/src/main/java/br/com/gems/app/model/entity/solicitacao/Solicitacao.java`
- `backend/app-security/src/main/java/br/com/gems/app/security/PasswordUtil.java`
- `backend/app-security/src/main/java/br/com/gems/app/security/config/KeycloakConfig.java`
- `backend/app-security/src/main/java/br/com/gems/app/security/config/SecurityConfig.java`
- `backend/app-security/src/main/java/br/com/gems/app/security/service/KeycloakUserService.java`

Linhas e checks são a lista fechada de `backend/app-deploy/src/test/resources/checkstyle-baseline.txt`.

## Retomada compacta — baseline + delta

C00–C06 são histórico verde e não são regenerados. T061 permanece em curso no programa até todos os
blocos S encerrarem, embora sua execução técnica tenha ficado verde em C06. A execução restante usa
`baseline-codex-20260916.md`: C07 (T062–T065), C08 (T066–T069), C09 (T008, T070–T073 e Docker),
C10-SDK, C11 (T013–T024), C12 (T025–T036), C13 (T074–T076), C14 (T077–T078) e C15 (T079).
C10-SDK tem de ficar verde antes de C11; C12 depende de C11 verde.

## Contagem

79 tarefas em 8 fases: decisões 4; dependências 8; autorização/Keycloak 12; auditoria atômica 12;
mappers/transações 15; Java zero 10; frontend 12; fechamento 6.

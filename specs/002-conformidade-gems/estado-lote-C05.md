# Estado — Lote C05, rodada 002

## 1. Rollback deste lote

Na branch de trabalho, restaurar somente os arquivos rastreados enumerados em `EDIT` no pacote C05,
após conferir cada um contra a âncora `C:/Users/gabri/AppData/Local/Temp/sample-c05-pre-dispatch-20260916.porcelain`.
Remover somente `specs/002-conformidade-gems/estado-lote-C05.md`; preservar o pacote C05 já não
rastreado e qualquer alteração que não pertença a este executor. Nunca usar rollback amplo.

## 2. Concluído

| Tarefa | O que provou |
| :-- | :--- |
| T056 | 177 identidades antigas ausentes e 177 métodos conformes nos 23 arquivos; `git diff --numstat` mostrou exatamente 177 linhas removidas/adicionadas. No portão, o teste de nomes passou; a única falha entre os 9 casos de `ConvencoesFonteTest` foi a ordem residual descrita em T058. |
| T057 | Zero `import *` nos 22 alvos; `suppressions.xml` e `checkstyle-baseline.txt` sem dívida. O primeiro estágio do portão compilou os módulos e reportou zero violações Checkstyle em produção e testes. |
| T059 (implementação nominal) | `spotless:apply` terminou BUILD SUCCESS; 29 + 56 = 85 Java formatados, sem 86º arquivo. `ratchetFrom` foi removido e o primeiro estágio do portão reportou zero arquivos pendentes no Spotless. A cadeia completa não prosseguiu além do teste por conflito em T058. |
| T058 / recuperação T055 | `archunit-baseline.txt` terminou com zero entradas reais. C05-R1 moveu somente o privado `possuiAuthority`, sem alterar corpo, assinatura ou chamadas, para depois do público `loginDoUsuarioAutenticado`; `ConvencoesFonteTest` passou com 9 testes, zero falhas/erros/skips. |
| T059 / fechamento C05 | Os seis espaços finais declarados foram removidos de quatro Javadocs. A cadeia literal terminou exit 0/BUILD SUCCESS nos três Maven: teste de fonte, `validate` com Checkstyle zero nos seis módulos e `spotless:check` integral sem arquivo pendente. `git diff --check` terminou exit 0. |

## 3. Em curso

- **Tarefa:** nenhuma; C05-R1 e C05 concluídos
- **Arquivo:** nenhum com mutação ativa
- **O que já está feito nele:** T056–T059, correção residual de T055, limpeza de whitespace, portão literal, `git diff --check` e registros em tasks/roteiro concluídos
- **O que falta:** nada neste pacote; parar antes de T060/C06

## 4. Não iniciado

Nenhuma.

## 5. Arquivos tocados

`git status --porcelain --untracked-files=all`

```text
 M backend/app-core/src/main/java/br/com/gems/app/auditoria/controller/AuditoriaController.java
 M backend/app-core/src/main/java/br/com/gems/app/auditoria/interceptor/AuditoriaGravador.java
 M backend/app-core/src/main/java/br/com/gems/app/auditoria/interceptor/AuditoriaInterceptor.java
 M backend/app-core/src/main/java/br/com/gems/app/auditoria/interceptor/RegistroPendente.java
 M backend/app-core/src/main/java/br/com/gems/app/auditoria/repository/RegistroAuditoriaRepository.java
 M backend/app-core/src/main/java/br/com/gems/app/auditoria/service/AuditoriaService.java
 M backend/app-core/src/main/java/br/com/gems/app/autorizacao/AutorizacaoBootstrap.java
 M backend/app-core/src/main/java/br/com/gems/app/autorizacao/MapaPerfilAcoes.java
 M backend/app-core/src/main/java/br/com/gems/app/normalizacao/dto/ViaCepResponse.java
 M backend/app-core/src/main/java/br/com/gems/app/normalizacao/repository/ContatoRepository.java
 M backend/app-core/src/main/java/br/com/gems/app/normalizacao/repository/EnderecoRepository.java
 M backend/app-core/src/main/java/br/com/gems/app/normalizacao/repository/PessoaRepository.java
 M backend/app-core/src/main/java/br/com/gems/app/normalizacao/service/CepService.java
 M backend/app-core/src/main/java/br/com/gems/app/parceiro/controller/ParceiroController.java
 M backend/app-core/src/main/java/br/com/gems/app/parceiro/listener/SolicitacaoConcluidaListener.java
 M backend/app-core/src/main/java/br/com/gems/app/parceiro/repository/ParceiroRepository.java
 M backend/app-core/src/main/java/br/com/gems/app/solicitacao/controller/SolicitacaoController.java
 M backend/app-core/src/main/java/br/com/gems/app/solicitacao/repository/SolicitacaoRepository.java
 M backend/app-core/src/main/java/br/com/gems/app/usuario/listener/UsuarioKeycloakListener.java
 M backend/app-core/src/main/java/br/com/gems/app/usuario/repository/UsuarioRepository.java
 M backend/app-core/src/main/java/br/com/gems/app/usuario/service/UsuarioService.java
 M backend/app-core/src/test/java/br/com/gems/app/auditoria/controller/AuditoriaControllerSemEscritaTest.java
 M backend/app-core/src/test/java/br/com/gems/app/auditoria/interceptor/AuditoriaInterceptorTest.java
 M backend/app-core/src/test/java/br/com/gems/app/auditoria/interceptor/AuditoriaIsolamentoTest.java
 M backend/app-core/src/test/java/br/com/gems/app/autorizacao/AutorizacaoBootstrapTest.java
 M backend/app-core/src/test/java/br/com/gems/app/autorizacao/MapaPerfilAcoesTest.java
 M backend/app-core/src/test/java/br/com/gems/app/normalizacao/service/CepServiceTest.java
 M backend/app-core/src/test/java/br/com/gems/app/normalizacao/service/ContatoServiceTest.java
 M backend/app-core/src/test/java/br/com/gems/app/normalizacao/service/EnderecoServiceTest.java
 M backend/app-core/src/test/java/br/com/gems/app/normalizacao/service/PessoaServiceTest.java
 M backend/app-core/src/test/java/br/com/gems/app/parceiro/service/ParceiroServiceTest.java
 M backend/app-core/src/test/java/br/com/gems/app/solicitacao/service/SolicitacaoServiceTest.java
 M backend/app-core/src/test/java/br/com/gems/app/usuario/listener/UsuarioKeycloakListenerTest.java
 M backend/app-core/src/test/java/br/com/gems/app/usuario/service/UsuarioServiceTest.java
 M backend/app-deploy/src/main/java/br/com/gems/app/AppApplication.java
 M backend/app-deploy/src/test/java/br/com/gems/app/AppApplicationTests.java
 M backend/app-deploy/src/test/java/br/com/gems/app/auditoria/AuditoriaInterceptorIntegrationTest.java
 M backend/app-deploy/src/test/java/br/com/gems/app/auditoria/AuditoriaSigiloIntegrationTest.java
 M backend/app-deploy/src/test/java/br/com/gems/app/auditoria/interceptor/AuditoriaCoberturaIntegrationTest.java
 M backend/app-deploy/src/test/java/br/com/gems/app/autorizacao/AutorizacaoPorAcaoIntegrationTest.java
 M backend/app-deploy/src/test/java/br/com/gems/app/autorizacao/CoberturaPreAuthorizeTest.java
 M backend/app-deploy/src/test/java/br/com/gems/app/security/ActuatorSecurityIntegrationTest.java
 M backend/app-deploy/src/test/java/br/com/gems/app/security/S3EndpointsSecurityIntegrationTest.java
 M backend/app-deploy/src/test/java/br/com/gems/app/support/AbstractIntegrationTest.java
 M backend/app-deploy/src/test/resources/archunit-baseline.txt
 M backend/app-deploy/src/test/resources/checkstyle-baseline.txt
 M backend/app-events/src/main/java/br/com/gems/app/normalizacao/event/PessoaExcluidaEvent.java
 M backend/app-events/src/main/java/br/com/gems/app/solicitacao/event/SolicitacaoConcluidaEvent.java
 M backend/app-events/src/main/java/br/com/gems/app/usuario/event/UsuarioAtualizadoEvent.java
 M backend/app-events/src/main/java/br/com/gems/app/usuario/event/UsuarioCriadoEvent.java
 M backend/app-events/src/main/java/br/com/gems/app/usuario/event/UsuarioDesativadoEvent.java
 M backend/app-events/src/main/java/br/com/gems/app/usuario/event/UsuarioReativadoEvent.java
 M backend/app-model/src/main/java/br/com/gems/app/model/dto/CampoAlteradoDTO.java
 M backend/app-model/src/main/java/br/com/gems/app/model/dto/EtapaSolicitacaoDTO.java
 M backend/app-model/src/main/java/br/com/gems/app/model/dto/OpcaoDTO.java
 M backend/app-model/src/main/java/br/com/gems/app/model/dto/ParceiroDTO.java
 M backend/app-model/src/main/java/br/com/gems/app/model/dto/RegistroAuditoriaDTO.java
 M backend/app-model/src/main/java/br/com/gems/app/model/dto/SolicitacaoDTO.java
 M backend/app-model/src/main/java/br/com/gems/app/model/dto/UsuarioDTO.java
 M backend/app-model/src/main/java/br/com/gems/app/model/entity/auditoria/CampoAlterado.java
 M backend/app-model/src/main/java/br/com/gems/app/model/entity/auditoria/RegistroAuditoria.java
 M backend/app-model/src/main/java/br/com/gems/app/model/entity/base/CampoSigiloso.java
 M backend/app-model/src/main/java/br/com/gems/app/model/entity/base/EntidadeAuditavel.java
 M backend/app-model/src/main/java/br/com/gems/app/model/entity/localidades/Estado.java
 M backend/app-model/src/main/java/br/com/gems/app/model/entity/localidades/Pais.java
 M backend/app-model/src/main/java/br/com/gems/app/model/entity/normalizacao/Contato.java
 M backend/app-model/src/main/java/br/com/gems/app/model/entity/normalizacao/Endereco.java
 M backend/app-model/src/main/java/br/com/gems/app/model/entity/normalizacao/Pessoa.java
 M backend/app-model/src/main/java/br/com/gems/app/model/entity/parceiro/Parceiro.java
 M backend/app-model/src/main/java/br/com/gems/app/model/entity/seguranca/Usuario.java
 M backend/app-model/src/main/java/br/com/gems/app/model/entity/solicitacao/EtapaSolicitacao.java
 M backend/app-model/src/main/java/br/com/gems/app/model/entity/solicitacao/Solicitacao.java
 M backend/app-model/src/main/java/br/com/gems/app/model/enums/OperacaoAuditoria.java
 M backend/app-model/src/main/java/br/com/gems/app/model/enums/PerfilAtuacao.java
 M backend/app-model/src/main/java/br/com/gems/app/model/enums/Sexo.java
 M backend/app-model/src/main/java/br/com/gems/app/model/enums/SituacaoSolicitacao.java
 M backend/app-model/src/main/java/br/com/gems/app/model/params/AuditoriaFilterParams.java
 M backend/app-model/src/main/java/br/com/gems/app/model/params/ParceiroFilterParams.java
 M backend/app-model/src/main/java/br/com/gems/app/model/params/SolicitacaoFilterParams.java
 M backend/app-model/src/main/java/br/com/gems/app/model/response/ParceiroResponseDTO.java
 M backend/app-model/src/main/java/br/com/gems/app/model/response/SolicitacaoResponseDTO.java
 M backend/app-security/src/main/java/br/com/gems/app/security/AcaoSistema.java
 M backend/app-security/src/main/java/br/com/gems/app/security/PasswordUtil.java
 M backend/app-security/src/main/java/br/com/gems/app/security/SecurityRoles.java
 M backend/app-security/src/main/java/br/com/gems/app/security/config/KeycloakConfig.java
 M backend/app-security/src/main/java/br/com/gems/app/security/config/SecurityConfig.java
 M backend/app-security/src/main/java/br/com/gems/app/security/dto/CreateUserDTO.java
 M backend/app-security/src/main/java/br/com/gems/app/security/dto/PublicRoute.java
 M backend/app-security/src/main/java/br/com/gems/app/security/service/KeycloakGroupService.java
 M backend/app-security/src/main/java/br/com/gems/app/security/service/KeycloakRoleService.java
 M backend/app-security/src/main/java/br/com/gems/app/security/service/UsuarioAutenticadoService.java
 M backend/app-security/src/test/java/br/com/gems/app/security/PasswordUtilTest.java
 M backend/app-security/src/test/java/br/com/gems/app/security/service/UsuarioAutenticadoServiceTest.java
 M backend/config/checkstyle/suppressions.xml
 M backend/pom.xml
 M specs/002-conformidade-gems/roteiro.md
 M specs/002-conformidade-gems/tasks.md
?? specs/002-conformidade-gems/estado-lote-C05.md
?? specs/002-conformidade-gems/pacote-de-execucao-C05.md
```

## 6. Decisões fora do pacote

A única divergência factual foi a já registrada na recuperação: C04/T055 declarou ordem zero, mas o
gate sem baseline revelou `possuiAuthority` antes do último método público. O pacote C05-R1 autorizou
explicitamente a correção exata; nenhuma decisão adicional nem expansão de escopo foi tomada. O
verificador terminou exit 0 com 97 arquivos declarados e 97 reais; registrou apenas
`KeycloakUserService.java` como um dos 98 caminhos nominais não entregue, pois ele não tem diff na
árvore final. Nenhuma edição artificial foi criada para eliminar essa ressalva.

## 7. O que está quebrado agora

Nada — nenhuma mutação ativa. O portão literal terminou exit 0 nos três Maven; 9 testes de fonte,
zero falhas/erros/skips, Checkstyle zero nos seis módulos, Spotless integral sem pendências e
`git diff --check` exit 0.

## 8. Retomada

- **Executor:** Codex
- **Token de retomada:** não exposto por este ambiente; a política desta execução é `nova` e nenhum token anterior foi recebido.

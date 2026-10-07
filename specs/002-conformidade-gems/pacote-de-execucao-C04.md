# Pacote de Execução — Rodada 002, Lote C04
| Controle | Valor |
| :--- | :--- |
| Checkpoint | C04; C03 verde; estado aberto |
| Sessão | nova; SHA aa5cd3c; sem token antigo |
| Modelo | gpt-5.6-sol; Astra fallback autorizado; sem Terra |
| Admissão 1–5 | sim: contrato aprovado; caminhos nominais; disjuntos; gate focado; repetição amortiza pacote/revisão |
| Custo | quatro repositories/dois controllers/seis services e testes supera onboarding/revisão; segurança com Sol |
| Âncora | C:/Users/gabri/AppData/Local/Temp/sample-c04-pre-dispatch-20260916.porcelain |
Crie estado-lote-C04.md do template antes de editar. Não commite/push/PR. READ fechado; falta exige parada.
## 1. Fronteira fechada do lote
Somente T052–T055. Sem adoção auditoria/Keycloak, frontend, ambiente ou baseline/ratchet.
### READ
- `.gems-ai/rules/backend-java/architecture.md`
- `.gems-ai/rules/backend-java/controllers.md`
- `.gems-ai/rules/backend-java/mapping.md`
- `.gems-ai/rules/backend-java/repositories.md`
- `.gems-ai/rules/backend-java/services.md`
- `.gems-ai/rules/backend-java/testing.md`
- `.specify/memory/constitution.md`
- `.specify/scripts/powershell/verificar-lote.ps1`
- `.specify/templates/estado-lote-template.md`
- `AGENTS.md`
- `backend/AGENTS.md`
- `backend/app-core/pom.xml`
- `backend/app-core/src/main/java/br/com/gems/app/auditoria/repository/CampoAlteradoRepository.java`
- `backend/app-core/src/main/java/br/com/gems/app/commons/exception/ProvedorExternoException.java`
- `backend/app-core/src/main/java/br/com/gems/app/commons/exception/ProvedorExternoExceptionHandler.java`
- `backend/app-core/src/main/java/br/com/gems/app/localidade/controller/LocalidadeController.java`
- `backend/app-core/src/main/java/br/com/gems/app/localidade/mapper/LocalidadeMapper.java`
- `backend/app-core/src/main/java/br/com/gems/app/localidade/repository/EstadoRepository.java`
- `backend/app-core/src/main/java/br/com/gems/app/localidade/repository/PaisRepository.java`
- `backend/app-core/src/main/java/br/com/gems/app/localidade/service/LocalidadeService.java`
- `backend/app-core/src/main/java/br/com/gems/app/normalizacao/controller/CepController.java`
- `backend/app-core/src/main/java/br/com/gems/app/normalizacao/mapper/ContatoMapper.java`
- `backend/app-core/src/main/java/br/com/gems/app/normalizacao/mapper/EnderecoMapper.java`
- `backend/app-core/src/main/java/br/com/gems/app/normalizacao/mapper/PessoaMapper.java`
- `backend/app-core/src/main/java/br/com/gems/app/normalizacao/service/ContatoService.java`
- `backend/app-core/src/main/java/br/com/gems/app/normalizacao/service/EnderecoService.java`
- `backend/app-core/src/main/java/br/com/gems/app/normalizacao/service/PessoaService.java`
- `backend/app-core/src/main/java/br/com/gems/app/parceiro/controller/ParceiroController.java`
- `backend/app-core/src/main/java/br/com/gems/app/parceiro/mapper/ParceiroMapper.java`
- `backend/app-core/src/main/java/br/com/gems/app/parceiro/repository/ParceiroRepository.java`
- `backend/app-core/src/main/java/br/com/gems/app/parceiro/service/ParceiroService.java`
- `backend/app-core/src/main/java/br/com/gems/app/solicitacao/mapper/SolicitacaoMapper.java`
- `backend/app-core/src/main/java/br/com/gems/app/solicitacao/repository/EtapaSolicitacaoRepository.java`
- `backend/app-core/src/main/java/br/com/gems/app/solicitacao/service/SolicitacaoService.java`
- `backend/app-deploy/pom.xml`
- `backend/app-deploy/src/test/java/br/com/gems/app/AppApplicationTests.java`
- `backend/app-deploy/src/test/java/br/com/gems/app/autorizacao/AutorizacaoPorAcaoIntegrationTest.java`
- `backend/app-deploy/src/test/java/br/com/gems/app/convencoes/ConvencoesArquiteturaTest.java`
- `backend/app-deploy/src/test/java/br/com/gems/app/convencoes/ConvencoesFonteTest.java`
- `backend/app-deploy/src/test/java/br/com/gems/app/convencoes/ExcecoesEscritas.java`
- `backend/app-deploy/src/test/java/br/com/gems/app/convencoes/ExcecoesEscritasTest.java`
- `backend/app-deploy/src/test/java/br/com/gems/app/convencoes/FontesJava.java`
- `backend/app-deploy/src/test/java/br/com/gems/app/convencoes/InventarioDeExcecoes.java`
- `backend/app-deploy/src/test/java/br/com/gems/app/convencoes/LocalizadorDeFonte.java`
- `backend/app-deploy/src/test/java/br/com/gems/app/convencoes/package-info.java`
- `backend/app-deploy/src/test/java/br/com/gems/app/convencoes/RegrasDeFonte.java`
- `backend/app-deploy/src/test/java/br/com/gems/app/convencoes/RelatorioDeConformidade.java`
- `backend/app-deploy/src/test/java/br/com/gems/app/support/AbstractIntegrationTest.java`
- `backend/app-deploy/src/test/resources/application.yml`
- `backend/app-deploy/src/test/resources/archunit.properties`
- `backend/app-deploy/src/test/resources/archunit-baseline.txt`
- `backend/app-security/pom.xml`
- `backend/app-security/src/main/java/br/com/gems/app/security/AccessDeniedExceptionHandler.java`
- `backend/app-security/src/main/java/br/com/gems/app/security/IdentityProviderExceptionHandler.java`
- `backend/app-security/src/main/java/br/com/gems/app/security/RequisicaoInvalidaExceptionHandler.java`
- `backend/app-security/src/main/java/br/com/gems/app/security/service/KeycloakUserService.java`
- `backend/pom.xml`
- `D:/repos/Meduc/meduc-workspace/.agents/skills/model-delegation/references/package-contract.md`
- `specs/002-conformidade-gems/estado-lote-C03.md`
- `specs/002-conformidade-gems/plan.md`
- `specs/002-conformidade-gems/research.md`
- `specs/002-conformidade-gems/roteiro.md`
- `specs/002-conformidade-gems/spec.md`
- `specs/002-conformidade-gems/tasks.md`
- `specs/AGENTS.md`
### EDIT / CREATE
| # | Ação | Arquivo |
| :-- | :--- | :--- |
| C04 | EDIT | `backend/app-core/src/main/java/br/com/gems/app/auditoria/repository/CampoAlteradoRepository.java` |
| C04 | EDIT | `backend/app-core/src/main/java/br/com/gems/app/localidade/repository/EstadoRepository.java` |
| C04 | EDIT | `backend/app-core/src/main/java/br/com/gems/app/localidade/repository/PaisRepository.java` |
| C04 | EDIT | `backend/app-core/src/main/java/br/com/gems/app/solicitacao/repository/EtapaSolicitacaoRepository.java` |
| C04 | EDIT | `backend/app-core/src/main/java/br/com/gems/app/localidade/controller/LocalidadeController.java` |
| C04 | EDIT | `backend/app-core/src/main/java/br/com/gems/app/normalizacao/controller/CepController.java` |
| C04 | EDIT | `backend/app-core/src/main/java/br/com/gems/app/commons/exception/ProvedorExternoException.java` |
| C04 | EDIT | `backend/app-core/src/main/java/br/com/gems/app/localidade/service/LocalidadeService.java` |
| C04 | EDIT | `backend/app-core/src/main/java/br/com/gems/app/normalizacao/service/ContatoService.java` |
| C04 | EDIT | `backend/app-core/src/main/java/br/com/gems/app/normalizacao/service/EnderecoService.java` |
| C04 | EDIT | `backend/app-core/src/main/java/br/com/gems/app/normalizacao/service/PessoaService.java` |
| C04 | EDIT | `backend/app-core/src/main/java/br/com/gems/app/parceiro/service/ParceiroService.java` |
| C04 | EDIT | `backend/app-core/src/main/java/br/com/gems/app/solicitacao/service/SolicitacaoService.java` |
| C04 | EDIT | `backend/app-security/src/main/java/br/com/gems/app/security/service/KeycloakUserService.java` |
| C04 | EDIT | `backend/app-deploy/src/test/java/br/com/gems/app/autorizacao/AutorizacaoPorAcaoIntegrationTest.java` |
| T054 | DELETE | `backend/app-core/src/main/java/br/com/gems/app/commons/exception/ProvedorExternoExceptionHandler.java` |
| T054 | DELETE | `backend/app-security/src/main/java/br/com/gems/app/security/AccessDeniedExceptionHandler.java` |
| T054 | DELETE | `backend/app-security/src/main/java/br/com/gems/app/security/IdentityProviderExceptionHandler.java` |
| T054 | DELETE | `backend/app-security/src/main/java/br/com/gems/app/security/RequisicaoInvalidaExceptionHandler.java` |
| T054 | CREATE | `backend/app-deploy/src/test/java/br/com/gems/app/security/ErrorEnvelopeIntegrationTest.java` |
| C04 | EDIT | `specs/002-conformidade-gems/tasks.md`, `specs/002-conformidade-gems/roteiro.md` |
## 2. Contrato consumido
Preservar assinaturas/comportamento funcional. Repositories BaseCustomJpaRepository + JpaRepository + @Repository; controllers BaseController; ordem findById/search/delete/insert/save/validate.
T054: ProvedorExternoException herda ExternalServiceException SDK 3.1.0. KeycloakUserService traduz falha Admin API para ExternalServiceException com causa/mensagem. Não migra gateway/ciclo/roles; D-P02 permanece.
Provar MockMvc MVC real: 502 provedores, 403 negação, 404 recurso, 400 tipo inválido; envelope SDK occurrenceTime/errorType/message/path/method. Preservar status e mensagens funcionais 502; não ecoar entrada. Diferenças mensagens SDK 403/404/400 devem ser registradas; não declare equivalência literal silenciosamente. Se precisar decisão nova, pare T054 e reporte.
API publicada pode ser inspecionada por javap nas dependências do grafo; não leia fontes não listadas.
## 3. Referências
ParceiroRepository/ParceiroController para estrutura; AbstractIntegrationTest/AutorizacaoPorAcaoIntegrationTest para integração.
## 4. Regras
Test-first envelope antes de retirar handlers. Não enfraquecer teste para acomodar regressão. Mutar alvo real da rede, vermelho, restaurar por edição inversa e verde.
Não modificar baseline, remover tolerâncias só C06. Spotless apply apenas alvos editados, sem formatar reator. Não renomear testes externos.
## 5. Portão
Em backend, PowerShell: cmd /c "mvnw.cmd -pl app-deploy -am -Dtest=ConvencoesArquiteturaTest,ConvencoesFonteTest,AppApplicationTests,AutorizacaoPorAcaoIntegrationTest,ErrorEnvelopeIntegrationTest -Dsurefire.failIfNoSpecifiedTests=false test"
Exit 0/BUILD SUCCESS, sem falhas/erros/skips. Exigir zero repository/controller/ordem reais por medição nominal mesmo com baseline tolerante. git diff --check verde.
Logs grandes TEMP. Estado §5 porcelain literal menos próprio estado. Gate completo reservado C06/C13.
## 6. Relatório
Feito/não feito, testes/contagens/mutações/divergências. T052–T055 só marcadas cumpridas. Pare após C04.

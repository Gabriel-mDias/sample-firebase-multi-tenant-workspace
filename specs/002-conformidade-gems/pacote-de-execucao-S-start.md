# Pacote de Execução — Rodada 002, lote S-start
Admissão: contrato sim; caminhos sim; escrita disjunta sim; gate documental sim;
economia de delegação mecânica não. Decisão: sem delegação para Terra.
Sol conduz julgamento por decisão 8 da 010, retomando sessão T058.
Preservar sessão reduz onboarding; pacote de implementação após planejamento e portão.
Contrato: D:/repos/Meduc/meduc-workspace/.agents/skills/model-delegation/references/package-contract.md.
Não comite; orquestrador verifica este checkpoint documental.
Antes de editar, crie estado-lote-S-start.md conforme estado-lote-template.md.
## 1. Fronteira fechada do lote
Validar entrada da 002 e registrar apenas §1/§2 do roteiro; parar no fim da fase 1.
### READ
- `.agents/skills/rodada-sdd/SKILL.md`
- `.claude/skills/rodada-sdd/references/fase-1-start.md`
- `.gems-ai/MIGRACAO-2.4.0.md`
- `.gems-ai/rules/backend-java/architecture.md`
- `.gems-ai/rules/backend-java/controllers.md`
- `.gems-ai/rules/backend-java/events.md`
- `.gems-ai/rules/backend-java/mapping.md`
- `.gems-ai/rules/backend-java/multi-tenant.md`
- `.gems-ai/rules/backend-java/observability.md`
- `.gems-ai/rules/backend-java/openapi.md`
- `.gems-ai/rules/backend-java/persistence-db.md`
- `.gems-ai/rules/backend-java/repositories.md`
- `.gems-ai/rules/backend-java/rest-common.md`
- `.gems-ai/rules/backend-java/security.md`
- `.gems-ai/rules/backend-java/services.md`
- `.gems-ai/rules/backend-java/storage-s3.md`
- `.gems-ai/rules/backend-java/testing.md`
- `.gems-ai/rules/backend-java/utils.md`
- `.gems-ai/rules/backend-java/validation.md`
- `.gems-ai/rules/conformance-exceptions.md`
- `.gems-ai/rules/frontend-angular/auth.md`
- `.gems-ai/rules/frontend-angular/feedback-alerts.md`
- `.gems-ai/rules/frontend-angular/forms.md`
- `.gems-ai/rules/frontend-angular/gems-components.md`
- `.gems-ai/rules/frontend-angular/http-interceptors.md`
- `.gems-ai/rules/frontend-angular/lists.md`
- `.gems-ai/rules/frontend-angular/organization.md`
- `.gems-ai/rules/frontend-angular/signals-standards.md`
- `.gems-ai/rules/frontend-angular/stepper.md`
- `.gems-ai/rules/frontend-angular/stores.md`
- `.gems-ai/rules/frontend-angular/testing.md`
- `.gems-ai/rules/frontend-angular/visual-standards.md`
- `.specify/feature.json`
- `.specify/memory/constitution.md`
- `.specify/templates/estado-lote-template.md`
- `.specify/templates/roteiro-template.md`
- `AGENTS.md`
- `backend/AGENTS.md`
- `backend/app-core/pom.xml`
- `backend/app-core/src/main/java/br/com/gems/app/auditoria/controller/AuditoriaController.java`
- `backend/app-core/src/main/java/br/com/gems/app/auditoria/repository/CampoAlteradoRepository.java`
- `backend/app-core/src/main/java/br/com/gems/app/auditoria/repository/RegistroAuditoriaRepository.java`
- `backend/app-core/src/main/java/br/com/gems/app/auditoria/service/AuditoriaService.java`
- `backend/app-core/src/main/java/br/com/gems/app/autorizacao/AutorizacaoBootstrap.java`
- `backend/app-core/src/main/java/br/com/gems/app/autorizacao/MapaPerfilAcoes.java`
- `backend/app-core/src/main/java/br/com/gems/app/localidade/controller/LocalidadeController.java`
- `backend/app-core/src/main/java/br/com/gems/app/localidade/repository/EstadoRepository.java`
- `backend/app-core/src/main/java/br/com/gems/app/localidade/repository/PaisRepository.java`
- `backend/app-core/src/main/java/br/com/gems/app/localidade/service/LocalidadeService.java`
- `backend/app-core/src/main/java/br/com/gems/app/normalizacao/controller/CepController.java`
- `backend/app-core/src/main/java/br/com/gems/app/normalizacao/repository/ContatoRepository.java`
- `backend/app-core/src/main/java/br/com/gems/app/normalizacao/repository/EnderecoRepository.java`
- `backend/app-core/src/main/java/br/com/gems/app/normalizacao/repository/PessoaRepository.java`
- `backend/app-core/src/main/java/br/com/gems/app/normalizacao/service/CepService.java`
- `backend/app-core/src/main/java/br/com/gems/app/normalizacao/service/ContatoService.java`
- `backend/app-core/src/main/java/br/com/gems/app/normalizacao/service/EnderecoService.java`
- `backend/app-core/src/main/java/br/com/gems/app/normalizacao/service/PessoaService.java`
- `backend/app-core/src/main/java/br/com/gems/app/parceiro/controller/ParceiroController.java`
- `backend/app-core/src/main/java/br/com/gems/app/parceiro/repository/ParceiroRepository.java`
- `backend/app-core/src/main/java/br/com/gems/app/parceiro/service/ParceiroService.java`
- `backend/app-core/src/main/java/br/com/gems/app/solicitacao/controller/SolicitacaoController.java`
- `backend/app-core/src/main/java/br/com/gems/app/solicitacao/repository/EtapaSolicitacaoRepository.java`
- `backend/app-core/src/main/java/br/com/gems/app/solicitacao/repository/SolicitacaoRepository.java`
- `backend/app-core/src/main/java/br/com/gems/app/solicitacao/service/SolicitacaoService.java`
- `backend/app-core/src/main/java/br/com/gems/app/usuario/repository/UsuarioRepository.java`
- `backend/app-core/src/main/java/br/com/gems/app/usuario/service/UsuarioService.java`
- `backend/app-security/pom.xml`
- `backend/app-security/src/main/java/br/com/gems/app/security/config/KeycloakConfig.java`
- `backend/app-security/src/main/java/br/com/gems/app/security/config/SecurityConfig.java`
- `backend/app-security/src/main/java/br/com/gems/app/security/service/KeycloakGroupService.java`
- `backend/app-security/src/main/java/br/com/gems/app/security/service/KeycloakRoleService.java`
- `backend/app-security/src/main/java/br/com/gems/app/security/service/KeycloakUserService.java`
- `backend/app-security/src/main/java/br/com/gems/app/security/service/UsuarioAutenticadoService.java`
- `backend/pom.xml`
- `CODEX.md`
- `D:/repos/Meduc/meduc-workspace/.agents/skills/model-delegation/references/package-contract.md`
- `frontend/AGENTS.md`
- `frontend/package.json`
- `frontend/src/app/app.config.ts`
- `frontend/src/app/app.routes.ts`
- `specs/002-conformidade-gems/levantamento.md`
- `specs/002-conformidade-gems/medicao-gates.md`
- `specs/AGENTS.md`

### EDIT / CREATE
| # | Ação | Arquivo |
| :-- | :--- | :--- |
| T060-start | CREATE | `specs/002-conformidade-gems/roteiro.md` |
## 2. O que já existe e você consome
Levantamento contém decisões de 2026-09-13 e reversão de sequência em 2026-09-14.
S-def entregue; medicao-gates.md é o estado real dos gates.
SDK Java 3.1.0 e Angular 2.1.0 publicadas (evidências da 010); nenhuma adoção agora.
Preservar parceiro, solicitação, localidade, auditoria e upload.
Roadmap OK-2 pelo Codex ao fim; ADACI não é alvo.
Assets S-def foram contribuição aprovada do orquestrador; explicitar em OK-1.
## 3. Arquivos de referência
Use roteiro-template.md, apenas §1/§2 preenchidas; resto é fase 2.
## 4. As regras que valem para este lote
1. Aplique rodada-sdd com argumento 002-conformidade-gems, apenas fase 1.
2. Não escreva spec, plan, tasks ou código; não assine portão.
3. Confira levantamento contra READ; histórico não representa definições atuais.
4. Não repita perguntas já decididas; reporte ambiguidades novas com recomendação.
5. Faltou fonte READ, pare e nomeie caminho necessário.
6. Não edite definições, backend, frontend, .codex, outras rodadas.
7. Sem gate de código por alteração documental; sem commit/push/PR/merge.
## 5. O portão
Da raiz: git diff --check
Exit 0; somente §1/§2 preenchidas; sem portão; decisões rastreáveis.
Orquestrador executará verificar-lote.ps1 com pacote, estado e âncora.
## 6. O relatório
Arquivos; decisões; ambiguidades; divergências; fontes insuficientes.
Gate com exit; §5 do estado com porcelain literal menos próprio estado.
Pare: fase 2 só após usuário pedir para seguir.

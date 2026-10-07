# Pacote de Execução — Rodada 002, Lote C03
| Controle | Valor |
| :--- | :--- |
| Checkpoint | C03 |
| Pré-requisitos | C02 verde |
| Política de sessão | nova |
| SHA anterior | 5a4df13 |
| Estado inicial | aberto |
| Gate focado | backend: comando da §5 |
| Admissão 1–5 | sim: contrato congelado; sim: caminhos nominais; sim: árvore limpa/disjunta; sim: T050; sim: dois fluxos integrados e mutações amortizam pacote/revisão |
| Modelo | gpt-5.6-sol, sem Terra conforme decisão 1 aprovada; risco de falso rollback silencioso |
| Custo | fixtures/controles/duas mutações em dois fluxos superam pacote fechado + onboarding + revisão + repetição do gate |
| Âncora | C:/Users/gabri/AppData/Local/Temp/sample-c03-pre-dispatch-20260916.porcelain |

> READ fechado; contexto ausente exige parada, não exploração. Você não comita, não faz push nem PR.
> Crie estado-lote-C03.md antes da primeira edição, do template listado, com contrato Codex listado.
> Política nova: nenhum token anterior. Use apply_patch; não toque outro checkpoint.

## 1. Fronteira fechada do lote

T046–T051: rollback de negócio em PostgreSQL/proxy, mutações reais e retirada do ModelMapper/pin.
Não adota auditoria/Keycloak SDK, não remove ratchet/baselines, não altera ambiente/frontend.

### READ

- `specs/002-conformidade-gems/pacote-de-execucao-C03.md`
- `D:/repos/Meduc/meduc-workspace/.agents/skills/model-delegation/references/package-contract.md`
- `.specify/templates/estado-lote-template.md`
- `.specify/scripts/powershell/verificar-lote.ps1`
- `AGENTS.md`
- `backend/AGENTS.md`
- `specs/AGENTS.md`
- `.gems-ai/rules/backend-java/testing.md`
- `.gems-ai/rules/backend-java/services.md`
- `.gems-ai/rules/backend-java/mapping.md`
- `specs/002-conformidade-gems/spec.md`
- `specs/002-conformidade-gems/tasks.md`
- `specs/002-conformidade-gems/roteiro.md`
- `specs/002-conformidade-gems/research.md`
- `backend/pom.xml`
- `backend/app-core/pom.xml`
- `backend/app-deploy/pom.xml`
- `backend/app-deploy/src/test/java/br/com/gems/app/support/AbstractIntegrationTest.java`
- `backend/app-deploy/src/test/java/br/com/gems/app/auditoria/AuditoriaInterceptorIntegrationTest.java`
- `backend/app-deploy/src/test/resources/application.yml`
- `backend/app-core/src/main/java/br/com/gems/app/localidade/service/LocalidadeService.java`
- `backend/app-core/src/main/java/br/com/gems/app/normalizacao/service/ContatoService.java`
- `backend/app-core/src/main/java/br/com/gems/app/normalizacao/service/EnderecoService.java`
- `backend/app-core/src/main/java/br/com/gems/app/normalizacao/service/PessoaService.java`
- `backend/app-core/src/main/java/br/com/gems/app/parceiro/service/ParceiroService.java`
- `backend/app-core/src/main/java/br/com/gems/app/solicitacao/service/SolicitacaoService.java`
- `backend/app-core/src/main/java/br/com/gems/app/parceiro/mapper/ParceiroMapper.java`
- `backend/app-core/src/main/java/br/com/gems/app/solicitacao/mapper/SolicitacaoMapper.java`
- `backend/app-core/src/main/java/br/com/gems/app/parceiro/listener/SolicitacaoConcluidaListener.java`
- `backend/app-core/src/main/java/br/com/gems/app/parceiro/repository/ParceiroRepository.java`
- `backend/app-core/src/main/java/br/com/gems/app/solicitacao/repository/SolicitacaoRepository.java`
- `backend/app-core/src/main/java/br/com/gems/app/solicitacao/repository/EtapaSolicitacaoRepository.java`
- `backend/app-core/src/main/java/br/com/gems/app/auditoria/interceptor/AuditoriaInterceptor.java`
- `backend/app-core/src/main/java/br/com/gems/app/auditoria/interceptor/AuditoriaGravador.java`
- `backend/app-core/src/main/java/br/com/gems/app/auditoria/interceptor/RegistroPendente.java`
- `backend/app-model/src/main/java/br/com/gems/app/model/entity/parceiro/Parceiro.java`
- `backend/app-model/src/main/java/br/com/gems/app/model/entity/solicitacao/Solicitacao.java`
- `backend/app-model/src/main/java/br/com/gems/app/model/entity/solicitacao/EtapaSolicitacao.java`
- `backend/app-model/src/main/java/br/com/gems/app/model/entity/normalizacao/Pessoa.java`
- `backend/app-model/src/main/java/br/com/gems/app/model/entity/normalizacao/Contato.java`
- `backend/app-model/src/main/java/br/com/gems/app/model/entity/normalizacao/Endereco.java`
- `backend/app-model/src/main/java/br/com/gems/app/model/entity/localidades/Pais.java`
- `backend/app-model/src/main/java/br/com/gems/app/model/entity/localidades/Estado.java`
- `backend/app-model/src/main/java/br/com/gems/app/model/entity/base/EntidadeAuditavel.java`
- `backend/app-model/src/main/java/br/com/gems/app/model/entity/auditoria/RegistroAuditoria.java`
- `backend/app-model/src/main/java/br/com/gems/app/model/entity/auditoria/CampoAlterado.java`
- `backend/app-model/src/main/java/br/com/gems/app/model/dto/ParceiroDTO.java`
- `backend/app-model/src/main/java/br/com/gems/app/model/dto/PessoaDTO.java`
- `backend/app-model/src/main/java/br/com/gems/app/model/dto/ContatoDTO.java`
- `backend/app-model/src/main/java/br/com/gems/app/model/dto/EnderecoDTO.java`
- `backend/app-model/src/main/java/br/com/gems/app/model/dto/SolicitacaoDTO.java`
- `backend/app-model/src/main/java/br/com/gems/app/model/enums/SituacaoSolicitacao.java`
- `backend/app-events/src/main/java/br/com/gems/app/solicitacao/event/SolicitacaoConcluidaEvent.java`

### EDIT / CREATE

| # | Ação | Arquivo |
| :-- | :--- | :--- |
| T046 | CREATE | `backend/app-deploy/src/test/java/br/com/gems/app/parceiro/ParceiroRollbackIntegrationTest.java` |
| T047 | CREATE | `backend/app-deploy/src/test/java/br/com/gems/app/solicitacao/SolicitacaoRollbackIntegrationTest.java` |
| T048 | EDIT | `backend/app-core/src/main/java/br/com/gems/app/parceiro/service/ParceiroService.java` — somente mutação temporária/restauração rollbackFor |
| T049 | EDIT | `backend/app-core/src/main/java/br/com/gems/app/solicitacao/service/SolicitacaoService.java` — somente mutação temporária/restauração rollbackFor |
| T051 | EDIT | `backend/app-core/pom.xml` — retirar gems-model-mapper |
| T051 | EDIT | `backend/pom.xml` — retirar pin/propriedade ModelMapper e gerenciamento gems-model-mapper órfão |
| T046–T051 | EDIT | `specs/002-conformidade-gems/tasks.md`, `specs/002-conformidade-gems/roteiro.md` — progresso/evidências apenas |

## 2. O que já existe e você consome

Seis services já usam MapStruct. Parceiro.insert grava Pessoa/Contato/Endereco e raiz sem cascade.
Solicitacao.insert cria RASCUNHO; concluir altera estado/data e publica SolicitacaoConcluidaEvent.
Listener real BEFORE_COMMIT incrementa solicitacoesConcluidas; evento é em memória, não tabela.
Auditoria local escreve trilha no commit da mesma transação; preservá-la até C12.
Teste chama service real proxied; não usa @Transactional no teste/classe nem TransactionTemplate envolvendo chamada.
Pode usar @MockitoSpyBean somente no mapper colaborador com doAnswer: mapeamento real, flush, captura de ids, exceção checked.
Não use exceção runtime no ponto injetado, erro de constraint ou outro service transacional que marque rollbackOnly.
A exceção checked pode ser encapsulada pelo proxy externo: assertar causa/tipo e mensagem, não só qualquer exceção.
Para insert: raízes e filhos novos, flush comprovado e ids capturados antes da falha; depois leitura independente/SQL físico por ids, inclusive soft delete e trilha.
Para solicitação: caso insert com etapa criada no ponto de falha (explicitamente fixture/hook), e caso concluir com fixture EM_ANDAMENTO/etapa concluída: após falha estado/data/etapa/contador e trilha iguais ao baseline; não exigir ausência da fixture preexistente.
Inclua controles positivos sem falha para provar que inserts, trilha e evento/contador funcionam e que asserções não são vacuamente verdes.
Fixtures e limpeza nominais/por ids ou correlação própria antes/depois, nunca truncate/limpeza global. Não crie tabela/esquema novo.

## 3. Arquivos de referência — copie destes

| Para escrever | Copie de |
| :--- | :--- |
| integração PostgreSQL | AbstractIntegrationTest.java e AuditoriaInterceptorIntegrationTest.java listados |
| payload/fixtures | DTOs/entidades/services nominais de READ |
| estado | template listado, substituir link legado pelo contrato Codex listado |

## 4. As regras que valem para este lote

1. AAA, português e nomes deveAcao_quandoCondicao; imports explícitos.
2. ⚠️ Nenhuma transação de teste envolvendo chamada service: nova leitura após exceção.
3. ⚠️ Flush e ids capturados antes da checked; SQL físico não esconde soft delete.
4. ⚠️ Só mapper colaborador recebe injeção de falha; service alvo e repositories reais.
5. Cada mutação remove rollbackFor do método real exercitado, exige falha por persistência indevida, restaura por edição inversa e repete verde.
6. T048/T049 podem ser executadas pelo executor; orquestrador repetirá independentemente.
7. Nenhum skip, baseline/supressão/ratchet enfraquecido; sem alteração permanente em services.
8. Remova só dependências/pin órfãos; versão GEMS/MapStruct/processadores ficam intactos.
9. Gate focado neste checkpoint; gates completos reservados C06/C13; nenhum frontend tocado.

## 5. O portão

Em backend, PowerShell:
```powershell
cmd /c "mvnw.cmd -pl app-deploy -am -Dtest=LocalidadeServiceTest,ContatoServiceTest,EnderecoServiceTest,PessoaServiceTest,ParceiroServiceTest,SolicitacaoServiceTest,ParceiroRollbackIntegrationTest,SolicitacaoRollbackIntegrationTest -Dsurefire.failIfNoSpecifiedTests=false test"
```
Exit 0, BUILD SUCCESS, seis testes unitários (70 casos) e ambos novos integrados executados, zero falhas/erros/skips.
Executar antes/depois remoção T051; repetir verde após cada restauração. Spotless apply apenas nos dois novos testes se necessário.
Busca nominal em ambos POMs e seis services: zero ModelMapper/modelmapper/gems-model-mapper.
dependency:tree filtrada ModelMapper após remoção sem artefatos no grafo; git diff --check exit 0.
Não leia diretórios proibidos; relatórios do gate apenas se falha. Logs grandes podem ir para TEMP, não repo.

## 6. O relatório

Feito/não feito, gates reais/contagens, mutações com método/falha persistida/restauração e divergências.
Atualize §1–§7 de estado-lote-C03.md nas fronteiras de tarefa; §5 contém porcelain literal menos próprio estado.
Nenhum commit/push/PR. Se insuficiente, pare e deixe ponto exato de recuperação.

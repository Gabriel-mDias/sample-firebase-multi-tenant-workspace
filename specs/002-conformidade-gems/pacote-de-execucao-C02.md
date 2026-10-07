# Pacote de Execução — Rodada 002, checkpoint C02 (T037–T045)

| Controle | Valor |
| :--- | :--- |
| Checkpoint | `C02` |
| Pré-requisitos | `C01 verde em 59112fe` |
| Política de sessão | `nova` |
| SHA anterior | `59112fe` |
| Estado inicial | `em recuperação após gate final vermelho por FQCN ausente no pacote v1` |
| Gate focado | `seis testes unitários de service no reator app-core -am` |

> **Admissão:** contrato congelado: sim; caminhos conhecidos: sim; escritas disjuntas: sim; gate
> focado: sim; economia maior que autoria/revisão: sim. **Modelo: `gpt-5.6-sol`; sem Terra**, por
> decisão 1 e §8 do roteiro aprovado. São seis mappers, seis services e seis testes repetitivos; o
> pacote, a revisão nominal e uma repetição do gate custam menos que executar a migração inteira no
> orquestrador. O executor não comita, não faz push e não abre PR.

> **A lista `READ` é fechada.** Se faltar contexto, surgir caminho fora de `EDIT / CREATE`, houver
> conflito de contrato ou a árvore diferir da âncora, pare e registre no estado. Não pesquise nem
> amplie o lote. Retome o `estado-lote-C02.md` existente e atualize-o antes da primeira edição.

## 1. Fronteira fechada do lote

Concluir T037–T045: primeiro adaptar/criar os seis testes para injetar mappers e provar vermelho;
depois criar seis mappers MapStruct estritos e trocar apenas os seis services. T046–T051, testes de
rollback reais, remoção do ModelMapper/POM e qualquer outra conformação Java ficam fora deste lote.

### READ

- `specs/002-conformidade-gems/pacote-de-execucao-C02.md` — contrato deste checkpoint
- `specs/002-conformidade-gems/estado-lote-C02.md` — estado da tentativa anterior e ponto de recuperação
- `.specify/templates/estado-lote-template.md` e `.specify/scripts/powershell/verificar-lote.ps1` — estado e verificação
- `D:/repos/Meduc/meduc-workspace/.agents/skills/model-delegation/references/package-contract.md` — protocolo do lote
- `AGENTS.md`, `backend/AGENTS.md` e `specs/AGENTS.md` — regras locais
- `.gems-ai/rules/backend-java/mapping.md`, `.gems-ai/rules/backend-java/services.md` e `.gems-ai/rules/backend-java/testing.md` — regras tocadas
- `specs/002-conformidade-gems/tasks.md`, `specs/002-conformidade-gems/roteiro.md` e `specs/002-conformidade-gems/research.md` — escopo aprovado e R-02
- `backend/pom.xml` e `backend/app-core/pom.xml` — MapStruct/GEMS já configurados; somente leitura
- `backend/app-deploy/src/test/java/br/com/gems/app/convencoes/ConvencoesArquiteturaTest.java` — prova local do FQCN exigido
- `backend/app-core/src/main/java/br/com/gems/app/localidade/service/LocalidadeService.java`
- `backend/app-core/src/main/java/br/com/gems/app/normalizacao/service/ContatoService.java`
- `backend/app-core/src/main/java/br/com/gems/app/normalizacao/service/EnderecoService.java`
- `backend/app-core/src/main/java/br/com/gems/app/normalizacao/service/PessoaService.java`
- `backend/app-core/src/main/java/br/com/gems/app/parceiro/service/ParceiroService.java`
- `backend/app-core/src/main/java/br/com/gems/app/solicitacao/service/SolicitacaoService.java`
- `backend/app-core/src/test/java/br/com/gems/app/normalizacao/service/ContatoServiceTest.java`
- `backend/app-core/src/test/java/br/com/gems/app/normalizacao/service/EnderecoServiceTest.java`
- `backend/app-core/src/test/java/br/com/gems/app/normalizacao/service/PessoaServiceTest.java`
- `backend/app-core/src/test/java/br/com/gems/app/parceiro/service/ParceiroServiceTest.java`
- `backend/app-core/src/test/java/br/com/gems/app/solicitacao/service/SolicitacaoServiceTest.java`
- `backend/app-model/src/main/java/br/com/gems/app/model/entity/base/EntidadeAuditavel.java` — campos preservados em update
- `backend/app-model/src/main/java/br/com/gems/app/model/dto/PaisDTO.java` e `backend/app-model/src/main/java/br/com/gems/app/model/entity/localidades/Pais.java`
- `backend/app-model/src/main/java/br/com/gems/app/model/dto/EstadoDTO.java` e `backend/app-model/src/main/java/br/com/gems/app/model/entity/localidades/Estado.java`
- `backend/app-model/src/main/java/br/com/gems/app/model/dto/ContatoDTO.java` e `backend/app-model/src/main/java/br/com/gems/app/model/entity/normalizacao/Contato.java`
- `backend/app-model/src/main/java/br/com/gems/app/model/dto/EnderecoDTO.java` e `backend/app-model/src/main/java/br/com/gems/app/model/entity/normalizacao/Endereco.java`
- `backend/app-model/src/main/java/br/com/gems/app/model/dto/PessoaDTO.java` e `backend/app-model/src/main/java/br/com/gems/app/model/entity/normalizacao/Pessoa.java`
- `backend/app-model/src/main/java/br/com/gems/app/model/dto/ParceiroDTO.java` e `backend/app-model/src/main/java/br/com/gems/app/model/entity/parceiro/Parceiro.java`
- `backend/app-model/src/main/java/br/com/gems/app/model/dto/SolicitacaoDTO.java` e `backend/app-model/src/main/java/br/com/gems/app/model/entity/solicitacao/Solicitacao.java`
- `backend/app-model/src/main/java/br/com/gems/app/model/dto/EtapaSolicitacaoDTO.java` e `backend/app-model/src/main/java/br/com/gems/app/model/entity/solicitacao/EtapaSolicitacao.java`

**Nada fora de READ pode ser pesquisado ou lido.** Precisou de outro arquivo, pare e reporte.

### EDIT / CREATE

| # | Ação | Arquivo |
| :-- | :--- | :--- |
| T037 | CREATE | `backend/app-core/src/test/java/br/com/gems/app/localidade/service/LocalidadeServiceTest.java` |
| T037 | EDIT | `backend/app-core/src/test/java/br/com/gems/app/normalizacao/service/ContatoServiceTest.java` `backend/app-core/src/test/java/br/com/gems/app/normalizacao/service/EnderecoServiceTest.java` `backend/app-core/src/test/java/br/com/gems/app/normalizacao/service/PessoaServiceTest.java` `backend/app-core/src/test/java/br/com/gems/app/parceiro/service/ParceiroServiceTest.java` `backend/app-core/src/test/java/br/com/gems/app/solicitacao/service/SolicitacaoServiceTest.java` |
| T038–T043 | CREATE | `backend/app-core/src/main/java/br/com/gems/app/localidade/mapper/LocalidadeMapper.java` `backend/app-core/src/main/java/br/com/gems/app/normalizacao/mapper/ContatoMapper.java` `backend/app-core/src/main/java/br/com/gems/app/normalizacao/mapper/EnderecoMapper.java` `backend/app-core/src/main/java/br/com/gems/app/normalizacao/mapper/PessoaMapper.java` `backend/app-core/src/main/java/br/com/gems/app/parceiro/mapper/ParceiroMapper.java` `backend/app-core/src/main/java/br/com/gems/app/solicitacao/mapper/SolicitacaoMapper.java` |
| T044–T045 | EDIT | `backend/app-core/src/main/java/br/com/gems/app/localidade/service/LocalidadeService.java` `backend/app-core/src/main/java/br/com/gems/app/normalizacao/service/ContatoService.java` `backend/app-core/src/main/java/br/com/gems/app/normalizacao/service/EnderecoService.java` `backend/app-core/src/main/java/br/com/gems/app/normalizacao/service/PessoaService.java` `backend/app-core/src/main/java/br/com/gems/app/parceiro/service/ParceiroService.java` `backend/app-core/src/main/java/br/com/gems/app/solicitacao/service/SolicitacaoService.java` |
| C02 | EDIT | `specs/002-conformidade-gems/tasks.md` `specs/002-conformidade-gems/roteiro.md` |

O estado `specs/002-conformidade-gems/estado-lote-C02.md` é a exceção protocolar permitida.

## 2. Contrato congelado

Todos os mappers importam **`br.com.gems.mapstruct.GemsMappingConfig`** e usam
`@Mapper(config = GemsMappingConfig.class)`. A API publicada 3.1.0 foi conferida pelo orquestrador
com `jar tf`/`javap`; a classe é `br.com.gems.mapstruct.GemsMappingConfig`. `LocalidadeMapper` expõe país→DTO
e estado→DTO sem o pai no retorno de estados. Contato, endereço e pessoa expõem `toDTO` e update com
`@MappingTarget`, preservando id/auditoria; endereço também preserva país/estado gerenciados.
`ParceiroMapper.toDTO` preserva os DTOs aninhados e a ocultação de comissão continua no service.
`SolicitacaoMapper.toDTO` declara `parceiro.id → parceiroId` e `parceiro.nome → parceiroNome` e mapeia
etapas. Nenhum payload, validação, evento, ordem de persistência ou status observável muda.

## 3. Regras deste lote

1. Test-first: ajuste/crie os seis testes, rode o gate e registre o vermelho antes de criar mappers.
2. Mapper é o primeiro campo do service; repository próprio vem depois; use `@RequiredArgsConstructor`.
3. Remova `ModelMapperUtils`, `ModelMapper` e `configurarMapeamentos` somente dos seis services/testes.
4. `@Mapping(ignore = true)` exige comentário local com a garantia preservada; não cale o compilador.
5. T045 troca somente Parceiro/Solicitação para `org.springframework.transaction.annotation.Transactional` com `rollbackFor = Exception.class`; não escreva testes de rollback deste lote.
6. Preserve identidade da entidade recebida, referências gerenciadas, normalização de CPF, comissão sigilosa e relações parceiro/etapas.
7. Não edite POMs nem remova a dependência ModelMapper; isso pertence a C03/T051.
8. Não faça commit, push, PR, operação destrutiva ou leitura fora de READ.

## 4. Portão

A partir de `backend/`:

```powershell
.\mvnw.cmd -pl app-core -am -Dtest=LocalidadeServiceTest,ContatoServiceTest,EnderecoServiceTest,PessoaServiceTest,ParceiroServiceTest,SolicitacaoServiceTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Verde: exit 0, os seis testes selecionados executados e nenhum erro de MapStruct. Execute também
`git diff --check` na raiz, exit 0. Registre separadamente o vermelho test-first e o verde final.

## 5. Relatório

Informe arquivos/tarefas, vermelho inicial, gate final com exit e contagens, o que ficou para C03,
e qualquer divergência pacote×código. Atualize o estado a cada tarefa. Pare após C02.

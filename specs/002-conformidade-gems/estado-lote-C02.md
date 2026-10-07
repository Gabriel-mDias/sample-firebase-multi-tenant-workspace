# Estado — Lote C02, rodada 002

## 1. Rollback deste lote

Restaurar somente os arquivos rastreados deste lote com `git restore --` seguido dos caminhos nominais de services, testes, `tasks.md` e `roteiro.md` listados no pacote.
Remover somente os arquivos novos `LocalidadeServiceTest.java`, `LocalidadeMapper.java`, `ContatoMapper.java`, `EnderecoMapper.java`, `PessoaMapper.java`, `ParceiroMapper.java`, `SolicitacaoMapper.java` e este `estado-lote-C02.md`, cada um por caminho literal; preservar o pacote C02 já presente na âncora.

## 2. Concluído

| Tarefa | O que provou |
| :-- | :--- |
| T037 | Gate focado vermelho, exit 1 em 2026-09-15: `testCompile` reportou 12 erros, dois por mapper ausente (`LocalidadeMapper`, `ContatoMapper`, `EnderecoMapper`, `PessoaMapper`, `ParceiroMapper`, `SolicitacaoMapper`). Os três Javadocs residuais de normalização também deixaram de mencionar `ModelMapper`. |
| T038–T043 | Seis mappers MapStruct estritos compilados com `br.com.gems.mapstruct.GemsMappingConfig`; gate focado final exit 0. |
| T044 | Localidade, Contato, Endereço e Pessoa usam mapper como primeiro campo; testes selecionados verdes. |
| T045 | Parceiro e Solicitação usam mapper e transação Spring com `rollbackFor = Exception.class`; gate focado final executou 70 testes, 0 falhas, 0 erros e 0 ignorados. |
| C02 | `git diff --check` exit 0; tarefas e roteiro atualizados após o gate verde. Verificador exit 0: 20 esperados, 2 declarados e 2 reais após desconto da âncora; 18 itens apareceram como `NAO ENTREGUE` porque já estavam presentes na âncora de recuperação, sem `FORA DA LISTA`, `NAO DECLARADO` ou `DECLARADO MAS AUSENTE`. |

## 3. Em curso

- **Tarefa:** nenhuma; checkpoint C02 fechado e aguardando aceitação do orquestrador.
- **Arquivo:** nenhum arquivo aberto.
- **O que já está feito nele:** T037–T045 concluídas e registradas, inclusive a remoção das referências textuais residuais a `ModelMapper` nos testes.
- **O que falta:** nada dentro de C02; T046–T051 pertencem ao próximo checkpoint.

## 4. Não iniciado

Nenhuma tarefa do pacote C02 permaneceu sem início.

## 5. Arquivos tocados

`git status --porcelain --untracked-files=all`

```text
 M backend/app-core/src/main/java/br/com/gems/app/localidade/service/LocalidadeService.java
 M backend/app-core/src/main/java/br/com/gems/app/normalizacao/service/ContatoService.java
 M backend/app-core/src/main/java/br/com/gems/app/normalizacao/service/EnderecoService.java
 M backend/app-core/src/main/java/br/com/gems/app/normalizacao/service/PessoaService.java
 M backend/app-core/src/main/java/br/com/gems/app/parceiro/service/ParceiroService.java
 M backend/app-core/src/main/java/br/com/gems/app/solicitacao/service/SolicitacaoService.java
 M backend/app-core/src/test/java/br/com/gems/app/normalizacao/service/ContatoServiceTest.java
 M backend/app-core/src/test/java/br/com/gems/app/normalizacao/service/EnderecoServiceTest.java
 M backend/app-core/src/test/java/br/com/gems/app/normalizacao/service/PessoaServiceTest.java
 M backend/app-core/src/test/java/br/com/gems/app/parceiro/service/ParceiroServiceTest.java
 M backend/app-core/src/test/java/br/com/gems/app/solicitacao/service/SolicitacaoServiceTest.java
 M specs/002-conformidade-gems/roteiro.md
 M specs/002-conformidade-gems/tasks.md
?? backend/app-core/src/main/java/br/com/gems/app/localidade/mapper/LocalidadeMapper.java
?? backend/app-core/src/main/java/br/com/gems/app/normalizacao/mapper/ContatoMapper.java
?? backend/app-core/src/main/java/br/com/gems/app/normalizacao/mapper/EnderecoMapper.java
?? backend/app-core/src/main/java/br/com/gems/app/normalizacao/mapper/PessoaMapper.java
?? backend/app-core/src/main/java/br/com/gems/app/parceiro/mapper/ParceiroMapper.java
?? backend/app-core/src/main/java/br/com/gems/app/solicitacao/mapper/SolicitacaoMapper.java
?? backend/app-core/src/test/java/br/com/gems/app/localidade/service/LocalidadeServiceTest.java
?? specs/002-conformidade-gems/pacote-de-execucao-C02.md
```

## 6. Decisões fora do pacote

Nenhuma decisão fora do pacote. A lista READ não foi ampliada para descobrir o pacote Java de `GemsMappingConfig`.

## 7. O que está quebrado agora

Nada — o gate focado final ficou verde (exit 0; 70 testes, 0 falhas, 0 erros e 0 ignorados) e `git diff --check` terminou com exit 0. T046–T051 continuam deliberadamente fora deste checkpoint.

## 8. Retomada

- **Executor:** Codex
- **Token de retomada:** não exposto ao executor; política do pacote é sessão nova.

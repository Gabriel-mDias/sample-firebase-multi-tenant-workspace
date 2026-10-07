# Research: Parceiro e Solicitação

Por que cada decisão técnica, e o que foi rejeitado. Uma decisão sem alternativa registrada é uma
decisão que ninguém vai conseguir revisar depois.

---

## R1 — A situação da solicitação não entra pelo corpo da requisição

**Decisão.** Cada transição é um endpoint de ação (`POST /{id}/abrir`, `/iniciar`, `/concluir`,
`/cancelar`), com a sua própria ação de `AcaoSistema`. O `PUT` de cadastro ignora o campo.

**Alternativas rejeitadas:**

| Alternativa | Por que não |
| :--- | :--- |
| Situação como campo do `PUT`, validando a transição no service | A autorização passaria a ser "quem edita, muda estado". Quem pudesse corrigir um título poderia concluir. A máquina de estados continuaria correta, e a **permissão** estaria errada — sem que nada acusasse |
| Um endpoint único `POST /{id}/transicao` com o destino no corpo | Uma ação só para todas as transições, e voltamos ao problema acima com mais passos |
| Campo editável só para certos perfis, checado no service | É a autorização de campo, e ela serve para **valor**, não para **transição**. Um estado é um ato, não um dado |

**Evidência.** O teste `SolicitacaoServiceTest.putIgnoraSituacao` envia um `PUT` declarando
`CONCLUIDA` sobre um rascunho e assere que a situação **não** mudou. Sem a decisão, ele passaria.

---

## R2 — A comunicação entre `solicitacao` e `parceiro` é por evento

**Decisão.** Concluir publica `SolicitacaoConcluidaEvent`; um listener em `parceiro.listener`
incrementa o contador.

**Alternativas rejeitadas:**

| Alternativa | Por que não |
| :--- | :--- |
| `SolicitacaoService` chamar `ParceiroService.incrementar()` | Escrever no dado de outro domínio. O Princípio I proíbe, e o motivo prático é que o grafo de chamadas vira teia em três rodadas |
| Contador calculado por `COUNT` na consulta | Correto e mais simples — mas paga o `COUNT` em toda listagem, e a listagem é a tela mais aberta. Fica registrado como o caminho se o contador der problema |
| Listener em `app-events` | **Não compila**: ele precisaria de `ParceiroRepository`, e o reator recusaria o ciclo `events → core` |

**`BEFORE_COMMIT`, e não `AFTER_COMMIT`.** Com `AFTER_COMMIT` haveria uma janela em que a solicitação
está concluída e o contador não subiu; uma falha nessa janela deixaria os dois divergentes sem nada
que reconciliasse. Dentro da transação, ou entram os dois ou nenhum.

**O que isso custa.** A entrega é em memória. Uma queda entre publicação e listener perde o
incremento. Aceito porque o número é recalculável a partir das solicitações — e **declarado** no
roteiro, §7.

---

## R3 — O sigilo da comissão vive no service, nos três caminhos

**Decisão.** `ParceiroService` consulta `UsuarioAutenticadoService.possuiAcao(VISUALIZAR_COMISSAO)`
em `toDTO`, em `search` e em `aplicar`.

**Por que os três.** Cada um falha sozinho, e nenhum acusa a falta do outro:

- só `toDTO` → o valor vaza pela **listagem**, que é a tela que mais gente abre;
- sem `aplicar` → o payload de quem não vê o campo (que o recebeu `null`) **apaga** o valor gravado
  no primeiro salvamento. A autorização de leitura vira autorização de escrita ao contrário;
- só na UI → a API entrega o número a quem chamar direto. Isso é defeito, não estilo.

**A quarta porta é a trilha de auditoria**, e ela se fecha com `@CampoSigiloso` na entidade: o
interceptador registra **que** o campo mudou e não grava os valores. Fechar a porta da frente e
deixar a lateral aberta é pior que não fechar nenhuma, porque dá impressão de proteção.

---

## R4 — As transições válidas vivem no enum, não em `if` no service

**Decisão.** `SituacaoSolicitacao` carrega o conjunto de destinos possíveis de cada valor, e expõe
`permiteTransicaoPara`. O service tem **um** ponto que altera a situação.

**Alternativa rejeitada.** Um `switch` no service por transição: funciona, mas espalha a regra por
quatro métodos, e acrescentar uma transição passa a exigir tocar em todos. No enum, é uma linha.

**Alternativa rejeitada.** `CHECK` constraint no banco: ela sabe quais **valores** existem, não quais
**transições** partem de qual origem. A pergunta que importa é a segunda.

---

## R5 — Unicidade do nome do parceiro fora do banco

**Decisão.** Sem `UNIQUE` em `NM_PARCEIRO`; a checagem é `existsByNomeIgnoreCase` no service, e o
`@SQLRestriction` da entidade já a limita aos ativos.

**Por quê.** A regra é "único **entre os ativos**". Com soft delete, um `UNIQUE` de banco impediria
reutilizar o nome de um parceiro excluído — o que é comportamento errado, não proteção. A alternativa
correta no banco seria um índice parcial (`WHERE DT_EXCLUSAO IS NULL`), que resolveria; ficou de fora
porque acrescentaria sintaxe específica de PostgreSQL ao changeset sem ganho sobre a checagem que já
existe no service.

---

## R6 — `CPF` e `DT_NASCIMENTO` nuláveis em `PESSOA`

**Decisão.** As duas colunas nascem nuláveis; a obrigatoriedade é decidida pelo service que grava.

**Por quê.** `Pessoa` é compartilhada. Uma constraint `NOT NULL` aqui obrigaria **todo** cadastro
futuro que grave uma Pessoa a ter CPF em mãos no momento do cadastro — e nem todo tem. O
`PessoaService.validar` recebe um parâmetro que diz se o CPF é exigido, e quem chama sabe de qual
cadastro veio.

---

## R7 — Testcontainers só no `app-deploy`

**Decisão.** Os testes que tocam banco vivem no módulo executável; os módulos de biblioteca têm
apenas testes unitários com Mockito.

**Por quê.** Configuração de ambiente não desce para módulo de biblioteca — é a mesma razão de o
`application.yml` viver só ali. Um `src/test/resources` no `app-core` reintroduziria ambiente onde
ele não deve estar, e mataria a possibilidade de um segundo executável sobre o mesmo core.

**Um container para a suíte inteira**, iniciado em bloco `static` e derrubado pelo Ryuk — não um por
classe. A alternativa custaria dezenas de segundos por classe de teste.
# Roteiro da Rodada — 001 PARCEIRO E SOLICITAÇÃO

**Feature**: `001-parceiro-e-solicitacao` · **Branch**: `feature/001-parceiro-e-solicitacao` · **Requisito**: RF001
**Data**: 2026-09-05 · **Status**: ✅ homologada

> **O que é este documento.** A peça de aprovação da rodada: o que será feito, onde, com que testes,
> com que risco para o que já existe e em que ordem. Escrito para ser lido de ponta a ponta **sem
> abrir outro arquivo** — os demais artefatos ficam como referência de detalhe, não como
> pré-requisito de leitura. Também é a porta de entrada para quem for entender esta feature no
> futuro.

> **Este roteiro é o modelo de forma citado pela skill `/rodada-sdd`.** As fases 2 e 4 mandam copiar
> daqui a estrutura das 12 seções e, em especial, o formato dos dois blocos `> ✅` da §12.

**Índice**: [1 Objetivo](#1-objetivo-da-rodada) · [2 Decisões de entrada](#2-decisões-de-entrada) ·
[3 Banco](#3-banco-de-dados) · [4 Backend](#4-backend) · [5 Frontend](#5-frontend) ·
[6 Testes](#6-testes-esperados-por-bloco) · [7 Interação e isolamento](#7-interação-e-isolamento) ·
[8 Tasks e delegação](#8-divisão-de-tasks-e-delegação-por-modelo) · [9 Prompts SDD](#9-prompts-sdd-da-rodada) ·
[10 Riscos](#10-riscos-e-mitigações) · [11 Fora de escopo](#11-fora-de-escopo) ·
[12 Aprovação](#12-critérios-de-aprovação)

---

## 1. Objetivo da rodada

Entregar os dois domínios que sustentam o esqueleto: **`parceiro`**, o cadastro de referência que
todo domínio novo copia, e **`solicitacao`**, o fluxo que exercita máquina de estados, coleção filha
ordenada e comunicação entre domínios por evento.

### Por que este requisito agora

É a primeira rodada — não há grafo de dependência a respeitar. A ordem interna, sim, é rígida:

```
normalizacao (pessoa, contato, endereco)
      │
      ▼
   parceiro ──────┐
      ▲           │  evento de conclusão
      │           ▼
      └────── solicitacao ──▶ etapa
```

`parceiro` precisa de `normalizacao`; `solicitacao` precisa de `parceiro`; o evento fecha o ciclo
sem criar dependência de código na volta — que é justamente o ponto.

### O que a rodada carrega junto

A **trilha de auditoria** e a **autorização por ações** entram nesta rodada, e não numa posterior.
Não é escopo esticado: construídas depois, teriam de ser retrofitadas sobre os dois domínios. O
Princípio I diz que o mecanismo transversal nasce antes do que ele protege, e a auditoria é o caso
mais claro disso — uma entidade criada em qualquer rodada futura nasce auditada sem que ninguém
decida nada.

---

## 2. Decisões de entrada

Decisões tomadas com o product owner **antes** da escrita da especificação. São entrada do ciclo, não
questões em aberto — e é por elas que o `spec.md` não tem nenhum `[NEEDS CLARIFICATION]`.

| # | Ponto | Decisão | O que isso fecha |
| :-- | :--- | :--- | :--- |
| D1 | Como a situação da solicitação avança | **Um endpoint de ação por transição**, cada um com a sua ação de `AcaoSistema` | Fecha a porta de "quem edita, conclui". Sem isso a autorização por transição não existe |
| D2 | O que acontece ao concluir | **Evento de domínio**, consumido por um listener em `parceiro` | Fecha o acoplamento direto entre os dois services |
| D3 | Persistência do evento | **Em memória**, sem registro de publicações | Aceito porque o efeito é um contador recalculável. Registrado na §7 como o que fica frouxo |
| D4 | Sigilo da comissão | Filtrado no **service**, nos três caminhos: leitura, busca e gravação | Fecha a porta lateral da listagem e a inversão leitura→escrita |
| D5 | Unicidade do nome do parceiro | Entre os **ativos**, no service — sem `UNIQUE` no banco | Um nome liberado por exclusão volta a ser usável, e soft delete não compõe com `UNIQUE` |
| D6 | Quantidade de perfis | **Três**: administrador, operador, consulta | Suficiente para exercitar o modelo. Mais perfis seriam decoração num molde |
| D7 | Exclusão de solicitação | Só **rascunho** e **cancelada** | Excluir concluída apagaria o fato que alimentou o contador, e o evento já foi publicado |
| D8 | Obrigatoriedade de CPF e nascimento em `Pessoa` | **Nuláveis no banco**, obrigatórios no service conforme o cadastro de origem | Uma constraint obrigaria todos os domínios futuros que gravam Pessoa |

**Tomadas por convenção, sem pergunta** — e registradas porque são as que geram discussão depois:
soft delete em toda entidade de negócio; `POST /search` para busca paginada; wizard para cadastro
composto; `null` (nunca `''`) como valor inicial de select; e o tema como único lugar de cor.

---

## 3. Banco de dados

### 3.1 Tabelas novas

| Tabela | Colunas próprias | Referências |
| :--- | :--- | :--- |
| `PARCEIRO` | `NM_PARCEIRO`, `DS_PARCEIRO`, `VL_COMISSAO`, `NR_SOLICITACOES_CONCLUIDAS` | `ID_PESSOA`, `ID_CONTATO`, `ID_ENDERECO` |
| `SOLICITACAO` | `NM_SOLICITACAO`, `DS_SOLICITACAO`, `CD_SITUACAO`, `DS_MOTIVO_CANCELAMENTO`, `DT_CONCLUSAO` | `ID_PARCEIRO` |
| `ETAPA_SOLICITACAO` | `NM_ETAPA`, `NR_ORDEM`, `FL_CONCLUIDA`, `DT_CONCLUSAO` | `ID_SOLICITACAO` |

Todas herdam `DT_CRIACAO` e `DT_EXCLUSAO` de `EntidadeAuditavel`.

### 3.2 Ordem obrigatória dos changesets

```
changelog-localidades   → PAIS, ESTADO (+ seed)
changelog-normalizacao  → CONTATO, ENDERECO, PESSOA
changelog-endereco-internacional → país próprio no ENDERECO; UF e CEP viram opcionais
changelog-security      → schema security, USUARIO
changelog-auditoria     → schema auditoria, REGISTRO_AUDITORIA, CAMPO_ALTERADO
changelog-parceiro      → PARCEIRO
changelog-solicitacao   → SOLICITACAO, depois ETAPA_SOLICITACAO
```

⚠️ **Duas ordens não são estilo, são requisito.** `changelog-auditoria` vem **antes** dos domínios,
para que eles nasçam auditados. E `ETAPA_SOLICITACAO` vem **depois** de `SOLICITACAO`, porque tem FK
para ela — inverter quebra no primeiro boot de um banco limpo, e em ambiente nenhum que já tenha as
tabelas.

### 3.3 O que NÃO entra no banco

| Regra | Onde vive | Por quê |
| :--- | :--- | :--- |
| Unicidade do nome do parceiro | `ParceiroService` | É "único entre ativos"; `UNIQUE` de banco não expressa isso sem índice parcial |
| Transições válidas da situação | enum `SituacaoSolicitacao` | Uma `CHECK` saberia quais valores existem, não quais transições partem de qual origem |
| Obrigatoriedade de CPF e nascimento | `PessoaService` | Varia por cadastro de origem — ver D8 |

---

## 4. Backend

### 4.1 `app-model`

| Arquivo | Pacote | Situação |
| :--- | :--- | :--- |
| `EntidadeAuditavel`, `CampoSigiloso` | `model.entity.base` | novo |
| `RegistroAuditoria`, `CampoAlterado` | `model.entity.auditoria` | novo |
| `Pessoa`, `Contato`, `Endereco` | `model.entity.normalizacao` | novo |
| `Pais`, `Estado` | `model.entity.localidades` | novo |
| `Usuario` | `model.entity.seguranca` | novo |
| `Parceiro` | `model.entity.parceiro` | novo |
| `Solicitacao`, `EtapaSolicitacao` | `model.entity.solicitacao` | novo |
| `SituacaoSolicitacao`, `PerfilAtuacao`, `Sexo`, `OperacaoAuditoria` | `model.enums` | novo |

### 4.2 `app-security`

| Arquivo | Situação |
| :--- | :--- |
| `AcaoSistema` — 21 ações | novo |
| `SecurityConfig`, `AppKeycloakProperties`, `KeycloakConfig` | novo |
| `Keycloak{User,Group,Role}Service`, `UsuarioAutenticadoService` | novo |
| `PublicEndpoint`, `PasswordUtil`, os três exception handlers | novo |

### 4.3 `app-events`

| Arquivo | Situação |
| :--- | :--- |
| `solicitacao.event.SolicitacaoConcluidaEvent` | novo |
| `usuario.event.Usuario{Criado,Atualizado,Desativado,Reativado}Event` | novo |
| `normalizacao.event.PessoaExcluidaEvent` | novo |

Somente POJOs. Um listener aqui dependeria de `app-core`, e o reator recusaria o ciclo.

### 4.4 `app-core`

| Arquivo | Pacote | Situação |
| :--- | :--- | :--- |
| `AuditoriaInterceptor`, `AuditoriaGravador`, `RegistroPendente` | `auditoria.interceptor` | novo |
| `MapaPerfilAcoes`, `AutorizacaoBootstrap` | `autorizacao` | novo |
| `Pessoa/Contato/Endereco/CepService` | `normalizacao.service` | novo |
| `ParceiroRepository`, `ParceiroService`, `ParceiroController` | `parceiro.*` | novo |
| `SolicitacaoConcluidaListener` | `parceiro.listener` | novo |
| `Solicitacao/EtapaSolicitacaoRepository`, `SolicitacaoService`, `SolicitacaoController` | `solicitacao.*` | novo |

### 4.5 `app-deploy`

`AppApplication`, `application.yml`, os sete changelogs e os testes de integração.

### 4.6 Onde cada regra vive

A tabela mais útil do documento para quem vai revisar o PR.

| Regra | Onde | Novo? |
| :--- | :--- | :--- |
| Gravação atômica do parceiro | `ParceiroService.insert` — agregados gerenciados **antes** do save | novo |
| Validação acumulada | `ParceiroService.validate` — único ponto que lança `BusinessException` | novo |
| Unicidade do nome entre ativos | `ParceiroService.existeDuplicado` + `@SQLRestriction` da entidade | novo |
| Sigilo da comissão (leitura) | `ParceiroService.toDTO` | novo |
| Sigilo da comissão (busca) | `ParceiroService.search` — apaga linha a linha | novo |
| Sigilo da comissão (gravação) | `ParceiroService.aplicar` — só escreve quem pode ler | novo |
| Sigilo da comissão (trilha) | `@CampoSigiloso` na entidade + `AuditoriaInterceptor` | novo |
| Situação nunca vem do corpo | `SolicitacaoService.insert`/`save` ignoram `dto.situacao` | novo |
| Transições válidas | `SituacaoSolicitacao.permiteTransicaoPara` | novo |
| Trava de conclusão com etapa pendente | `SolicitacaoService.concluir` | novo |
| Contador do parceiro | `SolicitacaoConcluidaListener`, `BEFORE_COMMIT` | novo |
| Autorização por ação | `@PreAuthorize` em cada endpoint + `MapaPerfilAcoes` | novo |
| Auditoria automática | `AuditoriaInterceptor` — nenhum service a chama | novo |

### 4.7 Contrato REST

| Verbo | Rota | Autorização | Retorno |
| :--- | :--- | :--- | :--- |
| GET | `/api/parceiros/{id}` | `CONSULTAR_PARCEIRO` | 200 · 400 se inexistente |
| POST | `/api/parceiros/search` | `CONSULTAR_PARCEIRO` | 200 |
| POST | `/api/parceiros` | `INSERIR_PARCEIRO` | **201** |
| PUT | `/api/parceiros/{id}` | `ALTERAR_PARCEIRO` | 200 |
| DELETE | `/api/parceiros/{id}` | `EXCLUIR_PARCEIRO` | **204** |
| GET | `/api/solicitacoes/{id}` | `CONSULTAR_SOLICITACAO` | 200 |
| POST | `/api/solicitacoes/search` | `CONSULTAR_SOLICITACAO` | 200 |
| POST | `/api/solicitacoes` | `INSERIR_SOLICITACAO` | **201** |
| PUT | `/api/solicitacoes/{id}` | `ALTERAR_SOLICITACAO` | 200 · situação ignorada |
| DELETE | `/api/solicitacoes/{id}` | `EXCLUIR_SOLICITACAO` | **204** · 400 se em andamento ou concluída |
| POST | `/api/solicitacoes/{id}/abrir` | `ABRIR_SOLICITACAO` | 200 · 400 se transição inválida |
| POST | `/api/solicitacoes/{id}/iniciar` | `INICIAR_SOLICITACAO` | 200 |
| POST | `/api/solicitacoes/{id}/concluir` | `CONCLUIR_SOLICITACAO` | 200 · 400 com etapa pendente |
| POST | `/api/solicitacoes/{id}/cancelar` | `CANCELAR_SOLICITACAO` | 200 · 400 sem motivo |

Os **quatro endpoints de transição** são o desenho central da rodada. Sem eles, `ALTERAR_SOLICITACAO`
seria permissão para tudo.

---

## 5. Frontend

### 5.1 O que é novo

```
core/           auth (acoes.ts, auth.service, keycloak.init), guards (auth, role, routes-coverage),
                menu (menu.builder + spec), footer, localidade
commons/        data-br, moeda-br, page-response, table-sort, gems-table-labels
shared/forms/   dados-pessoais, contato, endereco, field-error, enter-busca, rg
shared/auditoria/ botao-historico, historico-registro
features/parceiro/     list + form (wizard de 4 passos) + models + services
features/solicitacao/  list + form com os botões de fluxo + models + services
features/{home,acesso-negado}
```

Decisões de tela: listagem é **cabeçalho + card de filtros + tabela solta**; a mesma rota base atende
os três modos (`novo`, `:id/editar`, `:id`), nessa ordem; cadastro composto usa wizard com submit
único.

### 5.2 O que é alterado fora da feature

Nada — é a primeira rodada.

### 5.3 Comportamentos da SDK que o roteiro já contorna

| Comportamento | Contorno |
| :--- | :--- |
| `gems-field-error` não renderiza mensagem (o `errorMessage` é `computed` sobre não-sinais) | `app-field-error` próprio, em `shared/forms/field-error/` |
| `gems-select` só mostra o placeholder com valor `null` | Todo select nasce em `null`, nunca `''` |
| `gems-table` é **controlada**: lê `sortField`/`sortDirection` como input | O componente devolve os dois, ou a seta não alterna |
| `gems-table` ignora `actions` sem uma coluna `type: 'actions'` | A coluna existe em toda listagem |
| `alertService.confirm` devolve `SweetAlertResult`, não boolean | Sempre checar `.isConfirmed` |
| `colorClass` fora do CSS encapsulado da tabela sai sem cor | Só `btn-info`/`btn-warning`/`btn-danger` |

---

## 6. Testes esperados por bloco

Para cada teste, o que ele **prova** — não que ele existe.

### 6.1 Banco de dados

| Teste | O que prova |
| :--- | :--- |
| `AppApplicationTests.contextLoads` | Paridade entidade JPA ↔ changeset, com `ddl-auto: validate`. É o gate que pegou duas colunas faltantes nesta rodada |

### 6.2 Backend

| Teste | Situação | O que prova |
| :--- | :--- | :--- |
| `ParceiroServiceTest` (sigilo, 4 casos) | novo | Que a comissão é filtrada nos **três** caminhos, e que a gravação não apaga o valor de quem não pode lê-lo |
| `ParceiroServiceTest` (validação) | novo | Que as violações saem acumuladas, e que os services de normalização não lançam sozinhos |
| `SolicitacaoServiceTest` (transições) | novo | Que rascunho não conclui direto e que situação terminal não volta |
| `SolicitacaoServiceTest` (conclusão) | novo | Que o evento carrega o parceiro, e que **não** é publicado quando há etapa pendente |
| `SolicitacaoServiceTest.putIgnoraSituacao` | novo | Que um `PUT` declarando `CONCLUIDA` não altera a situação — o teste que sustenta a D1 |
| `MapaPerfilAcoesTest` | novo | Que a matriz bate com o contrato aprovado, por contagem transcrita à mão |
| `AutorizacaoBootstrapTest` | novo | Ordem dos três passos, totalidade das ações e que falha do Keycloak não derruba a aplicação |
| `AuditoriaInterceptorTest` | novo | A fronteira do que é auditado, atacada por tipo — o de integração não discrimina |
| `AuditoriaIsolamentoTest` | novo | Que nenhum service de domínio referencia o pacote de auditoria |
| `CoberturaPreAuthorizeTest` | novo | Que **nenhum** endpoint mapeado ficou sem declaração de ação |
| `AutorizacaoPorAcaoIntegrationTest` | novo | Que a autorização **morde** em nível HTTP, e com asserção sobre o **dado**, não só sobre o status |
| `AuditoriaSigiloIntegrationTest` | novo | Que o valor da comissão não existe em coluna nenhuma da trilha — asserção contra o **banco** |
| `ActuatorSecurityIntegrationTest` | novo | Que só `/actuator/health` é anônimo |
| `S3EndpointsSecurityIntegrationTest` | novo | Que o controller publicado pelo jar de terceiro está negado |

### 6.3 Frontend

| Teste | Situação | O que prova |
| :--- | :--- | :--- |
| `routes-coverage.spec.ts` | novo | Que nenhuma rota guardada ficou sem `data.roles`, resolvendo cada `loadChildren` |
| `role.guard.spec.ts` | novo | Que o guard falha **fechado** |
| `menu.builder.spec.ts` | novo | Que cada item depende da ação de **consulta**, e que ação de escrita não abre item |
| `parceiro-form-state.service.spec.ts` | novo | Que a comissão é **omitida** do payload de quem não pode vê-la, e enviada como `null` por quem pode e a limpou |
| `perfil-atuacao.spec.ts` | novo | Que a lista local bate com o enum do backend |

### 6.4 Validação manual

Os gates do `quickstart.md`: subir o ambiente, cadastrar um parceiro pelo wizard, levar uma
solicitação de rascunho a concluída, e conferir a comissão com dois perfis diferentes.

---

## 7. Interação e isolamento

**A pergunta direta: esta feature é isolada o suficiente para não impactar as demais?**

**Resposta: sim, por vacuidade — é a primeira rodada.** O que importa aqui é o inverso: o que ela
cria de **compartilhado**, e que toda rodada seguinte vai herdar.

| Recurso | Natureza | Quem é afetado | O que garante |
| :--- | :--- | :--- | :--- |
| `EntidadeAuditavel`, `CampoSigiloso` | Novo compartilhado | toda entidade futura | `AuditoriaCoberturaIntegrationTest` |
| `AuditoriaInterceptor` | Novo compartilhado | toda entidade futura | Auditoria automática; `AuditoriaIsolamentoTest` impede chamada direta |
| `AcaoSistema` + `MapaPerfilAcoes` | Novo compartilhado | todo endpoint futuro | `CoberturaPreAuthorizeTest` |
| `Pessoa`/`Contato`/`Endereco` + services | Novo compartilhado | todo cadastro com identidade civil | Testes próprios dos três services |
| `shared/forms/*` | Novo compartilhado | toda tela de cadastro | Um dono só; feature não importa feature |
| `parceiro` como referência | Novo isolado | — | É o alvo do "espelhe este domínio" dos `AGENTS.md` |

### Fronteiras arquiteturais respeitadas

O grafo do reator segura tudo: `app-events` só tem POJOs, e o listener que reage à conclusão vive em
`app-core`, junto do domínio que reage. `solicitacao` **lê** `ParceiroRepository` na validação (é
consulta) e **não escreve** nele (é evento). Nenhum desvio a registrar no Complexity Tracking.

### O que fica frouxo depois desta feature

- **A entrega de eventos é em memória.** Uma queda do processo entre a conclusão e o listener perde o
  incremento do contador. Aceito porque o número é recalculável — e porque nenhum evento desta rodada
  carrega efeito irreversível. **O dia em que um carregar, a persistência de publicações volta como
  decisão consciente, não por omissão.**
- **`acoes.ts` é espelho manual de `AcaoSistema.java`.** Não há ferramenta que ligue os dois. A
  divergência quebra de forma barulhenta — a tela some ou o acesso é negado — e é por isso que a
  dívida é aceitável.
- **O motivo do cancelamento vem de um valor fixo na tela.** A regra do backend (motivo obrigatório)
  está fechada; o campo na UI é o que falta.

---

## 8. Divisão de tasks e delegação por modelo

**32 tarefas** em 6 fases. Distribuição: US1 9 · US2 11 · US3 3 · US4 5 · infraestrutura
compartilhada 4.

| Fase | Escopo | Tarefas | Modelo | Gate de saída |
| :-- | :--- | :--- | :--- | :--- |
| 1 | Reator, changelogs, entidades base | 6 | **sem delegação** | `contextLoads` — paridade JPA ↔ Liquibase |
| 2 | Autorização e auditoria (transversais) | 8 | **sem delegação** | Testes test-first verdes; mutação prova as varreduras |
| 3 | Domínio `parceiro` | 6 | **sem delegação** | `mvnw clean install` |
| 4 | Domínio `solicitacao` + evento | 7 | **sem delegação** | `mvnw clean install` |
| 5 | Frontend | 4 | **sem delegação** | `npm test` + `npm run build` |
| 6 | Ambiente e fechamento | 1 | **sem delegação** | Homologação |

### Critério de delegação

**Sem delegação, e a decisão é deliberada.** A regra do lote exige os três: portão barato, lista
`READ` fechada e trabalho que amortize uma sessão nova. As fases 1, 2 e 4 falham no terceiro critério
por motivo de conteúdo — são as que **decidem** o desenho (fronteiras do reator, mecanismo
transversal, máquina de estados), e o orquestrador já carregado é o executor mais barato para elas.
As fases 3 e 5 amortizariam, mas nesta rodada não havia ainda um domínio de referência a espelhar —
que é exatamente o que torna um lote delegável. **A partir da rodada 002 haverá**, e o critério muda.

### Ordem e paralelismo

```
1 ──▶ 2 ──▶ 3 ──▶ 4 ──▶ 6
              └──▶ 5 ──┘
```

Estritamente serial até a fase 3. As fases 4 e 5 poderiam correr em paralelo — uma é backend, a outra
frontend, zero arquivo em comum —, mas o frontend consome o contrato REST da 4.

**MVP = fases 1, 2 e 3.** Um cadastro de parceiros auditado e autorizado já entrega valor. O que
**não** pode ser cortado é a fase 2: construída depois, teria de ser retrofitada sobre os domínios.

---

## 9. Prompts SDD da rodada

| Ordem | Comando | Situação | Saída |
| :-- | :--- | :--- | :--- |
| 1 | `/speckit-specify` | ✅ executado | `spec.md` — 4 user stories, 15 FRs, 6 SCs |
| 2 | `/speckit-clarify` | ✅ executado | 8 decisões de entrada, todas na §2. Zero `[NEEDS CLARIFICATION]` no spec |
| 3 | `/speckit-plan` | ✅ executado | `plan.md`, `research.md`, `data-model.md`, `contracts/` |
| 4 | `/speckit-tasks` | ✅ executado | `tasks.md` — 32 tarefas em 6 fases |
| 5 | `/speckit-analyze` | ✅ executado | 5 achados; ver abaixo |
| 6 | **`roteiro.md`** | ✅ este documento | — |
| 7 | `/speckit-implement` | ✅ executado | Código, testes e ambiente |

### Achados do `/speckit-analyze` já incorporados

| Achado | Sev. | Decisão |
| :--- | :--- | :--- |
| O sigilo da comissão estava especificado só na leitura; a busca e a gravação não tinham requisito | **HIGH** | **Incorporado**: FR-013 passou a citar os três caminhos, e a §6.2 ganhou um teste por caminho |
| A ordem `auditoria` antes dos domínios não estava escrita em lugar nenhum — era hábito | **HIGH** | **Incorporado**: virou a §3.2, com o ⚠️ |
| Nenhum requisito dizia o que acontece ao concluir solicitação de parceiro excluído | MEDIUM | **Incorporado**: virou cenário 2 da US3 e o `ifPresentOrElse` do listener |
| Sugestão de criar um `SituacaoService` para as transições | MEDIUM | **Recusado**: a regra é do enum, e um service intermediário só acrescentaria uma camada sem decisão própria |
| Sugestão de índice em `SOLICITACAO.DT_CRIACAO` pela ordenação padrão | LOW | **Recusado por ora**: volume de uma rodada inicial não justifica. Registrado como o primeiro candidato se a listagem ficar lenta |

---

## 10. Riscos e mitigações

| # | Risco | Gravidade | Mitigação |
| :-- | :--- | :--- | :--- |
| 1 | Sigilo implementado pela metade — só na leitura | **Alta** | Um teste por caminho, e os três falham sozinhos |
| 2 | Alguém acrescentar um endpoint sem `@PreAuthorize` | **Alta** | `CoberturaPreAuthorizeTest` com lista fechada de controllers |
| 3 | Transição nova esquecida na máquina de estados | Média | As transições vivem no enum; o teste percorre os pares |
| 4 | Evento perdido por queda do processo | Média | Efeito recalculável; declarado na §7 |
| 5 | Divergência entre `acoes.ts` e `AcaoSistema.java` | Média | Quebra barulhenta (tela some); dívida aceita e registrada |
| 6 | Coluna mapeada sem changeset | Média | `ddl-auto: validate` no boot e no `contextLoads` |

---

## 11. Fora de escopo

Nada aqui pode ser esquecimento — cada item tem destino.

| Item | Por quê | Volta em |
| :--- | :--- | :--- |
| Upload de arquivo | Nenhuma história desta rodada precisa. A infraestrutura de S3 existe e está fechada | A rodada que trouxer anexo |
| Campo de motivo no cancelamento | A regra do backend está fechada; falta o input | Próxima rodada de frontend |
| CRUD de etapas pela tela | O backend as trata; a tela desta rodada só as exibe | A rodada que trouxer a esteira |
| Tela de auditoria | O mecanismo e o endpoint existem; a listagem não | A rodada de conformidade |
| Dashboards | Sem dado histórico suficiente para valer | Depois de duas ondas |

---

## 12. Critérios de aprovação

### Para aprovar esta rodada (antes do código)

- [x] O escopo da §1 é o que se espera da próxima entrega
- [x] As decisões da §2 refletem o que foi combinado
- [x] O impacto da §7 é aceitável
- [x] A cobertura de testes da §6 é suficiente para o risco assumido
- [x] A decisão de delegação (inclusive `sem delegação`) da §8 está adequada
- [x] Os itens fora de escopo (§11) podem mesmo esperar

> ✅ **PORTÃO APROVADO pelo product owner em 2026-09-05.** Aceitos: as oito decisões de entrada, o
> modelo de dados das três tabelas novas, o contrato REST com os quatro endpoints de transição, e o
> raio de impacto da §7 — em especial a declaração de que a entrega de eventos é em memória.
> **O `/speckit-implement` está liberado.**

### Para considerar a rodada concluída (depois do código)

- [x] `.\mvnw.cmd clean install` verde no reator inteiro, com testes
- [x] `npm test` e `npm run build` verdes
- [x] Aplicação sobe com `ddl-auto: validate` sem reclamar de schema
- [x] Os gates do `quickstart.md` marcados
- [x] **Zero regressão** — primeira rodada, nada a regredir
- [x] `/speckit-analyze` final sem CRITICAL e sem violação de constituição
- [x] Arquivos de agente atualizados

> ✅ **HOMOLOGADA pelo product owner em 2026-09-05**, em tela, com dado real de negócio. Percorridos:
> o wizard de parceiro nos quatro passos, a listagem com filtro e ordenação, o ciclo completo da
> solicitação de rascunho a concluída, a recusa de conclusão com etapa pendente, e a comparação da
> comissão entre dois perfis. A rodada libera o domínio `parceiro` como referência para as demais.

#### Números medidos em 2026-09-05

| Métrica | Antes | Depois |
| :--- | ---: | ---: |
| Testes de backend (reator inteiro) | 0 | **151** |
| — `app-security` | 0 | 5 |
| — `app-core` | 0 | 112 |
| — `app-deploy` (integração, Testcontainers) | 0 | 34 |
| Testes de frontend (Vitest) | 0 | **118** em 21 arquivos |
| Ações em `AcaoSistema` | 0 | 21 |
| Constantes em `acoes.ts` | 0 | 21 |
| Controllers com `@PreAuthorize` | 0 | 5 |
| Rotas guardadas com `data.roles` | 0 | 7 |
| Changelogs incluídos no master | 0 | 7 |

#### Redes provadas por mutação

| Rede | Alvo quebrado | O teste falhou? |
| :--- | :--- | :--- |
| `CoberturaPreAuthorizeTest` | `@PreAuthorize` removida de um endpoint | ✅ |
| `routes-coverage.spec.ts` | `data.roles` removido de uma rota | ✅ |
| Sigilo na gravação | Guarda `podeVerComissao()` removida do `aplicar` | ✅ |
| Situação não vem do corpo | `aplicar` passando a copiar `dto.situacao` | ✅ |

#### O que a homologação encontrou

Duas divergências que **nenhum teste unitário pegaria**, e que só o boot real contra um banco limpo
revelou:

1. `ENDERECO.NR_NUMERO` nascia como `CD_NUMERO` — o `ddl-auto: validate` recusou subir.
2. `PESSOA` não tinha `CD_SEXO`, `DT_OBITO` nem `FL_DATA_APROXIMADA`, todas mapeadas na entidade.

As duas foram corrigidas no changeset de criação, e não por um changeset de renomeação: num banco que
nasce do zero, a regra "changeset aplicado nunca é editado" protege o que já rodou — e aqui nada
havia rodado.

---

## Referência dos artefatos

| Arquivo | Para quê |
| :--- | :--- |
| [spec.md](spec.md) | Requisitos, user stories, critérios de sucesso |
| [plan.md](plan.md) | Decisões técnicas congeladas, Constitution Check, estrutura |
| [research.md](research.md) | Por que cada decisão técnica |
| [data-model.md](data-model.md) | Colunas, changesets, DTOs, consultas |
| [contracts/](contracts/) | Payloads, códigos de retorno, contrato do módulo de frontend |
| [tasks.md](tasks.md) | Tarefas, dependências e paralelismo |
| [quickstart.md](quickstart.md) | Roteiro de validação manual |
| [checklists/](checklists/) | Qualidade da especificação e cobertura do requisito |

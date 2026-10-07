# Roteiro da Rodada — 002 Conformidade GEMS

**Feature**: `002-conformidade-gems` · **Branch**: `feature/conformidade-gems` · **Requisito**: prova S-codex da rodada 010
**Data**: 2026-09-14 · **Status**: **portão aprovado em 2026-09-14** — fase 3 liberada com os
bloqueios de release de D-P01 e D-P02 preservados

> Este documento apresenta escopo, decisões, impactos, testes e ordem da rodada. D-P01–D-P03 e a
> expansão T004 foram decididas no portão; D-P04 foi decidida após a medição e corrigida no C14-R1.

**Índice**: [1 Objetivo](#1-objetivo-da-rodada) · [2 Decisões](#2-decisões-de-entrada-e-do-portão) ·
[3 Banco](#3-banco-de-dados) · [4 Backend](#4-backend) · [5 Frontend](#5-frontend) ·
[6 Testes](#6-testes-esperados-por-bloco) · [7 Impacto](#7-interação-e-isolamento) ·
[8 Tasks](#8-divisão-de-tasks-e-delegação-por-modelo) · [9 SDD](#9-prompts-sdd-da-rodada) ·
[10 Riscos](#10-riscos-e-mitigações) · [11 Fora](#11-fora-de-escopo) ·
[12 Aprovação](#12-critérios-de-aprovação)

---

## 1. Objetivo da rodada

Tornar o sample coerente com a constituição 2.0.0, regras GEMS 2.4.0 e módulos publicados, preservando
as features de referência. O backend termina sem dívida tolerada; o frontend termina no mínimo medido,
e qualquer residual continua pendente até aceite humano nominal.

### Por que agora

S-def instalou definições/gates e T058 confirmou o reator verde. S-codex precisa transformar o molde
em exemplo conforme: reduzir apenas totais não satisfaz S-12, e adotar SDK sem equivalência viola a
própria constituição.

### O que a rodada carrega

Medição de grafo; MapStruct em seis services; rollback integrado em dois fluxos; auditoria atômica;
autorização/Keycloak; Angular 2.1.0; quatro repositories, dois controllers, quatro handlers, 177 nomes;
remoção das 22 supressões de dívida e do Spotless ratchet; medição/decisão de residual frontend.

---

## 2. Decisões de entrada e do portão

### 2.1 Entradas aprovadas na fase Start

| # | Ponto | Decisão |
| :-- | :--- | :--- |
| 1 | Condução | Codex/Sol conduz a prova S-codex dentro da 010, sem Terra. |
| 2 | Autoridade | Constituição 2.0.0 e regras 2.4.0 vencem código; `medicao-gates.md` vence histórico. |
| 3 | Fronteira | Preservar parceiro, solicitação, localidade, normalização, usuário, auditoria e S3. |
| 4 | Java | Adotar versão publicada, uma propriedade pai e árvores antes/depois. |
| 5 | Angular | Adotar 2.1.0/overrides e retirar a flag operacional de peers. |
| 6 | Mapping | Seis services em MapStruct/GemsMappingConfig estrito. |
| 7 | Transação | Spring `rollbackFor`, com mutação nas duas exceções. |
| 8 | Auditoria | SDK só entra com tela/consulta, autoria, correlação, agregados, sigilo, busca e data preservados; escrita local sai junto. |
| 9 | Keycloak/autorização | SDKs só substituem clientes locais com catálogo, bootstrap, perfis e ciclo de usuário preservados. |
| 10 | Backend | Controllers, repositories, services, handlers, OpenAPI, eventos, persistência, validação, observabilidade e S3 conformes; agente não cria exceção. |
| 11 | Frontend | `isAuthorized`, quatro paths por feature, `shared`, componentes SDK, forms/signals/stores/tokens. |
| 12 | Gates | T057 é ponto inicial; S-12 exige Java zero e frontend mínimo medido, não mera redução. |
| 13 | Baseline | T058: 204 backend e 21/118 frontend verdes. |
| 14 | Dependências | D-01/D-02 e labels não são reabertas. |
| 15 | Tenant | O sample permanece single tenant. |
| 16 | Fora | ADACI, Keycloak Angular 22, TS7 e promoção de commons ficam fora. |
| 17 | Prova 010 | Silent check-sso recebe crédito OK-1; roadmap OK-2 no fechamento. |

### 2.2 Clarificações resolvidas no portão de 2026-09-14

| ID | Evidência | Opções | Decisão do product owner |
| :--- | :--- | :--- | :--- |
| D-P01 | Auditing 3.1.0 não grava identidade estável/correlação. | Release SDK prévia ou emenda aceitando perda. | **Preservar e bloquear até release compatível publicada.** |
| D-P02 | Keycloak gateway 3.1.0 não cobre update/enable/disable/delete/compensação/grupos completos nem criar/compor roles. | Release SDK prévia ou emenda de contrato. | **Preservar e bloquear até release compatível publicada.** |
| D-P03 | Realm declarativo altera ambiente fora do EDIT original. | Não alterar; ou expandir e derivar só roles/compostas. | **Não aplicável: manter o ambiente e o realm atuais.** |
| T004 | CI, Dependabot e README ainda instruem a flag operacional antiga. | Manter fora do EDIT ou autorizar correção nominal. | **Expansão autorizada para `.github/workflows/ci.yml`, `.github/dependabot.yml` e `README.md`.** |
| D-P04 | Mínimo frontend só existe depois da correção/medição. | Aceitar ou recusar cada residual. | **Devolvido para correção em 2026-09-23; C14-R1 resolveu 63/63 e zerou o baseline.** |

---

## 3. Banco de dados

O schema atual preserva `ID_AUTOR` e `CD_CORRELACAO`. A SDK 3.1.0 escreve somente nome/tipo do ator;
colunas extras não resolvem porque o writer não as preenche. Resolver id por login atual falha após
rename/delete/reuso e foi retirado do plano.

### Bloco de auditoria

Depois de D-P01 e de nova API publicada aferida, migration, read adapter, provider, configuração,
ativação e remoção dos escritores locais entram em **uma única alteração**. Testes de upgrade são
escritos primeiro; backfills precedem constraints; dado incompatível para a migração; índices/FKs são
recriados; `ddl-auto: validate` fecha verde. Não existe checkpoint com tabela e writer incompatíveis.

Se o PO escolher perda semântica, a §12 precisa nomear que linhas novas não terão identidade estável
nem correlação e que o filtro por autor ficará incompleto. Este roteiro não registra esse aceite.

---

## 4. Backend

### 4.1 Dependências e mapping

POM pai concentra versão/processadores. Entram seis mappers nominais para localidade, contato,
endereço, pessoa, parceiro e solicitação. `ModelMapper` sai quando a última referência sair.

### 4.2 Transações

Parceiro e solicitação usam transação Spring com `rollbackFor`. A prova vive em `app-deploy`, chama
bean proxied contra PostgreSQL e lança checked após escrita. O teste não é transacional; nova leitura
afirma dado, filhos, efeito e trilha ausentes. Retirar `rollbackFor` precisa persistir algo e falhar.

### 4.3 Conformidade nominal

| Dívida atual | Alvo final |
| :--- | :--- |
| 314 Checkstyle em 22 supressões | zero; supressões de dívida removidas |
| repositories | exatamente CampoAlterado, Estado, Pais e EtapaSolicitacao corrigidos |
| controllers | Localidade e Cep em BaseController |
| ordens | seis services corrigidos |
| nomes | 177 identidades `deveX_quandoY` |
| Spotless | `spotless:apply`, rechecagem total e `ratchetFrom` removido |

### 4.4 Handlers e órfãos

Saem juntos `ProvedorExternoExceptionHandler`, `AccessDeniedExceptionHandler`,
`IdentityProviderExceptionHandler` e `RequisicaoInvalidaExceptionHandler`, após testes de envelope.
`ProvedorExternoException` adota a base externa publicada. `PublicEndpoint`, `PublicRoute`,
`CreateUserDTO` e outros tipos saem somente quando busca nominal provar zero consumidor; `SecurityRoles`
e `PasswordUtil` permanecem enquanto tiverem uso real.

### 4.5 Autorização/Keycloak

Catálogo/scan da SDK são compatíveis. A remoção das quatro classes administrativas fica bloqueada por
D-P02: o gateway aferido não preserva todo o ciclo. Um adapter novo não será criado para contornar.

---

## 5. Frontend

### 5.1 Forma e comportamento

Parceiro e solicitação terão `list`, `form`, `form/:id`, `view/:id`. Os 12 arquivos medidos deixam
`hasRole`/`hasAnyRole`; sete componentes deixam navegação direta onde a SDK se aplica; dez arquivos
`commons` ganham destinos CREATE nominais em `shared`; field-error local sai após equivalência.

### 5.2 Instalação e expansão

SDK 2.1.0/overrides permitem `npm ci` sem flag. Os pontos operacionais atuais estão em
`frontend/Dockerfile`, `.github/workflows/ci.yml`, `README.md` e `.github/dependabot.yml`. Os três
últimos exigem expansão do escopo de implementação. Menções normativas em constituição/regras são
imutáveis e reportadas, não alteradas por conveniência.

### 5.3 Mínimo medido

O gate mede visual, testes, signals e organização por identidade. O objetivo é zero quando aplicável.
Qualquer residual vem com arquivo, regra, motivo e alternativa e continua desmarcado até D-P04. O
agente não transforma contagem menor em autorização.

---

## 6. Testes esperados por bloco

| Bloco | Prova | Gate |
| :--- | :--- | :--- |
| Dependências | árvores antes/depois; host/Docker/CI sem flag | `dependency:tree`, `npm ci`, Docker build |
| Keycloak/autorização | 21 ações, scan, ciclo completo, compensações | C11 verde: T024 com 52 testes; SDK 3.3.0 |
| Auditoria atômica | upgrade, commit/rollback, sigilo, identidade/correlação, consulta | teste focado T035 + mutações T036 |
| Mapping/transação | seis mappers; proxy/banco/checked sem falso rollback | T050 + mutações T048/T049 |
| Java zero | quatro repos, dois controllers, seis ordens, 177 nomes, 314/22, ratchet | T052–T061 |
| Frontend | auth, oito rotas, SDK/shared, 70 cores, residual nominal | T072/T073 e gate completo |
| Fechamento | todas as redes mutadas e quickstart | T074–T079 |

Baseline reutilizado nesta fase: backend exit 0/204 (13 security, 132 core, 59 deploy) e frontend exit
0/21 arquivos/118 testes. Não foi repetido porque não houve alteração de código.

---

## 7. Interação e isolamento

**Parcialmente isolada.** A rodada preserva domínio, mas altera build, persistência, segurança,
ambiente condicional e UI compartilhada. A atomicidade da auditoria e os gates completos limitam a
exposição.

| Recurso | Impacto | Garantia |
| :--- | :--- | :--- |
| POM/build Java | todos os módulos | árvores e clean install; zero/ratchet removido |
| auditoria | toda entidade auditável | bloco único e D-P01 |
| Keycloak | usuário/perfis/endpoints | D-P02 e testes completos |
| realm condicional | local/staging/prod | D-P03, one-shot e seções não derivadas preservadas |
| Angular/shared | shell e duas features | testes focados, 21/118 + novos e D-P04 |

Não há violação constitucional justificada. Pendência humana não é exceção nem Complexity Tracking.

---

## 8. Divisão de tasks e delegação por modelo

**79 tarefas em 8 fases**: decisões 4; dependências 8; autorização/Keycloak 12; auditoria atômica 12;
mappers/transações 15; Java zero 10; frontend 12; fechamento 6.

| Fase | Tasks | Modelo | Gate |
| :--- | :--- | :--- | :--- |
| 0 decisões | T001–T004 | Humano + Sol registra | decisões/expansões explícitas |
| 1 dependências | T005–T012 | Sol/Codex | resolução publicada e sem flag operacional |
| 2 auth/Keycloak | T013–T024 | Sol/Codex | C11 concluído; contrato completo na SDK 3.3.0 |
| 3 auditoria | T025–T036 | Sol/Codex | um gate atômico; bloqueado D-P01 |
| 4 mapping/rollback | T037–T051 | Sol/Codex | seis mappers e dois rollbacks reais |
| 5 Java zero | T052–T061 | Sol/Codex | zero Java e ratchet removido |
| 6 frontend | T062–T073 | Sol/Codex | mínimo medido; D-P04 humano |
| 7 fechamento | T074–T079 | Sol/Codex | gates/quickstart/analyze |

Sem delegação para Terra. Opus é elegível, mas não foi chamado por proibição do pacote. Auditoria não
admite paralelismo interno nem checkpoint parcial.

---

## 9. Prompts SDD da rodada

| Ordem | Comando | Situação | Saída |
| :-- | :--- | :--- | :--- |
| 1 | `/speckit-specify` | revisado | 5 histórias, 30 FR, 9 SC |
| 2 | `/speckit-clarify` | concluído | D-P01/D-P02 preservam garantias; D-P03 não aplicável; T004 autorizada; D-P04 fica para a medição |
| 3 | `/speckit-plan` | revisado | nove princípios reais, desenho e contratos condicionais |
| 4 | `/speckit-tasks` | revisado | 79 tasks em 8 fases |
| 5 | `/speckit-analyze` | revisado | 0 CRITICAL documental; achados incorporados |
| 6 | **`roteiro.md`** | aprovado em 2026-09-14 | portão assinado pelo product owner |
| 7 | `/speckit-implement` | C00–C08 verdes; próxima retomada C09 | T005–T007 revalidadas; T009–T012 e T037–T069 concluídas; antecipações autorizadas de T067/T071 registradas; T008 preservada com gate Docker pendente para C09 |

### Resultado C01 — 2026-09-15

CI e README usam `npm ci`; comentários de CI/Dependabot refletem Angular 22, SDK 2.1.0 e overrides.
YAML do workflow e step de instalação validados (exit 0). Busca nos seis alvos operacionais do
pacote sem ocorrências da flag (rg exit 1). `dependency:tree` retornou exit 0/BUILD SUCCESS nos seis
módulos, mantendo GEMS 3.1.0, ModelMapper 3.2.4 temporário e MapStruct 1.6.3 conforme POMs e C00;
os POMs não receberam alterações. `git diff --check` verde (exit 0).
As três referências imutáveis informadas pelo pacote estão separadas no estado C01, sem nova
leitura fora de READ. T008 e Docker build seguem em C09; o próximo checkpoint cabe ao orquestrador.

### Resultado C02 — 2026-09-15

Os seis services passaram a usar mappers MapStruct nominais configurados por
`br.com.gems.mapstruct.GemsMappingConfig`; identidade, auditoria, referências gerenciadas,
normalização de CPF, sigilo de comissão e relações de solicitação foram preservados. Parceiro e
solicitação usam a transação Spring com `rollbackFor = Exception.class`. O vermelho test-first foi
registrado no `testCompile` com 12 erros para seis mappers ausentes. Após a implementação e a
recuperação do FQCN, o gate focado terminou com exit 0: 70 testes, 0 falhas, 0 erros e 0 ignorados;
`git diff --check` também terminou com exit 0. T046–T051 permanecem para o checkpoint seguinte.

### Resultado C03 — 2026-09-16

T046–T051 executadas em sessão nova por Codex/Sol, sem Terra. Dois testes integrados de parceiro e
quatro de solicitação chamam services reais proxied contra PostgreSQL, sem transação de teste nem
TransactionTemplate envolvendo a operação. O mapper colaborador executa mapeamento real, flush e
comprovação física dos IDs antes de lançar IOException checked diretamente. SQL independente não
esconde soft delete; fixtures/limpeza restringem IDs/correlação próprios. Controles positivos provam
inserts/trilha e consumo do evento em memória pelo listener BEFORE_COMMIT/contador. A etapa criada
no insert e sua alteração no concluir são explicitamente fixtures/hooks, não comportamento do insert.

Três mutações isoladas ficaram vermelhas (cada exit 1, 1 falha, 0 erros/skips): ParceiroService.insert
persistiu uma linha PARCEIRO; SolicitacaoService.insert persistiu uma SOLICITACAO; concluir(UUID)
persistiu CONCLUIDA/data/alteração de etapa/contador 1 em vez de 0. Cada rollbackFor foi restaurado
por patch inverso e T050 repetido verde, sem alteração permanente nos services. T050 pré/pós-T051
também retornou exit 0/BUILD SUCCESS: 70 unitários + 2 parceiro + 4 solicitação = 76, zero
falhas/erros/skips. ModelMapper saiu somente dos dois POMs (dependência/gerenciamento/propriedade/pin
órfãos); GEMS/MapStruct/processadores ficaram intactos. Busca dois POMs/seis services sem ocorrências
(rg exit 1), dependency:tree filtrada exit 0 sem os artefatos e git diff --check exit 0.
O primeiro gate encontrou fixture sem país estrangeiro; payload parceiro ajustado ao seed Brasil com
estado/CEP via LocalidadeService de READ. Logs completos em TEMP; nenhuma expansão, frontend/ambiente
ou auditoria SDK tocados. O executor não fez commit/push/PR.

Aceitação independente do orquestrador: diff integral revisado; verificador exit 0, 8 esperados/
6 declarados/6 reais (ressalva esperada dos dois services restaurados). T050 pós-remoção e final
pós-restauração repetidos com exit 0, 76 testes/zero falhas/erros/skips. A repetição conjunta dos
três pontos independentes resultou em exatamente três falhas de asserção/zero erros/skips, nas
três garantias nomeadas acima. Anotações restauradas por patch inverso, services sem diff.
Busca/tree repetidas sem referências/artefatos ModelMapper. C03 aceito; próxima invocação C04.

### Resultado C04 — 2026-09-16

T052–T055 concluídas sob o ajuste humano do envelope SDK registrado abaixo. Os quatro repositories
estendem `JpaRepository` antes de `BaseCustomJpaRepository`, e os dois controllers somente leitura
passaram a estender `BaseController`; medições nominais encerraram com zero repository e zero
controller. A medição de ordem dos seis services também terminou em zero; o privado residual de
`PessoaService` foi movido depois dos públicos, assim como o de `KeycloakUserService`, já alterado
por T054.

`ProvedorExternoException` passou a `ExternalServiceException`; falhas da Admin API do Keycloak são
traduzidas com mensagem e causa. Os quatro handlers locais foram removidos na mesma alteração. O
teste MockMvc nasceu antes da remoção e ficou vermelho em três casos; depois provou o envelope SDK
para 502/403/404/400, inclusive path/method e ausência de eco da entrada na mensagem. A mensagem
403 passou para “Você não possui acesso para este serviço!” e a 404 para “Recurso não encontrado.”;
a mensagem 400 e as mensagens funcionais 502 foram preservadas. Em 404/400, os códigos específicos
passaram para `codigo`, com `errorType` `FALHA`/`VALIDACAO`, conforme aprovação.

A mutação real retirou temporariamente a herança SDK de `ProvedorExternoException`: o teste recebeu
500 onde exigia 502 e ficou vermelho com uma falha exata. O patch inverso restaurou o alvo e o teste
voltou a verde. O portão final do C04 terminou exit 0/BUILD SUCCESS, 28 testes, zero
falhas/erros/skips; medições nominais: repository 0, controller 0, ordem 0. D-P01/D-P02 permanecem e
nenhum baseline, frontend, ambiente ou adoção de auditoria/Keycloak foi tocado.

### Resultado C05 — 2026-09-16

T056–T059 concluídas: as 177 identidades de teste foram renomeadas, as 314 ocorrências Checkstyle
foram corrigidas, e os baselines de arquitetura/Checkstyle e as supressões terminaram com zero
entrada de dívida. O `spotless:apply` formatou exatamente 29 + 56 = 85 Java nominais, sem 86º
arquivo, e `ratchetFrom` saiu do POM pai.

O primeiro portão revelou uma divergência factual em C04/T055: `possuiAuthority` ainda aparecia
antes do público `loginDoUsuarioAutenticado`. C05-R1 moveu somente esse método privado para depois
dos públicos e removeu seis espaços finais já identificados em quatro Javadocs. A repetição literal
do portão terminou exit 0: `ConvencoesFonteTest` com 9 testes e zero falhas/erros/skips, `validate`
com zero violações Checkstyle nos seis módulos, `spotless:check` integral verde e `git diff --check`
exit 0. Nenhum arquivo fora da lista fechada foi tocado; o executor não fez commit, push ou PR.

### Resultado C06 — 2026-09-16

T060 fechou as tolerâncias Java em zero: o limite de entradas do baseline passou a `0`, o gate de
arquitetura expressa a recusa de qualquer entrada, e o gate de fonte recusa qualquer supressão
Checkstyle. A regra de nomes agora também admite explicitamente o padrão `naoDeve..._quando...` da
regra GEMS.

Três mutações temporárias foram observadas vermelhas e restauradas por edição inversa: uma entrada no
baseline de arquitetura (1 entrada contra 0), uma supressão Checkstyle e a remoção artificial de
`_quando` da regra de nomes. O reator completo `mvnw.cmd clean install` terminou verde nos seis
módulos, com 216 testes em 30 relatórios, zero falhas/erros/skips, Checkstyle zero e Spotless verde.
Próxima retomada: C07; C11/C12 continuam bloqueados pelas releases compatíveis das SDKs.

### Resultado C07 — 2026-09-20

T062–T065 concluídas. Guard, menu, fachada, auditoria e consumidores usam `isAuthorized` com requisito
global explícito; os 12 alvos ficaram com zero `hasRole`/`hasAnyRole`. Sete arquivos de teste
focados passaram com 20 casos. A mutação que retirou `isAuthorized` produziu erros de compilação nos
consumidores e foi restaurada por patch inverso.

`provideGemsKeycloak`, interceptor DI e silent check-sso já estavam conformes na âncora `b807233`;
`app.spec.ts` passou a provar as opções sem gerar alteração gratuita nos providers. O product owner
autorizou antecipar exclusivamente a caracterização de `field-error` de T067 e a formatação de
`verify-conventions.mjs` de T071 para destravar o gate; a segunda não gerou diff material.

Gate final: 25 arquivos / 123 testes verdes; 13 regras em 126 arquivos, zero violações; Prettier
integral verde; build verde, bundle inicial 726,67 kB, com apenas o aviso de budget conhecido;
`git diff --check` verde. Próxima retomada: C08.

### Resultado C08 — 2026-09-20

T066–T069 concluídas. Parceiro e solicitação expõem `list`, `form`, `form/:id` e `view/:id`,
com redirect da base, menu e navegação alinhados. A cópia local de `field-error` saiu depois de
6 casos de equivalência verdes. Os dez artefatos de `commons/` migraram para `shared/`; os 32 nomes
de teste que passaram a entrar no alcance de `testing.md#7` foram conformados sem alterar os corpos.

T072 passou com 6 arquivos/30 testes e T073 com 7 arquivos/18 testes; os quatro specs movidos
passaram com 32 testes. O verificador terminou com 13 regras, 124 arquivos e zero violações. Duas
mutações ficaram vermelhas e foram restauradas: rota `view/:id`→`:id` e navegação
`/parceiros/form`→`/parceiros/novo`. `app.ts` manteve `Router` somente para observar eventos; zero
transporte operacional de domínio por sessão. Próxima retomada: C09.

### Resultado C09 — 2026-09-21

T008 e T070–T073 foram concluídas. O Docker stage `build` passou com `npm ci` e build de produção,
usando exclusivamente o secret BuildKit previsto para o registry privado. O gate de ações confirmou
21 ações em paridade com `AcaoSistema`; convenções terminou com 13 regras, 124 arquivos e zero
violações. Os gates focados T072 (6 arquivos/30 testes) e T073 (7 arquivos/18 testes), o `npm test`
completo (26 arquivos/123 testes), build, Prettier e `git diff --check` ficaram verdes. O bundle
inicial foi 735,79 kB, mantendo somente o aviso de budget conhecido. D-P04 segue sem aceite humano.
Próxima retomada: C10-SDK; C11 e C12 continuam bloqueados até a release compatível.

### Resultado dos checklists

Requirements, technical e security registram **PASS DE PORTÃO**. D-P01 e D-P02 foram resolvidas pela
preservação, D-P03 não se aplica e T004 foi autorizada; D-P04 continua no portão posterior previsto.

### Achados incorporados

| Achado | Sev. | Decisão |
| :--- | :--- | :--- |
| Java precisa zerar | HIGH | zero 314/189, 177 nomes; 22 supressões e ratchet removidos |
| auditoria separada | HIGH | Phase 3 atômica |
| rollback Mockito | HIGH | integração proxied/PostgreSQL/checked sem transação de teste |
| identidade/correlação SDK | HIGH | D-P01, preservação recomendada |
| Keycloak incompleto | MEDIUM | D-P02, preservação recomendada |
| realm/escopo | MEDIUM | D-P03 condicional, seções derivadas apenas |
| frontend residual | MEDIUM | D-P04 devolvida; residual zero no C14-R1 |
| paths/contagens | MEDIUM | 79 tasks nominais e números atuais |
| linguagem spec | MEDIUM | histórias/SC por contrato |

---

## 10. Riscos e mitigações

| # | Risco | Gravidade | Mitigação |
| :-- | :--- | :--- | :--- |
| 1 | misturar autores após rename/reuso | alta | D-P01; identidade estável publicada |
| 2 | perder correlação | alta | D-P01; não aceitar nome como equivalência |
| 3 | schema/writer incompatível | alta | bloco único sem checkpoint |
| 4 | falso positivo de rollback | alta | teste sem transação própria e mutação no `rollbackFor` real |
| 5 | perder ciclo/compensação Keycloak | alta | D-P02; não remover classes antes de release equivalente |
| 6 | realm apagar clients/mappers/defaults | alta | D-P03; derivar só partes e gate semântico |
| 7 | dívida Java sobreviver menor | alta | alvo zero e remoção de baseline/ratchet |
| 8 | residual frontend aceito pelo agente | média | D-P04 desmarcada e inventário nominal |
| 9 | CI/Docker ainda usarem flag | média | inventário operacional e expansão T004 |

---

## 11. Fora de escopo

| Item | Destino |
| :--- | :--- |
| editar SDK Java/Angular nesta rodada | rodada própria + publicação, se D-P01/D-P02 aprovarem |
| ADACI | roadmap próprio |
| keycloak-angular 22 / TypeScript 7 | atualização futura |
| multi-tenancy | requisito futuro |
| promover commons à SDK | roadmap OK-2 |
| alterar constituição/regras imutáveis para limpar menção textual | processo de definições, não S-codex |

---

## 12. Critérios de aprovação

### Decisões deste portão

- [x] **D-P01**: preservar identidade estável/correlação e bloquear a adoção até release SDK compatível publicada.
- [x] **D-P02**: preservar ciclo/roles Keycloak e bloquear a substituição até release SDK compatível publicada.
- [x] **D-P03**: não aplicável; ambiente e realm atuais permanecem inalterados.
- [x] **T004**: expansão autorizada para `.github/workflows/ci.yml`, `.github/dependabot.yml` e `README.md`.
- [x] O escopo, atomicidade, zero Java, testes reais e execução Sol/Codex estão aprovados.

### Decisão posterior à implementação

- [x] **D-P04**: residual devolvido para correção e resolvido integralmente no C14-R1.

### Para concluir a rodada depois do código

> ✅ **AJUSTE C04 APROVADO pelo product owner em 2026-09-16.** “Aprovar o envelope da SDK”
> aceita o ajuste registrado em `D:/repos/Meduc/meduc-workspace/specs/010-habilitacao-de-agentes/ajuste-c04-envelope.md`:
> status 400/403/404/502 preservados; 400 usa `VALIDACAO`/`codigo=PARAMETRO_INVALIDO`,
> 404 usa `FALHA`/`codigo=RECURSO_NAO_ENCONTRADO`, 502 usa `SERVICO_INDISPONIVEL`;
> mensagens de 403 e 404 passam às da SDK 3.1.0, 400 e mensagens funcionais 502 preservadas.
> T054 liberada com testes HTTP e mutações. D-P01/D-P02 permanecem; não é homologação.

- [x] Backend `clean install` verde, Java zero, supressões/ratchet removidos.
- [x] Frontend `npm ci`, testes, build e Prettier verdes; residual corrigido e baseline vazio.
- [x] Auditoria/Keycloak preservam contratos escolhidos e quickstart passa.
- [x] Analyze final sem CRITICAL nem violação constitucional.

> ✅ **PORTÃO APROVADO pelo product owner em 2026-09-14.** Foram aceitos o escopo, a atomicidade,
> o alvo Java zero, os testes reais, a execução Sol/Codex, a preservação das garantias de auditoria e
> Keycloak mediante releases prévias compatíveis, a não aplicação de D-P03 e a expansão T004.
> Registro histórico do portão inicial: D-P04 permaneceu posterior à medição. No fechamento,
> foi devolvida para correção e resolvida com residual zero no C14-R1.

---

## Retomada C12 — 2026-09-23

C12 concluiu T025–T036 localmente, sem commit/push. `gems-auditing` 3.3.0 substitui o writer local
num bloco atômico com migração preservadora, read model, providers, ativação e remoções. O primeiro
vermelho ocorreu por API/provider ausentes; o gate final literal passou com 20 testes selecionados
(2 security, 2 core e 16 deploy), zero
falhas/erros/skips, Liquibase e `ddl-auto: validate` verdes. Writer, configuração de contexto,
schema, filtro inclusivo, identidade e correlação reprovaram por mutação e foram restaurados. A
prova real de rollback passou no consumidor; o alvo mutável reside no writer package-private da SDK
já certificado na produtora. C13 concluiu T074 em 7/7 redes e T075 com `clean install` verde: 16
testes em security, 124 em core e 73 em deploy, total 213, zero falhas/erros/skips. O único ajuste
adicional foi autorizado nominalmente em `SolicitacaoRollbackIntegrationTest`, alinhando quatro
operações SQL e duas extrações de identificador ao schema C12 sem alterar produção. T076
também ficou verde: `npm ci`, 26 arquivos/123 testes, build e Prettier, além dos verificadores de
SSO, 21 ações e 13 regras/124 arquivos com zero violações. C13 está 3/3 concluído; próximo lote:
C14. C11/C12/C13 continuam sem commit/push.

C14 concluiu T077: upgrade local 32→33 changesets e segundo boot idempotente, três compose files
válidos, one-shot 2/2, bootstrap real 21 ações/3 grupos com 21→0 novos filhos e gate das oito
rotas 9/9. O inventário D-P04 revelou 63 ocorrências e foi devolvido nominalmente para correção.
C14-R1 corrigiu 57 títulos, duas cores e quatro violações de organização, esvaziou o baseline e
passou 13 regras/126 arquivos, 27 arquivos/125 testes, build e Prettier. C15 refez o analyze com
30/30 FR, 9/9 SC, 79/79 tarefas, zero CRITICAL e zero violação constitucional. O Sample fecha sem
commit/push; o próximo portão humano é a preservação da worktree do Meduc.

---

## Referência

[spec.md](spec.md) · [plan.md](plan.md) · [research.md](research.md) ·
[data-model.md](data-model.md) · [contracts/](contracts/) · [tasks.md](tasks.md) ·
[quickstart.md](quickstart.md) · [analysis.md](analysis.md) · [checklists/](checklists/)

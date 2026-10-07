# Migração `.gems-ai` 1.0.0 → 2.4.0 — o que chegou, e o que **não** entrou

**Data:** 2026-09-14. **Origem:** rodada `008-padronizacao-de-escrita` do Meduc
(`specs/008-padronizacao-de-escrita/`), que mediu as regras contra código de produção, corrigiu as que
mentiam e inverteu a hierarquia entre regra e código. Este repositório recebe o **pacote de regras**;
conformar o código dele é **rodada deste repositório**, e este documento existe para que ela comece
sabendo o tamanho.

> Os números abaixo foram medidos em 2026-09-13 sobre `chore/saneamento-e-dependencias` (`0cc88a7`),
> com o comando indicado em cada linha. Refaça a medição antes de planejar — o código anda.

---

## 1. O que chegou

Atualização da rodada 010: origem Meduc `2.4.0`, 50/50 arquivos com SHA-256 idêntico,
incluindo `.version` e o arquivo oculto de integração. O contrato estimava 48; a árvore
real tem 52 arquivos antes de excluir `conformance-baseline.json` e
`conformance-exceptions.json`. Este documento local não integra a comparação.

| Versão | Evolução recebida | Desvio do sample a conformar |
| :--- | :--- | :--- |
| 2.1.0 | exemplos/templates com formatação compatível e `getDetalhes()` | imports estrela e nomes de teste medidos abaixo |
| 2.2.0 | token local de cor declarado somente em `styles.css` | 70 hex medidos no levantamento; usar prefixo `--app-*` neste projeto |
| 2.3.0 | componentes de campo e senha da SDK; navegação e organização | cópia local de field-error, `commons/`, 7 componentes com Router |
| 2.4.0 | autorização por ação e proibição de cor nomeada ou funcional em componente | catálogo local a migrar; `white`/`rgba` serão medidos pelos gates |

As versões do SDK aprovadas para a conformação são Java **3.1.0** e Angular
**2.1.0**, ambas publicadas. A flag de peer dependencies sai junto com SDK e
`overrides`, nunca antes. A constituição **2.0.0** e os gates serão instalados
nos passos seguintes de S-def; as observações de ausência abaixo registram a base.
Os números definitivos serão registrados em `specs/002-conformidade-gems/medicao-gates.md`.

Os **50 arquivos de origem** (52 incluindo os dois JSON locais, excluídos da cópia) de `.gems-ai/` foram substituídos pela árvore do Meduc, byte a byte, e o `.version`
subiu de `1.0.0` para `2.4.0`. A árvore daqui era idêntica ao `Initial commit` do Meduc — nenhuma
edição local foi perdida, porque não havia nenhuma.

Cada arquivo carrega agora o cabeçalho de procedência (`origem: local`, `precedencia: local-vence`,
`versao: 2.0.0`). O contrato dele é o da `data-model.md` §4 da rodada 008: um `sync` futuro
sobrescreve arquivo de versão **estritamente inferior** à sua e **nunca** sobrescreve `local-vence`.

### As correções de conteúdo

| Arquivo | O que mudou | Por quê |
| :--- | :--- | :--- |
| `rules/backend-java/mapping.md` | reescrito: MapStruct com `@Mapper( config = GemsMappingConfig.class )` e `unmappedTargetPolicy = ERROR`. `ModelMapper` fica **suportado e não recomendado**, sem `@Deprecated` | campo novo não mapeado passa a reprovar a **compilação**, não a execução |
| `rules/backend-java/services.md` | `@Transactional( rollbackFor = ... )` de `org.springframework.transaction.annotation`; a âncora de ordem de campos deixou de ser o `ModelMapper` e passou ao mapper do domínio, condicional | ver §2.2 — aqui isso é uma **troca de anotação**, não só de atributo |
| `rules/backend-java/architecture.md`, `testing.md`, `events.md`, `observability.md` e 6 receitas | desancorados do `ModelMapper` e do `rollbackOn` | eram 26 ocorrências que prescreviam o que a `mapping.md` nova recusa |
| `templates/backend/ServiceTemplate.java` | não injeta mais `ModelMapper` como primeiro campo | idem |
| `rules/frontend-angular/testing.md` | reescrito para **Vitest** (`vi.fn`, `vi.spyOn`); Jasmine deixa de existir na regra | a 1.0.0 prescrevia Jasmine sobre um projeto que já roda Vitest |
| `rules/frontend-angular/stepper.md` | reescrito sobre `GemsWizardComponent` | a 1.0.0 mandava montar à mão o que a SDK entrega |
| `rules/frontend-angular/auth.md` | reescrito: `keycloak-angular` e autorização **por ação**, não por role | a 1.0.0 prescrevia `angular-oauth2-oidc` e o modelo por role, que a constituição do Meduc proíbe |
| `rules/frontend-angular/http-interceptors.md` | reescrito | acompanha `auth.md` |
| as outras 8 regras de `frontend-angular/` | corrigidas: os símbolos importados de `'gems-sdk'` ganharam o prefixo `Gems` que a SDK publica | os nomes sem prefixo não resolvem |
| `rules/conformance-exceptions.md` | **novo**: formato `GEMS-EXCEPTION(<regra>#<seção>): <motivo>; expira AAAA-MM-DD`, quem pode escrever, e a distinção entre exceção e baseline | é a porta autorizada para desviar de uma regra que vence o código |

Além dessas, a árvore traz as evoluções que o Meduc fez entre as rodadas `002` e `007` e que nunca
voltaram para cá: `multi-tenant.md`, `rest-common.md` (envelope uniforme de erro), `validation.md`,
`repositories.md`, `persistence-db.md`, `gems-components.md` (política de senha), `feedback-alerts.md`
e as receitas `error-and-validation` e `custom-input-cva`.

### O que **não** veio de propósito

`conformance-baseline.json` e `conformance-exceptions.json` são medições do **código do Meduc** —
baseline congelado dos gates dele e inventário das exceções dele. Este repositório gera os seus quando
ligar os gates (§2.5).

---

## 2. O que não entrou — a rodada deste repositório

### 2.1 Conformar os 118 arquivos Java

`Get-ChildItem backend -Recurse -Filter *.java | ? FullName -notmatch '\\target\\'` → **118**.

Nenhum deles foi tocado. O que a medição desta rodada indica que a conformação vai encontrar:

| Medida | Valor | Comando |
| :--- | ---: | :--- |
| Arquivos que citam `ModelMapper` | **9** | `Select-String '\bModelMapper\b' -List` |
| Ocorrências do identificador `ModelMapper` | **42** | `Select-String '\bModelMapper\b'` (linhas) |
| Imports `org.modelmapper` | **6** | `Select-String 'import org\.modelmapper'` |
| Chamadas `modelMapper.map(` | **3** | `Select-String 'modelMapper\.map\('` |
| Uso de `org.mapstruct` | **0** | — |
| Repositories | **11** | `*Repository.java` |
| … que estendem `BaseCustomJpaRepository` | **8** | 3 fora da tétrade que `repositories.md` descreve |
| Controllers que estendem `BaseController` | **3 de 5** | `extends BaseController` |

> O levantamento da 008 contava **37 usos** de `ModelMapper` com outro critério de contagem; os 42
> acima são linhas com o identificador. A ordem de grandeza é a mesma, e a quantidade de **arquivos**
> — 9 — é o número que dimensiona o trabalho.

O que a conformação faz, na ordem que a `mapping.md` 2.0.0 descreve: um `@Mapper( config =
GemsMappingConfig.class )` por domínio, `unmappedTargetPolicy = ERROR`, e o `ModelMapper` sai do
construtor da service. **Não** é obrigatório remover `gems-model-mapper` do `pom.xml`: a regra o mantém
suportado. É obrigatório não o prescrever para código novo.

### 2.2 `@Transactional`: aqui é troca de anotação, não de atributo

`Select-String 'import .*\.Transactional;'` → **2× `jakarta.transaction.Transactional`**, e
`Select-String 'rollbackOn'` → **14** ocorrências.

No Meduc o problema era o inverso: o código importava a anotação do **Spring** e a regra 1.0.0
prescrevia `rollbackOn`, atributo que só existe na de **Jakarta** — a regra estava errada para o
código. Aqui o código importa a de Jakarta, então os 14 `rollbackOn` **estão certos para o import
que usam**. A `services.md` 2.0.0 prescreve `org.springframework.transaction.annotation.Transactional`
com `rollbackFor`, porque é a que o Princípio IV da constituição do Meduc passou a citar depois da
correção (R-00 da 008). Conformar é trocar o import **e** o atributo nas 14 ocorrências, nos 2
arquivos — e é uma mudança de comportamento transacional, não cosmética: a anotação de Jakarta é
reconhecida pelo Spring, mas a propagação dela é por `TxType` e ela não expõe `isolation` nem
`readOnly`. Leia os dois arquivos antes.

### 2.3 Subir `gems.version` de `2.0.0` para `3.0.0`

`backend/pom.xml`: `<gems.version>2.0.0</gems.version>`. A `3.0.0` está no registry desde
2026-09-11 (rodada `006` do Meduc). Ela trouxe módulos novos — `gems-mapstruct`, `gems-auditing`,
`gems-jpa-multi-tenant` — e mudou o `gems-exception` (envelope uniforme e handlers 403/400/502) e
o `gems-security-authorization` (catálogo derivado de enum). O `rest-common.md` e a `mapping.md`
2.0.0 já assumem a `3.0.0`; enquanto o `pom.xml` fica na `2.0.0`, essas duas regras descrevem
mecanismos que **este classpath não tem**. A subida é a primeira coisa a fazer, antes de conformar
qualquer arquivo — a rodada `007` do Meduc percorreu exatamente esse caminho e os achados dela
(`specs/007-emenda-constitucional-backend/`) valem aqui, em especial a armadilha do
`annotationProcessorPaths`, que **substitui** o classpath de processadores e deixa o mapper sem gerar.

No frontend, `@gabriel-mdias/angular-gems-sdk` está em `^1.1.0`; a `2.0.0` é a que dispensa
`--legacy-peer-deps` e a que `auth.md` e `stepper.md` 2.0.0 descrevem.

### 2.4 Passagens das regras que falam do Meduc, e que este repositório precisa localizar

A cópia foi literal, e algumas regras carregam o contexto de onde foram corrigidas:

| Onde | O que diz | O que fazer aqui |
| :--- | :--- | :--- |
| `rules/backend-java/architecture.md` §"No Meduc" | reator de **cinco** módulos (`meduc-model`, `-security`, `-events`, `-core`, `-deploy`) e pacote raiz `br.com.gems.meduc` | este molde tem **cinco** módulos `app-*`; a regra continua valendo no que é invariante (só o executável tem ambiente e migrations; listener com regra de negócio fica no core) |
| `rules/backend-java/multi-tenant.md` | descreve o Meduc multitenant por organização, com schema `tenant_<alias>` | este molde é **single-tenant**: a regra é referência para quem estender, não obrigação |
| `rules/frontend-angular/gems-components.md` §"app-password-input (override local Meduc)" e `recipes/custom-input-cva-end-to-end.md` | apontam para `shared/forms/password-input` do Meduc | o componente não existe aqui; use `gems-input-password` da SDK `2.0.0` com `enforcePolicy`, `showCriteria` e `matchControl` |
| `rules/frontend-angular/testing.md` | os exemplos usam `OrganizacaoStore` e `appName === 'Meduc'` | exemplos, não prescrição — troque pelo domínio de referência daqui |
| `rules/conformance-exceptions.md` §"Inventário" | cita `backend/meduc-deploy/src/test/resources/archunit-baseline.txt` | o caminho aqui será o do módulo executável deste repositório, quando o gate existir |

### 2.5 Gates: nenhum existe aqui

`backend/pom.xml` não declara Spotless, Checkstyle, ArchUnit nem JavaParser;
`frontend/scripts/verify-conventions.mjs` não existe. As regras chegaram; **nada as mede**. A
implantação no Meduc foi por catraca — baseline congelado que só encolhe — e o contrato de cada gate
(invocação, mensagem de reprovação, prova por mutação) está em
`specs/008-padronizacao-de-escrita/contracts/gates.md` do Meduc. A ordem que funcionou lá: medir
com os gates desligados, formatar de uma vez em commit isolado (com o SHA em
`.git-blame-ignore-revs`), e só então ligar os gates de conteúdo com o baseline do que sobrou.

### 2.6 Autoridade: a constituição daqui ainda diz que o código vence

`.specify/memory/constitution.md` está em **1.0.0**, e o Princípio que rege divergência ainda diz que,
quando spec e código divergem, **o código vence** — a hierarquia que a 008 inverteu no Meduc
(constituição 2.4.0: regra de escrita vence código consolidado; desvio exige exceção escrita; "código
vence documentação" fica só para specs e documentos de negócio). As regras 2.0.0 declaram
`local-vence` e o formato de exceção, mas **quem decide se elas vencem o código aqui é a
constituição deste repositório**, e isso é uma emenda, não um `sync`. Sem ela, a conformação do §2.1
é higiene; com ela, é obrigação. A ordem que evitou retrabalho no Meduc foi: emenda primeiro, regras
verdadeiras depois, código por último — conformar contra regra que ainda mente é trabalho jogado fora.

---

## 3. Ordem sugerida para a rodada

1. Emenda constitucional (§2.6) — decisão do product owner.
2. `gems.version` `3.1.0` e SDK Angular `2.1.0` (§2.3), com o reator verde **antes** de tocar código.
3. Localizar as passagens do §2.4 — são edições de regra, e a regra agora tem procedência.
4. Medir com os gates desligados, formatar, ligar os gates com baseline (§2.5).
5. `@Transactional` (§2.2) e `ModelMapper` → MapStruct (§2.1), um domínio por lote, cada lote com
   o reator verde e ao menos uma mutação confirmada e restaurada.

O que não está aqui não foi medido. O que está foi medido uma vez, na data do cabeçalho.

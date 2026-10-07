# Entrada da rodada 002 — conformidade GEMS

Preparada em 2026-09-14, T058 da rodada 010 do Meduc. Este arquivo é a cópia histórica
integral do levantamento medido e das sete decisões, conforme contrato §2 READ.
Origem: Meduc `specs/programa-estabilizacao/levantamento-sample-e-dependencias.md`.
A tabela original descreve a base anterior; estado atual das definições e dívida está em
[medicao-gates.md](medicao-gates.md). Regras 2.4.0, constituição 2.0.0, gates em reprovação.
Java 3.1.0 e Angular 2.1.0 são os destinos aprovados; domínio ainda não conformado.
Assets silent check-sso instalados em S-def pelo orquestrador, ajuste aprovado na §12 da
010. Essa contribuição deve constar em OK-1; não é implementação da prova S-codex.

A próxima invocação começa pela fase 1 da skill rodada-sdd. Não existe portão de
planejamento desta rodada; nenhum código de conformação está liberado antes da fase 2.

---

# Levantamento — conformação do `sample-single-tenant-workspace` e alinhamento de dependências

**Medido em**: 2026-09-13 · **Por**: Claude, a pedido do product owner · **Sobre**:
`sample-single-tenant-workspace` na branch `chore/saneamento-e-dependencias` em `057bfc7` (1 commit à
frente de `origin/main` `fc53e81`, 2 atrás — a PR #11 já foi mergeada), e `meduc-workspace` em
`main` `0cb8845` para a metade de dependências.

> **Por que este documento existe.** O bloco O da `008` levou ao sample as regras `.gems-ai` 2.0.0 e
> o `MIGRACAO-2.0.0.md`, e declarou fora de escopo conformar o código dele — "é rodada do repositório
> dele". A pergunta do product owner em 2026-09-13 foi direta: *um projeto que nasça do sample
> responde no padrão?* A resposta medida abaixo é **não**: ele tem as regras e um código que as
> contradiz, sob uma constituição que manda o agente copiar o código. Este é o levantamento da rodada
> que fecha isso, no mesmo formato dos das rodadas `007`–`009` — cada item com evidência em
> `arquivo:linha` e decisão em aberto. Ele também cobre, a pedido, a revisão das **15 PRs do
> Dependabot** abertas nos dois repositórios, para que o alinhamento de dependências seja um só.
>
> **Onde a rodada vive.** Os commits caem no repositório do sample (como os da `006` caíram nas
> SDKs), e os artefatos SDD nascem lá, em `specs/002-*/` — o sample tem só a `001`. O ledger do
> programa acompanha, como fez com a `006`. Não há renumeração: `010` continua sendo a habilitação de
> agentes e `011`–`013` as features do Meduc.

---

## 0. O diagnóstico em uma tabela

| Aspecto | Sample hoje | Meduc hoje | Evidência no sample |
| :--- | :--- | :--- | :--- |
| Regras `.gems-ai/` | **2.0.0** | 2.1.0 | `.gems-ai/.version`; **26 arquivos divergem** por hash (§S-01) |
| Constituição | **1.0.0 — o código vence** | 2.4.0 — as regras vencem | `.specify/memory/constitution.md:292`; `CODEX.md:14` |
| `gems.version` (Java) | **2.0.0** | 3.0.0 | `backend/pom.xml` |
| SDK Angular | **`^1.1.0`** | `^1.1.0` (a `009` sobe para 2.0.0) | `frontend/package.json` |
| Gates de conformidade | **zero** — nenhum Spotless, Checkstyle, ArchUnit, gate de fonte, `verify-conventions` | 6 gates no build | `backend/pom.xml` sem os plugins; `package.json` `test=ng test` sem `verify-*`; `ci.yml` só reator, `npm test`, `build`, `prettier` |
| Java | 94 main + 24 test = **118 arquivos** | 84 classes, 142 provas | §S-03 a §S-09 |
| Frontend | 85 `.ts`, 17 componentes, 21 specs, 4 features | 11 componentes, 14 specs, 1 feature | §S-10 a §S-14 |

O que o sample **já** tem certo, e que vale registrar para não ser "conformado" de novo: Vitest (20
specs importam de `'vitest'`, zero Jasmine); zero `*ngIf`/`*ngFor`, `@Input()`, `BehaviorSubject`,
`HttpClient` em componente; zero import invertido `core/shared → features`; 3 de 5 controllers em
`BaseController`; 8 de 11 repositories no fragmento `BaseCustomJpaRepository`; `appendFilters` em 3
arquivos e zero `Specification` inline; 5 `@Operation` de OpenAPI; a formatação `( x )` já é
majoritária (1754 contra 109 sem o espaço interno). A conformação é menor do que "118 arquivos"
sugere — mas o que falta é o que muda comportamento.

---

## Grupo S — o sample como molde

### S-01 · Regras em 2.0.0, com os defeitos que a SC-011 mediu

**Evidência.** `.gems-ai/.version` = `2.0.0`. Comparação por hash com o Meduc (2.1.0): **26
arquivos divergem** — `.version`, 14 regras de `backend-java/` (`architecture`, `controllers`,
`events`, `mapping`, `multi-tenant`, `observability`, `openapi`, `persistence-db`, `repositories`,
`security`, `services`, `storage-s3`, `testing`, `validation`), 6 receitas e os 5 templates de
backend. Só no sample: `MIGRACAO-2.0.0.md`. Só no Meduc: os 2 JSON de baseline/inventário, que são
medições de código e não vão.

**O que a 2.0.0 ainda ensina errado** (fase 4 da `008`, `prova-sc011/README.md`): exemplos de
anotação `@Anotacao( ... )` que o `eclipse-formatter.xml` rejeita, `getErrors()` inexistente na
`BusinessException` 3.0.0, `multi-tenant.md:11` "MECANISMO AINDA NÃO IMPLEMENTADO", 5 templates
com star import e `RepositoryTemplate` sem `JpaRepository`. A receita `setup-frontend-sdk.md:33-38`
ainda manda `npm install gems-sdk --legacy-peer-deps` e `angular-oauth2-oidc`.

**O que fecha.** Propagar os 26 arquivos 2.1.0 (hash a hash, como o bloco O fez) e atualizar o
`MIGRACAO-2.0.0.md` para o que a 2.1.0 mudou. É o item que o ledger já dava à `010`.

### S-02 · Constituição 1.0.0: o código vence, e o agente tem licença para copiar o código errado

**Evidência.** `.specify/memory/constitution.md:292` — "**o código vence** e a divergência é
registrada no spec"; `CODEX.md:14` — "Código vence somente documentos…". Nenhum arquivo de contexto
do sample (`AGENTS.md`, `CLAUDE.md`, `CODEX.md`, `GEMINI.md`, os de camada) declara que as regras
`.gems-ai` vencem o código consolidado, nem conhece `GEMS-EXCEPTION(...)`.

**Por que importa mais que qualquer item de código.** Com S-03 a S-09 de pé, um agente que leia
`services.md` 2.1.0 (`rollbackFor` do Spring) e `ParceiroService.java` (`rollbackOn` do Jakarta) tem
autorização escrita para seguir o código. É o defeito nº 1 que originou o programa, armado no molde.

**O que fecha.** A emenda que a `008` fez no Meduc (bloco J: 2.3.0 → 2.4.0 — inversão de
hierarquia, `rollbackFor`, autorização da exceção escrita), propagada aos arquivos de contexto na
mesma alteração, com `verificar-contexto-agentes.ps1` verde. O sample sobe de 1.0.0 direto para a
forma da 2.4.0; o que da 2.1.0–2.3.0 do Meduc é específico dele (multitenancy, trilha em
`tenant_<alias>`) não entra.

### S-03 · `gems.version` 2.0.0: as regras citam módulos que o classpath não tem

**Evidência.** `backend/pom.xml` — `<gems.version>2.0.0</gems.version>`, 10 módulos declarados
(`rest-common`, `validation`, `exception`, `observability`, `utils`, `model-mapper`, `jpa`, `aws`,
`aws-web`, `openapi`). Imports de SDK em uso: `exception` 17 arquivos, `utils` 17, `mapper` 9, `jpa`
7, `rest` 1, `aws` 1. A `architecture.md` 2.1.0 e `mapping.md` prescrevem `gems-mapstruct` e
`gems-auditing`, que **só existem na 3.0.0**.

**O que fecha.** Subir para `3.0.0`, no modelo do C-09 da `007` (bump sem uma linha de código e
`dependency:tree` refeito). O 3.0.0 é MAJOR em `gems-jpa-multi-tenant`, que o sample **não usa** —
o bump deve ser aditivo aqui, e é para provar, não supor.

### S-04 · `ModelMapper` em 9 arquivos; zero MapStruct

**Evidência.** `ModelMapper` em 9 arquivos, 6 deles services: `LocalidadeService`,
`ContatoService`, `EnderecoService`, `PessoaService`, `ParceiroService`, `SolicitacaoService`
(`app-core/.../service/`). `@Mapper` do MapStruct: **0**. `mapping.md` 2.1.0 prescreve
`@Mapper( config = GemsMappingConfig.class )` com `unmappedTargetPolicy = ERROR`; `ModelMapper` é
**suportado e não recomendado** (decisão 9 da `006`, sem `@Deprecated`).

**Decisão em aberto.** Conformar ao MapStruct (é o C-07 da `007`, provado: campo novo reprova a
compilação) **ou** manter `ModelMapper` com a razão escrita. Molde que ensina o não-recomendado
ensina errado — a recomendação é migrar. Raio: 6 services e os DTOs que eles montam.

### S-05 · `@Transactional` do Jakarta com `rollbackOn` — a única mudança de comportamento

**Evidência.** `import jakarta.transaction.Transactional` em 2 arquivos, 14 ocorrências de
`rollbackOn`: `ParceiroService.java` (5) e `SolicitacaoService.java` (9); `backend/AGENTS.md` cita o
atributo (1). Zero `org.springframework.transaction.annotation.Transactional`.

**Atenção (O-A do bloco O).** No sample o atributo **está certo** para a anotação que ele usa. A
conformação à `services.md` 2.1.0 é **troca de anotação** (Jakarta → Spring), com efeito
transacional: `rollbackOn` vira `rollbackFor`, e a semântica de propagação e de exceção checada muda.
É mudança de comportamento observável — **decisão do product owner** (regra 5 do ledger), e a
mutação do bloco tem de provar o rollback nas duas exceções que os services lançam.

### S-06 · Controllers, handlers e o envelope de erro

**Evidência.** 5 `@RestController`: `AuditoriaController`, `ParceiroController`,
`SolicitacaoController` estendem `BaseController`; **`LocalidadeController` e `CepController` não**.
Princípio V do Meduc (2.3.0) tornou a extensão incondicional. 4 `@RestControllerAdvice` locais —
`AccessDeniedExceptionHandler`, `IdentityProviderExceptionHandler`,
`RequisicaoInvalidaExceptionHandler` (`app-security`) e `ProvedorExternoExceptionHandler`
(`app-core/commons`) — e **zero** uso de `ExceptionResponseDTO`: nenhum handler escreve o envelope
uniforme do `gems-exception` 3.0.0.

**O que fecha.** Os 2 controllers estendem; os handlers passam ao envelope no modelo do C-05 +
D-04 da `007`. Os que preservam status que a SDK mudaria (403, 404/400, 502) ficam **com a razão
medida** — é a mesma dívida G6 da `010`, e a decisão de cobri-la na SDK vale para os dois
consumidores de uma vez.

### S-07 · Repositories, star imports e nomes de teste — o que os gates da `008` reprovariam

**Evidência.**
- Repositories: 3 de 11 fora do fragmento — `CampoAlteradoRepository`, `EstadoRepository`,
  `EtapaSolicitacaoRepository` (L da `008` pôs os 5 do Meduc por uniformidade).
- Star import: **11 arquivos, 20 linhas** — 9 entidades de `app-model` (`Estado`, `Pais`,
  `Contato`, `Endereco`, `Pessoa`, `Parceiro`, `Usuario`, `EtapaSolicitacao`, `Solicitacao`) e 2
  controllers. `AvoidStarImport` do Checkstyle reprova todos.
- Métodos de teste: **179 `@Test`, 0 na convenção `deveX_quandoY`**. O gate de fonte da `008`
  reprova os 179. (Na `007`, F-03 renomeou 101 no Meduc.)
- Formatação: 109 linhas `(x` sem o espaço interno que o `eclipse-formatter.xml` impõe —
  `spotless:apply` resolve; o que importa é medir antes, como o P da `008` fez (N-01).

**O que fecha.** É o bloco L da `008`, repetido: ligar os gates em modo relatório, medir, conformar,
ligar em modo reprovação com baseline **vazio**.

### S-08 · Auditoria local completa, onde a SDK 3.0.0 tem `gems-auditing`

**Evidência.** Domínio `auditoria` inteiro em `app-core` (controller, `AuditoriaInterceptor`,
`AuditoriaGravador`, `RegistroPendente`, 2 repositories, service) e 6 classes em `app-model`
(`RegistroAuditoria`, `EntidadeAuditavel`, `OperacaoAuditoria`, DTOs). `@Auditable`/`@SensitiveField`:
**0**. É a implementação que a `006` promoveu para `gems-auditing` e a `007` substituiu no Meduc
(C-03, 7 classes locais removidas).

**Decisão em aberto — a maior da rodada.** Substituir segue a seção "Adoção de Módulo da SDK GEMS"
da constituição do Meduc (módulo publicado, alcance medido, garantia provada por teste, classe local
saindo na mesma alteração). O sample tem **tela de auditoria** (`AuditoriaController` com
`hasRole('CONSULTAR_AUDITORIA')`), então a substituição precisa preservar a consulta — e a divergência
de DDL que o C-03 achou (`CD_ACTOR_TYPE`) vai aparecer aqui também. Alternativa: manter a
implementação local com razão escrita, aceitando que o molde ensina a reimplementar o que a SDK
entrega — contra a regra 5 do `CLAUDE.md` do Meduc.

### S-09 · Keycloak Admin direto, onde a SDK tem `gems-keycloak-admin`

**Evidência.** `org.keycloak.admin.client` importado em 4 arquivos de `app-security`
(`KeycloakConfig`, `KeycloakGroupService`, `KeycloakRoleService`, `KeycloakUserService`);
`br.com.gems.keycloak`: 0. É o C-01 da `007` (4 classes locais removidas no Meduc, boot recusado sem
`clientSecret` virou teste).

**Autorização.** `hasRole('CONSULTAR_PARCEIRO')` em 7 arquivos, 13 `@PreAuthorize` — o nome é de
**ação**, e o sample tem `AutorizacaoBootstrap` e `CoberturaPreAuthorizeTest`. Não é o modelo por
nível que a constituição do Meduc proíbe; é o modelo por ação escrito com a primitiva `hasRole`.
Substituir por `gems-security-authorization` (catálogo derivado de enum, JSON do frontend gerado —
C-02 da `007`) é decisão de adoção, não correção. ⚠️ **Divergência nas regras do Meduc, achada
aqui**: `.gems-ai/rules/backend-java/security.md:58-67` (2.1.0) ainda tem a seção "Autorização por
Role" com `hasRole('ADMINISTRADOR')`, enquanto `architecture.md:145` e `frontend-angular/auth.md`
§3 prescrevem ação. A `008` corrigiu `auth.md` do frontend e não alcançou `security.md` do backend.
É item de regra, do Meduc, e entra na `010` (ou na propagação de S-01, para não levar a regra errada
uma segunda vez).

### S-10 · SDK Angular `^1.1.0`, `--legacy-peer-deps` em 13 lugares — e o achado que atinge a `009`

**Evidência.** `frontend/package.json` `@gabriel-mdias/angular-gems-sdk=^1.1.0`. A flag está em 13
lugares do sample (`git grep`): `ci.yml:129-132`, `frontend/Dockerfile:33-35`, `README.md:31,148`,
`CLAUDE.md:49`, `constitution.md:343`, `inicializar-projeto.ps1:293`, `dependabot.yml:43`, receita
`setup-frontend-sdk.md:33-38`, `MIGRACAO-2.0.0.md:108`, skill `verificacao-e-rollback.md:8`,
`specs/001-*/quickstart.md:22`.

⚠️ **A flag não cai só com a SDK 2.0.0.** `keycloak-angular@16.1.0` declara peer
`@angular/core ^18` (`npm view`), e os dois projetos estão em Angular 22. A `006`/S7 derrubou a flag
no repositório da SDK com `overrides` no `keycloak-angular` (`Angular-GEMS-SDK/package.json`:
`overrides.keycloak-angular.@angular/*: $@angular/*`) — o consumidor precisa do mesmo `overrides`, ou
de `keycloak-angular@22`, **a única versão com peer `^22`**. A 22 removeu o `KeycloakService` que
`core/auth/auth.service.ts:24` do Meduc injeta, que `auth.md` 2.1.0 (`:60-69`) prescreve e que o
`provideGemsKeycloak` da SDK consome (`dependencies.keycloak-angular ^16.0.0`). Migrar é rodada da
SDK primeiro. **Conclusão para a `009` (E-10)**: o caminho é o `overrides`, e a §10 do roteiro deve
registrá-lo — o risco "a subida da SDK pode mudar o comportamento do `keycloak-angular` sem a flag"
tem forma concreta agora.

**O que fecha.** SDK `^2.0.0`, `overrides` no `package.json`, `npm ci` sem a flag nos 13 lugares.

### S-11 · Frontend: os mesmos itens E da `009`, em número maior

| Item (espelho da `009`) | Sample | Evidência |
| :--- | :--- | :--- |
| E-02 `commons/` órfã | **10 arquivos** (`data-br`, `moeda-br`, `page-response`, `password.validator`, `table-sort`, `gems-table-labels` e specs) | `frontend/src/app/commons/` |
| E-03 `Router` em componente | **7** (`app.ts`, `acesso-negado`, `parceiro-form`, `step-dados`, `parceiro-list`, `solicitacao-form`, `solicitacao-list`) | `git grep -l "inject(Router)"` |
| E-05 componentes locais | `shared/forms/field-error` presente; `password-input` **não** (o sample tem `password.validator.ts` em `commons/`) | `frontend/src/app/shared/forms/` |
| E-05 proibição a inverter | `frontend/AGENTS.md:18` — "usa `app-field-error`, nunca `gems-field-error`" | — |
| E-06 paleta | **70 hex** em `styles.css` (Meduc: 35) | regex `#[0-9a-fA-F]{3,8}` |
| `hasRole` no frontend | **12 arquivos**; `isAuthorized`: 0 | `auth.md` 2.1.0 §3 — "nunca `hasRole`" |
| `standalone: true` residual | 1 | `verify-conventions.mjs` reprova (N-E da `008`) |
| `GemsNavigationService` | 0 | — |

**O que fecha.** O que a `009` fizer no Meduc, o sample repete — e é por isso que esta rodada vem
**depois** da `009`: o Meduc entrega a forma provada, o sample copia. O `hasRole` no frontend é o
item que a `009` não tem (o Meduc já usa `isAuthorized`), e pesa: 12 arquivos.

### S-12 · Gates: nenhum, e o CI não os conhece

**Evidência.** `backend/pom.xml` sem Spotless, Checkstyle, ArchUnit, JavaParser, JaCoCo ou enforcer
(`Select-String` nos 6 poms: zero); `frontend/package.json` `scripts.test = "ng test"`, sem
`verify-conventions`, `verify-authorization-actions` ou `verify-silent-check-sso`; `frontend/scripts/`
não existe; `.github/workflows/ci.yml` roda reator, `npm test`, `npm run build` e `prettier`.
`conformance-baseline.json` e `conformance-exceptions.json` não existem, de propósito.

**O que fecha.** O bloco N da `008` inteiro, com os arquivos de configuração copiados do Meduc
(`eclipse-formatter.xml`, `checkstyle.xml`, `suppressions.xml`, `archunit.properties`, os `verify-*.mjs`)
e os **baselines nascendo do código conformado** — vazios no Java; no frontend, o que restar dos 70
hex depois de tokenizar. Cada gate provado por mutação, como lá (23 mutações).

### S-13 · Divergência de contexto: 1 commit parado e a branch atrás de `main`

**Evidência.** `git status -sb`: `chore/saneamento-e-dependencias...origin/chore/saneamento-e-dependencias
[ahead 1]`; `origin/main` em `fc53e81` já contém a PR #11 (que mergeou `0cc88a7` e `2de05a0`), e a
branch local está 2 commits atrás dela. O commit à frente é `057bfc7` — as regras 2.0.0 do bloco O.

**O que fecha.** Antes de qualquer bloco: `push`, PR de `057bfc7` e branch nova da rodada a partir
de `main`. É a lição da `006` (PRs mergeadas sem os commits finais), e é ato do product owner.

### S-14 · O que o sample tem e o Meduc não — e não é item

`app-events` (6 arquivos), `gems-aws`/`gems-aws-web` (upload S3), `localidade`/`normalizacao` (CEP,
endereço, pessoa, contato), o domínio `solicitacao` com etapas. São o **conteúdo** do molde, e
ficam. O que esta rodada muda é a forma e os mecanismos, não o que o molde demonstra.

---

## Grupo D — alinhamento de dependências (Dependabot, os dois repositórios)

**Contexto medido.** Os dois `dependabot.yml` são **idênticos** (maven semanal com 5 PRs, npm
semanal com 5 e grupo `angular` minor/patch, actions semanal com 3; ignora major de Spring Boot,
`br.com.gems:*`, major de `@angular/*`, a SDK Angular e major de `typescript`). As labels que eles
declaram (`dependencias`, `backend`, `frontend`, `ci`) **não existem em nenhum dos dois
repositórios** — o Dependabot comenta o erro em toda PR (`meduc-workspace` #1, #5, #6…). No Meduc,
as PRs #1–#8 foram **fechadas à mão em 2026-09-08** sem merge e sem `@dependabot ignore`, então
voltam a cada release; no sample, o commit `0cc88a7` absorveu 9 PRs em bloco (Actions, keycloak
26.0.12, Angular 22.1.5) e deixou de fora o TypeScript 7 pelo peer estreito do `compiler-cli`. Os
`package.json` de frontend dos dois repositórios são **idênticos, dependência a dependência**.

Todas as 15 PRs abertas estão `MERGEABLE` e com os 3 jobs do CI **verdes** (as duas do Meduc que
estavam `IN_PROGRESS` — ArchUnit 1.5.0 e Checkstyle 14.1.0 — concluíram `SUCCESS` durante esta
medição).

### D-01 · `sample-single-tenant-workspace` — 9 PRs abertas

| PR | Dependência | De → para | Tipo | Recomendação | Por quê |
| :-- | :--- | :--- | :--- | :--- | :--- |
| #6 | `keycloak-admin-client` | 26.0.9 → 26.0.12 | patch | **Fechar** | Já aplicada em `0cc88a7`: o `pom.xml` está em `26.0.12`. PR órfã |
| #13 | `keycloak-angular` | 16.1.0 → 22.0.0 | **major** | **`@dependabot ignore this major version`** | S-10: a 22 removeu o `KeycloakService` que o Meduc, a regra `auth.md` e o `provideGemsKeycloak` da SDK usam. Só depois da SDK migrar — e aí é rodada, não PR |
| #14 | `appleboy/ssh-action` | 1.0.3 → 1.2.5 | minor | **Mergear** | CI verde; action de deploy, sem mudança de entrada |
| #15 | `docker/build-push-action` | 6 → 7 | major | **Mergear** | Release notes sem breaking para o uso do `build-images.yml`; CI verde |
| #16 | `prettier` | 3.9.0 → 3.9.6 | patch | **Mergear** | O CI roda `prettier --check` e passou — a formatação não mudou |
| #17 | `jsdom` | 28.1.0 → 30.0.1 | major (dev) | **Mergear** | Só o ambiente de teste do Vitest; 118 provas verdes no CI. Notas citam breaking changes de API que o projeto não chama |
| #18 | `docker/metadata-action` | 5 → 6 | major | **Mergear** | Como #15 |
| #19 | `vitest` | 4.1.11 → 5.0.0 | **major** (dev) | **Mergear com o Meduc junto** | 🚨 breaking changes declaradas; CI verde nos dois repositórios. Subir num só e não no outro desalinha o `package.json` que hoje é idêntico — e a `testing.md` 2.1.0 cita a API do Vitest |
| #20 | grupo `angular` | 22.1.5 → 22.1.6 (+ cli 22.0.4 → 22.1.7) | patch/minor | **Mergear** | É o grupo funcionando como o `dependabot.yml` desenhou; CI verde |

### D-02 · `meduc-workspace` — 6 PRs abertas

| PR | Dependência | De → para | Tipo | Recomendação | Por quê |
| :-- | :--- | :--- | :--- | :--- | :--- |
| #22 | `checkstyle` | 10.21.4 → 14.1.0 | **major** | **Mergear, com uma prova a mais** | CI verde = zero violação nova no código conformado. Mas o gate N-03 existe para reprovar: antes do merge, repetir **uma** mutação de `ParenPad` e uma de `AvoidStarImport` local (as da `008`, T066) e confirmar vermelho na 14.1.0 — major de Checkstyle já renomeou e removeu checks |
| #23 | `spotless-maven-plugin` | 2.44.4 → 3.10.2 | **major** | **Mergear, com uma prova a mais** | CI verde = `spotless:check` sem diff, o `eclipse-formatter.xml` sobreviveu. Repetir a mutação de `join_wrapped_lines` (N-01-B) na 3.x — o índice incremental do Spotless quase deixou passar na `008` |
| #24 | `archunit-junit5` | 1.3.0 → 1.5.0 | minor | **Mergear e reavaliar `archunit.properties`** | CI verde sob JDK 21. A 1.5.0 pode ler `major version 70`; se ler, o comentário de N-B em `meduc-deploy/src/test/resources/archunit.properties` fica datado — manter `resolveMissingDependenciesFromClassPath=false` (é custo sem retorno de qualquer forma) e corrigir o comentário |
| #25 | `maven-enforcer-plugin` | 3.5.0 → 3.6.3 | minor | **Mergear** | O enforcer é o do FR-017 (JDK `[21,)`); CI verde |
| #26 | `javaparser-core` | 3.26.2 → 3.28.2 | minor | **Mergear e repetir 1 mutação do gate de fonte** | É o parser do gate N-05 (8 mutações na `008`); a AST não muda entre minors, mas o gate é o que garante `deveX_quandoY` e a ordem de campos — uma mutação basta |
| #27 | grupo `angular` | 22.1.5 → 22.1.6 (+ cli 22.1.7) | patch/minor | **Mergear** | Mesmo conteúdo da #20 do sample; CI verde |

**Já fechadas no Meduc e que voltarão** (#1–#8 de 2026-09-08): `jsdom` 30, `prettier` 3.9.6,
`keycloak-angular` 22, `vitest` 5, `metadata-action` 6, `ssh-action` 1.2.5, `build-push-action` 7.
O Dependabot **reabre** PR fechada à mão quando sai versão nova; a decisão de cada uma deve ser a
mesma da tabela D-01 — e `keycloak-angular` precisa do `ignore` explícito, ou volta toda semana.

### D-03 · O que "alinhamento" significa aqui — três correções de configuração

1. **Criar as 4 labels** nos dois repositórios (`dependencias`, `backend`, `frontend`, `ci`) — ou
   remover `labels:` do `dependabot.yml`. Hoje toda PR nasce com um comentário de erro.
2. **`ignore` explícito de `keycloak-angular` major** nos dois `dependabot.yml`, com o motivo de
   S-10 escrito no comentário — e o `@dependabot ignore this major version` na #13.
3. **Rever o comentário do `ignore` de `@angular/*` major** (`dependabot.yml:43-44` nos dois): ele
   diz que a flag existe "por causa do descompasso entre a SDK (peer ^20) e o Angular 22". Com a SDK
   2.0.0 o descompasso passa a ser o `keycloak-angular`; o comentário fica errado no dia em que a
   `009` fechar E-10.

Depois disso, os dois `package.json` de frontend e os dois `pom.xml` (nas dependências que
compartilham: Spring Boot 4.1.1, Java 21, Lombok, Keycloak 26.0.12) ficam com a **mesma decisão para
a mesma dependência**, que é o alinhamento pedido. As dependências que só um tem (MapStruct,
ArchUnit, JavaParser, Spotless, Checkstyle no Meduc; `gems-aws` no sample) alinham quando S-12 ligar
os gates no sample — a versão copiada é a que o Meduc estiver usando naquele dia.

---

## Decisões do product owner — 2026-09-13

As sete perguntas abaixo foram respondidas no mesmo dia. Elas são **entrada** da rodada do sample e
do bloco D; não são mais questões em aberto.

| # | Ponto | Decisão | O que fecha |
| :-- | :--- | :--- | :--- |
| 1 | Sequência | ~~**Depois da `010`, conduzida pelo Codex**~~ → ✅ **Revertida em 2026-09-14, na fase 1 da `010`: a rodada do sample acontece dentro da `010`** (decisão 8 da §2 do [`roteiro.md`](../010-habilitacao-de-agentes/roteiro.md) dela), com o Codex conduzindo a conformidade sozinho e as definições propagadas antes | A rodada do sample continua sendo a segunda metade da prova final ("ele conduz sozinho?"); muda só o momento — a `010` entrega G6, `security.md`, a propagação e o handoff **e então** aciona o Codex, na mesma rodada |
| 2 | S-05 | **Trocar** `jakarta.transaction.Transactional` pelo `@Transactional` do Spring, com mutação provando o rollback | Mudança de comportamento autorizada; a prova é por teste nas exceções que os 2 services lançam |
| 3 | S-08 | **Adotar `gems-auditing`**, preservando a tela de consulta | Segue "Adoção de Módulo da SDK GEMS"; o domínio `auditoria` local sai na mesma alteração; `CD_ACTOR_TYPE` entra por changeset |
| 4 | S-09 | **Adotar `gems-keycloak-admin` e `gems-security-authorization`** | Espelha C-01 e C-02 da `007`: 4 classes de Keycloak saem, catálogo derivado de enum, JSON do frontend gerado com gate |
| 5 | S-04 | **MapStruct nos 6 services** | Espelha C-07 da `007`; `ModelMapper` sai do molde |
| 6 | D-01/D-02 | **As tabelas valem como decisão** | Executado: ver "Bloco D — executado" abaixo |
| 7 | D-03.1 | **Criar as 4 labels** nos dois repositórios | Criadas em 2026-09-13 (`dependencias`, `backend`, `frontend`, `ci`) |

### Bloco D — executado em 2026-09-13 (Claude, em sessão)

**Mutações antes do merge**, em worktree de `main` `0cb8845` com os 5 bumps aplicados de uma vez —
baseline `clean install` **verde, 142 provas, 0 falhas, 81 s** sob a JVM 26 local:

| Gate | Versão nova | Mutação | Resultado |
| :--- | :--- | :--- | :--- |
| Checkstyle | 10.21.4 → **14.1.0** | `findById( UUID id )` → `findById(UUID id)` e `GestorCreateRequest` → `organizacao.*` em `OrganizacaoService.java` (Spotless desligado para isolar) | 🔴 **3 erros**: `AvoidStarImport` na `:5:47`, `ParenPad` na `:63:49` e `:63:57` — nomeando o arquivo e a linha |
| Spotless | 2.44.4 → **3.10.2** | a mesma linha `(UUID id)` | 🔴 `spotless:check` reprova com o diff `-findById(UUID id)` / `+findById( UUID id )` — o `eclipse-formatter.xml` está ativo na 3.x |
| Gate de fonte (JavaParser) | 3.26.2 → **3.28.2** | `deveReconhecerAClasseComoAuditavel_quandoElaDeclaraAAnotacao` → `reconheceClasseAuditavel` em `AuditPolicyAdoptionTest` | 🔴 `ConvencoesFonteTest` 7/8, a falha nomeia `reconheceClasseAuditavel` e o formato esperado |
| ArchUnit | 1.3.0 → **1.5.0** | `resolveMissingDependenciesFromClassPath=true` sob a JVM 26 | 🟢 **8/8 em 14 s** — a 1.5.0 lê `major version 70`; o comentário de N-B em `archunit.properties` ficou datado e foi corrigido. A propriedade permanece `false`: a resolução segue custo sem retorno |

As três mutações foram restauradas por edição inversa; o worktree foi removido com só o `pom.xml`
alterado.

**Merges no Meduc**: #22, #23, #24, #25 e #27 mergeados em sequência (`2c3d88a` … `29c6b26`); a
**#26** (JavaParser) conflitou por linha vizinha do `pom.xml` (`archunit.version` e
`javaparser.version` são adjacentes), recebeu `@dependabot rebase` e ficou com merge automático
armado depois do CI.

**GitHub no sample**: #6 fechada com o motivo (já aplicada em `0cc88a7`); #13 recebeu
`@dependabot ignore this major version` com o motivo de S-10. As PRs #14–#20 **ficam com o product
owner** — recomendação: mergear #14, #15, #16, #17, #18 e #20; **#19 (`vitest` 5) só quando o
Dependabot reabrir a equivalente no Meduc**, para que os dois subam no mesmo dia.

**Pendente do bloco, do product owner**: `ignore` explícito de `keycloak-angular` major nos dois
`dependabot.yml` e a correção do comentário `:43-44` — entram na `009` (E-10), porque é ela que muda
o motivo da flag.

## Perguntas que o levantamento não podia decidir (respondidas acima)

1. **Onde a rodada entra na sequência.** Depois da `009` e antes da `010`, ou depois da `010`? A
   favor de **depois da `010`**: a `010` fecha G6 (handlers), a regra `security.md` (S-09), a
   propagação 2.1.0 (S-01) e o handoff do Codex — e o sample copia tudo pronto. Contra: até lá o molde
   segue ensinando errado. Recomendação: depois da `010`, e o Codex **conduz** essa rodada como a
   segunda metade da prova final ("ele conduz sozinho?") — o sample é o repositório certo para isso,
   porque errar nele não custa cliente.
2. **S-05** — trocar `jakarta.transaction.Transactional` por `org.springframework...Transactional`
   é mudança de comportamento; autorizada?
3. **S-08** — adotar `gems-auditing` no lugar da auditoria local (com a tela preservada), ou manter
   com razão escrita?
4. **S-09** — adotar `gems-keycloak-admin` e `gems-security-authorization`, ou só o primeiro?
5. **S-04** — MapStruct nos 6 services, ou `ModelMapper` com razão escrita?
6. **D-01/D-02** — as recomendações da tabela valem como decisão? Em particular: `vitest` 5 nos dois
   ao mesmo tempo, e `keycloak-angular` ignorado até a SDK migrar.
7. **D-03.1** — labels criadas ou `labels:` removido?

## Fora do escopo candidato, com destino

- Migrar `keycloak-angular` para 22 (`provideKeycloak` funcional): rodada da SDK Angular, depois
  consumidores — não tem número.
- TypeScript 7: preso ao peer `>=6.0 <6.1` do `@angular/compiler-cli` 22 — entra com o Angular 23.
- Promover `commons/` do sample (`data-br`, `moeda-br`, `password.validator`) à SDK: "sem segundo
  projeto pedindo" (006 §11) — o sample é o segundo projeto; é pergunta para a próxima rodada da SDK,
  não para esta.
- ADACI (`D:\repos\adaci-project`): referência, não alvo (invariante 3). Ninguém mediu o
  `rollbackOn` dele, e este levantamento também não.

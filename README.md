# sample-single-tenant-workspace

Molde de projeto **single-tenant** com a GEMS SDK: uma constituição, um ciclo de trabalho com IA, e
um esqueleto que compila.

Clone, renomeie com um comando, e a primeira rodada já começa com governança, gates, contextos de
agente e um domínio de referência a copiar.

---

## Comece aqui

```powershell
git clone <este-repo> meu-projeto
cd meu-projeto
.\scripts\inicializar-projeto.ps1 -Nome meuprojeto -Descricao "O que o sistema faz"
```

O script troca o nome neutro `app` pelo do seu projeto — módulos Maven, pacotes, classes, realm,
compose, banco, imagens e as ocorrências textuais nos arquivos de governança. Rode-o **uma vez**, com
a árvore limpa; ele se recusa a rodar com alterações não commitadas, para que o `git diff` continue
sendo o caminho de volta. Use `-DryRun` para ver o que mudaria.

Depois, os gates:

```powershell
cd backend
.\mvnw.cmd clean install

cd ..\frontend
npm ci
npm test
npm run build

cd ..\environments\local
docker compose up -d
```

---

## O que vem dentro

### A metodologia

| O quê | Onde | Para quê |
| :--- | :--- | :--- |
| **Constituição** | `.specify/memory/constitution.md` | Nove princípios. Autoridade máxima — prevalece sobre todo arquivo de agente |
| **Ciclo da rodada** | skill `/rodada-sdd` | Cinco fases (Start, Plan, SDD, Homologação, História), com portão humano entre elas |
| **Portão humano** | `roteiro.md` | Doze seções. Nenhuma linha de código antes da §12 assinada |
| **Delegação** | skill `delegacao-de-modelos` | Protocolo pacote/estado, âncora git, verificação na volta, rollback exato |
| **Contextos de agente** | `AGENTS.md` em cascata + adaptadores | Claude, Codex, Gemini, Cursor, Copilot, Kilo — todos apontando para o mesmo núcleo |
| **Regras de código** | `.gems-ai/rules/` e `.gems-ai/recipes/` | 28 regras por camada, 11 receitas ponta a ponta, templates de código |
| **Funil de requisitos** | `utils/documentação do projeto/` | Do levantamento cru até a rodada, com um template por nível |

### O esqueleto

**Backend** — reator Maven de cinco módulos, com a direção das dependências imposta **em tempo de
compilação**: um import na direção errada não compila.

```
app-model      entidades JPA, DTOs, enums          (raiz)
app-security   resource server Keycloak, ações     (raiz)
app-events     somente POJOs de evento             (depende de model)
app-core       regra de negócio                    (model + security + events)
app-deploy     executável, config, Liquibase       (core)
```

**Frontend** — Angular 22 standalone, signals, feature autocontida registrada por `loadChildren`,
Vitest.

**Ambiente** — `docker compose up -d` e nada depois: Postgres 16, Keycloak 26 com realm declarativo
aplicado por um serviço one-shot, e LocalStack.

### O domínio de referência

Dois domínios de exemplo, que existem para serem copiados — e depois removidos:

- **`parceiro`** — o cadastro de referência. CRUD, `POST /search` com projeção por construtor,
  validação acumulada, gravação atômica sem cascade, wizard de quatro passos, e a **autorização de
  campo** da comissão aplicada em leitura, busca e gravação.
- **`solicitacao`** — o domínio de fluxo. Máquina de estados no enum, situação que **nunca** entra
  pelo corpo da requisição, etapas ordenadas, e um evento de domínio consumido pelo outro domínio.

A rodada que os produziu está inteira em [`specs/001-parceiro-e-solicitacao/`](specs/001-parceiro-e-solicitacao/) —
é o exemplo de como uma rodada bem feita se parece neste projeto.

> ⚠️ **Eles saem quando o primeiro domínio real chegar.** É a última cláusula do Princípio VIII:
> código de exemplo é removido **por inteiro** — backend, frontend, testes, changeset e a spec —
> assim que deixar de ser referência ativa.

---

## O que é invariante, e o que se troca

| Invariante — mudar exige emenda à constituição | Substituível — decisão de cada projeto |
| :--- | :--- |
| Spec antes de código, com portão humano | O domínio, óbvio |
| Autorização por **ação**, nunca por nível | Quantos perfis, e o que cada um alcança |
| Situação de fluxo fora do corpo da requisição | Quais são as situações |
| Testes como gate de merge | Qual a meta de cobertura |
| Config de ambiente como código | Qual provedor de identidade, qual nuvem |
| Auditoria automática pela persistência | O que é sigiloso |
| Fronteiras impostas pelo build | Quantos módulos |
| Documentação proporcional ao risco | O formato do documento de negócio |

Trocar a **stack** (banco, IdP, framework de front) é possível e é emenda: com Sync Impact Report,
bump semântico e propagação para os arquivos de agente na mesma alteração.

---

## Stack

- **Backend**: Java 21 + Spring Boot 4.1 (Maven multi-módulo — reator + 5 filhos)
- **Frontend**: Angular 22 standalone, TypeScript 6, Vitest, Prettier (sem ESLint)
- **Identidade**: Keycloak 26 (PKCE + session check-sso)
- **Persistência**: PostgreSQL 16, schemas `public` / `security` / `auditoria`, Liquibase
- **Infra local**: Docker Compose + LocalStack (S3)
- **Dependências privadas**: `br.com.gems:gems-*` (Maven) e `@gabriel-mdias/angular-gems-sdk` (npm)

### Acesso ao registry privado

Sem credencial, as dependências GEMS não instalam.

**Maven** (`~/.m2/settings.xml`) — o `<id>` **precisa** ser `github`, igual ao `<repository>` do POM
agregador. Se divergirem, a credencial não é aplicada e o download falha com 401, que qualquer um
confunde com token errado:

```xml
<server>
  <id>github</id>
  <username>seu-usuario</username>
  <password>seu-token</password>
</server>
```

**NPM** (`frontend/.npmrc`, fora do Git):

```
@gabriel-mdias:registry=https://npm.pkg.github.com
//npm.pkg.github.com/:_authToken=seu-token
```

---

## Restrições operacionais

- No Windows o build é `.\mvnw.cmd`. O script shell `./mvnw` falha na invocação por `powershell.exe`.
- Use `npm ci` para instalar as versões do lockfile. O Angular 22 usa a SDK 2.1.0 e os overrides
  de peers do `keycloak-angular` declarados em `frontend/package.json`.
- **Nunca `docker compose down -v`.** O volume guarda o banco da aplicação **e** o do Keycloak, e as
  senhas dos usuários existem apenas no segundo.
- O Keycloak leva ~80s no primeiro boot. Espere `keycloak-config-cli ran in` nos logs.

---

## Como estender

1. **Abra a rodada**: `/rodada-sdd` — ela descobre em que fase está e para no portão seguinte.
2. **Leia as receitas** de `.gems-ai/recipes/` para o tipo de trabalho (CRUD, busca paginada, evento
   de domínio, wizard, upload).
3. **Espelhe o domínio de referência**: `parceiro` no backend e `features/parceiro` no frontend.
4. **Teste antes** da regra de negócio, e prove a rede nova por **mutação** — quebre o alvo, veja o
   teste falhar, restaure.
5. **Feche pelo gate**: reator completo uma vez, `npm test`, `npm run build`, e a aplicação subindo
   com `ddl-auto: validate`.

## Documentação

| Arquivo | Para quê |
| :--- | :--- |
| `.specify/memory/constitution.md` | Os nove princípios. Leia inteira só em mudança de arquitetura, governança ou ambiente |
| `AGENTS.md` | Regras transversais curtas, e o roteamento para os aninhados |
| `CLAUDE.md` · `CODEX.md` · `GEMINI.md` | Adaptadores por ferramenta |
| `backend/AGENTS.md` · `frontend/AGENTS.md` | Convenções da camada, incluindo as armadilhas já pagas |
| `environments/AGENTS.md` · `environments/vps/` | Ambiente, publicação e os runbooks de operação |
| `specs/AGENTS.md` | Como o ciclo SDD funciona aqui |
| `.gems-ai/` · `.gems-ai/MIGRACAO-2.4.0.md` | As regras de escrita (2.4.0) e o que o código daqui ainda **não** cumpre delas, medido |
| `business/` | O produto em linguagem de negócio, para o cliente |

## Gates de conformidade

Backend: `.\mvnw.cmd clean install`. Frontend: `npm test` e `npm run build`.
O CI executa os mesmos comandos e `npx prettier --check .`.
Spotless, Checkstyle, arquitetura/fonte e exceções escritas reprovam violações novas.
Frontend verifica silent check-sso e 13 regras antes de teste e build.
Catálogo de ações desligado até S-09, conforme contrato da rodada 010.

Dívida congelada em S-def: Checkstyle 314 ocorrências; arquitetura/fonte 189 identidades;
frontend 174 ocorrências. Baselines só encolhem. Supressões Checkstyle aceitas estão em
`backend/app-deploy/src/test/resources/checkstyle-baseline.txt`; ampliar o conjunto reprova.
Spotless usa `ratchetFrom=411156d`, preservando arquivos anteriores às definições.
S-codex conformará domínio e removerá o ratchet. Medições e mutações:
`specs/002-conformidade-gems/`. Baseline não é exceção individual.

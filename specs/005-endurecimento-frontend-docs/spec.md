# Feature Specification: Endurecimento do frontend, documentação e scripts

**Feature Branch**: `feature/005-endurecimento-frontend-docs`
**Created**: 2026-10-12
**Status**: Draft
**Input**: achados 1, 2, 3 e 6 da homologação local da 003
([evidence.md](../003-firebase-multi-tenant/evidence.md#homologacao-local-20261012)) e os `.pyc`
versionados em `scripts/`. Decisões de entrada no [roteiro](roteiro.md) §2. Os testes Python e o
manifesto do clean-demo saíram para a 006 (decisão 4).

## User Scenarios & Testing *(mandatory)*

### User Story 1 — A tela de login não pode ser embutida por outro site e só fala com quem precisa (Priority: P1)

Quem usa o produto abre a tela de login e entra com Google ou com e-mail e senha, como hoje. Um site de
terceiros que tente carregar a aplicação dentro de um quadro recebe recusa do navegador. A página só
carrega código e só abre conexões com a própria origem, com a API configurada e com os serviços de
login do Firebase e do Google.

**Why this priority**: a falta de proteção contra clickjacking é o único item do achado 6 com peso de
segurança. Sem ela, o sample não serve de molde para um SaaS com login.

**Independent Test**: com o container do frontend no ar, as respostas de `/`, de um asset, de `/env.js` e
de `/robots.txt` trazem os headers de segurança. O login com Google e o login por senha funcionam em tela,
sem violação de política no console do navegador.

**Acceptance Scenarios**:

1. **Given** o container em perfil `production` com um `authDomain` real, **When** qualquer caminho é
   pedido, **Then** a resposta traz a política de conteúdo, a recusa de enquadramento e os demais
   headers de segurança.
2. **Given** a política ativa, **When** o usuário entra com Google pelo popup ou por e-mail e senha,
   **Then** o login conclui sem nenhuma violação de política.
3. **Given** a API configurada numa origem absoluta, **When** a política é gerada, **Then** essa origem é
   a única outra origem de API permitida.
4. **Given** um perfil local com o emulador, **When** a política é gerada, **Then** o endereço do emulador
   é permitido, e ele nunca é permitido no perfil `production`.

---

### User Story 2 — O container sobe fora do Compose (Priority: P1)

Quem opera o frontend fora do Compose informa onde está a API e sobe o container sem truques de rede.
No Compose, nada muda.

**Why this priority**: o achado 2 exige `--add-host backend:<ip>` para o container subir fora do Compose,
o que é frágil e não está documentado.

**Independent Test**: o container sobe sem um host chamado `backend` resolvível e responde em `/healthz`.
Com a variável do upstream apontando para uma API real, `/api/` chega a ela. No Compose sem a variável, o
comportamento é o de hoje.

**Acceptance Scenarios**:

1. **Given** nenhum host `backend` resolvível, **When** o container sobe, **Then** ele fica saudável.
2. **Given** a variável do upstream com um endereço válido, **When** `/api/...` é pedido, **Then** a
   requisição chega a esse endereço.
3. **Given** a variável do upstream com um valor inválido, **When** o container sobe, **Then** ele recusa
   a subida com mensagem clara e sem expor o valor.

---

### User Story 3 — Cache correto e indexação explícita (Priority: P2)

O navegador guarda por muito tempo os arquivos com hash no nome e sempre revalida o `index.html`. Um
robô de busca encontra um `robots.txt` de verdade.

**Why this priority**: melhora Melhores práticas e SEO (decisão 3) sem perseguir meta de performance.

**Independent Test**: um asset com hash volta com cache longo e imutável. O `index.html` volta com
revalidação obrigatória, o `/env.js` continua sem cache e o `/robots.txt` volta como texto, não como
`index.html`.

**Acceptance Scenarios**:

1. **Given** um asset com hash, **When** é pedido, **Then** a resposta tem cache de um ano e imutável.
2. **Given** `/` ou uma rota do SPA, **When** é pedido, **Then** o `index.html` volta com revalidação.
3. **Given** `/robots.txt`, **When** é pedido, **Then** volta o arquivo de texto.

---

### User Story 4 — A documentação diz a verdade sobre o estado atual (Priority: P2)

Quem lê o README e o guia de configuração encontra o estado real da `main` e todas as variáveis
necessárias para a homologação, inclusive a de CORS.

**Why this priority**: os achados 1 e 3 fizeram a homologação depender de conhecimento que não estava
escrito.

**Independent Test**: o README não descreve como pendente nada que já foi entregue e provado. O guia
lista a variável de CORS com exemplo. Os `.pyc` não estão versionados.

**Acceptance Scenarios**:

1. **Given** o README, **When** comparado com a `evidence.md` da 003 e da 006, **Then** nenhuma entrega
   provada aparece como pendente.
2. **Given** o guia, **When** a homologação local é configurada, **Then** a variável de origens de CORS
   está documentada junto das demais.
3. **Given** o repositório, **When** os testes Python rodam, **Then** nenhum `.pyc` aparece como alterado
   ou não versionado.

### Edge Cases

- `authDomain` com caracteres fora de hostname: o entrypoint já recusa; a política nunca é escrita com
  valor não validado.
- API relativa (`/api`, o padrão): nenhuma origem extra entra na política.
- Rota do SPA com extensão (por exemplo, `/login.html`): continua caindo no `index.html`, como hoje; só
  `/robots.txt` ganha tratamento próprio.
- Popup do Google bloqueado pela política de abertura: o COOP fica em `same-origin-allow-popups` (C1).
- O `.gitignore` é metadado protegido do manifesto do clean-demo: mudar o arquivo exige atualizar o hash
  e o pin do manifesto na mesma alteração.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Toda resposta do container do frontend DEVE recusar enquadramento por outra origem.
- **FR-002**: Toda resposta DEVE trazer uma política de conteúdo que permita só a própria origem, a API
  configurada, os serviços de login do Firebase e do Google e, apenas em perfil local, o emulador.
- **FR-003**: A política DEVE ser gerada na subida do container a partir da configuração pública já
  validada, sem valor não validado.
- **FR-004**: As respostas DEVEM trazer também: proibição de adivinhar o tipo de conteúdo, política de
  referência restrita, permissões de hardware desligadas e isolamento de janela compatível com o popup do
  Google. HSTS fica fora (C1).
- **FR-005**: Os headers DEVEM valer em todos os caminhos, inclusive nos que definem cache próprio.
- **FR-006**: Assets com hash no nome DEVEM ter cache de um ano e imutável; o `index.html` DEVE exigir
  revalidação; o `/env.js` continua sem cache.
- **FR-007**: DEVE existir um `robots.txt` servido como texto.
- **FR-008**: O endereço da API usado pelo proxy DEVE ser configurável por variável, com o padrão de hoje,
  validado na subida, e o container DEVE subir sem resolver esse endereço.
- **FR-009**: O guia de configuração DEVE documentar a variável de origens de CORS do backend.
- **FR-010**: O README DEVE refletir o estado entregue da `main`.
- **FR-011**: Os `.pyc` DEVEM sair do versionamento e ser ignorados, sem quebrar o clean-demo nem o
  inicializador.

### Key Entities

- **Configuração pública de runtime**: os campos que o entrypoint já valida (`FIREBASE_*`, `API_BASE_URL`,
  `APP_RUNTIME_PROFILE`, `AUTH_EMULATOR_URL`), agora também fonte da política de conteúdo.
- **Upstream da API**: endereço do backend para o proxy, com o padrão do Compose.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: As quatro respostas de prova (`/`, um asset, `/env.js` e `/robots.txt`) trazem os 7 headers
  de segurança.
- **SC-002**: Login com Google e login por senha concluem em tela com a política ativa e com 0 violações
  no console.
- **SC-003**: Lighthouse na `/login`: Acessibilidade continua em 100, e Melhores práticas e SEO sobem em
  relação a 77 e 83 (medidos na 003). Performance é só registrada (decisão 3).
- **SC-004**: O container sobe e fica saudável sem host `backend` resolvível.
- **SC-005**: Os testes de configuração de runtime cobrem a geração da política e do upstream, com
  recusa de valor inválido observada.
- **SC-006**: Zero regressão: gates do frontend, testes Python (60), clean-demo e inicializador verdes.

## Assumptions

- O sample local roda em HTTP, então HSTS não se aplica (C1).
- A rodada começa depois do merge da 006 no sample, porque usa o `sample.py` e o manifesto da 006.
- A verificação do login Google em tela é humana, como na 003.

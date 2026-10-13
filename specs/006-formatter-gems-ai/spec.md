# Feature Specification: Formatter Java do GEMS AI SDK 3.1.0

**Feature Branch**: `feature/006-formatter-gems-ai`
**Created**: 2026-10-12
**Status**: Draft
**Input**: pedido do product owner em 2026-10-12: "Uma nova versão do gems-ai-sdk foi mergeada, adicionando
um formatter próprio. Valide se o novo pacote foi publicado, os impactos na CI e, se possível, aplique esse
formatador no projeto backend". Decisões de entrada fechadas no [roteiro](roteiro.md) §2.

## User Scenarios & Testing *(mandatory)*

### User Story 1 — O código Java do SDK segue o formato do product owner (Priority: P1)

Quem mantém o Java-GEMS-SDK abre qualquer arquivo Java, de produção ou de teste, e encontra o formato
definido pelo product owner. Um PR com Java fora desse formato é reprovado antes do merge.

**Why this priority**: a rodada 004 precisa ser implementada sobre o SDK já formatado (decisão 5), então
o SDK vem primeiro.

**Independent Test**: num checkout limpo do SDK, a verificação de formato completa passa e a verificação
de conformidade das regras GEMS também. Ao alterar um espaço num arquivo Java, a verificação falha.

**Acceptance Scenarios**:

1. **Given** o SDK sem adoção do GEMS AI, **When** a adoção com o perfil Java é aplicada, **Then** as
   regras, os blocos de agente, o snapshot e o lock passam a existir e a conformidade estrita passa.
2. **Given** o SDK adotado, **When** a formatação completa é aplicada, **Then** todos os arquivos Java
   ficam no formato e o build completo continua verde, com o mesmo número de testes de antes.
3. **Given** um PR no SDK com um arquivo Java fora do formato, **When** a CI roda, **Then** o job de
   formato falha e aponta o arquivo.

---

### User Story 2 — O backend do sample segue o mesmo formato, com uma fonte de verdade só (Priority: P1)

Quem mantém o backend do sample encontra o mesmo formato do SDK em todo o Java. Só existe uma ferramenta
que decide o layout. Ela não briga com outra ferramenta, então o formato não oscila.

**Why this priority**: é o pedido literal do product owner.

**Independent Test**: num checkout limpo do sample, a verificação de formato completa e a conformidade
estrita passam. O build completo do backend passa sem nenhuma outra ferramenta de layout reformatar ou
reprovar o código.

**Acceptance Scenarios**:

1. **Given** o sample na versão 3.0.0 do GEMS AI, **When** a migração para a 3.1.0 é aplicada, **Then** a
   conformidade estrita passa, inclusive num checkout limpo.
2. **Given** o sample migrado, **When** a formatação completa é aplicada, **Then** os 78 arquivos Java
   ficam no formato e o build completo continua verde, com o mesmo número de testes de antes.
3. **Given** o backend formatado, **When** o build roda, **Then** nenhuma ferramenta de layout diferente
   do formatter do GEMS AI reprova o código.

---

### User Story 3 — A história do código continua legível (Priority: P2)

Quem investiga a origem de uma linha encontra o commit que mudou o comportamento, e não o commit da
reformatação em massa.

**Why this priority**: sem isso, a reformatação apaga a autoria de praticamente todo o código.

**Independent Test**: a ferramenta de autoria por linha, configurada pelo arquivo de revisões ignoradas do
repositório, devolve para uma linha reformatada o commit anterior à reformatação.

**Acceptance Scenarios**:

1. **Given** o commit de reformatação em massa, **When** a autoria de uma linha reformatada é consultada
   com o arquivo de revisões ignoradas, **Then** aparece o commit de origem anterior.
2. **Given** o commit de reformatação, **When** o código compilado antes e depois é comparado, **Then**
   não há diferença de comportamento.

---

### Edge Cases

- Checkout limpo sem conversão de fim de linha: o snapshot do formatter precisa continuar conforme. No
  ensaio, os arquivos novos do formatter acusaram drift até o fim de linha deles ser fixado.
- Máquina com outro JDK: o formatter exige JDK 21 ou mais novo. O ensaio passou com o JDK 26.
- Branches abertas de outras rodadas (004 e 005): ainda não têm Java alterado, então não entram em
  conflito com a reformatação. O Java da 004 nasce depois da 006.
- Linhas muito longas: o formatter mantém numa linha só construções como a lista de constantes de um
  enum (máximo medido de 1.213 caracteres). Essa é a saída do formato do product owner, não um defeito
  desta rodada.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: O Java-GEMS-SDK DEVE adotar o GEMS AI 3.1.0 com o perfil Java completo: regras, blocos de
  agente, skills, snapshot do formatter e lock, todos gerados pela CLI oficial.
- **FR-002**: O sample DEVE migrar do GEMS AI 3.0.0 para o 3.1.0 pela CLI oficial, com a pré-visualização
  conferida antes da aplicação.
- **FR-003**: Todo arquivo Java versionado nos dois repositórios, de produção e de teste, DEVE estar no
  formato do formatter do GEMS AI 3.1.0.
- **FR-004**: A reformatação em massa DEVE ficar num commit isolado, sem nenhuma mudança de comportamento,
  e registrado no arquivo de revisões ignoradas para autoria de cada repositório.
- **FR-005**: A ausência de mudança de comportamento DEVE ser provada por comparação do código compilado
  antes e depois da reformatação, além do build completo verde com a mesma contagem de testes.
- **FR-006**: No backend do sample, o formatter do GEMS AI DEVE ser a única ferramenta que decide o
  layout. A verificação de layout anterior sai do build.
- **FR-007**: As regras de qualidade do Checkstyle DEVEM continuar ativas nos dois repositórios. Uma
  regra de layout só sai se conflitar com o formatter.
- **FR-008**: A CI dos dois repositórios DEVE reprovar um PR com Java fora do formato e com snapshot GEMS
  não conforme, usando a CLI fixada na versão 3.1.0, nunca numa referência móvel.
- **FR-009**: O gate local do sample DEVE incluir a verificação de formato completa, para o agente e o
  desenvolvedor verem a falha antes do push.
- **FR-010**: Os snapshots gerenciados DEVEM continuar conformes num checkout limpo, sem conversão de fim
  de linha.
- **FR-011**: A documentação para agentes e desenvolvedores DEVE deixar de mandar satisfazer o Spotless do
  sample e passar a apontar o formatter do GEMS AI.

### Key Entities

- **Manifesto GEMS AI** (`gems-ai.json`): versão, commit e perfil do consumidor.
- **Snapshot gerenciado** (`.gems-ai/`, lock): regras, formatter e hashes, escritos só pela CLI.
- **Commit de estilo**: a reformatação em massa, isolada e ignorada pela autoria.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A verificação completa de formato passa nos 78 arquivos Java do sample e nos 220 do SDK.
- **SC-002**: O build completo de cada repositório passa depois da reformatação, com a mesma contagem de
  testes de antes (sample: 290).
- **SC-003**: O código compilado de cada repositório é equivalente antes e depois da reformatação.
- **SC-004**: Um arquivo Java desformatado de propósito faz a CI e o gate local falharem (mutação
  observada).
- **SC-005**: Num checkout limpo, a conformidade estrita passa nos dois repositórios.
- **SC-006**: A autoria de uma linha reformatada aponta para o commit anterior à reformatação.

## Assumptions

- A release `v3.1.0` do GEMS AI SDK (commit `909c69aa`) é a versão alvo, publicada e com checksums
  conferidos.
- O frontend e o Angular-GEMS-SDK ficam fora, porque o formatter é só para Java (C4 e C5 do roteiro).
- A reformatação não exige release nova do Java-GEMS-SDK: ela não muda API nem comportamento, e a próxima
  versão publicada (3.5.0, rodada 004) já sai formatada.
- A instalação dos plugins de IDE é feita pelo usuário (C7).

---
name: spec-driven
description: Execute o ciclo Spec-driven do projeto para novas funcionalidades, novos CRUDs, mudanças de comportamento e alterações em especificações aprovadas. Use quando o Codex precisar criar ou atualizar spec.md, esclarecer requisitos, produzir plan/research/data-model/contracts/quickstart, gerar ou analisar tasks/checklists, ou implementar uma feature governada pelo spec-kit. Não use para bug fix que apenas restaura comportamento já contratado nem para edição exclusivamente documental.
---

# Spec-driven do projeto

> Para **conduzir uma rodada** — descobrir em que fase ela está, executar só essa fase e parar no
> portão humano — use `$rodada-sdd`. Esta skill descreve o ciclo spec-kit que a fase 2/3 daquela
> percorre; ela não sabe em que fase a rodada está.

## Preparar o contexto

1. Ler `AGENTS.md`, `specs/AGENTS.md` e os artefatos da feature. Ler `CODEX.md` e a constituição por
   inteiro somente quando a mudança tocar comportamento, arquitetura, ambiente ou governança.
2. Localizar a RF de origem em `utils/documentação do projeto/especificacoes_spec_driven/`.
3. Consultar o levantamento prévio apenas para contexto de negócio e terminologia.
4. Inspecionar a feature semelhante e o domínio de referência, quando houver, para restrições reais
   de implementação.
5. Classificar o trabalho e aplicar documentação proporcional:
   - Nova feature: executar o ciclo completo.
   - Mudança de comportamento aprovado: atualizar a partir do primeiro artefato impactado e propagar
     para todos os seguintes.
   - Bug que restaura comportamento já especificado: sair deste workflow, criar teste de regressão e
     seguir a rota de bug do `CODEX.md`.
   - Refatoração ou ajuste técnico sem mudança de comportamento: criar `change.md` a partir de
     `.specify/templates/change-template.md`; não criar `research`, `data-model` ou contratos vazios.

Não implementar diretamente a partir de RF preliminar. Não tratar `[NEEDS CLARIFICATION]` como
decisão. Não permitir que detalhes do código contaminem o `spec.md`; usar o código para restrições e
estado real durante o planejamento.

## Executar o ciclo

### 1. Specify

- Criar ou atualizar `specs/<numero>-<slug>/spec.md` usando
  `.specify/templates/spec-template.md` como estrutura.
- Descrever valor, atores, user stories independentes, cenários, requisitos funcionais, edge cases,
  premissas, fora de escopo e critérios mensuráveis.
- Manter o spec tecnológico-agnóstico: definir **o quê** e **por quê**, não classes, tabelas ou
  frameworks.
- Criar ou atualizar `checklists/requirements.md` e corrigir falhas de completude antes de avançar.

### 2. Clarify

- Encontrar ambiguidades de escopo, autorização, dados, UX, concorrência, integração e falhas.
- Resolver primeiro decisões com alto impacto e sem default seguro.
- Registrar cada resposta no `spec.md`. A rodada003 usa plano já aprovado e autorização literal
  "Implement the plan."; registrar no roteiro sem assinatura por proxy e sem pedir confirmação
  repetida para o MESMO escopo. Eliminar todos os marcadores
  `[NEEDS CLARIFICATION]` antes do plano.
- Não inventar decisão de produto quando alternativas alterarem materialmente o resultado.

### 3. Plan

- Usar `.specify/scripts/powershell/setup-plan.ps1 -Json` quando for inicializar os artefatos de
  planejamento de uma feature.
- Produzir e manter os artefatos aplicáveis; não criar arquivo vazio apenas para cumprir lista:
  - `plan.md`: contexto técnico, constitution check, estrutura e justificativas de complexidade.
  - `research.md`: decisões, riscos, alternativas rejeitadas e evidências.
  - `data-model.md`: entidades, relacionamentos, validações, índices e transições de estado.
  - `contracts/`: REST e módulo frontend, quando aplicáveis.
  - `quickstart.md`: cenários executáveis de validação ponta a ponta.
- Distribuir arquivos entre os cinco módulos Maven conforme a constituição.
- Manter configuração, migrations, resources e empacotamento somente em `app-deploy`.
- Consultar regras e recipes GEMS apenas para as camadas presentes no plano; aplicar os overrides do
  `CODEX.md` e tratar domínio de referência como forma; constituição, segurança e API publicada prevalecem.

### 4. Tasks

- Usar `.specify/scripts/powershell/setup-tasks.ps1 -Json` quando for inicializar `tasks.md`.
- Organizar tarefas por dependência e user story, com IDs `T001`, `T002` etc.
- Informar caminho exato e módulo Maven em cada tarefa.
- Usar `[P]` somente quando tarefas atuarem em arquivos diferentes e não dependerem de trabalho
  incompleto.
- Colocar teste antes da implementação para regra de negócio e regressões.
- Incluir checkpoints executáveis após cada fase significativa.
- Registrar a decisão de delegação. `Sem delegação` é válida quando o onboarding não se amortiza;
  modelo caro exige justificativa de risco material.

### 5. Checklists e análise

- Criar checklists específicos para riscos materiais: requirements, técnico, UX, segurança,
  acessibilidade ou integração.
- Tratar checklist como teste da qualidade dos requisitos, não como teste da implementação.
- Analisar consistência e rastreabilidade entre constituição, spec, plan, research, data model,
  contracts, quickstart e tasks.
- Classificar como crítico qualquer violação constitucional, artefato obrigatório ausente, contrato
  contraditório ou requisito essencial sem cobertura.
- Não marcar item aberto mecanicamente. Resolver, aceitar explicitamente ou registrar por que não se
  aplica.
- Não alterar arquivos durante uma execução solicitada como análise read-only.
- Depois do `speckit-analyze` e antes da assinatura do roteiro, avaliar os critérios da skill
  `opus-review`. Quando elegível, obter autorização específica do usuário antes de uma única revisão
  Opus de planejamento; registrar também quando não acionada, indisponível ou falha. O parecer é
  consultivo e cada achado precisa de adjudicação Codex fundamentada.

### 6. Implement

- Confirmar que não restam bloqueios críticos nem clarificações materiais.
- Preservar o escopo e executar `tasks.md` na ordem de dependência.
- Para executor delegado, exigir pacote com listas fechadas `READ`, `EDIT`, `CREATE` e `TEST`; se a
  leitura for insuficiente, o executor para e reporta em vez de explorar o repositório.
- Escrever ou ajustar testes antes da lógica de negócio correspondente.
- Manter `tasks.md` sincronizado com o trabalho realmente concluído.
- Não refatorar áreas não relacionadas só porque estão próximas da feature.
- Atualizar documentação de agente quando surgir uma convenção transversal ou um override recorrente.

### 7. Verify

- Executar os gates aplicáveis definidos no `CODEX.md` e na constituição.
- Usar gate focado durante a iteração e executar o gate completo uma vez no fechamento. Para entrega
  completa, exigir:
  - `backend/.\mvnw.cmd clean install`.
  - `frontend/npm test`.
  - `frontend/npm run build`.
  - `frontend/npx prettier --check .`.
  - aplicação subindo com `ddl-auto: validate` quando houver impacto de contexto ou banco.
- Executar os cenários relevantes de `quickstart.md` e registrar evidências.
- Depois dos gates completos e antes da homologação, repetir a avaliação de elegibilidade da skill
  `opus-review` para o checkpoint de implementação. A chamada continua opcional, exige nova
  autorização e não bloqueia automaticamente a rodada quando indisponível ou falha.
- Informar testes não executados e verificações manuais pendentes.

## Resolver conflitos de fontes

- Aplicar a constituição acima de qualquer outro arquivo.
- Tratar a spec-kit aprovada como contrato do comportamento pretendido.
- Tratar código e testes como estado real e referência de implementação.
- Se spec aprovada e código divergirem, investigar bug ou drift documental; não escolher
  silenciosamente.
- Fazer o código consolidado vencer apenas as especificações preliminares de `utils/`, registrando a
  diferença na spec convertida.
- Fazer as regras de `.gems-ai/rules/` vencerem o código consolidado (constituição 3.0.0): numa
  divergência de escrita, é o código que se corrige, e não a regra que cede. O domínio de referência
  serve de forma, não de autoridade. Desvio que fica de pé leva
  `GEMS-EXCEPTION(<regra>#<seção>): <motivo>; expira AAAA-MM-DD` na linha de cima.
- Distinguir descrição histórica de `plan.md`/`research.md` do estado atual em `tasks.md`, código e
  testes.

## Encerrar

Entregar um resumo com:

- artefatos criados ou atualizados;
- requisitos e user stories cobertos;
- decisões e divergências registradas;
- tarefas concluídas e pendentes;
- comandos de validação e resultados;
- riscos, clarificações ou verificações manuais restantes.

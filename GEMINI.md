# Contexto essencial do Gemini

Mantenha esta memória mínima: o Gemini a envia em todas as interações. Contexto de camada vive em
`backend/GEMINI.md`, `frontend/GEMINI.md`, `environments/GEMINI.md` e `specs/GEMINI.md`.

- Constituição: `.specify/memory/constitution.md`; leia-a integralmente apenas para governança,
  arquitetura, infraestrutura ou feature spec-driven.
- Código e testes consolidados vencem templates e documentos preliminares.
- Bug lê somente spec, contrato, código e teste afetados.
- Executor delegado lê somente pacote, contrato e lista fechada `READ`.
- Não pesquise `node_modules`, `target`, `dist`, `.angular` ou `business/**/*.png`.
- Não crie subagente para tarefa pequena, serial ou de arquivo único.
- Delegação zero é válida; retome a conversa existente sempre que possível.
- Nunca use `docker compose down -v` e nunca commite como executor delegado.

Gates finais: `backend/.\mvnw.cmd clean install`, `frontend/npm test` e
`frontend/npm run build`. No Windows, não use o script shell `./mvnw`.

As regras de `.gems-ai/rules/` vencem o código consolidado (constituição 2.0.0).
Código que diverge deve ser corrigido. Desvio autorizado exige
`GEMS-EXCEPTION(<regra>#<seção>): <motivo>; expira AAAA-MM-DD`, conforme
`.gems-ai/rules/conformance-exceptions.md`; agente não escreve exceção para desbloquear gate.
Código vence documentação de negócio e specs; parceiro/solicitacao são referências de forma.
Adoção de módulo SDK exige publicação, alcance medido, garantia nomeada, equivalência
provada por teste/mutação neste repositório e remoção da classe local na mesma alteração.

<!-- GEMS-AI:BEGIN -->
GEMS AI SDK 2.0.0 is mandatory for this repository. Base rules have highest precedence; profiles,
task overlays, local facts, and valid waivers may specialize but never weaken them. Generated files
under `.gems-ai/` must not be edited manually.

Before changing code, run `gems-ai rules bundle --project . --task <type>` and follow every returned
rule and required gate. Run `gems-ai check --project . --strict` before completion. A waiver must be
explicitly approved, scoped, unexpired, and recorded in `gems-ai.lock.json`; it cannot hide a broken
tool, build, functional gate, or infrastructure failure.
<!-- GEMS-AI:END -->

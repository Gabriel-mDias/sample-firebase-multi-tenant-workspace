# AGENTS.md

Instruções raiz são deliberadamente curtas. O arquivo `AGENTS.md` mais próximo da área trabalhada
completa estas regras.

<!-- GEMS-AI:BEGIN -->
As regras GEMS vivem em `.gems-ai/rules/` e as receitas em `.gems-ai/recipes/`. Leia somente os
arquivos da camada efetivamente tocada; nunca carregue o catálogo inteiro.
<!-- GEMS-AI:END -->

## Autoridade e classificação

1. `.specify/memory/constitution.md` é a autoridade máxima.
2. Estas regras e o `AGENTS.md` aninhado da camada vêm em seguida.
3. A spec aprovada define comportamento; código/testes consolidados mostram o estado real.
4. As regras de `.gems-ai/rules/` vencem o código consolidado (constituição 2.0.0).
Código que diverge deve ser corrigido. Desvio autorizado exige
`GEMS-EXCEPTION(<regra>#<seção>): <motivo>; expira AAAA-MM-DD`, conforme
`.gems-ai/rules/conformance-exceptions.md`; agente não escreve exceção para desbloquear gate.
Código vence documentação de negócio e specs; parceiro/solicitacao são referências de forma.
Adoção de módulo SDK exige publicação, alcance medido, garantia nomeada, equivalência
provada por teste/mutação neste repositório e remoção da classe local na mesma alteração.

Antes de agir:

- resposta/diagnóstico: leia somente arquivos relevantes;
- bug: spec, contrato, implementação e teste de regressão afetados;
- feature ou mudança de comportamento: use a rodada SDD (Claude: skill `/rodada-sdd`; Codex/demais: skill `spec-driven`);
- arquitetura, governança ou ambiente: leia a constituição completa;
- executor de lote: leia somente pacote, contrato e arquivos da lista `READ`.

Não é obrigatório ler `CODEX.md`, `CLAUDE.md`, specs concluídas ou planos de onda em toda tarefa.

## Regras universais

- Preserve alterações existentes; não use comandos destrutivos amplos.
- Nunca execute `docker compose down -v`.
- Não commite como executor delegado.
- Não pesquise `node_modules/`, `target/`, `dist/`, `.angular/` ou imagens de `business/`.
- Delegação zero é válida. Não delegue tarefa pequena, serial ou de arquivo único.
- Retome a sessão existente sempre que possível.
- Sucesso de gate é resumido; logs completos só entram no contexto em falhas.

## Estrutura e gates

- Backend Java 21/Spring Boot 4.1: `backend/AGENTS.md`.
- Frontend Angular 22: `frontend/AGENTS.md`.
- Ambientes e publicação: `environments/AGENTS.md`.
- Specs e governança: `specs/AGENTS.md`.

```powershell
cd backend
.\mvnw.cmd clean install

cd ..\frontend
npm test
npm run build
```

Use gate focado durante a iteração e o gate completo uma vez no fechamento.

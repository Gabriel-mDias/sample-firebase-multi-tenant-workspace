# Instruções essenciais para Claude Code

Este arquivo é carregado em toda sessão. Mantenha-o curto. Regras específicas vivem em
`.claude/rules/` e procedimentos extensos vivem em skills.

## Autoridade e entrada

1. `.specify/memory/constitution.md` é a autoridade máxima, mas só deve ser lida integralmente em
   alteração arquitetural, de governança, infraestrutura ou feature spec-driven.
2. `AGENTS.md` contém as regras transversais curtas.
3. A spec aprovada da feature define comportamento; código e testes mostram o estado real.
4. As regras de `.gems-ai/rules/` vencem o código consolidado (constituição 2.0.0).
Código que diverge deve ser corrigido. Desvio autorizado exige
`GEMS-EXCEPTION(<regra>#<seção>): <motivo>; expira AAAA-MM-DD`, conforme
`.gems-ai/rules/conformance-exceptions.md`; agente não escreve exceção para desbloquear gate.
Código vence documentação de negócio e specs; parceiro/solicitacao são referências de forma.
Adoção de módulo SDK exige publicação, alcance medido, garantia nomeada, equivalência
provada por teste/mutação neste repositório e remoção da classe local na mesma alteração.

Antes de agir, classifique a tarefa:

- bug: leia spec, contrato, código e teste afetados;
- feature ou mudança de comportamento: use `/rodada-sdd` e respeite o `roteiro.md` aprovado;
- backend, frontend ou ambiente: deixe as regras condicionais de `.claude/rules/` carregarem ao
  acessar arquivos da camada;
- executor delegado: leia somente o pacote, o contrato do executor e os arquivos da lista `READ`.

## Estado essencial

- Sistema interno single-tenant; schemas `public`, `security` e `auditoria`.
- Backend Java 21/Spring Boot 4.1 em cinco módulos Maven; somente `app-deploy` executa.
- Frontend Angular 22 standalone; Vitest; arquivos TS/HTML/CSS separados.
- Autorização por ações, não por níveis; os perfis são grupos do Keycloak. Cada endpoint usa
  uma ação concreta de `AcaoSistema` (nunca `hasAnyRole('ADMIN')`); erro do Keycloak é 502, negado é 403.
- Backend em domínios sob `app-core` (`parceiro`, `solicitacao`, ...); mapa em `backend/AGENTS.md`.
  Frontend em `features/<dominio>`; mapa em `frontend/AGENTS.md`.
- Sem cascade; eventos apenas em memória.
- Auditoria é automática pelo Hibernate e nunca chamada por serviço de domínio.
- Situação de fluxo nunca entra pelo corpo da requisição — cada transição é um endpoint de ação.

## Comandos e gates

No Windows:

```powershell
cd backend
.\mvnw.cmd clean install

cd ..\frontend
npm test
npm run build
```

- Iteração backend pode usar `-pl <modulo> -am`; gate final usa o reator inteiro uma vez.
- `npm install` exige `--legacy-peer-deps`.
- Sandbox ampliado é uma permissão temporária por rodada: liga-se somente com decisão registrada e
  volta ao modo padrão no fechamento. A rodada 002 o desligou em 2026-09-23.
- Ambiente local sobe com `docker compose up -d` em `environments/local`.
- Nunca execute `docker compose down -v`.
- Não faça commit como executor delegado.

## Economia de contexto

- Não leia o repositório inteiro nem use busca recursiva sem exclusões.
- Não leia `business/**/*.png`, `node_modules/`, `target/`, `dist/` ou `.angular/`.
- Não delegue tarefa pequena, serial ou de arquivo único.
- Retome a mesma sessão; não reinicie um lote em contexto frio.
- Em sucesso, reporte o gate resumidamente; só carregue logs completos quando houver falha.
- Delegação zero é válida. Modelo caro exige risco material e justificativa.

<!-- SPECKIT START -->
Feature ativa: o ponteiro está em `.specify/feature.json` e aponta para `specs/<ativa>/`. Não carregue
seus artefatos até a tarefa exigir.
<!-- SPECKIT END -->

<!-- GEMS-AI:BEGIN -->
GEMS AI SDK 2.0.0 is mandatory for this repository. Base rules have highest precedence; profiles,
task overlays, local facts, and valid waivers may specialize but never weaken them. Generated files
under `.gems-ai/` must not be edited manually.

Before changing code, run `gems-ai rules bundle --project . --task <type>` and follow every returned
rule and required gate. Run `gems-ai check --project . --strict` before completion. A waiver must be
explicitly approved, scoped, unexpired, and recorded in `gems-ai.lock.json`; it cannot hide a broken
tool, build, functional gate, or infrastructure failure.
<!-- GEMS-AI:END -->

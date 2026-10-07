# Pacote de execução — rodada 002, C14

> Lista `READ` fechada. Sem delegação. C11–C13 permanecem intactos na árvore. Não executar
> analyze final, commit ou push. D-P04 só muda por decisão nominal do product owner.

Checkpoint T077–T078. Âncora: `evidencias/sample-c14-anchor-porcelain.txt` no ledger de controle.
Gate: quickstart executado e inventário frontend apresentado ao product owner.

## 1. Fronteira fechada do lote

| # | Ação | Arquivo |
| :-- | :--- | :--- |
| C14 | CREATE | `specs/002-conformidade-gems/pacote-de-execucao-C14.md` |
| C14 | CREATE | `specs/002-conformidade-gems/estado-lote-C14.md` |
| T077–T078 | EDIT | `specs/002-conformidade-gems/tasks.md` |
| T077–T078 | EDIT | `specs/002-conformidade-gems/roteiro.md` |
| T077 | EDIT | `specs/002-conformidade-gems/quickstart.md` |
| T078 | EDIT | `specs/002-conformidade-gems/checklists/technical.md` somente após decisão nominal D-P04 |
| T078 | EDIT | `specs/002-conformidade-gems/checklists/requirements.md` somente após decisão nominal D-P04 |
| T078 | TEMP/RESTORE | `.gems-ai/conformance-baseline.json` para emitir o inventário residual exato |

### READ

- `AGENTS.md`, `backend/AGENTS.md`, `frontend/AGENTS.md`, `environments/AGENTS.md`, `specs/AGENTS.md`
- `.specify/memory/constitution.md`
- `specs/002-conformidade-gems/{quickstart.md,tasks.md,roteiro.md,estado-lote-C13.md}`
- os compose files, scripts, testes e rotas diretamente chamados pelo quickstart
- `.gems-ai/conformance-baseline.json` e `frontend/scripts/verify-conventions.mjs` para o inventário

## 2. T077 — quickstart

Executar os cenários reproduzíveis de auditoria, Keycloak/ambiente e frontend. Reusar as provas
automatizadas C11–C13 quando elas exercitam literalmente o mesmo contrato; registrar comando,
contagem e resultado. Cenário que exigir interação humana ou credencial externa deve ser nomeado,
nunca presumido verde.

O lote não repete os gates completos já verdes em C13 sem uma alteração que os invalide. Provas
focais, inspeção dos três compose files e execução local versionada são permitidas.

## 3. T078 — inventário e portão humano

Gerar inventário nominal por arquivo/regra/ocorrência a partir do verificador restaurado. Apresentar
ao product owner o residual mínimo e a alternativa de correção. Parar com D-P04 pendente; não marcar
T078 nem os checklists como concluídos sem aceite ou rejeição nominal.

## 4. Fechamento e rollback

Fechar com `git diff --check` e comparação literal contra a âncora C14, descontando somente os dois
documentos do próprio lote. Para cancelar C14, remover nominalmente apenas pacote/estado C14 e
restaurar por edição inversa os registros documentais do lote; nunca usar reset, clean ou checkout
amplo.

## 5. Extensão corretiva

D-P04 foi devolvida nominalmente para correção. A extensão autorizada e suas provas estão em
`pacote-de-execucao-C14-R1.md` e `estado-lote-C14-R1.md`; C14 terminou 2/2 com residual zero.

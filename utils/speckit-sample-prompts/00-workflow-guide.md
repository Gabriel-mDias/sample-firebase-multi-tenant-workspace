# Spec-Driven Development — Guia de Uso dos Prompts

Este diretório contém prompts de exemplo para cada skill do ciclo SDD com spec-kit.
Cada arquivo corresponde ao argumento passado para a skill respective.

## Fluxo completo (ordem de execução)

```
1. /speckit-constitution  ← constitution-prompt.md   (UMA VEZ por projeto)
         ↓
2. /speckit-specify       ← specify-prompt.md         (UMA VEZ por feature)
         ↓
3. /speckit-clarify       ← clarify-prompt.md         (opcional, antes do plan)
         ↓
4. /speckit-plan          ← plan-prompt.md            (UMA VEZ por feature)
         ↓
5. /speckit-tasks         ← tasks-prompt.md           (UMA VEZ por feature)
         ↓
6. /speckit-checklist     ← checklist-prompt.md       (opcional, por domínio)
         ↓
7. /speckit-analyze       ← analyze-prompt.md         (validação pré-implementação)
         ↓
8. /speckit-implement     ← implement-prompt.md       (execução)
```

## Como usar

Copie o conteúdo do arquivo `.md` correspondente e cole após o comando da skill:

```
/speckit-specify <cole aqui o conteúdo de specify-prompt.md>
```

Ou referencie o arquivo diretamente se o seu agente suportar:

```
/speckit-specify @utils/speckit-sample-prompts/specify-prompt.md
```

## Arquivos deste diretório

| Arquivo | Skill | Quando usar |
|---|---|---|
| `01-constitution-prompt.md` | `/speckit-constitution` | Setup inicial do projeto |
| `02-specify-prompt.md` | `/speckit-specify` | Início de cada feature |
| `03-clarify-prompt.md` | `/speckit-clarify` | Quando o spec tem ambiguidades |
| `04-plan-prompt.md` | `/speckit-plan` | Após spec aprovado |
| `05-tasks-prompt.md` | `/speckit-tasks` | Após plan gerado |
| `06-checklist-prompt.md` | `/speckit-checklist` | Por domínio (UX, segurança, etc.) |
| `07-analyze-prompt.md` | `/speckit-analyze` | Validação cruzada dos artefatos |
| `08-implement-prompt.md` | `/speckit-implement` | Execução das tasks |

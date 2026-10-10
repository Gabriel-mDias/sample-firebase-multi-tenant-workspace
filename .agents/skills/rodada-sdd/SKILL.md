---
name: rodada-sdd
description: Run or resume a GEMS spec-driven development round from discovery through terminal validation. Use for features, behavior changes, or explicit SDD requests; do not invoke for a narrow bug that only restores an already approved contract.
---

# Rodada SDD

Resolve a single round from the project pointer and current Git state. If no round exists, initialize
one with `gems-ai round init`; if multiple plausible rounds exist, stop for a human choice.

Follow the phase contract in [references/phases.md](references/phases.md). Read only the current
phase and explicitly named artifacts. Keep state as a reproducible projection of canonical Markdown,
not as a competing source of truth.

For toolkit CLI rounds, keep `.gems-ai/round/round.md` canonical. Write the current phase artifact,
obtain human approval evidence, then run `gems-ai round advance --approved-by ...
--approval-evidence ...`. Run `gems-ai round validate` before resuming. If only the JSON projection
diverges, use `gems-ai round sync`; changed artifact hashes require investigation.

Contrato de compatibilidade: índices de rodadas dos consumidores gerados de `roteiro.md` e
`tasks.md` permanecem sob seus scripts `gerar-estado-rodada.ps1`. Valide e regenere esses índices
com o script do respectivo repositório; a CLI do toolkit não os migra nem sobrescreve.

Before implementation, require testable criteria, an approved plan/roteiro, and closed task packages
with READ, EDIT, CREATE, and TEST boundaries. Use focused gates during execution and one complete
gate at closure. A round ends only as `completed` or `blocked`; blocked requires a real external
cause, evidence, owner, and resumption condition. Never infer a checkpoint approval.

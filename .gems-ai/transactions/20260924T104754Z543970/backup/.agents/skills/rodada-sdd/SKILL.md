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

When a repository maintains `estado-rodada.json`, treat it as a versioned, regenerable projection;
Markdown continues as the source of truth. Before resuming, validate the index with the repository's
`gerar-estado-rodada.ps1`. If it is absent or divergent, reconstruct it from `roteiro.md` and
`tasks.md`. After each transition or checkpoint, regenerate the index before proceeding.

Before implementation, require testable criteria, an approved plan/roteiro, and closed task packages
with READ, EDIT, CREATE, and TEST boundaries. Use focused gates during execution and one complete
gate at closure. A round ends only as `completed` or `blocked`; blocked requires a real external
cause, evidence, owner, and resumption condition. Never infer a checkpoint approval.

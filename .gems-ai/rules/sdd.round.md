<!-- gems-ai-rule: {"id":"sdd.round","version":"2.0.0","status":"candidate"} -->
# Spec-driven round

A round progresses through discovery, specification, plan, tasks, execution, validation, and
closure. Human checkpoints are explicit. Execution packages declare closed READ, EDIT, CREATE, and
TEST sets. The toolkit CLI uses `.gems-ai/round/round.md` as the canonical ordered journal and
`estado-rodada.json` as its reproducible projection. Each phase transition requires its named
Markdown artifact and a recorded human approval with evidence, both hashed. Validation checks
the sequence, hashes and projection before resuming or closing. Closure MUST be `completed` only
after all six phase checkpoints, or `blocked` with an external cause, evidence, owner and
resumption condition. Functional failure stays open for correction.

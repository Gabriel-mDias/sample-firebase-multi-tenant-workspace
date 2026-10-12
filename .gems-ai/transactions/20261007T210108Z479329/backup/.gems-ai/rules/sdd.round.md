<!-- gems-ai-rule: {"id":"sdd.round","version":"2.0.0","status":"candidate"} -->
# Spec-driven round

A round progresses through discovery, specification, plan, tasks, execution, validation, and
closure. Human checkpoints are explicit. Execution packages declare closed READ, EDIT, CREATE, and
TEST sets. State is a reproducible projection of canonical artifacts. Closure MUST be `completed`
or `blocked`; blocked requires an external cause, evidence, owner, and resumption condition.

<!-- gems-ai-rule: {"id":"base.safe-change","version":"2.0.0","status":"candidate"} -->
# Safe and auditable change

Existing work MUST be preserved. A mutating command MUST show its plan before writing, stage output
outside managed destinations, validate it, retain hashes and a transaction backup, and replace only
the named files. Re-execution MUST be idempotent. A malformed managed block or an unexpected local
edit MUST stop as a content conflict. Credentials, prompts, session identifiers, and full logs MUST
NOT be persisted. Required gates MUST be linked to the evaluated revision.

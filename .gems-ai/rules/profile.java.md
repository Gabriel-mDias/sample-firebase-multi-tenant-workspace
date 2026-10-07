<!-- gems-ai-rule: {"id":"profile.java","version":"2.0.0","status":"candidate"} -->
# Java profile

Module dependencies MUST follow the declared direction. Transport DTOs MUST NOT leak domain types.
Validation and error envelopes MUST be deterministic. Writes MUST have explicit transaction
semantics; production schema changes MUST be versioned and runtime schema inference MUST be off.
Authorization MUST fail closed. Unit, integration, architecture, formatting, and full reactor gates
MUST cover affected boundaries.

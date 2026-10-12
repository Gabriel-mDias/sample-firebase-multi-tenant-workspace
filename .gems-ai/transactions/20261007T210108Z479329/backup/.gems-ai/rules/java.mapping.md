<!-- gems-ai-rule: {"id":"java.mapping","version":"2.0.0","status":"candidate"} -->
# Java mapping

Mapping MUST be explicit at the domain/transport boundary and MUST compile under strict unmapped
target reporting. Create and update semantics MUST be separate when identity, audit fields, or
partial updates differ. IDs crossing the HTTP boundary use the consumer's declared transport type
and are converted once at the mapping boundary. Consumer-specific compatibility exceptions belong
in local facts or approved waivers, never in the canonical rule.

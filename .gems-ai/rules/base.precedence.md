<!-- gems-ai-rule: {"id":"base.precedence","version":"3.0.0","status":"candidate"} -->
# Authority and precedence

Law, security requirements and the consumer's local constitution govern its domain. The published
SDK contract governs names, signatures and observable API behavior. Toolkit base rules govern safe
operation: deterministic selection, closed conflicts, tests and traceability. Selected profiles and
task overlays specialize the base; local facts describe the project and may add restrictions. A
generic rule MUST NOT invent an SDK API or weaken a higher obligation.

Material conflicts MUST fail closed before mutation and name the conflicting rule IDs and paths.
Only an explicitly approved, scoped, unexpired waiver may suspend a dispensable toolkit rule; a
waiver cannot repair a false API claim, a security requirement or a broken gate. Base rules MUST
always be loaded for their operational safeguards, but do not override the local constitution or
the published SDK API contract.

The normative words MUST, MUST NOT, SHOULD, and MAY mean DEVE, NÃO DEVE, DEVERIA, and PODE.

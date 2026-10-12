<!-- gems-ai-rule: {"id":"base.precedence","version":"2.0.0","status":"candidate"} -->
# Authority and precedence

Base rules MUST always be loaded and MUST NOT be weakened. Selected profiles and task overlays may
specialize the base. Local facts may describe domain, topology, commands, prohibited data, and
legacy constraints, but MUST NOT silently remove an obligation. Only an explicitly approved,
scoped, unexpired waiver may suspend an applicable rule. Contradictions and unknown task classes
MUST fail closed and name the conflicting IDs or paths.

The normative words MUST, MUST NOT, SHOULD, and MAY mean DEVE, NÃO DEVE, DEVERIA, and PODE.

<!-- gems-ai-rule: {"id":"java.formatting","version":"3.1.0","status":"candidate"} -->
# Universal Java formatting

Every authored Java type MUST follow the canonical `eclipse-jdt-3.46.0-gems-3` executor,
including services, providers, DTOs, entities, configuration and tests. Generated sources and
external dependencies are excluded; there is no exception by architectural layer.

Use four spaces and same-line braces. Place exactly one blank line at type borders and between
constructors/methods, including nested types, records, enums, interfaces and empty types.
Consecutive fields, record components and enum constants stay grouped. Expand short bodies.
Annotations stay attached to declarations. Method/control parentheses have interior spaces.

Signatures, common declarations/statements and constructor arguments MUST occupy one line,
without a column cap. Existing deliberate breaks are preserved only for method chains, ternaries,
and `&&`/`||` conditions. Their dot/operator starts the continuation line, indented eight spaces
past the initial statement/field indentation. The broken AuditContext constructor call is joined.
Arrays, many arguments and lambdas do not grant additional wrapping exceptions.

Agents MUST create logical groups; the formatter preserves their positions and normalizes
separation to one blank line. Do not insert spacing before each control statement or return.
Comment/Javadoc text and line breaks, tokens, literal values, text blocks, Unicode and escapes
MUST be preserved. Only comment indentation can change. Import order and grouping are preserved.
Optional import cleanup uses the IDE's established semantic action after developer selection;
the canonical CLI does not remove imports or claim classpath-free unused-import analysis.

Run `gems-ai check-java --project . --changed --base <revision>` on whole changed files.
Run `gems-ai check-java --project . --all` for a separate full audit. Drift exits 10 with diff;
runtime/engine/syntax failures exit 50 and cannot be waived. Apply is explicit:
`gems-ai format java --project . --changed --apply`. Snapshot `check --strict` remains a
different integrity gate. A source formatting gate never starts Maven, compiles, or downloads.
Agents Codex, Claude, Gemini, Cursor and Copilot MUST use this same pinned executor.

Install the prebuilt VS Code/IntelliJ plugin shipped in the wheel (`gems-ai ide artifacts`)
to enable default formatting on save in configured Java/workspace projects. Both invoke
the complete public stdin CLI asynchronously on whole documents; see `docs/java-ide.md`
for installation, opt-out, supported IDE builds and the guarded second-save behavior.

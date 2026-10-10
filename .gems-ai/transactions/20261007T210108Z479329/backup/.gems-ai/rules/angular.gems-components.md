<!-- gems-ai-rule: {"id":"angular.gems-components","version":"2.0.0","status":"candidate"} -->
# Angular GEMS components

Use a GEMS component when its public contract matches the required behavior. Do not fork or wrap a
component merely to preserve obsolete consumer behavior; record an actual SDK gap and fix it at the
owning layer. Imports, inputs, outputs, validation, loading, empty, error, and accessibility states
MUST be typed and tested. A local component is allowed when no SDK equivalent exists and its scope
is documented without naming another consumer as precedent.

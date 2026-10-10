<!-- gems-ai-rule: {"id":"angular.testing","version":"2.0.0","status":"candidate"} -->
# Angular testing

Tests MUST assert observable behavior, not framework internals or private fields. Use the consumer
project's test infrastructure, deterministic providers, typed mocks, and explicit async control.
ADACI uses Vitest; Jasmine/Karma are not universal SDK requirements. Cover loading, success, empty,
denied, validation, and API error paths that apply. Route tests prove canonical paths and guards.
Test names describe expected behavior and condition without consumer-specific sample values. The
final gate uses the consumer's documented install, unit suite, formatter, and production build.

<!-- gems-ai-rule: {"id":"angular.testing","version":"2.0.0","status":"candidate"} -->
# Angular testing

Tests MUST assert observable behavior, not framework internals or private fields. Use deterministic
TestBed providers, typed mocks, and explicit async control. Cover loading, success, empty, denied,
validation, and API error paths that apply. Route tests prove canonical paths and guards. Test names
describe expected behavior and condition without consumer-specific sample values. The final gate is
a clean install, the complete unit suite, formatting, and a production build.

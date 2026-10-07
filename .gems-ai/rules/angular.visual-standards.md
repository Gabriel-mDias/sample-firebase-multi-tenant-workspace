<!-- gems-ai-rule: {"id":"angular.visual-standards","version":"2.0.0","status":"candidate"} -->
# Angular visual standards

SDK design tokens have precedence. A missing semantic value may become one project-local token,
declared centrally with the configured project prefix and named by role rather than color. Component
styles consume tokens and MUST NOT introduce raw color literals or impersonate the SDK namespace.
Layout, focus, contrast, responsive behavior, disabled/loading state, and reduced-motion behavior
MUST meet the applicable accessibility contract. The local token prefix is configuration, never a
hard-coded consumer name in this rule.

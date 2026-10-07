<!-- gems-ai-rule: {"id":"java.multi-tenant","version":"2.0.0","status":"candidate"} -->
# Java multi-tenancy

This rule applies only when the consumer declares multi-tenancy. The tenancy model (schema,
database, discriminator, or hybrid), global data boundary, tenant identifier source, provisioning,
and migration strategy MUST be explicit. Tenant context MUST come from authenticated server-side
context, never a trusted client parameter. Missing or inconsistent context fails closed. Pooling,
cache keys, async work, audit context, and tests MUST prove isolation across at least two tenants.

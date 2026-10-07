<!-- gems-ai-rule: {"id":"java.security","version":"2.0.0","status":"candidate"} -->
# Java security

Authentication and authorization MUST fail closed. Every non-public endpoint declares one concrete
authorization action or an explicit authenticated-only classification; broad administrative roles
MUST NOT replace action checks. Public endpoints are allowlisted and tested. Field-level sensitive
operations repeat authorization in the service boundary. Identity, tenant, audit actor, and
correlation context derive from trusted server-side sources. Denied, absent, malformed, and upstream
identity-provider failures have distinct tested responses and expose no sensitive detail.

<!-- gems-ai-rule: {"id":"base.waivers","version":"2.0.0","status":"candidate"} -->
# Conformance waivers

A waiver MUST name an existing rule and scope, explain the concrete reason, identify an owner and
human approver, expire on an ISO date, and define a compensating control. It MUST be recorded in
`gems-ai.json` and locked into the evaluated snapshot. Agents MUST NOT create or renew waivers to
make a red gate pass. Expired, orphaned, incomplete, or non-applicable waivers fail the check.

A waiver is not a baseline. Baselines may describe pre-existing debt and MUST be monotonic; a waiver
temporarily accepts one named deviation. Neither mechanism may hide tool failure, infrastructure
failure, build failure, missing evidence, or a newly introduced violation.

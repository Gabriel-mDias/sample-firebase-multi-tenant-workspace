---
name: gems-ai-sdk
description: Inspect, plan, install, update, verify, or roll back the GEMS AI SDK snapshot in a software repository. Use when a user asks to apply GEMS governance or diagnose toolkit drift; do not use for ordinary code changes that already have a valid locked snapshot.
---

# GEMS AI SDK

Preserve the repository's existing work and authorization boundaries.

1. Inspect applicable repository instructions and Git state, then run `gems-ai inspect --project <path>`.
2. Run `gems-ai plan --project <path> --task <type>` and report the selected profile, files, conflicts, and expected gates.
3. Before `apply` or `update`, obtain explicit human approval for the displayed mutation. Inspection, planning, rule lookup, bundling, and checking are read-only.
4. Apply only the approved plan. Do not create waivers, overwrite malformed managed blocks, merge, tag, release, or publish.
5. Run `gems-ai check --project <path> --strict` plus every gate named by the selected rules.
6. Report the transaction ID and exact rollback command: `gems-ai rollback --project <path> --transaction <id>`.

Treat exit 10 as drift, 20 as invalid configuration, 30 as content conflict, 40 as functional gate
failure, and 50 as infrastructure failure. Do not retry a functional failure without a change.

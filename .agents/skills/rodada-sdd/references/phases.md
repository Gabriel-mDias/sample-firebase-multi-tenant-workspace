# Phase contract

| Phase | Required input | Required output / toolkit CLI artifact |
| --- | --- | --- |
| discovery | request, domain facts, Git state | facts, unknowns, risks, exclusions in `discovery.md` |
| specification | approved decisions | story and testable acceptance criteria in `spec.md` |
| plan | contract and architecture | impact, gates, rollback in `plan.md` |
| tasks | approved plan | closed READ/EDIT/CREATE/TEST packages in `tasks.md` |
| execution | eligible package | reviewed diff and focused gate in `execution.md` |
| validation | all tasks complete | full gates, scenarios, acceptance evidence in `validation.md` |
| closure | six phase approvals linked | `completed` or evidenced `blocked` outcome |

An infrastructure failure may be retried once after recording evidence. A functional failure requires
a code, test, or contract change before retry. Concurrent rounds require an explicit human decision.
Each `round advance` requires separate approval evidence and records the named actor. The CLI checks
presence, order and hashes; it cannot independently verify that the named actor approved the work.

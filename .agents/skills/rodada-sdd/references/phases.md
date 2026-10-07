# Phase contract

| Phase | Required input | Required output |
| --- | --- | --- |
| discovery | request, domain facts, Git state | facts, unknowns, risks, exclusions |
| specification | approved decisions | story and testable acceptance criteria |
| plan | contract and architecture | impact, gates, rollback, human checkpoint |
| tasks | approved plan | closed READ/EDIT/CREATE/TEST packages |
| execution | eligible package | reviewed diff, focused gate, updated canonical state |
| validation | all tasks complete | full gates, scenarios, acceptance evidence |
| closure | linked evidence | `completed` or evidenced `blocked` outcome |

An infrastructure failure may be retried once after recording evidence. A functional failure requires
a code, test, or contract change before retry. Concurrent rounds require an explicit human decision.

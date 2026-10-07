<!-- gems-ai-rule: {"id":"java.persistence-db","version":"2.0.0","status":"candidate"} -->
# Java persistence and migrations

Production schema MUST be versioned by the declared migration tool and runtime ORM mode MUST
validate rather than create or update it. Table, column, constraint, index, sequence, and schema
names are explicit. A changeset is immutable after publication; corrections use a new changeset.
Forward migration, idempotent boot, rollback strategy, and application startup against the migrated
schema are required gates. Author names, schema names, and changeset numbering are local facts.

import { readFileSync } from 'node:fs';
import assert from 'node:assert/strict';
import { generatedTypes } from './action-catalog.mjs';
const root = new URL('../', import.meta.url);
const generated = readFileSync(
  new URL('src/app/core/auth/authorization.actions.json', root),
  'utf8',
);
const backend = readFileSync(
  new URL('../backend/app-deploy/src/main/resources/authorization.actions.json', root),
  'utf8',
);
assert.deepEqual(
  JSON.parse(generated),
  JSON.parse(backend),
  'O catálogo precisa ser regenerado pelo gerador Java oficial',
);
const ts = readFileSync(new URL('src/app/core/auth/authorization.actions.ts', root), 'utf8');
assert.equal(
  ts,
  await generatedTypes(),
  'Tipos de ações precisam ser regenerados: node scripts/action-catalog.mjs --write',
);
const catalog = JSON.parse(generated);
assert.equal(
  new Set([...catalog.global, ...catalog.tenant]).size,
  catalog.global.length + catalog.tenant.length,
);
console.log(
  `Catálogo oficial: ${catalog.global.length} globais / ${catalog.tenant.length} tenant.`,
);

import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
const root = new URL('../', import.meta.url);
const pkg = JSON.parse(readFileSync(new URL('package.json', root), 'utf8'));
assert.equal(pkg.dependencies['@gabriel-mdias/angular-gems-sdk'], '3.0.0');
assert.equal(pkg.dependencies.firebase, '12.19.0');
for (const name of [
  'keycloak-angular',
  'keycloak-js',
  '@gabriel-mdias/angular-gems-keycloak-auth',
]) {
  assert.equal(pkg.dependencies[name], undefined);
}
const core = JSON.parse(
  readFileSync(new URL('node_modules/@gabriel-mdias/angular-gems-sdk/package.json', root), 'utf8'),
);
assert.equal(core.version, '3.0.0');
assert.equal(core.exports['./auth'], undefined);
assert.ok(!JSON.stringify(core).includes('keycloak'));
const lock = JSON.parse(readFileSync(new URL('package-lock.json', root), 'utf8'));
assert.ok(!Object.keys(lock.packages).some((name) => name.includes('keycloak')));
assert.ok(
  lock.packages['node_modules/@gabriel-mdias/angular-gems-sdk'].resolved.startsWith('https://'),
);
console.log('SDK core remoto 3.0.0, Firebase 12.19.0 e isolamento do adapter: PASS');

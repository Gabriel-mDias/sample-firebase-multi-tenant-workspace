import { readFileSync, writeFileSync } from 'node:fs';
import prettier from 'prettier';
import { fileURLToPath } from 'node:url';
const path = new URL('../src/app/core/auth/authorization.actions.json', import.meta.url);
export async function generatedTypes() {
  const catalog = JSON.parse(readFileSync(path, 'utf8'));
  const source =
    '// Gerado de authorization.actions.json (gerador Java oficial). Não editar.\n' +
    'export const actionCatalog = ' +
    JSON.stringify(catalog, null, 2) +
    ' as const;\n' +
    'export const Acoes = {\n' +
    [...catalog.global, ...catalog.tenant]
      .sort()
      .map((action) => `  ${action}: '${action}',`)
      .join('\n') +
    '\n} as const;\n' +
    'export type Acao = (typeof Acoes)[keyof typeof Acoes];\n';
  return prettier.format(source, {
    ...(await prettier.resolveConfig(fileURLToPath(new URL('../package.json', import.meta.url)))),
    parser: 'typescript',
  });
}
if (process.argv.includes('--write'))
  writeFileSync(
    new URL('../src/app/core/auth/authorization.actions.ts', import.meta.url),
    await generatedTypes(),
  );

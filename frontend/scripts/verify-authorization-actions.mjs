/** Verifica o espelho frontend do catálogo único AcaoSistema do sample. */
import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';

const frontendRoot = resolve(import.meta.dirname, '..');
const acoesPath = resolve(frontendRoot, 'src/app/core/auth/acoes.ts');
const enumPath = resolve(
  frontendRoot,
  '../backend/app-security/src/main/java/br/com/gems/app/security/AcaoSistema.java',
);
const pattern = /^[A-Z][A-Z0-9]*_[A-Z0-9_]+$/;

function sorted(values) {
  return [...values].sort();
}

function validate(label, values) {
  if (values.length === 0) {
    throw new Error(`${label} não contém ação alguma; o gate ficou cego.`);
  }
  if (values.some((value) => !pattern.test(value))) {
    throw new Error(`${label} contém ação fora do padrão VERBO_RECURSO.`);
  }
  if (new Set(values).size !== values.length) {
    throw new Error(`${label} contém duplicidade.`);
  }
  return sorted(values);
}

function parseAcoes(source) {
  const match = source.match(/export\s+const\s+Acoes\s*=\s*\{([\s\S]*?)\}\s*as const;/);
  if (!match) {
    throw new Error('Não encontrei o objeto exportado Acoes em core/auth/acoes.ts.');
  }

  const entries = [...match[1].matchAll(/^\s*([A-Z][A-Z0-9_]*)\s*:\s*'([^']+)'\s*,?\s*$/gm)].map(
    ([, key, value]) => ({ key, value }),
  );
  if (entries.length === 0) {
    throw new Error('Não encontrei entradas de Acoes; a forma do objeto mudou.');
  }

  const keys = validate(
    'Acoes (chaves)',
    entries.map(({ key }) => key),
  );
  const values = validate(
    'Acoes (valores)',
    entries.map(({ value }) => value),
  );
  if (entries.some(({ key, value }) => key !== value)) {
    throw new Error('Cada chave de Acoes deve ter exatamente o mesmo valor de ação.');
  }
  return { keys, values };
}

function parseEnum(source) {
  const start = source.indexOf('public enum AcaoSistema');
  const open = source.indexOf('{', start);
  const end = source.indexOf(';', open);
  if (start < 0 || open < 0 || end < 0) {
    throw new Error('Não encontrei o corpo de AcaoSistema; a forma do enum mudou.');
  }

  const entries = [
    ...source.slice(open + 1, end).matchAll(/(?:^|,)\s*([A-Z][A-Z0-9_]*)\s*\(/gm),
  ].map(([, name]) => name);
  return validate('AcaoSistema', entries);
}

function assertSame(expected, actual) {
  if (JSON.stringify(expected) === JSON.stringify(actual)) return;
  const missing = expected.filter((action) => !actual.includes(action));
  const extra = actual.filter((action) => !expected.includes(action));
  throw new Error(
    `Acoes diverge de AcaoSistema.${missing.length ? ` Falta no frontend: ${missing.join(', ')}.` : ''}` +
      `${extra.length ? ` Não existe no backend: ${extra.join(', ')}.` : ''}`,
  );
}

const acoes = parseAcoes(readFileSync(acoesPath, 'utf8'));
const backendActions = parseEnum(readFileSync(enumPath, 'utf8'));
assertSame(backendActions, acoes.keys);
assertSame(backendActions, acoes.values);

console.log(`Acoes em paridade com AcaoSistema: ${backendActions.length} ações.`);

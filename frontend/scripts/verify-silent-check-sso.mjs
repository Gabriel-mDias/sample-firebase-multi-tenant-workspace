import { readFile } from 'node:fs/promises';
import { fileURLToPath } from 'node:url';

const publicDirectory = fileURLToPath(new URL('../public/', import.meta.url));
const [html, script] = await Promise.all([
  readFile(`${publicDirectory}silent-check-sso.html`, 'utf8'),
  readFile(`${publicDirectory}silent-check-sso.js`, 'utf8'),
]);

if (!html.includes('<script src="/silent-check-sso.js"></script>')) {
  throw new Error('silent-check-sso.html deve carregar o script externo publicado pela SPA.');
}

if (!script.includes('parent.postMessage(location.href, location.origin)')) {
  throw new Error('silent-check-sso.js deve devolver a resposta OIDC para a janela principal.');
}

console.log('Assets do silent check-sso verificados.');

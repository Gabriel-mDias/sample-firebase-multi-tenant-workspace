/**
 * Gate de convenções do frontend (N-07, FR-030) — o quarto `verify-*`, encadeado em `test` e
 * `build` como os três anteriores.
 *
 * O que ele afirma é o que a medição M-01 (`specs/008-padronizacao-de-escrita/medicao-m01.md`)
 * apurou contra as 12 regras de `.gems-ai/rules/frontend-angular/`, depois de elas terem sido
 * tornadas verdadeiras na fase 6 — regra que mente não vira gate. As regras aqui, na ordem em que
 * a medição as encontrou desviando (grupo B) ou zeradas (segundo achado):
 *
 *   1. cor literal em CSS — hex em todo `.css`; cor por NOME (`white`) e por FUNÇÃO (`rgba(...)`)
 *      em CSS de componente                       visual-standards.md#2   (B-1, B-3 — baseline zerado na 009;
 *                                                                          nome e função: decisão 6 da 010)
 *   2. `object` como tipo                          stores.md#7             (B-5)
 *   3. ordem dos métodos da Store                  stores.md#3             (B-8)
 *   4. linha em branco entre métodos da Store      stores.md#4             (B-4)
 *   5. `it('deveX_quandoY')`                       testing.md#7            (B-7)
 *   6. `standalone: true`                          signals-standards.md#1  (A-6, zero desvios)
 *   7. `@Input()` / `@Output()`                    signals-standards.md#2  (zero desvios)
 *   8. `*ngIf` / `*ngFor` / `*ngSwitch`            signals-standards.md#4  (zero desvios)
 *   9. `HttpClient` em componente ou Store         stores.md#anti-patterns (zero desvios)
 *  10. `alert()` / `confirm()` nativos             feedback-alerts.md#4    (zero desvios)
 *  11. `BehaviorSubject` em componente             signals-standards.md#anti-patterns
 *  12. direção de import (`core/`/`shared/` → `features/`, feature → outra feature)
 *                                                  organization.md#1       (T060 da 009, SC-002; sem baseline)
 *  13. nome de arquivo por subpasta de feature     organization.md#6       (decisão 6 da 010; sem baseline)
 *
 * TypeScript é lido pela ÁRVORE SINTÁTICA do compilador (`typescript`, que o projeto já tem) —
 * nunca por expressão regular, pelo mesmo motivo do gate de fonte do backend (R-04). A pesquisa R-05
 * nomeava `ts-morph`; ele é um invólucro sobre exatamente esta API, e traria uma cópia própria do
 * TypeScript 5.x para um projeto que compila com o 6. CSS e HTML não têm parser instalado, e são
 * percorridos caractere a caractere, sem regex.
 *
 * TRÊS MECANISMOS, e a diferença entre eles é a §4 de `conformance-exceptions.md`:
 *
 *   - EXCEÇÃO ESCRITA: `GEMS-EXCEPTION(<regra>#<seção>): <motivo>; expira AAAA-MM-DD` no comentário
 *     da linha imediatamente acima. Suprime UMA violação daquela regra naquela linha, enquanto a
 *     data não chega. Exceção sobre linha que não desvia é ÓRFÃ e reprova (INV-1) — a detecção
 *     avalia as regras uma segunda vez ignorando toda supressão. A validade da data e a existência
 *     da regra são conferidas também pelo `ExcecoesEscritasTest` do backend, que gera o inventário.
 *   - BASELINE (`.gems-ai/conformance-baseline.json`): a dívida que a rodada 008 não alcançou, por
 *     arquivo, regra e token, com o número de ocorrências. Só ENCOLHE: `CONGELADO_EM` abaixo é o total
 *     congelado, e um baseline que some mais do que isso reprova antes de qualquer regra rodar
 *     (INV-4). A rodada 009 (E-06) o zerou: `entradas: []` e `CONGELADO_EM = 0` — qualquer entrada
 *     reprova. Desvio novo nunca entra aqui — ou é corrigido, ou vira exceção escrita. A declaração
 *     de token local `--app-x: #hex` em `styles.css` não é desvio: `visual-standards.md#2.1` a
 *     admite, e o `hexEmCss` a isenta — só ali, e só a declaração.
 *   - O resto reprova, no formato do §2 de `contracts/gates.md`: caminho relativo, regra com âncora,
 *     violação e esperado.
 */
import { readdirSync, readFileSync, statSync } from 'node:fs';
import { join, relative, resolve } from 'node:path';
import ts from 'typescript';

const frontendRoot = resolve(import.meta.dirname, '..');
const repoRoot = resolve(frontendRoot, '..');
const srcRoot = resolve(frontendRoot, 'src');
const baselinePath = resolve(repoRoot, '.gems-ai', 'conformance-baseline.json');

/** Total medido e congelado em S-def, T057 (2026-09-14). Só encolhe em S-codex. */
const CONGELADO_EM = 174;

const REGRAS = '.gems-ai/rules/frontend-angular/';
const MARCA = 'GEMS-EXCEPTION(';
const hoje = new Date().toISOString().slice(0, 10);

// ---------------------------------------------------------------------------------------------
// Arquivos

function walk(dir, out = []) {
  for (const name of readdirSync(dir)) {
    const full = join(dir, name);
    if (statSync(full).isDirectory()) {
      walk(full, out);
    } else if (name.endsWith('.ts') || name.endsWith('.html') || name.endsWith('.css')) {
      out.push(full);
    }
  }
  return out.sort();
}

const arquivos = walk(srcRoot).map((full) => ({
  caminho: relative(repoRoot, full).split('\\').join('/'),
  texto: readFileSync(full, 'utf8'),
}));

const fontesTs = arquivos
  .filter((a) => a.caminho.endsWith('.ts'))
  .map((a) => ({
    ...a,
    ast: ts.createSourceFile(a.caminho, a.texto, ts.ScriptTarget.Latest, true, ts.ScriptKind.TS),
  }));

const linhaDe = (fonte, no) =>
  fonte.ast.getLineAndCharacterOfPosition(no.getStart(fonte.ast)).line + 1;
const linhaFinalDe = (fonte, no) => fonte.ast.getLineAndCharacterOfPosition(no.getEnd()).line + 1;

// ---------------------------------------------------------------------------------------------
// Exceções escritas — a mesma gramática do `ExcecoesEscritas` do backend, sem regex

function semPortador(linha) {
  let texto = linha.trim();
  for (const abertura of ['//', '/*', '<!--', '*']) {
    if (texto.startsWith(abertura)) {
      texto = texto.slice(abertura.length).trim();
      break;
    }
  }
  for (const fechamento of ['-->', '*/']) {
    if (texto.endsWith(fechamento)) {
      texto = texto.slice(0, -fechamento.length).trim();
    }
  }
  return texto;
}

function interpretar(texto) {
  const fechaRegra = texto.indexOf(')');
  if (!texto.startsWith(MARCA) || fechaRegra < 0) return null;
  const regra = texto.slice(MARCA.length, fechaRegra).trim();
  const depois = texto.slice(fechaRegra + 1).trim();
  if (!regra.includes('#') || !depois.startsWith(':')) return null;
  const corpo = depois.slice(1).trim();
  const ponto = corpo.lastIndexOf(';');
  if (ponto < 0) return null;
  const prazo = corpo.slice(ponto + 1).trim();
  if (!prazo.startsWith('expira')) return null;
  const expira = prazo.slice('expira'.length).trim();
  return { regra, motivo: corpo.slice(0, ponto).trim(), expira };
}

function excecoesDe(arquivo) {
  const linhas = arquivo.texto.split('\n');
  const encontradas = [];
  for (let i = 0; i < linhas.length; i++) {
    const marca = linhas[i].indexOf(MARCA);
    if (marca < 0) continue;
    let acumulado = semPortador(linhas[i].slice(marca));
    let ultima = i;
    while (!acumulado.includes('expira') && ultima + 1 < linhas.length) {
      ultima++;
      acumulado += ' ' + semPortador(linhas[ultima]);
    }
    const lida = interpretar(acumulado);
    if (lida) {
      encontradas.push({
        ...lida,
        caminho: arquivo.caminho,
        linhaDoComentario: i + 1,
        linhaCoberta: ultima + 2,
      });
    }
    i = ultima;
  }
  return encontradas;
}

const excecoes = arquivos.flatMap(excecoesDe);
const excecaoValida = (caminho, linha, regra) =>
  excecoes.find(
    (e) =>
      e.caminho === caminho &&
      e.linhaCoberta === linha &&
      e.regra === 'frontend-angular/' + regra &&
      e.expira > hoje,
  );

// ---------------------------------------------------------------------------------------------
// Baseline — a catraca

let baseline = { entradas: [] };
try {
  baseline = JSON.parse(readFileSync(baselinePath, 'utf8'));
} catch {
  // Sem arquivo, sem dívida congelada: tudo reprova.
}

const totalNoBaseline = baseline.entradas.reduce((soma, e) => soma + e.ocorrencias, 0);
if (totalNoBaseline > CONGELADO_EM) {
  throw new Error(
    `.gems-ai/conformance-baseline.json\n` +
      `  regra:    .gems-ai/rules/conformance-exceptions.md#4\n` +
      `  violação: o baseline soma ${totalNoBaseline} ocorrência(s), e o total congelado é ${CONGELADO_EM}\n` +
      `  esperado: o baseline SÓ ENCOLHE (INV-4). Desvio novo é corrigido ou vira GEMS-EXCEPTION(...) no ponto ` +
      `do código, com motivo e prazo — nunca uma entrada nova aqui`,
  );
}

const saldoDoBaseline = new Map(
  baseline.entradas.map((e) => [`${e.arquivo}|${e.regra}|${e.violacao}`, e.ocorrencias]),
);

// ---------------------------------------------------------------------------------------------
// Relatório

function novoRelatorio(modo) {
  const violacoes = [];
  return {
    violacoes,
    registrar(caminho, linha, regra, chave, violacao, esperado) {
      if (modo === 'suprimir') {
        if (excecaoValida(caminho, linha, regra)) return;
        const id = `${caminho}|${regra}|${chave}`;
        const saldo = saldoDoBaseline.get(id) ?? 0;
        if (saldo > 0) {
          saldoDoBaseline.set(id, saldo - 1);
          return;
        }
      }
      violacoes.push({ caminho, linha, regra, chave, violacao, esperado });
    },
  };
}

const formatar = (v) =>
  `${v.caminho}:${v.linha}\n  regra:    ${REGRAS}${v.regra}\n  violação: ${v.violacao}\n  esperado: ${v.esperado}`;

// ---------------------------------------------------------------------------------------------
// Regras

const ehHex = (c) => (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
// `#fff` seguido de letra, dígito, `_` ou `-` é um id ou um nome de classe (`#ffff-header`), não uma cor.
const ehIdentificador = (c) =>
  (c >= '0' && c <= '9') ||
  (c >= 'a' && c <= 'z') ||
  (c >= 'A' && c <= 'Z') ||
  c === '_' ||
  c === '-';

/** O único arquivo que pode DECLARAR um token local `--app-x: #hex` (visual-standards.md#2.1). */
const DONO_DOS_TOKENS_LOCAIS = 'frontend/src/styles.css';

const ehEspaco = (c) => c === ' ' || c === '\t' || c === '\n' || c === '\r';

/**
 * `--` em `texto[i]` abre uma DECLARAÇÃO de custom property: só espaço, `{`, `;`, `}` ou o fim de
 * um comentário antes dela, e `--identificador` seguido de `:` depois. `var(--x)` e
 * `var(--x, #hex)` não passam — o `(` antes do `--` os denuncia — e continuam sendo uso, avaliado
 * pelo ramo do `#`.
 */
function ehDeclaracao(texto, i) {
  let k = i - 1;
  while (k >= 0 && ehEspaco(texto[k])) k--;
  const fechaComentario = k >= 1 && texto[k] === '/' && texto[k - 1] === '*';
  if (k >= 0 && !fechaComentario && !['{', ';', '}'].includes(texto[k])) return false;
  let j = i + 2;
  while (j < texto.length && ehIdentificador(texto[j])) j++;
  while (j < texto.length && ehEspaco(texto[j])) j++;
  return texto[j] === ':';
}

/** As cores nomeadas do CSS (CSS Color Module Level 4, §6.1), mais `rebeccapurple`. */
const CORES_NOMEADAS = new Set(
  (
    'aliceblue antiquewhite aqua aquamarine azure beige bisque black blanchedalmond blue blueviolet ' +
    'brown burlywood cadetblue chartreuse chocolate coral cornflowerblue cornsilk crimson cyan darkblue ' +
    'darkcyan darkgoldenrod darkgray darkgreen darkgrey darkkhaki darkmagenta darkolivegreen darkorange ' +
    'darkorchid darkred darksalmon darkseagreen darkslateblue darkslategray darkslategrey darkturquoise ' +
    'darkviolet deeppink deepskyblue dimgray dimgrey dodgerblue firebrick floralwhite forestgreen fuchsia ' +
    'gainsboro ghostwhite gold goldenrod gray green greenyellow grey honeydew hotpink indianred indigo ' +
    'ivory khaki lavender lavenderblush lawngreen lemonchiffon lightblue lightcoral lightcyan ' +
    'lightgoldenrodyellow lightgray lightgreen lightgrey lightpink lightsalmon lightseagreen lightskyblue ' +
    'lightslategray lightslategrey lightsteelblue lightyellow lime limegreen linen magenta maroon ' +
    'mediumaquamarine mediumblue mediumorchid mediumpurple mediumseagreen mediumslateblue ' +
    'mediumspringgreen mediumturquoise mediumvioletred midnightblue mintcream mistyrose moccasin ' +
    'navajowhite navy oldlace olive olivedrab orange orangered orchid palegoldenrod palegreen ' +
    'paleturquoise palevioletred papayawhip peachpuff peru pink plum powderblue purple rebeccapurple red ' +
    'rosybrown royalblue saddlebrown salmon sandybrown seagreen seashell sienna silver skyblue slateblue ' +
    'slategray slategrey snow springgreen steelblue tan teal thistle tomato turquoise violet wheat white ' +
    'whitesmoke yellow yellowgreen'
  ).split(' '),
);

const FUNCOES_DE_COR = new Set(['rgb', 'rgba', 'hsl', 'hsla']);

/** Propriedade cujo VALOR é cor (visual-standards.md#2.1); custom property de componente também conta. */
const ehPropriedadeDeCor = (nome) =>
  nome === 'color' ||
  nome.startsWith('--') ||
  nome.startsWith('background') ||
  nome.startsWith('border') ||
  nome.startsWith('outline') ||
  ['fill', 'stroke', 'box-shadow', 'text-shadow', 'caret-color', 'accent-color'].includes(nome);

/**
 * As declarações `propriedade: valor` de um CSS, fora de comentário, com a linha do `:`. Um `:`
 * seguido de `{` antes de `;`/`}` é pseudo-classe ou `@media (...)`, não declaração — e é pulado.
 */
function declaracoesDe(texto) {
  const declaracoes = [];
  let linha = 1;
  for (let i = 0; i < texto.length; i++) {
    const c = texto[i];
    if (c === '\n') {
      linha++;
    } else if (c === '/' && texto[i + 1] === '*') {
      const fim = texto.indexOf('*/', i + 2);
      const trecho = texto.slice(i, fim < 0 ? texto.length : fim + 2);
      linha += trecho.split('\n').length - 1;
      i += trecho.length - 1;
    } else if (c === ':') {
      let k = i - 1;
      while (k >= 0 && ehEspaco(texto[k])) k--;
      let inicio = k;
      while (inicio >= 0 && ehIdentificador(texto[inicio])) inicio--;
      const propriedade = texto.slice(inicio + 1, k + 1).toLowerCase();
      let j = i + 1;
      while (j < texto.length && texto[j] !== ';' && texto[j] !== '}' && texto[j] !== '{') j++;
      if (propriedade && texto[j] !== '{') {
        declaracoes.push({ propriedade, valor: texto.slice(i + 1, j), linha });
      }
    }
  }
  return declaracoes;
}

/** Os identificadores de um valor CSS, fora de aspas, com o caractere que os segue. */
function identificadoresDe(valor) {
  const saida = [];
  let aspas = null;
  for (let i = 0; i < valor.length; i++) {
    const c = valor[i];
    if (aspas) {
      if (c === aspas) aspas = null;
    } else if (c === '"' || c === "'") {
      aspas = c;
    } else if (ehIdentificador(c) && !(c >= '0' && c <= '9')) {
      let j = i;
      while (j < valor.length && (ehIdentificador(valor[j]) || valor[j] === '.')) j++;
      let depois = j;
      while (depois < valor.length && ehEspaco(valor[depois])) depois++;
      saida.push({ nome: valor.slice(i, j), seguido: valor[depois] ?? '' });
      i = j - 1;
    }
  }
  return saida;
}

/**
 * 1. Cor literal em CSS, fora de comentário.
 *
 * Hex: a declaração de uma custom property (`--nome:` até o `;`) é isenta SOMENTE em `styles.css` —
 * é onde `visual-standards.md#2.1` manda declarar o token local, uma vez. O mesmo `--app-x: #hex`
 * em CSS de componente reprova, e qualquer hex em propriedade de USO reprova em qualquer arquivo.
 *
 * Cor por NOME (`white`) e por FUNÇÃO (`rgba(...)`): reprovam como valor de propriedade de cor em
 * CSS de COMPONENTE (decisão 6 da 010, R-06). `styles.css` fica fora deste ramo: a decisão aprovada
 * mediu "CSS de componente", e os usos de `white`/`rgba` que ele tem (sombras e fundos globais)
 * estão registrados como frouxo no roteiro da 010 — ampliar é decisão do product owner, não do gate.
 * `transparent`, `currentColor`, `inherit` e `var(...)` não são cor literal e passam.
 */
function hexEmCss(relatorio) {
  for (const arquivo of arquivos.filter(
    (a) => a.caminho.endsWith('.css') && a.caminho !== DONO_DOS_TOKENS_LOCAIS,
  )) {
    for (const { propriedade, valor, linha } of declaracoesDe(arquivo.texto)) {
      if (!ehPropriedadeDeCor(propriedade)) continue;
      for (const { nome, seguido } of identificadoresDe(valor)) {
        const minusculo = nome.toLowerCase();
        const funcao = seguido === '(' && FUNCOES_DE_COR.has(minusculo);
        const nomeada = seguido !== '(' && CORES_NOMEADAS.has(minusculo);
        if (!funcao && !nomeada) continue;
        const literal = funcao ? `${minusculo}(...)` : minusculo;
        relatorio.registrar(
          arquivo.caminho,
          linha,
          'visual-standards.md#2',
          literal,
          `cor cravada ${literal} em ${propriedade}`,
          'var(--gems-*) da paleta de provideGemsTheme() quando cor e papel coincidem; senão token local ' +
            'var(--app-*), declarado UMA vez em frontend/src/styles.css (visual-standards.md#2.1)',
        );
      }
    }
  }
  for (const arquivo of arquivos.filter((a) => a.caminho.endsWith('.css'))) {
    const texto = arquivo.texto;
    const podeDeclararToken = arquivo.caminho === DONO_DOS_TOKENS_LOCAIS;
    let linha = 1;
    for (let i = 0; i < texto.length; i++) {
      const c = texto[i];
      if (c === '\n') {
        linha++;
      } else if (c === '/' && texto[i + 1] === '*') {
        const fim = texto.indexOf('*/', i + 2);
        const trecho = texto.slice(i, fim < 0 ? texto.length : fim + 2);
        linha += trecho.split('\n').length - 1;
        i += trecho.length - 1;
      } else if (podeDeclararToken && c === '-' && texto[i + 1] === '-' && ehDeclaracao(texto, i)) {
        const fim = texto.indexOf(';', i);
        const trecho = texto.slice(i, fim < 0 ? texto.length : fim + 1);
        linha += trecho.split('\n').length - 1;
        i += trecho.length - 1;
      } else if (c === '#') {
        let j = i + 1;
        while (j < texto.length && ehHex(texto[j])) j++;
        const tamanho = j - i - 1;
        if ([3, 4, 6, 8].includes(tamanho) && !ehIdentificador(texto[j] ?? ' ')) {
          const hex = texto.slice(i, j).toLowerCase();
          relatorio.registrar(
            arquivo.caminho,
            linha,
            'visual-standards.md#2',
            hex,
            `cor cravada ${hex}`,
            'var(--gems-*) da paleta de provideGemsTheme() quando cor e papel coincidem; senão token local ' +
              'var(--app-*), declarado UMA vez em frontend/src/styles.css (visual-standards.md#2.1)',
          );
        }
        i = j - 1;
      }
    }
  }
}

function visitar(no, f) {
  f(no);
  ts.forEachChild(no, (filho) => visitar(filho, f));
}

const nomeDe = (no) => (no.name && ts.isIdentifier(no.name) ? no.name.text : '');

/** 2. `object` como tipo. */
function objectComoTipo(relatorio) {
  for (const fonte of fontesTs.filter((f) => !f.caminho.endsWith('.spec.ts'))) {
    visitar(fonte.ast, (no) => {
      if (no.kind === ts.SyntaxKind.ObjectKeyword && ts.isTypeNode(no)) {
        const dono = no.parent && nomeDe(no.parent);
        relatorio.registrar(
          fonte.caminho,
          linhaDe(fonte, no),
          'stores.md#7',
          dono || 'object',
          `${dono ? dono + ': ' : ''}object`,
          'a interface declarada — object passa pelo compilador sem proteger nada',
        );
      }
    });
  }
}

const ehStore = (classe) =>
  (classe.heritageClauses ?? []).some((h) =>
    h.types.some((t) => t.expression.getText().endsWith('GemsBaseStore')),
  );

function classes(fonte) {
  const encontradas = [];
  visitar(fonte.ast, (no) => {
    if (ts.isClassDeclaration(no)) encontradas.push(no);
  });
  return encontradas;
}

const CANONICOS = ['getAll', 'getById', 'search', 'create', 'update'];

function posicaoDe(metodo) {
  const nome = nomeDe(metodo);
  const privado = (ts.getCombinedModifierFlags(metodo) & ts.ModifierFlags.Private) !== 0;
  if (privado) return 7;
  const canonica = CANONICOS.indexOf(nome);
  if (canonica >= 0) return canonica;
  if (nome.startsWith('delete')) return 5;
  return 6;
}

/** 3. Ordem dos métodos da Store. */
function ordemDaStore(relatorio) {
  for (const fonte of fontesTs) {
    for (const classe of classes(fonte).filter(ehStore)) {
      const metodos = classe.members.filter(ts.isMethodDeclaration);
      let ultima = -1;
      let anterior = null;
      for (const metodo of metodos) {
        const posicao = posicaoDe(metodo);
        if (posicao < ultima) {
          relatorio.registrar(
            fonte.caminho,
            linhaDe(fonte, metodo),
            'stores.md#3',
            nomeDe(metodo),
            `${nomeDe(metodo)} vem depois de ${nomeDe(anterior)}`,
            'getAll > getById > search > create > update > delete{Entidade} > ações de negócio > helpers privados',
          );
        }
        if (posicao >= ultima) {
          ultima = posicao;
          anterior = metodo;
        }
      }
    }
  }
}

/** 4. Linha em branco entre métodos da Store. */
function linhaEmBrancoNaStore(relatorio) {
  for (const fonte of fontesTs) {
    for (const classe of classes(fonte).filter(ehStore)) {
      const membros = classe.members.filter(
        (m) => ts.isMethodDeclaration(m) || ts.isConstructorDeclaration(m),
      );
      for (let i = 1; i < membros.length; i++) {
        if (linhaDe(fonte, membros[i]) <= linhaFinalDe(fonte, membros[i - 1]) + 1) {
          relatorio.registrar(
            fonte.caminho,
            linhaDe(fonte, membros[i]),
            'stores.md#4',
            nomeDe(membros[i]) || 'constructor',
            `${nomeDe(membros[i]) || 'constructor'} colado ao método anterior`,
            'uma linha em branco entre métodos — é na borda que o revisor procura o erro',
          );
        }
      }
    }
  }
}

/** 5. `it('deveX_quandoY', ...)`. */
function nomeDeProva(relatorio) {
  for (const fonte of fontesTs.filter((f) => f.caminho.endsWith('.spec.ts'))) {
    visitar(fonte.ast, (no) => {
      if (!ts.isCallExpression(no)) return;
      const alvo = ts.isPropertyAccessExpression(no.expression)
        ? no.expression.expression
        : no.expression;
      if (!ts.isIdentifier(alvo) || !['it', 'test'].includes(alvo.text)) return;
      const primeiro = no.arguments[0];
      if (!primeiro || !ts.isStringLiteralLike(primeiro)) return;
      const nome = primeiro.text;
      // `naoDeve…` é forma listada pela própria regra (§7: `naoDevePersistir_quandoDadosInvalidos`).
      if (!((nome.startsWith('deve') || nome.startsWith('naoDeve')) && nome.includes('_quando'))) {
        relatorio.registrar(
          fonte.caminho,
          linhaDe(fonte, primeiro),
          'testing.md#7',
          nome,
          `prova chamada '${nome}'`,
          "it('deve{Acao}_quando{Condicao}', ...) — a mesma convenção do backend",
        );
      }
    });
  }
}

function decoradoresDe(no) {
  return (ts.canHaveDecorators(no) ? ts.getDecorators(no) : undefined) ?? [];
}

/** 6. `standalone: true`. */
function standaloneDeclarado(relatorio) {
  for (const fonte of fontesTs) {
    visitar(fonte.ast, (no) => {
      if (!ts.isClassDeclaration(no)) return;
      for (const decorador of decoradoresDe(no)) {
        if (!ts.isCallExpression(decorador.expression)) continue;
        const argumento = decorador.expression.arguments[0];
        if (!argumento || !ts.isObjectLiteralExpression(argumento)) continue;
        for (const propriedade of argumento.properties) {
          if (
            ts.isPropertyAssignment(propriedade) &&
            nomeDe(propriedade) === 'standalone' &&
            propriedade.initializer.kind === ts.SyntaxKind.TrueKeyword
          ) {
            relatorio.registrar(
              fonte.caminho,
              linhaDe(fonte, propriedade),
              'signals-standards.md#1',
              'standalone',
              'standalone: true declarado',
              'omitir — é o padrão desde o Angular 19, e declará-lo é ruído que envelhece',
            );
          }
        }
      }
    });
  }
}

/** 7. `@Input()` / `@Output()`. */
function decoradoresDeEntradaESaida(relatorio) {
  for (const fonte of fontesTs) {
    visitar(fonte.ast, (no) => {
      if (!ts.isPropertyDeclaration(no) && !ts.isSetAccessorDeclaration(no)) return;
      for (const decorador of decoradoresDe(no)) {
        const expressao = ts.isCallExpression(decorador.expression)
          ? decorador.expression.expression
          : decorador.expression;
        if (ts.isIdentifier(expressao) && ['Input', 'Output'].includes(expressao.text)) {
          relatorio.registrar(
            fonte.caminho,
            linhaDe(fonte, decorador),
            'signals-standards.md#2',
            `${expressao.text}|${nomeDe(no)}`,
            `@${expressao.text}() em ${nomeDe(no)}`,
            expressao.text === 'Input'
              ? `${nomeDe(no)} = input<T>()`
              : `${nomeDe(no)} = output<T>()`,
          );
        }
      }
    });
  }
}

/** 8. `*ngIf` / `*ngFor` / `*ngSwitch` nos templates. */
function diretivasEstruturais(relatorio) {
  for (const arquivo of arquivos.filter((a) => a.caminho.endsWith('.html'))) {
    const linhas = arquivo.texto.split('\n');
    linhas.forEach((linha, indice) => {
      for (const diretiva of ['*ngIf', '*ngFor', '*ngSwitch']) {
        if (linha.includes(diretiva)) {
          relatorio.registrar(
            arquivo.caminho,
            indice + 1,
            'signals-standards.md#4',
            diretiva,
            `${diretiva} no template`,
            `${diretiva === '*ngIf' ? '@if' : diretiva === '*ngFor' ? '@for' : '@switch'} — o controle de fluxo nativo`,
          );
        }
      }
    });
  }
}

/** 9. `HttpClient` em componente ou Store. */
function httpClientForaDoLugar(relatorio) {
  for (const fonte of fontesTs.filter(
    (f) => f.caminho.endsWith('.component.ts') || f.caminho.endsWith('.store.ts'),
  )) {
    visitar(fonte.ast, (no) => {
      const ehInject =
        ts.isCallExpression(no) &&
        ts.isIdentifier(no.expression) &&
        no.expression.text === 'inject' &&
        no.arguments[0] &&
        no.arguments[0].getText() === 'HttpClient';
      const ehParametro = ts.isParameter(no) && no.type && no.type.getText() === 'HttpClient';
      if (ehInject || ehParametro) {
        relatorio.registrar(
          fonte.caminho,
          linhaDe(fonte, no),
          'stores.md#anti-patterns',
          'HttpClient',
          'HttpClient injetado',
          'GemsBaseStore.get/post/put/delete — o componente fala com a Store, e a Store com a SDK',
        );
      }
    });
  }
}

/** 10. `alert()` / `confirm()` nativos. */
function alertaNativo(relatorio) {
  for (const fonte of fontesTs.filter((f) => !f.caminho.endsWith('.spec.ts'))) {
    visitar(fonte.ast, (no) => {
      if (
        ts.isCallExpression(no) &&
        ts.isIdentifier(no.expression) &&
        ['alert', 'confirm'].includes(no.expression.text)
      ) {
        relatorio.registrar(
          fonte.caminho,
          linhaDe(fonte, no),
          'feedback-alerts.md#4',
          no.expression.text,
          `${no.expression.text}() nativo do browser`,
          'GemsAlertService — único ponto de alertas, toasts e confirmações',
        );
      }
    });
  }
}

/** 11. `BehaviorSubject` em componente. */
function behaviorSubjectEmComponente(relatorio) {
  for (const fonte of fontesTs.filter((f) => f.caminho.endsWith('.component.ts'))) {
    visitar(fonte.ast, (no) => {
      if (
        ts.isNewExpression(no) &&
        ts.isIdentifier(no.expression) &&
        no.expression.text === 'BehaviorSubject'
      ) {
        relatorio.registrar(
          fonte.caminho,
          linhaDe(fonte, no),
          'signals-standards.md#anti-patterns',
          'BehaviorSubject',
          'BehaviorSubject para estado local do componente',
          'signal() + computed()',
        );
      }
    });
  }
}

const APP = 'frontend/src/app/';

/** A pasta de feature (`organizacoes`, `home`…) de um caminho sob `features/`, ou `null`. */
function featureDe(caminho) {
  if (!caminho.startsWith(APP + 'features/')) return null;
  const resto = caminho.slice((APP + 'features/').length);
  const barra = resto.indexOf('/');
  return barra < 0 ? null : resto.slice(0, barra);
}

/** O caminho (relativo à raiz do repositório) que um especificador relativo aponta, ou `null`. */
function resolverImport(fonte, especificador) {
  if (!especificador.startsWith('.')) return null;
  const dir = fonte.caminho.slice(0, fonte.caminho.lastIndexOf('/'));
  const partes = dir.split('/');
  for (const parte of especificador.split('/')) {
    if (parte === '..') partes.pop();
    else if (parte !== '.') partes.push(parte);
  }
  return partes.join('/');
}

/**
 * 12. Direção de import. `core/` e `shared/` são a base e não conhecem `features/`; uma feature não
 * conhece outra. O gate lê `import … from`, `export … from` e `import()` dinâmico, e resolve o
 * especificador relativo pelo caminho — o projeto não tem `paths` no `tsconfig`, então especificador
 * absoluto é pacote (`@angular/*`, a SDK) e não entra na conta. Sem baseline: o total na abertura
 * da T060 era zero, e permanece zero.
 */
function direcaoDeImport(relatorio) {
  for (const fonte of fontesTs) {
    const origemEhBase =
      fonte.caminho.startsWith(APP + 'core/') || fonte.caminho.startsWith(APP + 'shared/');
    const featureDeOrigem = featureDe(fonte.caminho);
    if (!origemEhBase && !featureDeOrigem) continue;
    visitar(fonte.ast, (no) => {
      let especificador = null;
      if ((ts.isImportDeclaration(no) || ts.isExportDeclaration(no)) && no.moduleSpecifier) {
        especificador = no.moduleSpecifier;
      } else if (
        ts.isCallExpression(no) &&
        no.expression.kind === ts.SyntaxKind.ImportKeyword &&
        no.arguments[0]
      ) {
        especificador = no.arguments[0];
      }
      if (!especificador || !ts.isStringLiteralLike(especificador)) return;
      const alvo = resolverImport(fonte, especificador.text);
      const featureDoAlvo = alvo && featureDe(alvo);
      if (!featureDoAlvo || featureDoAlvo === featureDeOrigem) return;
      const camada = origemEhBase
        ? fonte.caminho.slice(APP.length).split('/')[0] + '/'
        : `features/${featureDeOrigem}/`;
      relatorio.registrar(
        fonte.caminho,
        linhaDe(fonte, especificador),
        'organization.md#1',
        especificador.text,
        `${camada} importa features/${featureDoAlvo}/ (${especificador.text})`,
        origemEhBase
          ? 'core/ e shared/ não conhecem features/ — o que a base precisa mora nela ou na SDK'
          : 'uma feature não importa outra — o que é comum sobe para shared/ ou core/',
      );
    });
  }
}

const FEATURES = APP + 'features/';

/**
 * Os sufixos admitidos por subpasta de uma feature (organization.md#6), já sem a extensão. O que
 * sobra antes do sufixo é o `{modulo}`; `.spec.ts` é admitido ao lado de qualquer `.ts` admitido.
 */
const SUFIXOS_POR_PASTA = {
  models: [
    { sufixo: '-filter.model', extensoes: ['ts'] },
    { sufixo: '.model', extensoes: ['ts'] },
  ],
  services: [{ sufixo: '.store', extensoes: ['ts'] }],
  list: [{ sufixo: '-list.component', extensoes: ['ts', 'html', 'css'] }],
  view: [{ sufixo: '-view.component', extensoes: ['ts', 'html', 'css'] }],
  form: [
    { sufixo: '-form.component', extensoes: ['ts', 'html', 'css'] },
    { sufixo: '-state.service', extensoes: ['ts'] },
    { sufixo: '.config', extensoes: ['ts'] },
  ],
};

const ehKebab = (s) =>
  s.length > 0 &&
  !s.startsWith('-') &&
  !s.endsWith('-') &&
  [...s].every((c) => (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c === '-');

/** `{ base, extensao, spec }` de `organizacao.store.spec.ts`: base `organizacao.store`, `ts`, spec. */
function partesDoNome(nome) {
  const spec = nome.endsWith('.spec.ts');
  const semSpec = spec ? nome.slice(0, -'.spec.ts'.length) + '.ts' : nome;
  const ponto = semSpec.lastIndexOf('.');
  return { base: semSpec.slice(0, ponto), extensao: semSpec.slice(ponto + 1), spec };
}

/** O `{modulo}` que o nome carrega se ele casa com um dos sufixos admitidos na pasta; senão `null`. */
function moduloDe(base, extensao, admitidos) {
  for (const { sufixo, extensoes } of admitidos) {
    if (base.endsWith(sufixo) && extensoes.includes(extensao)) {
      const modulo = base.slice(0, -sufixo.length);
      return ehKebab(modulo) ? modulo : null;
    }
  }
  return null;
}

/**
 * 13. Nome de arquivo por subpasta de feature. Lê a árvore real de `features/` — o sistema de
 * arquivos é a fonte, não uma lista à mão — e aplica o padrão da §6 por subpasta: raiz só
 * `{pasta}.routes.ts` e página `{pasta}.component.*`; `models/`, `services/`, `list/`, `view/`,
 * `form/` com os sufixos da tabela; `form/steps/step-{nome}/` com `step-{nome}.component.*`. O
 * `{modulo}` que os arquivos carregam tem de ser um só por feature (`organizacao-*` em
 * `organizacoes/`). Sem baseline: o total na abertura era zero, e permanece zero.
 */
function nomeDeArquivoPorCamada(relatorio) {
  const REGRA = 'organization.md#6';
  const modulosPorFeature = new Map();
  const registrar = (arquivo, violacao, esperado) =>
    relatorio.registrar(
      arquivo.caminho,
      1,
      REGRA,
      arquivo.caminho.split('/').pop(),
      violacao,
      esperado,
    );

  for (const arquivo of arquivos.filter((a) => a.caminho.startsWith(FEATURES))) {
    const segmentos = arquivo.caminho.slice(FEATURES.length).split('/');
    const pasta = segmentos[0];
    const nome = segmentos[segmentos.length - 1];
    const subpastas = segmentos.slice(1, -1);
    const { base, extensao } = partesDoNome(nome);

    if (subpastas.length === 0) {
      const admitido =
        (base === `${pasta}.routes` && extensao === 'ts') ||
        (base === `${pasta}.component` && ['ts', 'html', 'css'].includes(extensao));
      if (!admitido) {
        registrar(
          arquivo,
          `${nome} na raiz de features/${pasta}/`,
          `${pasta}.routes.ts ou página ${pasta}.component.{ts,html,css}; entidade vai para models/, services/, list/, view/ ou form/`,
        );
      }
      continue;
    }

    const [subpasta, ...resto] = subpastas;
    if (subpasta === 'form' && resto[0] === 'steps') {
      const step = resto[1];
      const admitido =
        resto.length === 2 &&
        step?.startsWith('step-') &&
        ehKebab(step) &&
        base === `${step}.component` &&
        ['ts', 'html', 'css'].includes(extensao);
      if (!admitido) {
        registrar(
          arquivo,
          `${nome} em features/${pasta}/${subpastas.join('/')}/`,
          'form/steps/step-{nome}/step-{nome}.component.{ts,html,css} — a pasta e o arquivo com o mesmo nome',
        );
      }
      continue;
    }

    const admitidos = resto.length === 0 ? SUFIXOS_POR_PASTA[subpasta] : undefined;
    if (!admitidos) {
      registrar(
        arquivo,
        `subpasta ${subpastas.join('/')}/ em features/${pasta}/`,
        'só models/, services/, list/, view/, form/ e form/steps/step-{nome}/ (organization.md#1)',
      );
      continue;
    }

    const modulo = moduloDe(base, extensao, admitidos);
    if (!modulo) {
      registrar(
        arquivo,
        `${nome} em features/${pasta}/${subpasta}/`,
        admitidos.map((a) => `{modulo}${a.sufixo}.{${a.extensoes.join(',')}}`).join(' ou ') +
          ' (e o .spec.ts ao lado)',
      );
      continue;
    }
    const vistos = modulosPorFeature.get(pasta) ?? new Map();
    vistos.set(modulo, (vistos.get(modulo) ?? 0) + 1);
    modulosPorFeature.set(pasta, vistos);
    arquivo.modulo = modulo;
  }

  for (const [pasta, vistos] of modulosPorFeature) {
    if (vistos.size < 2) continue;
    const dominante = [...vistos.entries()].sort((a, b) => b[1] - a[1])[0][0];
    for (const arquivo of arquivos.filter(
      (a) => a.caminho.startsWith(`${FEATURES}${pasta}/`) && a.modulo && a.modulo !== dominante,
    )) {
      registrar(
        arquivo,
        `{modulo} "${arquivo.modulo}" numa feature cujos arquivos usam "${dominante}"`,
        `um único {modulo} por feature — ${dominante}`,
      );
    }
  }
}

const regras = [
  hexEmCss,
  objectComoTipo,
  ordemDaStore,
  linhaEmBrancoNaStore,
  nomeDeProva,
  standaloneDeclarado,
  decoradoresDeEntradaESaida,
  diretivasEstruturais,
  httpClientForaDoLugar,
  alertaNativo,
  behaviorSubjectEmComponente,
  direcaoDeImport,
  nomeDeArquivoPorCamada,
];

// ---------------------------------------------------------------------------------------------
// Execução — o modo normal, e o modo bruto que revela a exceção órfã

const relatorio = novoRelatorio('suprimir');
regras.forEach((regra) => regra(relatorio));

const bruto = novoRelatorio('bruto');
regras.forEach((regra) => regra(bruto));

const orfas = excecoes.filter(
  (e) =>
    !bruto.violacoes.some(
      (v) =>
        v.caminho === e.caminho &&
        v.linha === e.linhaCoberta &&
        'frontend-angular/' + v.regra === e.regra,
    ),
);

if (process.env.GEMS_CONVENCOES_RELATORIO === 'true') {
  const { writeFileSync } = await import('node:fs');
  writeFileSync(
    resolve(repoRoot, 'frontend', 'conventions-report.json'),
    JSON.stringify(bruto.violacoes, null, 2) + '\n',
  );
  console.log(`Convenções: ${bruto.violacoes.length} violações brutas (relatório)`);
  process.exit(0);
}

const mensagens = [
  ...relatorio.violacoes.map(formatar),
  ...orfas.map(
    (e) =>
      `${e.caminho}:${e.linhaDoComentario}\n  regra:    .gems-ai/rules/conformance-exceptions.md#3\n` +
      `  violação: exceção órfã — a linha coberta já está conforme a ${e.regra}\n` +
      `  esperado: remover a exceção: o que sobrou é ruído com aparência de autorização (INV-1)`,
  ),
];

if (mensagens.length > 0) {
  throw new Error(
    `Convenções do frontend reprovou — ${mensagens.length} violação(ões):\n\n${mensagens.join('\n\n')}\n`,
  );
}

const suprimidasPeloBaseline =
  totalNoBaseline - [...saldoDoBaseline.values()].reduce((s, v) => s + v, 0);
console.log(
  `Convenções do frontend: ${regras.length} regras em ${arquivos.length} arquivos, 0 violações ` +
    `(${suprimidasPeloBaseline} no baseline, ${totalNoBaseline - suprimidasPeloBaseline} entrada(s) do baseline já corrigida(s)).`,
);

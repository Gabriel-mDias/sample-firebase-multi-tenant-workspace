---
stack: all
module: conformance
severity: mandatory
see-also: [backend-java/repositories.md, backend-java/services.md, frontend-angular/stores.md]
origem: local
precedencia: local-vence
versao: 2.0.0
---

# Exceção escrita de conformidade — `GEMS-EXCEPTION`

> **TL;DR:**
> - Forma única: `GEMS-EXCEPTION(<arquivo-de-regra>#<seção>): <motivo>; expira AAAA-MM-DD`.
> - Vai no comentário nativo da linguagem, na linha **imediatamente acima** do desvio.
> - Os quatro campos são obrigatórios. Data **no passado reprova o build**.
> - Exceção sobre linha que **não** desvia (órfã) reprova. Regra inexistente reprova.
> - Ela **não** é baseline, e a diferença está na §4 — é a regra mais importante deste arquivo.
> - Quem escreve é **pessoa**, decidindo. Agente não escreve exceção para contornar gate vermelho.

---

## 1. Por que este mecanismo existe

A regra de escrita vence o código consolidado. Sem uma válvula, essa inversão seria **absoluta** — e
absoluto é exatamente o que se desliga no primeiro caso legítimo em que a regra não serve.

A exceção escrita é essa válvula, e ela é desenhada para ser **cara o suficiente para não virar
hábito**: exige nomear a regra, escrever o motivo no ponto do código e assumir uma data. Quando a
data chega, o build reprova. Não há terceira saída — ou o desvio é corrigido, ou alguém revisa o
motivo e assume uma data nova, por escrito.

É o que separa este mecanismo de um `TODO`: `TODO` não tem dono, não tem prazo e nunca reprovou
nada.

---

## 2. Forma canônica

```text
GEMS-EXCEPTION(<arquivo-de-regra>#<seção>): <motivo>; expira <AAAA-MM-DD>
```

| Campo | Obrigatório | Regra |
| :--- | :--- | :--- |
| `arquivo-de-regra` | sim | caminho dentro de `.gems-ai/rules/`. Arquivo inexistente **reprova** |
| `seção` | sim | âncora dentro do arquivo de regra |
| `motivo` | sim | texto não vazio. MUST dizer **por que aqui é diferente** — repetir o que a regra diz não é motivo |
| `expira` | sim | data ISO `AAAA-MM-DD`, **futura no momento em que é escrita** |

### Portadores, por linguagem

| Linguagem | Forma |
| :--- | :--- |
| Java | `// GEMS-EXCEPTION(...)` |
| TypeScript | `// GEMS-EXCEPTION(...)` |
| HTML | `<!-- GEMS-EXCEPTION(...) -->` |
| CSS | `/* GEMS-EXCEPTION(...) */` |

O comentário vai **na linha imediatamente acima** da linha que desvia. Não vale no fim da linha, não
vale duas linhas acima, não vale no topo do arquivo: a exceção suprime **um** desvio, aquele, e é a
proximidade que impede que ela vire uma licença de arquivo inteiro.

### Exemplos

```java
// GEMS-EXCEPTION(backend-java/repositories.md#2.4): o filtro de período compara duas colunas
// entre si, e o ObjectUtil não expressa a comparação; expira 2026-12-31
if ( inicio != null && fim != null ) {
```

```typescript
// GEMS-EXCEPTION(frontend-angular/signals-standards.md#3): o valor vem do Router antes do primeiro
// ciclo de detecção e não pode ser signal; expira 2026-10-30
private readonly rotaInicial = this.route.snapshot.params['id'];
```

```html
<!-- GEMS-EXCEPTION(frontend-angular/lists.md#4): a tabela legada do relatório fiscal é substituída
     na rodada 009; expira 2026-11-30 -->
<table class="tabela-legada">
```

```css
/* GEMS-EXCEPTION(frontend-angular/visual-standards.md#2): cor exigida por contrato de marca do
   cliente, fora da paleta; expira 2027-01-31 */
--cor-destaque: #ff6600;
```

---

## 3. Ciclo de vida

```text
escrita (data futura) ──► VÁLIDA ──► suprime o desvio e aparece no inventário
                             │
                             └──► a data chega ──► VENCIDA ──► reprova o build
                                                               e deixa de suprimir
```

Uma exceção vencida **falha duas vezes de propósito**: ela reprova por estar vencida, e o desvio que
ela protegia volta a reprovar por conta própria. Não há como deixá-la apodrecer em silêncio.

### O inventário é derivado, nunca editado

A **fonte única** são os comentários no código. O inventário é o **artefato derivado**, e o build
verifica que um bate com o outro — o mesmo desenho do catálogo de ações (`AcaoSistema` →
`authorization.actions.json` → gate Maven), que este projeto já opera.

**Editar o inventário à mão é o defeito, não o atalho.** Era exatamente o que o catálogo de ações
tinha antes da rodada 007, quando três donos mantinham cópias que divergiam em silêncio.

### As três formas de reprovar

| Situação | Por que reprova |
| :--- | :--- |
| **Vencida** | a data passou. O prazo era o compromisso, e ele venceu |
| **Órfã** — exceção sobre linha que já está conforme | ela suprime um desvio que não existe mais; o que sobrou é ruído com aparência de autorização |
| **Regra inexistente** | `arquivo-de-regra#seção` não resolve. Exceção que aponta para o nada não foi revisada por ninguém |

A detecção de órfã exige avaliar a regra **ignorando a própria supressão** — o gate roda em dois
modos, e é a comparação entre eles que revela a exceção que perdeu o objeto.

---

## 4. Exceção **não** é baseline

Esta é a regra mais importante deste arquivo. Confundir os dois transforma o baseline no caminho pelo
qual o desvio volta — com autorização, e sem ninguém ter decidido nada.

| | Baseline | Exceção escrita |
| :--- | :--- | :--- |
| O que é | dívida que uma rodada não alcançou | desvio **decidido** |
| Justificativa | nenhuma, individual | obrigatória, no ponto do código |
| Prazo | nenhum | obrigatório, e reprova quando vence |
| Quem escreve | a rodada, uma vez | uma pessoa, quando decide |
| Cresce? | **nunca** — só encolhe | sim, com justificativa |

Os três baselines do projeto:

| Arquivo | Gate |
| :--- | :--- |
| `backend/config/checkstyle/suppressions.xml` | Checkstyle |
| `backend/meduc-deploy/src/test/resources/archunit-baseline.txt` | ArchUnit e gate de fonte |
| `.gems-ai/conformance-baseline.json` | convenções do frontend |

- **O baseline só encolhe.** Entrada nova nele é regressão, e o gate reprova. É o que "catraca"
  significa.
- **Desvio novo nunca entra no baseline.** Ou é corrigido, ou vira exceção escrita. Não há terceira
  porta, e é essa ausência que mantém o mecanismo honesto.

---

## 5. Quem escreve, e quem não escreve

**Escreve**: uma pessoa, tomando uma decisão que ela assume pelo prazo que escreveu.

**Não escreve**: agente de codificação, para fazer um gate vermelho ficar verde. Gate vermelho é
informação — ou o código está errado, ou a regra está errada. Nos dois casos a saída é corrigir o que
está errado, e num deles a correção é **na regra**, como a rodada 008 fez com a anotação transacional
de `services.md`.

Exceção escrita por agente para se desbloquear é o defeito que este mecanismo inteiro existe para
tornar visível.

---

## Anti-Patterns (NUNCA)

| Proibido | Correto |
| :--- | :--- |
| `// TODO: arrumar isso depois` sobre um desvio | `GEMS-EXCEPTION(...)` com regra, motivo e data |
| Exceção sem `expira` | a data é obrigatória — sem prazo, é permissão permanente |
| Exceção com motivo que repete a regra ("aqui não usamos ObjectUtil") | o motivo diz **por que aqui é diferente** |
| Exceção no topo do arquivo, cobrindo tudo | uma exceção, um desvio, na linha de cima |
| Acrescentar o desvio novo ao baseline | corrigir, ou escrever a exceção |
| Editar o inventário derivado | editar a fonte — o comentário no código |
| Renovar a data sem revisar o motivo | revisar o motivo **é** a renovação |

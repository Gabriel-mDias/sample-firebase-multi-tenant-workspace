---
stack: frontend-angular
module: vitest, @angular/core/testing
severity: mandatory
see-also: [stores.md, forms.md, lists.md, auth.md]
origem: local
precedencia: local-vence
versao: 2.0.0
---

# Testes — Angular / gems-sdk

> **TL;DR:**
> - Framework: **Vitest**. `describe`/`it`/`expect`/`vi` são **importados** de `'vitest'`, nunca globais.
> - **Nunca** Jasmine, Karma ou Jest — nem `jasmine.createSpyObj`, nem `and.returnValue`, nem `toBeFalse()`.
> - Dublê: `vi.fn()` e objeto literal `Partial<T>` em `useValue`. Não existe `createSpyObj` em Vitest.
> - Componentes: `TestBed` com o próprio componente em `imports` — ele é standalone.
> - Padrão AAA: **Arrange** (setup) → **Act** (invocar) → **Assert** (verificar).
> - Nomenclatura: `deveFazerAlgo_quandoCondicao` (camelCase, português, sem "it should").
> - `HttpClientTestingModule` **não** se usa em teste de componente — só em teste de Store.

> **Por que esta regra foi reescrita.** Ela prescrevia Jasmine + Karma. A rodada `006` trocou a
> plataforma de teste do Angular por **Vitest** (bloco S7), e o `AGENTS.md` do frontend proíbe
> explicitamente Jasmine, Karma e Jest. A regra literal não compilava: `jasmine` não existe no
> projeto. Corrigida na rodada `008`, bloco M.

---

## 1. Estrutura de Arquivo

O `.spec.ts` mora **ao lado** do arquivo que ele testa — nunca numa pasta `test/` paralela.

```
features/{modulo}/
├── list/
│   ├── {modulo}-list.component.ts
│   └── {modulo}-list.component.spec.ts   ← mesmo diretório
├── form/
│   ├── {modulo}-form.component.ts
│   └── {modulo}-form.component.spec.ts
└── services/
    ├── {modulo}.store.ts
    └── {modulo}.store.spec.ts
```

---

## 2. Imports Obrigatórios

Vitest **não** injeta globais neste projeto. Todo `describe`, `it`, `expect`, `vi`, `beforeEach` e
`afterEach` é importado explicitamente:

```typescript
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { TestBed } from '@angular/core/testing';
```

Esquecer o import de `'vitest'` é o erro mais comum na migração de um exemplo escrito para Jasmine:
o arquivo passa no editor (os tipos globais do Jasmine ainda vêm do Angular) e falha na execução.

---

## 3. Dublês — `vi.fn()` e `Partial<T>`

Vitest não tem `createSpyObj`. O dublê é um **objeto literal tipado como `Partial<T>`**, com
`vi.fn()` nos métodos que o teste observa:

```typescript
// CORRETO — Vitest
const isAuthorized = vi.fn(() => true);
const auth: Partial<AuthService> = { isLoggedIn: () => true, isAuthorized };

TestBed.configureTestingModule({
  providers: [{ provide: AuthService, useValue: auth }],
});

expect(isAuthorized).toHaveBeenCalledWith(requirement);
```

Para um método que devolve `Observable`, devolva o `Observable` direto — não existe `and.returnValue`:

```typescript
import { of, throwError } from 'rxjs';

const store: Partial<OrganizacaoStore> = {
  search: vi.fn(() => of({ content: [], totalElements: 0 } as Page<OrganizacaoSummary>)),
};

// Caminho de erro, no mesmo teste ou em outro:
store.search = vi.fn(() => throwError(() => ({ status: 500 })));
```

### Equivalência com Jasmine, para quem traz exemplo de fora

| Jasmine (proibido) | Vitest (use este) |
| :--- | :--- |
| `jasmine.createSpyObj('X', ['a','b'])` | `{ a: vi.fn(), b: vi.fn() } as Partial<X>` |
| `spy.and.returnValue(of(x))` | `vi.fn(() => of(x))` |
| `spy.and.resolveTo(x)` | `vi.fn(async () => x)` |
| `expect(v).toBeFalse()` / `toBeTrue()` | `expect(v).toBe(false)` / `toBe(true)` |
| `expect(spy).toHaveBeenCalledTimes(1)` | igual — esta existe nos dois |
| `jasmine.SpyObj<T>` como tipo | `Partial<T>`, ou `ReturnType<typeof vi.fn>` no método |

---

## 4. Teste de Componente

O componente é standalone: ele entra em `imports`, não em `declarations`. As dependências entram
como `useValue`, e o roteador como `provideRouter([])`.

```typescript
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { GemsAlertService } from '@gabriel-mdias/angular-gems-sdk';
import { {Entidade}ListComponent } from './{entidade}-list.component';
import { {Entidade}Store } from '../services/{entidade}.store';

function criar(store: Partial<{Entidade}Store>, alert: Partial<GemsAlertService> = {}) {
  TestBed.configureTestingModule({
    imports: [{Entidade}ListComponent],
    providers: [
      provideRouter([]),
      { provide: {Entidade}Store, useValue: store },
      { provide: GemsAlertService, useValue: { errorFromApi: vi.fn(), success: vi.fn(), ...alert } },
    ],
  });
  const fixture = TestBed.createComponent({Entidade}ListComponent);
  fixture.detectChanges();   // dispara ngOnInit e propaga os signals
  return fixture;
}

describe('{Entidade}ListComponent', () => {
  afterEach(() => TestBed.resetTestingModule());

  it('deveCarregarRegistros_quandoInicializar', () => {
    const search = vi.fn(() => of({ content: [{ id: '1' }], totalElements: 1 }));

    const fixture = criar({ search });

    expect(search).toHaveBeenCalledTimes(1);
    expect(fixture.componentInstance.totalRecords()).toBe(1);
  });

  it('deveAvisarOUsuario_quandoABuscaFalhar', () => {
    const errorFromApi = vi.fn();
    const search = vi.fn(() => throwError(() => ({ status: 500 })));

    criar({ search }, { errorFromApi });

    expect(errorFromApi).toHaveBeenCalled();
  });
});
```

### Membro `protected` no teste

O padrão do projeto declara o estado do componente como `protected readonly`. Para lê-lo no teste
sem afrouxar o componente, faça o cast no ponto de leitura — não troque o modificador:

```typescript
expect((app as unknown as { appName: string }).appName).toBe('Meduc');
```

---

## 5. Teste de Store

É o **único** lugar onde `HttpClientTestingModule` (ou `provideHttpClientTesting`) entra:

```typescript
import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { {Entidade}Store } from './{entidade}.store';

describe('{Entidade}Store', () => {
  let store: {Entidade}Store;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), {Entidade}Store],
    });
    store = TestBed.inject({Entidade}Store);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('deveBuscarPorId_quandoChamarDetail', () => {
    const esperado = { id: '1', nome: 'Teste' };

    store.detail('1').subscribe((res) => expect(res).toEqual(esperado as never));

    http.expectOne((req) => req.url.endsWith('/1')).flush(esperado);
  });
});
```

---

## 6. Teste de Função Pura

Construtor de menu, parser de claim, conversor de ordenação: função pura se testa **sem** `TestBed`,
sem Keycloak e sem HTTP. É o teste mais barato e o mais estável — prefira extrair a lógica para uma
função pura sempre que ela não depender do ciclo de vida do Angular.

```typescript
import { describe, expect, it } from 'vitest';
import { parseTableSort } from './table-sort';

describe('parseTableSort', () => {
  it('deveLerOFormatoDoSpringData_quandoSortPresente', () => {
    expect(parseTableSort(['nome,desc'], { field: 'id', direction: 'asc' })).toEqual({
      field: 'nome',
      direction: 'desc',
    });
  });
});
```

---

## 7. Padrões

### Nomenclatura obrigatória

```
deveRetornarLista_quandoSearchComSucesso()
deveNegarAcesso_quandoAcaoAusenteNoToken()
naoDevePersistir_quandoDadosInvalidos()
deveNavegar_quandoSalvarComSucesso()
```

camelCase, em português, sem "it should" e sem frase corrente. É a mesma convenção que o backend
adotou (`deveX_quandoY`), para que uma prova se leia igual dos dois lados da pilha.

### Regras

| Regra | Detalhe |
| :--- | :--- |
| `describe`/`it`/`expect`/`vi` importados de `'vitest'` | Este projeto não usa globais de teste |
| Componente standalone vai em `imports` | Nunca `declarations`; nunca `NgModule` de teste |
| `fixture.detectChanges()` após o setup | Dispara `ngOnInit` e a propagação dos signals |
| `await fixture.whenStable()` | Para o que depende de Promise (ex.: `alertService.confirm`) |
| `TestBed.resetTestingModule()` no `afterEach` | Quando o arquivo monta o TestBed mais de uma vez |
| Nunca `HttpClientTestingModule` em teste de componente | Componente recebe dublê da Store |
| Signal se lê com `()` e se escreve com `.set()` | `component.isLoading()` / `component.isLoading.set(true)` |
| AAA obrigatório | Arrange → Act → Assert, sem misturar |

---

## Anti-Patterns (NUNCA)

| Proibido | Correto |
| :--- | :--- |
| `import 'jasmine'`, `jasmine.createSpyObj`, `karma.conf.js` | Vitest: `vi.fn()` + `Partial<T>` em `useValue` |
| `expect(v).toBeFalse()` | `expect(v).toBe(false)` — matcher de Jasmine não existe em Vitest |
| `spy.and.returnValue(of(x))` | `vi.fn(() => of(x))` |
| `describe`/`it` sem import, contando com global | `import { describe, it } from 'vitest'` |
| `HttpClientTestingModule` em teste de componente | Apenas em teste de Store |
| `component.isLoading = true` (mutação direta) | `component.isLoading.set(true)` |
| Nome de teste: `it('testa busca', ...)` | `it('deveRetornarLista_quandoSearchComSucesso', ...)` |

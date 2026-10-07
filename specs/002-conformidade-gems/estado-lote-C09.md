# Estado — C09, rodada 002

Data: 2026-09-21. Estado: **verde**. Âncora inicial:
`aedaf47cdcf52f1ab7b920fd1afec1a29cfb3066`.

## 1. Rollback deste lote

Reverter por edição inversa somente os caminhos da tabela do pacote C09 contra a âncora. Remover
nomeadamente apenas `pacote-de-execucao-C09.md` e este estado se necessário. Preservar integralmente
os artefatos descontados em `ancora-porcelain-C08.txt`; nenhum rollback amplo é autorizado.

## 2. Concluído

| Tarefa | O que provou |
| :-- | :--- |
| Preparação | Âncora `aedaf47` confirmada; fronteira, rollback e arquivos pré-existentes registrados antes de editar código. |
| T070 | `standalone: true` removido do footer; contraste usa `--gems-secondary-contrast`; `styles.css` não tem uso literal de cor fora das declarações `--meduc-*` no `:root`. |
| T071 | Gate compara as 21 chaves e valores de `Acoes` com `AcaoSistema`; passou verde. A mutação `CONSULTAR_PARCEIRO` → `CONSULTAR_PARCEIRO_DIVERGENTE` falhou por chave/valor divergentes e foi restaurada por edição inversa. |
| Gate estático | `verify-conventions.mjs`: 13 regras/124 arquivos/0 violações efetivas; Prettier e `git diff --check` verdes. |
| T072 | 6 arquivos/30 testes verdes. |
| T073 | 7 arquivos/18 testes verdes. |

## 3. Em curso

- Nenhuma. O checkpoint foi fechado após o build Docker e os gates finais.

## 4. Não iniciado

O próximo checkpoint é C10-SDK. Ele permanece fora deste pacote e não foi iniciado.

## 5. Arquivos tocados

`git status --porcelain --untracked-files=all`, menos este próprio estado:

```text
 M .github/workflows/build-images.yml
 M frontend/Dockerfile
 M frontend/package.json
 M frontend/scripts/verify-authorization-actions.mjs
 M frontend/src/app/core/footer/app-footer.component.css
 M frontend/src/app/core/footer/app-footer.component.ts
 M frontend/src/styles.css
 M specs/002-conformidade-gems/roteiro.md
 M specs/002-conformidade-gems/tasks.md
?? specs/002-conformidade-gems/pacote-de-execucao-C09.md
```

## 6. Decisões fora do pacote

O pacote C09 que constava na fotografia C08 não estava presente no disco no início desta retomada;
foi recriado com a mesma fronteira C09, sem alterar artefatos descontados. D-P04 segue desmarcado.
Antes do fechamento, a fronteira de T008 foi corrigida para declarar o workflow que fornece o secret
BuildKit exigido pelo Dockerfile; o diff já o continha e a omissão documental não foi tratada como
precedente nem mantida em silêncio.

O modo relatório de `verify-conventions.mjs` mediu 63 residuais brutos, somente para inventário:
2 em `acesso-negado.component.css | visual-standards.md#2`; 57 em `testing.md#7`
(`routes-coverage` 1, `localidade.store` 2, `parceiro-form-state` 8,
`historico-registro` 8, `field-tooltip` 2, `contato-fields` 1, `contato-form` 1,
`dados-pessoais-fields` 4, `dados-pessoais-form` 4, `cep-lookup` 7, `enter-busca` 5,
`perfil-atuacao` 4, `rg-format` 10); e 4 em `organization.md`
(`solicitacao-form | #1` 1; `parceiro-form-state` spec e source | #6 2;
`parceiro-response.model | #6` 1). Alternativa: encaminhar o inventário ao portão humano D-P04;
nenhum residual foi aceito, alterado ou convertido em exceção.

## 7. Gates de fechamento

`docker build --target build .` terminou com exit 0 usando somente o secret BuildKit previsto.
`npm test` terminou com 26 arquivos e 123 testes verdes; `npm run build` terminou verde, com bundle
inicial de 735,79 kB e o aviso de budget conhecido. Os gates de ações (21 em paridade), convenções
(13 regras, 124 arquivos, 0 violações), Prettier e `git diff --check` também terminaram verdes.

## 8. Retomada

- **Executor:** Codex
- **Token de retomada:** não aplicável; C09 está fechado. A seleção seguinte é C10-SDK.

# Frontend

- Angular 22 standalone; arquivos `.ts`, `.html` e `.css` separados.
- Feature autocontida em `features/<dominio>` e registrada na raiz somente por `loadChildren`.
- `shared/` não importa `features/`; uma feature não importa outra feature.
- Estado usa signals e stores `GemsBaseStore`; componente não usa `HttpClient` diretamente.
- Autorização espelha a ação concreta do backend em rota, menu e botões.
- Testes usam Vitest; não Jasmine, Karma ou Jest.

Features em `src/app/features/`: `parceiro`, `solicitacao`, `home`, `acesso-negado`.

Armadilhas obrigatórias — cada uma já produziu defeito silencioso na `angular-gems-sdk` 1.1.0:

- `gems-select`: `placeholder` e valor inicial `null`.
- `gems-select`/`gems-input-document` não têm `hint`.
- RG usa `gems-input-text` + `appRgMask`; `maskType="rg"` é proibido.
- Dentro de `gems-form-card`, popover usa `position: fixed`.
- Erro de campo usa `gems-field-error`; a cópia local da SDK 1.1.0 foi removida na rodada 002.
- Fora da tabela use `btn-secondary`, não `btn-default`.

Leia apenas as regras GEMS da tela afetada e use `features/parceiro` como referência. Gates:

```powershell
npm test
npm run build
```

Transição aprovada na constituição 2.0.0: a rodada 002 usa SDK 2.1.0,
`gems-field-error`, somente `shared/`, `isAuthorized` e rotas `list`/`form`/`form/:id`/`view/:id`.
`gems-input-password` permanece no checkpoint frontend seguinte.

Regras GEMS vencem código consolidado; referência não autoriza desvio. Consulte a constituição 2.0.0 e a seção Adoção de Módulo da SDK GEMS.

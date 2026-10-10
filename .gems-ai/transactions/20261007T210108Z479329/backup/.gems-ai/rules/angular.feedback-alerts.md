<!-- gems-ai-rule: {"id":"angular.feedback-alerts","version":"2.0.0","status":"candidate"} -->
---
stack: frontend-angular
module: gems-sdk (GemsAlertService)
severity: mandatory
see-also: [stores.md, forms.md, lists.md]
origem: local
precedencia: local-vence
versao: 2.0.0
---

# Feedback e Alertas — Angular / gems-sdk

> **TL;DR:**
>
> - `GemsAlertService` (gems-sdk): único ponto de alertas, toasts e confirmações. **NUNCA** `alert()` nativo.
> - `success/warning/error`: toasts efêmeros (auto-dismiss).
> - `errorFromApi(err)`: parse automático de `errorType` e `message` do envelope GEMS.
> - `confirm()`: modal SweetAlert2 com `await`. Checar `result.isConfirmed` antes de agir.
> - **Nunca** encadear alertas em sequência. Uma ação = um alert.
> - Erros HTTP: sempre `errorFromApi(err)` — **nunca** montar string de erro manualmente.

## 1. Injeção

```typescript
private alertService = inject(GemsAlertService);
```

Injetar via `inject()`, nunca como parâmetro de construtor.

---

## 2. Métodos Disponíveis

### success — Toast de sucesso

```typescript
this.alertService.success("Sucesso", "Operação realizada com sucesso.");
```

- Aparece no canto superior direito, auto-fecha em ~3s.
- Use após `next:` de operações de escrita (create/update/delete).

### warning — Toast de atenção

```typescript
this.alertService.warning("Atenção", "Preencha todos os campos obrigatórios.");
```

- Use para validação de formulário antes de chamar a API, ou para guiar o usuário.

### error — Toast de erro genérico

```typescript
this.alertService.error("Erro", "Ocorreu um problema inesperado.");
```

- Use apenas quando não há resposta de API (ex: erro de rede sem corpo).

### errorFromApi — Erro da API (Obrigatório para respostas HTTP)

```typescript
this.alertService.errorFromApi(err);
```

- Parse automático do envelope retornado pelo backend: `errorType` vira título e `message` vira texto.
- Uma `BusinessException(errors)` une a lista por quebra de linha dentro de `message`.
- **Sempre use este método** no `error:` de chamadas HTTP — nunca `error()` genérico para erros de API.

### confirm — Modal de confirmação

```typescript
async remove(id: string): Promise<void> {
  const result = await this.alertService.confirm('Excluir', 'Esta ação não pode ser desfeita.');
  if (result.isConfirmed) {
    this.store.delete{Entidade}(id).subscribe({
      next: () => {
        this.alertService.success('Sucesso', '{Entidade} excluído(a) com sucesso.');
        this.loadEntidades();
      },
      error: (err) => this.alertService.errorFromApi(err)
    });
  }
}
```

- Sempre `await`. O método retorna `{ isConfirmed: boolean }`.
- Checar `result.isConfirmed` antes de executar qualquer ação destrutiva.
- Usar em: exclusão, cancelamento com dados não salvos, ações irreversíveis.

---

## 3. Padrão Completo em Operações CRUD

### Create / Update

```typescript
save(): void {
  if (this.form.invalid) {
    this.alertService.warning('Atenção', 'Preencha todos os campos obrigatórios.');
    return;
  }
  this.isLoading.set(true);
  const call$ = this.isEdit()
    ? this.store.update(this.id, this.form.value)
    : this.store.create(this.form.value);

  call$.subscribe({
    next: () => {
      this.alertService.success('Sucesso', `{Entidade} ${this.isEdit() ? 'atualizado(a)' : 'criado(a)'} com sucesso.`);
      this.nav.navigate(['/{modulo}/list']);
    },
    error: (err) => {
      this.alertService.errorFromApi(err);
      this.isLoading.set(false);
    }
  });
}
```

### Delete

```typescript
async remove(id: string): Promise<void> {
  const result = await this.alertService.confirm(
    'Confirmar Exclusão',
    'Tem certeza que deseja excluir este(a) {entidade}? Esta ação não pode ser desfeita.'
  );
  if (!result.isConfirmed) return;

  this.store.delete{Entidade}(id).subscribe({
    next: () => {
      this.alertService.success('Excluído', '{Entidade} removido(a) com sucesso.');
      this.loadEntidades();
    },
    error: (err) => this.alertService.errorFromApi(err)
  });
}
```

---

## 4. Regras

| Regra                                          | Detalhe                                           |
| :--------------------------------------------- | :------------------------------------------------ |
| Nunca `alert()` nativo                         | Prejudica UX e não integra com o tema da SDK      |
| Nunca `console.log()` para feedback ao usuário | Logs são para debugging, não para UX              |
| Nunca dois alerts em sequência                 | Uma ação deve resultar em **um** feedback         |
| Erro HTTP → `errorFromApi()`                   | Parse automático de `errorType` e `message`       |
| Erro inesperado (rede) → `error()`             | Só quando não há corpo de resposta da API         |
| Confirmação → `confirm()` + `await`            | Antes de qualquer ação destrutiva ou irreversível |
| `isLoading.set(false)` no `error:`             | O loading deve ser desativado mesmo em falha      |

---

## Anti-Patterns (NUNCA)

| Proibido                                            | Correto                                                                           |
| :-------------------------------------------------- | :-------------------------------------------------------------------------------- |
| `alert('Salvo!')` nativo do browser                 | `this.alertService.success('Sucesso', 'Registro salvo.')`                         |
| `new GemsAlertService()`                                | `private alertService = inject(GemsAlertService)`                                     |
| `this.alertService.error('Erro', err.message)`      | `this.alertService.errorFromApi(err)` — parse automático de `errorType`/`message` |
| Dois `alertService.*()` em sequência na mesma ação  | Uma ação = um feedback apenas                                                     |
| Não chamar `isLoading.set(false)` no bloco `error:` | Sempre desativar loading em sucesso **e** em erro                                 |

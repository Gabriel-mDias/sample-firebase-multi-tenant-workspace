<!-- gems-ai-rule: {"id":"java.utils","version":"2.0.0","status":"candidate"} -->
---
stack: backend-java
module: gems-utils
severity: mandatory
see-also: [services.md, repositories.md, validation.md]
origem: local
precedencia: local-vence
versao: 2.0.0
---

# Utilitários — gems-utils

> **TL;DR:**
> - `ObjectUtil`: substituto obrigatório de `!= null`, `.isEmpty()`, `StringUtils`. Use `isNullOrEmpty()` e `isNotNullAndNotEmpty()`.
> - `EmailUtil.isValid(email)` para validar e-mail antes de persistir.
> - Utilitários de data, documento e identificador disponíveis no módulo `gems-utils`.
> - **NUNCA** use `!= null`, `.isEmpty()`, `StringUtils`, `Objects.nonNull()` em código de validação — use `ObjectUtil`.

## 1. ObjectUtil (Principal)

`ObjectUtil` centraliza checagens de nulidade e vazio para qualquer tipo de objeto.

### Checagens obrigatórias

| Situação | Use | NUNCA use |
| :--- | :--- | :--- |
| Campo é nulo ou vazio | `ObjectUtil.isNullOrEmpty(valor)` | `valor == null`, `valor.isEmpty()` |
| Campo tem valor | `ObjectUtil.isNotNullAndNotEmpty(valor)` | `valor != null`, `!valor.isEmpty()` |

```java
// CORRETO
if ( ObjectUtil.isNullOrEmpty( dto.getNome() ) ) {
    errors.add( "O nome é obrigatório." );
}

if ( ObjectUtil.isNotNullAndNotEmpty( dto.getEmail() ) && !EmailUtil.isValid( dto.getEmail() ) ) {
    errors.add( "O e-mail é inválido!" );
}

// ERRADO — não use
if ( dto.getNome() == null || dto.getNome().isEmpty() ) { ... }
if ( dto.getEmail() != null && !dto.getEmail().isEmpty() ) { ... }
```

### Funciona para:
- `String` (nulo ou blank)
- `Collection` / `List` / `Set` (nulo ou vazio)
- `Object` (nulo)
- `UUID` (nulo)
- Tipos primitivos boxed (`Integer`, `Long`, `BigDecimal` — nulo ou zero)

---

## 2. EmailUtil

```java
// Valida formato de e-mail
if ( ObjectUtil.isNotNullAndNotEmpty( dto.getEmail() ) && !EmailUtil.isValid( dto.getEmail() ) ) {
    errors.add( "O e-mail informado é inválido!" );
}
```

---

## 3. Outros utilitários do módulo gems-utils

- **DateUtil**: manipulação de datas (formatação, conversão, comparação) sem `SimpleDateFormat`.
- **DocumentUtil**: validação de CPF e CNPJ (dígito verificador). Prefira `gems-validation` para Bean Validation em DTOs.
- **IdUtil**: geração e validação de UUIDs.

---

## 4. Onde usar

- Validação em Services (`validate()` privado).
- Guards em métodos públicos antes de delegar para o repositório.
- Filtros em `appendFilters()` dos Repositories.
- Em **nenhum** lugar use `== null`, `!= null`, `.isEmpty()`, `StringUtils.isNotBlank()` ou `Objects.nonNull()` — o `ObjectUtil` é o contrato do projeto.

---

## Anti-Patterns (NUNCA)

| Proibido | Correto |
| :--- | :--- |
| `if (str == null \|\| str.isEmpty())` | `ObjectUtil.isNullOrEmpty(str)` |
| `Objects.nonNull(obj)` | `ObjectUtil.isNotNullAndNotEmpty(obj)` |
| `StringUtils.isNotBlank(str)` | `ObjectUtil.isNotNullAndNotEmpty(str)` |
| `if (list != null && !list.isEmpty())` | `ObjectUtil.isNotNullAndNotEmpty(list)` |
| Validação de e-mail com regex manual | `EmailUtil.isValid(dto.getEmail())` |

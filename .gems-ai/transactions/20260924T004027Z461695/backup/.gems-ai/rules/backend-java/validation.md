---
stack: backend-java
module: gems-validation, gems-exception
severity: mandatory
see-also: [services.md, rest-common.md, controllers.md]
origem: local
precedencia: local-vence
versao: 2.1.0
---

# Validação — gems-validation + BusinessException

> **TL;DR:**
>
> - `gems-validation`: Bean Validation constraints para documentos BR (`@ValidCpf`, `@ValidCnpj`, `@ValidEmail`).
> - `BusinessException(errors)`: lançada pela Service com lista acumulada — advice da `gems-exception` serializa para `ExceptionResponseDTO`.
> - Duas camadas: Bean Validation (`@Valid` em DTOs = estrutural) e `validate()` na Service (= regras de domínio).
> - **NUNCA** misture as camadas — Bean Validation não acessa o banco; regras de unicidade ficam na Service.

## 1. Duas Camadas de Validação

### Camada 1 — Bean Validation (gems-validation + Jakarta Validation)

Para validações estruturais no DTO (campo obrigatório, formato, tamanho):

```java
// No DTO (módulo model)
public class {Entidade}DTO {
    @NotNull(message = "O nome é obrigatório.")
    @NotBlank(message = "O nome não pode ser vazio.")
    private String nome;

    @ValidCpf(message = "CPF inválido.")           // gems-validation
    private String cpf;

    @ValidCnpj(message = "CNPJ inválido.")         // gems-validation
    private String cnpj;

    @ValidEmail(message = "E-mail inválido.")       // gems-validation
    private String email;
}

// No Controller — ativa Bean Validation
public ResponseEntity<{Entidade}DTO> insert( @Valid @RequestBody {Entidade}DTO dto, ... )
```

### Camada 2 — Validação de Negócio Manual (BusinessException)

Para regras de domínio que dependem de contexto (banco, estado atual, relações):

```java
// Na Service — método validate() privado
private void validate( {Entidade}DTO dto ) {
    var errors = new ArrayList<String>();

    // Regra de negócio: unicidade
    if ( repository.existsByEmail( dto.getEmail() ) ) {
        errors.add( "Já existe um cadastro com este e-mail." );
    }

    // Regra de negócio: consistência de estado
    if ( ObjectUtil.isNotNullAndNotEmpty( dto.getDataInicio() ) && ObjectUtil.isNotNullAndNotEmpty( dto.getDataFim() ) ) {
        if ( dto.getDataInicio().isAfter( dto.getDataFim() ) ) {
            errors.add( "A data de início não pode ser posterior à data de fim." );
        }
    }

    if ( ObjectUtil.isNotNullAndNotEmpty( errors ) ) {
        throw new BusinessException( errors );
    }
}
```

---

## 2. Constraints do gems-validation

| Annotation    | Valida                        |
| :------------ | :---------------------------- |
| `@ValidCpf`   | CPF (com dígito verificador)  |
| `@ValidCnpj`  | CNPJ (com dígito verificador) |
| `@ValidEmail` | Formato de e-mail (RFC 5322)  |

Todas implementam `ConstraintValidator` e funcionam com `@Valid` nos Controllers.

---

## 3. BusinessException

Fornecida pelo `gems-exception`. Aceita uma string simples ou lista de erros:

```java
// Erro único (validação de pré-condição)
throw new BusinessException( "{Entidade} não encontrado(a)!" );

// Lista de erros (validação acumulada)
var errors = new ArrayList<String>();
errors.add( "Campo A é obrigatório." );
errors.add( "Campo B é inválido." );
throw new BusinessException( errors );
```

O `@RestControllerAdvice` da `gems-exception` intercepta a `BusinessException` e serializa
`occurrenceTime`, `errorType`, `message`, `path` e `method` com status 400 automaticamente. O construtor
com lista une as mensagens por quebra de linha no campo `message`. Não trate na Service nem no
Controller, salvo quando uma subclasse precisar preservar status/código contratados; nesse caso o
handler específico deve espelhar esses cinco campos e acrescentar metadados, nunca renomear `message`.

---

## 4. Configuração do Módulo

```xml
<!-- pom.xml do módulo {app}-core -->
<dependency>
    <groupId>br.com.gems</groupId>
    <artifactId>gems-validation</artifactId>
</dependency>
<dependency>
    <groupId>br.com.gems</groupId>
    <artifactId>gems-exception</artifactId>
</dependency>
```

Ative o Spring Security support da `gems-exception` se o projeto usa autenticação:

```yaml
gems:
  exception:
    security:
      enabled: true
```

---

## Anti-Patterns (NUNCA)

| Proibido                                                             | Correto                                                                  |
| :------------------------------------------------------------------- | :----------------------------------------------------------------------- |
| Lançar `BusinessException` individualmente por campo na `validate()` | Acumular em `ArrayList<String>`, lançar UMA `BusinessException(errors)`  |
| Verificar unicidade com Bean Validation                              | Regras de banco (unicidade, existência) ficam na `validate()` da Service |
| `@Valid` omitido no Controller para DTOs com constraints             | `@Valid @RequestBody {Entidade}DTO dto` em **todo** endpoint mutante     |
| Misturar Bean Validation com validação de negócio no DTO             | Bean Validation = estrutural; Service `validate()` = regras de domínio   |

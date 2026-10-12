<!-- gems-ai-rule: {"id":"java.observability","version":"2.0.0","status":"candidate"} -->
---
stack: backend-java
module: gems-observability, gems-rest-common
severity: recommended
see-also: [rest-common.md, services.md]
origem: local
precedencia: local-vence
versao: 2.1.0
---

# Observabilidade — gems-observability

> **TL;DR:**
> - `gems-observability` ativa `@Observed` (Micrometer) via `ObservedAspect` — sem configuração adicional.
> - Use `@Observed` em métodos **públicos** de Service para rastreamento automático de latência e erros.
> - Correlation ID no MDC permite correlacionar logs de uma mesma requisição.
> - Combine com `gems-rest-common` (`correlation-id.enabled: true`) para propagar `X-Correlation-Id` header.

## 1. Setup

```xml
<!-- pom.xml -->
<dependency>
    <groupId>br.com.gems</groupId>
    <artifactId>gems-observability</artifactId>
</dependency>
```

O módulo registra um `ObservedAspect` bean automaticamente — sem configuração adicional.

---

## 2. @Observed em Services

Anote métodos que devem ser rastreados (Micrometer cria spans e métricas automaticamente):

```java
@Service
@RequiredArgsConstructor
public class {Entidade}Service {

    @Observed(name = "{entidade}.insert", contextualName = "Criação de {Entidade}")
    @Transactional(rollbackFor = Exception.class)
    public {Entidade}DTO insert( {Entidade}DTO dto ) {
        // Micrometer rastreia latência, sucesso e erros deste método
        validate( dto );
        var entity = repository.save( mapper.toEntity( dto ) );
        return mapper.toDTO( entity );
    }

    @Observed(name = "{entidade}.search")
    public Page<{Entidade}ResponseDTO> search( {Entidade}FilterParams filterParams, Pageable pageable ) {
        return repository.search( filterParams, pageable );
    }
}
```

---

## 3. Correlation ID no MDC (com gems-rest-common)

Quando `gems-rest-common` está configurado com `correlation-id.enabled: true`, o `X-Correlation-Id` é automaticamente adicionado ao **MDC** (Mapped Diagnostic Context) do Logback:

```xml
<!-- logback-spring.xml — adicione %X{correlationId} ao padrão de log -->
<pattern>%d{yyyy-MM-dd HH:mm:ss} [%X{correlationId}] [%-5level] %logger{36} - %msg%n</pattern>
```

Todos os logs da requisição (Service, Repository, Listener) incluirão o mesmo `correlationId`, facilitando rastreamento em logs distribuídos.

---

## 4. Métricas Geradas pelo @Observed

Para cada método anotado, o Micrometer registra automaticamente:

| Métrica | Descrição |
| :--- | :--- |
| `{name}.active` | Contador de execuções ativas |
| `{name}.seconds` | Histograma de latência |
| `{name}.seconds.max` | Latência máxima na janela |

As métricas ficam disponíveis em `/actuator/prometheus` (se Actuator + Prometheus estiverem configurados).

---

## 5. Regras

- Anote apenas métodos de **Service** — não Controllers (eles já têm instrumentação via servlet filter).
- Use `name` em snake_case com prefixo do domínio: `{entidade}.insert`, `{entidade}.search`.
- `contextualName` é opcional mas útil em dashboards de tracing (Zipkin, Jaeger).
- Não anote métodos `private` — o AspectJ não os intercepta (use um método `public` delegador se necessário).
- Para a receita de correlation-id ponta-a-ponta (Angular → Spring), veja `recipes/correlation-id-end-to-end.md`.

---

## Anti-Patterns (NUNCA)

| Proibido | Correto |
| :--- | :--- |
| `@Observed` em método `private` | Apenas métodos `public` (AspectJ não intercepta `private`) |
| `@Observed(name = "insert")` sem prefixo de domínio | `@Observed(name = "{entidade}.insert")` — snake_case com prefixo |
| `@Observed` em Controllers | Apenas em Services — Controllers já têm instrumentação via servlet filter |

---
origem: local
precedencia: local-vence
versao: 2.0.0
---

# Recipe: Correlation ID Ponta-a-Ponta

**Objetivo:** Propagar um ID de correlação único por requisição do Angular até os logs do Spring, passando pelo MDC, para rastreabilidade fim-a-fim.

**Pré-requisitos:**
- `gems-rest-common` (CorrelationIdFilter, MDC)
- `gems-observability` (@Observed, Micrometer)

**Rules relacionadas:**
- [rest-common.md](../rules/backend-java/rest-common.md)
- [observability.md](../rules/backend-java/observability.md)
- [http-interceptors.md](../rules/frontend-angular/http-interceptors.md)

---

## Fluxo Completo

```
Angular (correlationIdInterceptor)
  → gera UUID v4 por requisição
  → adiciona X-Correlation-Id: {uuid} no header HTTP

Spring (gems-rest-common CorrelationIdFilter)
  → lê X-Correlation-Id do request
  → se ausente, gera novo UUID
  → MDC.put("correlationId", uuid)
  → propaga para todos os logs do request
  → inclui X-Correlation-Id na response

Logs (logback-spring.xml)
  → %X{correlationId} em cada linha
  → correlacionável com o request do frontend
```

---

## Passo 1: Interceptor Angular

```typescript
// src/app/core/interceptors/correlation-id.interceptor.ts
import { HttpInterceptorFn } from '@angular/common/http';

export const correlationIdInterceptor: HttpInterceptorFn = (req, next) => {
  const correlationId = generateUuidV4();
  const cloned = req.clone({
    headers: req.headers.set('X-Correlation-Id', correlationId)
  });
  return next(cloned);
};

function generateUuidV4(): string {
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (c) => {
    const r = (Math.random() * 16) | 0;
    const v = c === 'x' ? r : (r & 0x3) | 0x8;
    return v.toString(16);
  });
}
```

Registrar em `app.config.ts`:
```typescript
provideHttpClient(
  withFetch(),
  withInterceptors([loadingInterceptor, correlationIdInterceptor])
)
```

## Passo 2: Filter no Backend (gems-rest-common)

O `gems-rest-common` já inclui `CorrelationIdFilter`. Configurar no `application.yml`:

```yaml
gems:
  rest:
    correlation-id:
      header-name: X-Correlation-Id
      include-in-response: true
```

O filtro:
1. Lê `X-Correlation-Id` do request.
2. Gera UUID se ausente.
3. Chama `MDC.put("correlationId", value)`.
4. Processa o request.
5. Chama `MDC.remove("correlationId")` no `finally` (evita leak entre threads).
6. Adiciona o header na response (para o frontend logar o ID).

## Passo 3: Configurar Logback

```xml
<!-- src/main/resources/logback-spring.xml -->
<configuration>
  <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
    <encoder>
      <pattern>%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} [%X{correlationId}] - %msg%n</pattern>
    </encoder>
  </appender>

  <root level="INFO">
    <appender-ref ref="CONSOLE" />
  </root>
</configuration>
```

Resultado:
```
14:23:01.123 [http-nio-8080-exec-1] INFO  c.g.{app}.{modulo}.{Entidade}Service [3fa85f64-5717-4562-b3fc] - Iniciando criação de {Entidade}
```

## Passo 4: Correlação nos Logs do Service (opcional)

Se precisar propagar via `@Observed`:

```java
// {Entidade}Service.java
@Observed(name = "{entidade}.insert", contextualName = "Criando {Entidade}")
@Transactional(rollbackFor = Exception.class)
public {Entidade} insert({Entidade}DTO dto) {
    log.info("Iniciando criação de {Entidade}: {}", dto.getNome());
    // ...
}
```

O MDC com `correlationId` já está disponível — qualquer `log.info/warn/error` automaticamente inclui o ID no padrão configurado.

## Passo 5: Rastrear no Frontend (opcional)

Para logar o correlation ID da resposta no frontend (desenvolvimento):

```typescript
// Adicionar ao interceptor de correlação:
return next(cloned).pipe(
  tap(event => {
    if (event instanceof HttpResponse) {
      const responseId = event.headers.get('X-Correlation-Id');
      if (responseId && !environment.production) {
        console.debug(`[Correlation] ${req.method} ${req.url} → ${responseId}`);
      }
    }
  })
);
```

---

## Checklist de Conformidade

- [ ] `correlationIdInterceptor` registrado em `withInterceptors([])`.
- [ ] UUID v4 gerado client-side por requisição (não reutilizar entre requests).
- [ ] Header: `X-Correlation-Id` (exato — case-insensitive no Spring, case-sensitive nos headers HTTP/2).
- [ ] `gems-rest-common` configurado com `include-in-response: true`.
- [ ] Logback com `%X{correlationId}` no pattern.
- [ ] MDC limpo no `finally` (já feito pelo filter da SDK — não duplicar).
- [ ] Logs do service usam `log.info/warn/error` (não `System.out.println`).

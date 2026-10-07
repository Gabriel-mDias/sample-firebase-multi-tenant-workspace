---
origem: local
precedencia: local-vence
versao: 2.0.0
---

# Recipe: Domain Event Ponta-a-Ponta

**Objetivo:** Publicar e consumir eventos de domínio com Spring Events, escolhendo entre execução na mesma transação (`@EventListener`) ou após commit (`@TransactionalEventListener`).

**Pré-requisitos:**
- Spring Context (`ApplicationEventPublisher`)
- `gems-exception` (BusinessException)

**Rules relacionadas:**
- [events.md](../rules/backend-java/events.md)
- [services.md](../rules/backend-java/services.md)
- [observability.md](../rules/backend-java/observability.md)

---

## Quando usar eventos

| Cenário | Abordagem |
| :--- | :--- |
| Sub-fluxo que deve fazer rollback junto com o principal | `@EventListener` (mesma transação) |
| Notificação externa (e-mail, webhook, fila) após commit | `@TransactionalEventListener(AFTER_COMMIT)` |
| Lógica que não precisa do retorno do publicador | Evento (desacoplamento) |
| Lógica que precisa do retorno | Chamada direta via método/serviço |

**Nunca use eventos** para: substituir validação, encadear eventos (evento A publica evento B), ou comunicação síncrona onde o resultado importa.

---

## Passo 1: Classe do Evento (POJO Imutável)

```java
// {Entidade}CriadoEvent.java
public record {Entidade}CriadoEvent(String {entidade}Id) {}

// Ou com Lombok:
@Value   // Imutável via Lombok
public class {Entidade}CriadoEvent {
    String {entidade}Id;
}
```

Regra: o evento carrega **apenas o ID** da entidade. O listener faz o `findById` se precisar de mais dados.

## Passo 2: Publicar o Evento no Service

```java
@Service
@RequiredArgsConstructor
public class {Entidade}Service {

    private final {Entidade}Repository repository;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Transactional(rollbackFor = Exception.class)
    public {Entidade} insert({Entidade}DTO dto) {
        validate(dto);
        {Entidade} entidade = mapper.toEntity(dto);
        repository.save(entidade);
        applicationEventPublisher.publishEvent(new {Entidade}CriadoEvent(entidade.getId()));
        return entidade;
    }
}
```

- Publicar **após** `repository.save()` — o ID já existe.
- O evento é publicado dentro da transação ativa.

## Passo 3a: Listener Síncrono (mesma transação)

Use quando a ação do listener deve fazer rollback junto se falhar:

```java
@Component
@RequiredArgsConstructor
public class {Entidade}EventListener {

    private final SubEntidadeService subEntidadeService;

    @EventListener
    public void on{Entidade}Criado({Entidade}CriadoEvent event) {
        subEntidadeService.inicializarDados(event.{entidade}Id());
    }
}
```

- Se `inicializarDados` lançar exceção → toda a transação (incluindo o `insert` original) faz rollback.
- Executa na mesma thread/transação do publicador.

## Passo 3b: Listener Assíncrono Pós-Commit (integrações externas)

Use quando a ação **não deve** fazer rollback na transação principal (e-mail, webhook):

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class {Entidade}NotificacaoListener {

    private final EmailService emailService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on{Entidade}Criado({Entidade}CriadoEvent event) {
        try {
            emailService.enviarBoasVindas(event.{entidade}Id());
        } catch (Exception e) {
            log.error("Falha ao enviar e-mail para {entidade} {}: {}", event.{entidade}Id(), e.getMessage());
            // NÃO relançar — transação principal já commitou
        }
    }
}
```

- Executa **após** o commit. Se falhar, a transação original **não** faz rollback.
- Erros devem ser logados (não relançados).
- Para garantia de entrega, use fila SQS/RabbitMQ — eventos Spring não são persistentes.

## Passo 4: Múltiplos Listeners

Múltiplos listeners para o mesmo evento são suportados. Não há garantia de ordem entre eles:

```java
@EventListener
public void on{Entidade}Criado({Entidade}CriadoEvent event) { /* listener A */ }

@EventListener
public void sincronizarComLegado({Entidade}CriadoEvent event) { /* listener B */ }
```

Para ordenar, use `@Order(1)`, `@Order(2)`.

---

## Checklist de Conformidade

- [ ] Evento é imutável (`record` ou `@Value`).
- [ ] Evento carrega apenas o ID (não a entidade completa).
- [ ] `applicationEventPublisher.publishEvent()` chamado dentro de `@Transactional`.
- [ ] `@EventListener` para sub-fluxos que fazem rollback junto.
- [ ] `@TransactionalEventListener(AFTER_COMMIT)` para integrações externas.
- [ ] Listeners de AFTER_COMMIT capturam exceções e logam (nunca relançam).
- [ ] Sem encadeamento de eventos (A → B → C).

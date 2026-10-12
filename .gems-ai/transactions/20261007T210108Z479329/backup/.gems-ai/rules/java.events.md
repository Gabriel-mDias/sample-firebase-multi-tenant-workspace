<!-- gems-ai-rule: {"id":"java.events","version":"2.0.0","status":"candidate"} -->
---
stack: backend-java
module: spring-context
severity: recommended
see-also: [services.md, observability.md]
origem: local
precedencia: local-vence
versao: 2.1.0
---

# Spring Application Events — Desacoplamento de Subfluxos

> **TL;DR:**
> - Use eventos para desacoplar efeitos colaterais do fluxo principal (evita Services com centenas de linhas).
> - Nomenclatura: `{Entidade}CriadaEvent` (POJO imutável) + `{Entidade}CriadaListener` (`@Component` com `@EventListener`).
> - `@EventListener`: síncrono, mesma transação — se o listener quebrar, o `insert`/`save` faz rollback completo.
> - `@TransactionalEventListener(AFTER_COMMIT)`: roda só após commit — use para e-mails, APIs externas.
> - Trafegue apenas IDs no evento — **nunca** entities completas (evita dados dessincronizados).
> - **NUNCA** use `@Async` dentro de listeners que devem participar da transação principal.

## 1. Quando usar eventos

Emita um evento quando o sucesso de um fluxo primário deve disparar ações em outros domínios **sem** que o domínio original conheça os detalhes dessas ações:

- `EntidadeCriadaEvent` → outros domínios reagem (cópia de dados, criação de vínculos).
- `ContratoSuspensoEvent` → bloqueia acesso, suspende cobranças.
- `FechamentoConcluidoEvent` → dispara cálculos de dashboard.

---

## 2. Estrutura Completa

### Passo 1 — Classe do Evento (POJO imutável)

```java
package br.com.gems.{app}.{dominio}.event;          // módulo {app}-events

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import java.util.UUID;

@Getter
@RequiredArgsConstructor
public class {Entidade}CriadaEvent {
    private final UUID {entidade}Id;  // Trafegue apenas o ID
}
```

### Passo 2 — Emissor (na Service)

```java
@Service
@RequiredArgsConstructor
public class {Entidade}Service {

    private final {Entidade}Repository repository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(rollbackFor = Exception.class)
    public {Entidade}DTO insert( {Entidade}DTO dto ) {
        validate( dto );
        var entity = repository.save( mapper.toEntity( dto ) );

        // Emissão após save, ainda dentro da transação
        eventPublisher.publishEvent( new {Entidade}CriadaEvent( entity.getId() ) );

        return mapper.toDTO( entity );
    }
}
```

### Passo 3 — Listener (componente isolado)

```java
package br.com.gems.{app}.{dominio}.listener;       // módulo {app}-core

import org.springframework.context.event.EventListener;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class {Entidade}CriadaListener {

    private final OutroDominioService outroDominioService;

    // Cenário 1: Síncrono — mesma transação do emissor.
    // Se quebrar → rollback total (entidade principal NÃO é salva).
    @EventListener
    public void processarSincrono( {Entidade}CriadaEvent event ) {
        outroDominioService.fazerAlgumaCoisa( event.get{Entidade}Id() );
    }

    // Cenário 2: Pós-commit — roda APENAS SE a transação original commitar com sucesso.
    // Use para e-mails, APIs externas, notificações (ações não desfazíveis).
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void processarAposCommit( {Entidade}CriadaEvent event ) {
        outroDominioService.enviarNotificacao( event.get{Entidade}Id() );
    }
}
```

---

## 3. Regras

| Regra | Detalhe |
| :--- | :--- |
| Trafegue apenas IDs | Nunca passe entities no evento — dados podem estar dessincronizados |
| Pacote `event` | Classes de evento em `{app}-core/event/` |
| Pacote `listener` | Listeners em `{app}-core/event/listener/` |
| Escolha do tipo | Rollback obrigatório → `@EventListener`; APIs externas → `@TransactionalEventListener(AFTER_COMMIT)` |
| Sem `@Async` | Nunca use `@Async` em listeners que participam da transação principal |
| Um listener por responsabilidade | Não agrupe múltiplos domínios em um listener só |

---

## 4. Quando NÃO usar eventos

- Lógica simples de persistência de sub-entidades: use delegação para Services (`subEntidadeService.insert(dto)`).
- Validações cruzadas: faça na Service principal via `validate()`.
- Operações triviais de pós-save: faça inline no método.

Eventos são para **desacoplamento de domínios** — não para qualquer pós-processamento.

---

## Anti-Patterns (NUNCA)

| Proibido | Correto |
| :--- | :--- |
| Passar entity completa no evento (`new EntidadeCriadaEvent(entity)`) | Passar apenas o ID (`new EntidadeCriadaEvent(entity.getId())`) |
| `@Async` em listener que participa da transação | `@EventListener` (sync) ou `@TransactionalEventListener(AFTER_COMMIT)` |
| `throw exception` em listener `AFTER_COMMIT` | `try { ... } catch (Exception e) { log.error(...) }` — rollback já não é possível |
| Um listener agregando múltiplos domínios | Um listener por responsabilidade de domínio |

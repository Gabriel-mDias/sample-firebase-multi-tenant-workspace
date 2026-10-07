# Prompt para: /speckit-constitution
#
# QUANDO USAR: Uma única vez ao iniciar o projeto. Reexecute apenas quando
# houver mudanças arquiteturais fundamentais (nova stack, novo módulo, etc).
#
# COMO USAR:
#   /speckit-constitution <cole o bloco de PRINCÍPIOS abaixo>
#
# O que a skill faz com este input:
#   - Preenche os placeholders do template em .specify/memory/constitution.md
#   - Propaga mudanças para artefatos dependentes
#   - Incrementa a versão semântica da constitution
#
# ADAPTE: Substitua os valores entre colchetes [] e ajuste os princípios
# conforme a realidade do seu projeto.
# ─────────────────────────────────────────────────────────────────────────────

PROJECT_NAME: Sample Project
CONSTITUTION_VERSION: 1.0.0
RATIFICATION_DATE: 2026-06-27

## Core Principles

### I. Modular Isolation (NON-NEGOTIABLE)
A lógica de negócio reside exclusivamente no módulo `core`. Nenhum outro módulo
expõe ou acessa internals do core diretamente. Qualquer acesso externo ocorre
via interfaces declaradas pelo próprio core.
Pacote raíz: br.com.gems.sample.core.{domínio}

### II. Authentication via Keycloak (NON-NEGOTIABLE)
Keycloak é o único provedor de autenticação e autorização. Não implementar
mecanismos alternativos de auth. Qualquer tematização de login ocorre no
próprio Keycloak. Integração via keycloak-angular no frontend.

### III. Technology Lock-in (NON-NEGOTIABLE)
Stack definida: Java 21 + Spring Boot, Angular 20+, PostgreSQL, Keycloak, AWS S3.
Qualquer adição de dependência de runtime requer aprovação explícita e atualização
desta constitution. Substituição de item da stack requer amendment formal.

### IV. Domain-Driven Package Structure
Pacotes do backend organizados por domínio de negócio, nunca por camada técnica.
Ex: br.com.gems.sample.core.usuario — correto.
Ex: br.com.gems.sample.services — incorreto.
Frontend organizado por feature em src/app/features/{modulo}/.

### V. Frontend Standalone-First
Todo componente Angular é standalone por padrão. Não declarar `standalone: true`
explicitamente (é o default). Separar sempre .ts, .html e .css em arquivos distintos.
Proibido inline templates e inline styles em componentes de feature.

### VI. Test-First for Core Business Logic
Lógica no módulo core deve ter cobertura de testes escrita antes da implementação.
Outros módulos (controllers, mappers) seguem TDD por conveniência, não obrigação.

### VII. PWA & Responsiveness (NON-NEGOTIABLE)
A aplicação deve funcionar como PWA em dispositivos mobile e desktop.
Toda tela deve ser responsiva. Acessibilidade (WCAG AA) é requisito mínimo.

### VIII. Event-Driven Cross-Domain Flows
Fluxos que integram mais de um domínio de negócio devem ser orquestrados via
eventos no módulo `event`. Domínios não se invocam diretamente entre si.

## Governance
A constitution é a autoridade máxima sobre decisões arquiteturais.
Amendments requerem: (1) justificativa documentada, (2) atualização deste arquivo,
(3) notificação à equipe via PR descritivo.
Violações de princípios NON-NEGOTIABLE bloqueiam merge de PR.

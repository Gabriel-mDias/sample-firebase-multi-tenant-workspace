# Prompt para: /speckit-tasks
#
# QUANDO USAR: Após /speckit-plan gerar os artefatos de design (plan.md,
# data-model.md, contracts/).
#
# COMO USAR:
#   /speckit-tasks <cole as RESTRIÇÕES abaixo>
#   — ou —
#   /speckit-tasks
#   (sem argumento: gera tasks baseado em todos os artefatos disponíveis)
#
# O que a skill faz com este input:
#   - Lê plan.md, spec.md, data-model.md, contracts/
#   - Gera tasks.md ordenado por dependência
#   - Cria exemplos de execução paralela por user story
#   - Valida que cada user story tem todas as tasks necessárias
#
# BOAS PRÁTICAS para o argumento:
#   - Informe se quer tasks mais granulares (1 task = 1 arquivo) ou mais
#     agregadas (1 task = 1 camada completa)
#   - Indique se alguma user story tem prioridade e deve vir primeiro
#   - Mencione tasks de infraestrutura que precisam acontecer antes
#   - Se for implementação parcial (spike, MVP), diga quais P2/P3 ignorar
#
# ADAPTE: Ajuste as restrições de granularidade e prioridade.
# ─────────────────────────────────────────────────────────────────────────────

Gere tasks com granularidade MÉDIA: cada task deve representar um componente
completo verticalmente (ex: "implementar UsuarioRepository com seus métodos",
não "criar arquivo UsuarioRepository.java").

## Prioridades de execução

1. Primeiro: tasks de setup e infraestrutura (migration SQL, configuração de módulo)
2. Segundo: camada de domínio backend (Entity, Repository, Service)
3. Terceiro: camada de API backend (Controller, DTOs, OpenAPI)
4. Quarto: integração de evento de exclusão
5. Quinto: frontend (model, store, list, form)
6. Último: testes de integração e checklist

## Restrições de escopo para esta iteração

- Incluir APENAS user stories P1 (obrigatórias para o MVP).
- Excluir por agora: upload de foto, exportação de dados, histórico de alterações.
- Tasks de teste devem ser incluídas ao final de cada camada, não agrupadas.

## Contexto de paralelismo

A equipe tem 1 desenvolvedor full-stack. Não otimizar para execução paralela
entre múltiplas pessoas — otimizar para sequência lógica de um único dev.

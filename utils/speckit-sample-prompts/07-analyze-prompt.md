# Prompt para: /speckit-analyze
#
# QUANDO USAR: Após /speckit-tasks (e idealmente após /speckit-checklist).
# É o ponto de controle final antes da implementação. READ-ONLY — não modifica
# arquivos, apenas emite um relatório de inconsistências.
#
# COMO USAR:
#   /speckit-analyze <cole o FOCO abaixo>
#   — ou —
#   /speckit-analyze
#   (sem argumento: análise completa de todos os artefatos)
#
# O que a skill faz com este input:
#   - Lê spec.md + plan.md + tasks.md (obrigatórios)
#   - Verifica consistência cruzada entre os três artefatos
#   - Verifica conformidade com constitution.md (violações = CRITICAL)
#   - Emite relatório categorizado: CRITICAL / WARNING / INFO
#   - Propõe remediações (você deve aprovar antes de aplicar)
#
# CATEGORIAS de problema:
#   CRITICAL  → Viola constitution NON-NEGOTIABLE ou contradição entre artefatos
#   WARNING   → Ambiguidade que pode gerar retrabalho na implementação
#   INFO      → Sugestão de melhoria não bloqueante
#
# BOAS PRÁTICAS para o argumento:
#   - Indique domínios de maior risco para priorizar a análise
#   - Mencione decisões recentes que podem ter gerado inconsistência
#   - Se algum WARNING conhecido é aceitável, diga aqui para não ser bloqueante
#
# ADAPTE: Ajuste o foco para os domínios de maior risco da sua feature.
# ─────────────────────────────────────────────────────────────────────────────

Foque a análise nos seguintes domínios de maior risco para esta feature:

## Domínios prioritários

1. **Consistência constitution × plan**: Verificar se o design do módulo `core`
   respeita o Princípio I (Modular Isolation). Especificamente, confirmar que
   nenhum controller acessa diretamente o Repository sem passar pelo Service.

2. **Consistência spec × tasks**: Verificar se todas as user stories do spec.md
   têm tasks correspondentes no tasks.md. A exclusão de usuário com orquestração
   via evento tem task de implementação do handler no módulo `event`?

3. **Consistência plan × tasks**: O plan.md descreve integração com ViaCEP como
   um Service separado. Verificar se há task de implementação desse Service
   e se a task de implementação do form frontend depende corretamente dela.

4. **Incomplete spec coverage**: Verificar se os critérios de aceite do spec
   (CPF único, e-mail único, CEP obrigatório) têm tasks de validação
   correspondentes tanto no backend quanto no frontend.

## Violações conhecidas e aceitas (não reportar como CRITICAL)

- O Princípio VI (Test-First) será relaxado para os mappers e controllers
  nesta feature por ser um projeto de portfólio sem CI gate de cobertura.
  Testes de integração do core ainda são obrigatórios.

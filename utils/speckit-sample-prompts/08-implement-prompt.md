# Prompt para: /speckit-implement
#
# QUANDO USAR: Após /speckit-analyze retornar sem CRITICALs (ou com CRITICALs
# remediados). É a skill de execução — ela realmente escreve código.
#
# COMO USAR:
#   /speckit-implement <cole as DIRETRIZES abaixo>
#   — ou —
#   /speckit-implement
#   (sem argumento: executa todas as tasks pendentes em ordem)
#
# O que a skill faz com este input:
#   - Verifica checklists (todos devem estar PASS para avançar)
#   - Lê tasks.md e executa tasks na ordem definida
#   - Marca cada task como concluída ao finalizar
#   - Pode ser invocada com filtro para executar apenas parte das tasks
#
# FILTROS úteis no argumento:
#   "apenas tasks do backend"         → executa só tasks de Java/Spring
#   "apenas tasks do frontend"        → executa só tasks de Angular
#   "apenas task T-001"               → executa uma task específica
#   "a partir da task T-005"          → retoma de onde parou
#   "apenas tasks de teste"           → gera/completa testes
#
# BOAS PRÁTICAS para o argumento:
#   - Na primeira execução, deixe sem filtro para a ordem ser respeitada
#   - Use filtros apenas para retomada após interrupção
#   - Sempre mencione os padrões GEMS SDK a seguir durante a implementação
#   - Se quiser revisão humana entre tasks, diga "pause após cada task"
#
# ADAPTE: Ajuste os padrões e restrições para o seu projeto.
# ─────────────────────────────────────────────────────────────────────────────

Execute todas as tasks pendentes em ordem. Diretrizes de implementação:

## Padrões obrigatórios (GEMS SDK)

Durante a implementação, leia e siga rigorosamente as rules do GEMS SDK:
- Backend: `.gems-ai/rules/backend-java/`
- Frontend: `.gems-ai/rules/frontend-angular/`
- Recipes quando aplicável: `.gems-ai/recipes/`

Para esta feature, as recipes mais relevantes são:
- `crud-end-to-end.md` — fluxo principal de cadastro/edição/exclusão
- `domain-event-end-to-end.md` — evento de exclusão de usuário
- `error-and-validation-end-to-end.md` — validação e mensagens de erro

## Comportamento esperado durante a execução

- Ao criar cada arquivo, confirme o pacote/caminho antes de escrever.
- Não crie abstrações além do necessário — siga o que o plan.md define.
- Se encontrar ambiguidade não coberta pelo spec, pause e pergunte antes
  de assumir uma decisão técnica.
- Ao finalizar o backend, confirme que os endpoints batem com os contracts/
  antes de iniciar o frontend.

## Restrições

- Não usar `@Autowired` — usar injeção por construtor.
- Não usar classes utilitárias genéricas não previstas no plan.md.
- Mensagens de validação em português (pt-BR).
- Nenhum `System.out.println` ou `console.log` de debug no código commitado.

# ─────────────────────────────────────────────────────────────────────────────
# VARIAÇÃO: Retomada de execução parcial
# Use quando a implementação foi interrompida:
#
# /speckit-implement a partir da task T-006 (implementação do frontend).
# O backend já está completo e testado. Siga os padrões GEMS SDK para Angular.

# Prompt para: /speckit-checklist
#
# QUANDO USAR: Após /speckit-tasks. Pode ser executado múltiplas vezes,
# uma por domínio de qualidade (UX, segurança, acessibilidade, etc).
#
# COMO USAR:
#   /speckit-checklist <cole o DOMÍNIO e FOCO abaixo>
#
# O que a skill faz com este input:
#   - Lê spec.md e plan.md
#   - Gera um checklist de QUALIDADE DE REQUISITOS (não de implementação)
#   - Salva em .specify/specs/{feature}/checklists/{domínio}.md
#   - O /speckit-implement só avança se todos os checklists estiverem PASS
#
# CONCEITO CENTRAL: Checklists são "testes unitários para o spec".
#   ✅ "O spec define o comportamento do campo CPF quando inválido?" (completude)
#   ✅ "Os estados de loading do formulário estão especificados?" (cobertura)
#   ❌ "O CPF está sendo validado corretamente?" (isso é teste de implementação)
#
# ADAPTE: Troque o domínio e ajuste o foco para o contexto da sua feature.
# Execute uma vez por domínio relevante.
# ─────────────────────────────────────────────────────────────────────────────

# Exemplo 1: Checklist de UX/Formulário
# Execute com: /speckit-checklist ux

Domínio: UX e Formulários
Foco: Verificar se o spec define claramente todos os estados e comportamentos
      visuais do formulário de cadastro de usuário.

Áreas a cobrir:
- Estados do formulário: idle, loading, success, error
- Feedback inline de validação: quando aparece, qual mensagem, como some
- Comportamento do autocomplete de CEP: loading state, erro de CEP inválido,
  CEP não encontrado, preenchimento parcial
- Hierarquia visual dos campos obrigatórios vs opcionais
- Comportamento do modal de confirmação de exclusão
- Estados dos botões: disabled, loading, hover
- Responsividade: o spec define como o form se comporta em mobile?

# ─────────────────────────────────────────────────────────────────────────────
# Exemplo 2: Checklist de Segurança
# Execute com: /speckit-checklist security

# Domínio: Segurança
# Foco: Verificar se o spec cobre os requisitos de segurança da feature
#       de cadastro e exclusão de usuário.
#
# Áreas a cobrir:
# - Autorização: o spec define quem pode editar/excluir (apenas o próprio usuário)?
# - Dados sensíveis: o spec define mascaramento de CPF na exibição?
# - Exclusão: o spec define o que acontece com dados de auditoria após exclusão?
# - Validação server-side: o spec menciona que validações ocorrem no backend
#   (não apenas no frontend)?
# - Exposição de IDs: o spec define se o ID interno é exposto na URL ou se
#   usa um identificador público?

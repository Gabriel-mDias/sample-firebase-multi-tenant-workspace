# Prompt para: /speckit-clarify
#
# QUANDO USAR: Após /speckit-specify, ANTES de /speckit-plan.
# Use quando o spec contém ambiguidades, decisões em aberto ou
# pontos que podem gerar retrabalho na fase de planejamento.
#
# COMO USAR:
#   /speckit-clarify <cole o bloco de FOCO abaixo>
#   — ou —
#   /speckit-clarify
#   (sem argumento: a skill identifica automaticamente as ambiguidades)
#
# O que a skill faz com este input:
#   - Lê o spec.md ativo
#   - Faz até 5 perguntas direcionadas às áreas mais ambíguas
#   - Após suas respostas, atualiza o spec.md com as decisões tomadas
#
# BOAS PRÁTICAS para o argumento:
#   - Indique domínios específicos onde você sabe que há dúvidas
#   - Mencione restrições técnicas que a skill deve considerar
#   - Se quiser que a skill ignore certos pontos, diga explicitamente
#
# ADAPTE: Liste os domínios que precisam de clarificação no seu contexto.
# ─────────────────────────────────────────────────────────────────────────────

Foque as perguntas nos seguintes domínios de maior risco:

1. **Validação de CPF**: O spec menciona unicidade, mas não define se a validação
   deve ser apenas de formato (11 dígitos, máscara) ou também algorítmica
   (dígitos verificadores). Preciso de uma decisão antes de modelar o domínio.

2. **Integração com API de CEP**: O spec diz "consultar API de endereço" mas não
   define qual API (ViaCEP, Correios, OpenCEP), se há fallback, e o que acontece
   quando o CEP não é encontrado (erro ou campos em branco para preenchimento manual).

3. **Fluxo de exclusão e Keycloak**: Ao excluir a conta, o sistema deve remover
   apenas os dados do banco ou também desativar/remover o usuário no Keycloak?
   Quem orquestra isso — o backend ou um evento?

4. **Estado do cadastro no fluxo de autenticação**: O usuário pode acessar o
   sistema sem ter completado o cadastro? Ou o primeiro login redireciona
   obrigatoriamente para a tela de cadastro?

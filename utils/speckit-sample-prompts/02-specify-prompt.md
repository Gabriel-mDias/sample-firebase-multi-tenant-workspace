# Prompt para: /speckit-specify
#
# QUANDO USAR: No início de cada feature, antes de qualquer planejamento técnico.
#
# COMO USAR:
#   /speckit-specify <cole o bloco de DESCRIÇÃO DA FEATURE abaixo>
#
# O que a skill faz com este input:
#   - Gera um short name para a feature (usado no nome de pasta e branch)
#   - Cria .specify/specs/{feature-name}/spec.md com user stories estruturadas
#   - Opcionalmente cria a branch via hook
#
# BOAS PRÁTICAS para o argumento:
#   - Descreva O QUÊ o usuário faz, não COMO o sistema implementa
#   - Inclua: objetivo, personas, fluxos principais, edge cases conhecidos
#   - Não inclua detalhes técnicos (tabelas, APIs) — isso vai para o plan
#   - Seja específico nos critérios de aceite — evite "deve funcionar bem"
#
# ADAPTE: Substitua pelo contexto real da feature que está implementando.
# ─────────────────────────────────────────────────────────────────────────────

Quero implementar o cadastro completo de usuário no sistema.

## Contexto
O sistema precisa permitir que usuários se cadastrem, visualizem, editem e
excluam seu próprio perfil. Este é o fluxo central do projeto sample.

## Personas
- **Usuário comum**: pessoa que acessa o sistema via browser ou dispositivo mobile
  para gerenciar seu próprio perfil. Não tem perfil administrativo.

## Fluxos principais

### Cadastro
1. Usuário acessa a tela de cadastro (pré-autenticação ou pós-login).
2. Preenche dados pessoais: nome completo, e-mail, CPF, data de nascimento, telefone.
3. Preenche dados de endereço: CEP, logradouro, número, complemento, bairro,
   cidade, estado.
4. Sistema valida os campos e exibe erros inline.
5. Ao confirmar, sistema salva e redireciona para a tela de perfil.

### Edição
1. Usuário acessa seu perfil.
2. Clica em editar e altera os campos desejados.
3. Salva. Sistema exibe feedback de sucesso.

### Exclusão
1. Usuário solicita exclusão da conta.
2. Sistema exibe confirmação (modal).
3. Ao confirmar, dados são excluídos e sessão encerrada.

## Critérios de aceite
- CPF deve ser único no sistema.
- E-mail deve ser único e validado no formato.
- CEP deve consultar API de endereço e preencher campos automaticamente.
- Campos obrigatórios: nome, e-mail, CPF, CEP, logradouro, número, cidade, estado.
- Complemento e telefone são opcionais.
- Exclusão é irreversível e deve alertar o usuário antes de confirmar.
- Todas as telas devem ser responsivas (mobile-first).

## Fora do escopo
- Recuperação de senha (gerenciada pelo Keycloak).
- Upload de foto de perfil (feature futura).
- Administração de outros usuários.

# Prompt para: /speckit-plan
#
# QUANDO USAR: Após spec.md aprovado (e clarify executado se necessário).
# Esta skill é a mais pesada do ciclo — gera os artefatos de design técnico.
#
# COMO USAR:
#   /speckit-plan <cole as DIRETRIZES abaixo>
#   — ou —
#   /speckit-plan
#   (sem argumento: a skill usa apenas o spec.md e a constitution)
#
# O que a skill faz com este input:
#   - Lê spec.md + constitution.md
#   - Gera: research.md, data-model.md, contracts/ (OpenAPI), quickstart.md
#   - Verifica conformidade com os princípios da constitution
#   - Emite ERROR se violar um princípio NON-NEGOTIABLE
#
# BOAS PRÁTICAS para o argumento:
#   - Informe decisões técnicas já tomadas para não gerar alternativas
#   - Indique se há restrições de prazo que exigem simplificação de design
#   - Mencione integrações externas existentes que o plan deve considerar
#   - Se houver padrões do GEMS SDK a seguir, referencie-os aqui
#
# ADAPTE: Ajuste as diretrizes com as decisões técnicas do seu contexto.
# ─────────────────────────────────────────────────────────────────────────────

Siga as regras da GEMS SDK definidas em AGENTS.md. Diretrizes adicionais:

## Decisões técnicas já tomadas

- **Módulo backend**: A feature pertence ao módulo `core`, domínio `usuario`.
  Pacote: `br.com.gems.sample.core.usuario`.
- **Entidade**: A entidade principal é `Usuario`. Endereço será um `@Embeddable`
  dentro de `Usuario`, não uma tabela separada — simplicidade é prioritária aqui.
- **API de CEP**: Usar ViaCEP (https://viacep.com.br). Implementar como `@Service`
  no módulo core, sem acoplamento direto no controller.
- **Eventos**: A exclusão de usuário deve publicar um evento `UsuarioExcluidoEvent`
  para que o módulo `event` possa orquestrar a desativação no Keycloak de forma
  assíncrona. Seguir a recipe `domain-event-end-to-end.md`.
- **Frontend**: Feature em `src/app/features/usuario/`. Seguir a estrutura da
  constitution: models/, services/, list/, form/.

## Padrões GEMS SDK obrigatórios

- Controllers devem seguir `.gems-ai/rules/backend-java/controllers.md`
- Validação via `.gems-ai/rules/backend-java/validation.md`
- Mapping com MapStruct conforme `.gems-ai/rules/backend-java/mapping.md`
- OpenAPI conforme `.gems-ai/rules/backend-java/openapi.md`
- Frontend com GemsBaseStore conforme `.gems-ai/rules/frontend-angular/stores.md`
- Formulário com `.gems-ai/rules/frontend-angular/forms.md`

## Restrições de design

- Não criar módulo separado para endereço.
- Não usar Lombok em classes que usam Records do Java 21.
- O endpoint de exclusão deve retornar 204 No Content, sem corpo.

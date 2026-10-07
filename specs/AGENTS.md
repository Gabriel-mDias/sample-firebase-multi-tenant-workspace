# Specs e governança

- Numeração de `specs/` é ordem de entrega, não número da RF.
- Feature ou mudança de comportamento usa a rodada SDD (Claude: `/rodada-sdd`; Codex/demais: `spec-driven`).
- Bug que restaura contrato não cria feature spec-kit.
- O ciclo completo é proporcional ao risco; artefato sem conteúdo aplicável não é criado só por
  cerimônia.
- `roteiro.md` continua sendo o portão humano antes de implementação de feature.
- Delegação deve ter decisão explícita; `sem delegação` é válida e preferível quando onboarding não
  se amortiza.
- Um executor lê somente a spec atual, seu pacote, contrato e referências nomeadas.
- Specs concluídas são históricas e não entram no contexto salvo quando citadas como referência.

Regras GEMS vencem código consolidado; referência não autoriza desvio. Consulte a constituição 2.0.0 e a seção Adoção de Módulo da SDK GEMS.

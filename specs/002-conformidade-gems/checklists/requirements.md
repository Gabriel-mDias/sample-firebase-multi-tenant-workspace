# Checklist de requisitos — Conformidade GEMS

**Data**: 2026-09-14 · **Spec**: [spec.md](../spec.md)

## Conteúdo e linguagem

- [x] Histórias descrevem ator, valor e comportamento observável sem prescrever implementação.
- [x] Cenários são verificáveis em termos de contrato.
- [x] Requisitos técnicos nomeiam mecanismos apenas quando definem conformidade.
- [x] Critérios medem resultado e não afirmam implementação existente.
- [x] Escopo preservado e caminhos condicionais estão explícitos.

## Cobertura

- [x] Nove princípios constitucionais têm requisito/gate.
- [x] S-12 exige zero Java, remoção das 22 supressões e do ratchet.
- [x] Frontend exige mínimo medido e decisão humana nominal por residual.
- [x] Auditoria exige bloco atômico, identidade estável e correlação.
- [x] Keycloak cobre o ciclo atual, roles/compostas e compensações.
- [x] Contagens atuais: 4 repositories, 2 controllers, 4 handlers, 6 mappers e 177 nomes.
- [x] Rollback exige Spring integrado, proxy, banco real e ausência de dado/efeito/trilha.

## Clarificação

- [x] D-P01 autoria estável/correlação preservada; adoção espera release compatível.
- [x] D-P02 superfície Keycloak completa preservada; substituição espera release compatível.
- [x] D-P03 não aplicável; ambiente/realm preservados.
- [x] D-P04 devolvida para correção; 63/63 ocorrências resolvidas e baseline zerado.

## Resultado

**PASS FINAL**: especificação completa e rastreável; D-P01/D-P02 foram satisfeitas pelas releases
compatíveis, D-P03 não se aplicou e D-P04 terminou corrigida com residual zero.

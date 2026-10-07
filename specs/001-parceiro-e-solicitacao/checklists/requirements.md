# Checklist: qualidade dos requisitos — rodada 001

Testa a **especificação**, não a implementação. Um item marcado aqui não diz que o código funciona;
diz que o requisito é escrito de forma que dê para verificar se funciona.

## Completude

- [x] Toda user story tem prioridade, teste independente e cenários Given/When/Then
- [x] Todo FR é verificável — nada como "o sistema deve ser rápido"
- [x] Todo SC é mensurável e agnóstico de tecnologia
- [x] Os edge cases cobrem os caminhos de recusa, não só os felizes
- [x] As premissas estão escritas, inclusive as incômodas (evento em memória)

## Ausência de ambiguidade

- [x] Zero marcadores `[NEEDS CLARIFICATION]`
- [x] Toda decisão que teve alternativa está na §2 do roteiro, com o que ela fecha
- [x] As decisões tomadas **por convenção, sem pergunta** também estão listadas — são as que geram
      discussão depois

## Cobertura das regras que falham em silêncio

- [x] O sigilo tem requisito para os **três** caminhos (leitura, busca, gravação), não só o primeiro
- [x] A trilha de auditoria tem requisito próprio para o campo sigiloso
- [x] "A situação nunca vem do corpo" é FR, e não comentário de código
- [x] A ordem obrigatória dos changesets está escrita, com o motivo

## Rastreabilidade

| FR | User story | Teste que prova |
| :--- | :--- | :--- |
| FR-001 | US1 | `ParceiroServiceTest` + `AutorizacaoPorAcaoIntegrationTest` |
| FR-002 | US1 | `ParceiroServiceTest.acumulaViolacoes` |
| FR-003 | US1 | `ParceiroServiceTest.recusaNomeDuplicado` |
| FR-004 | US1 | `ParceiroService.delete` + `AutorizacaoPorAcaoIntegrationTest` |
| FR-005 | US1 | `ParceiroRepository.search` + teste de integração |
| FR-006 | US2 | `SolicitacaoServiceTest` |
| FR-007 | US2 | `SolicitacaoServiceTest.putIgnoraSituacao` |
| FR-008 | US2 | `SolicitacaoServiceTest` (bloco Transicoes) |
| FR-009 | US2 | `SolicitacaoServiceTest.recusaComEtapaPendente` |
| FR-010 | US2 | `SolicitacaoServiceTest.exigeMotivo` |
| FR-011 | US3 | `SolicitacaoServiceTest.publicaEvento` |
| FR-012 | US2 | `SolicitacaoService.delete` |
| FR-013 | US4 | `ParceiroServiceTest` (bloco SigiloDaComissao, 4 casos) |
| FR-014 | US4 | `AuditoriaSigiloIntegrationTest` |
| FR-015 | — | `AuditoriaCoberturaIntegrationTest` + `AuditoriaIsolamentoTest` |

Nenhum FR sem teste; nenhum teste sem FR de origem.
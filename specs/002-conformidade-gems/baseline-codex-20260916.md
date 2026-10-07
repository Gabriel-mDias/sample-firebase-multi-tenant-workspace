# Baseline canônico — S-codex 2026-09-16

| Campo | Valor |
| :-- | :-- |
| ID | `BL-S-CODEX-20260916` |
| Hash/âncora | `b807233` (C06 verde) |
| Autoridade | `spec.md`, `plan.md`, `research.md`, `tasks.md` e `roteiro.md` da rodada 002; a constituição prevalece. |
| Decisões aprovadas | D-P01 e D-P02 preservam garantias até SDK publicada compatível; D-P03 não se aplica; T004 foi autorizada; ajuste C04 foi aprovado. D-P04 continua sem decisão. |
| Invariantes | Sem adapter/local branch para contornar SDK; adoção remove a classe local no mesmo diff; nenhum aceite humano é inferido; baseline de conformidade só encolhe; C00–C06 são histórico imutável. |
| Versões/SHAs | Java GEMS 3.1.0 (`137f18f`), Angular GEMS 2.1.0 (`e452ba0`), sample C06 (`b807233`). A próxima SDK proposta é 3.2.0 apenas se aditiva. |
| Gates disponíveis | backend `./mvnw.cmd clean install`; frontend `npm ci`, `npm test`, `npm run build`; `git diff --check`; verificador de lote. |

Cada pacote posterior referencia exatamente `BL-S-CODEX-20260916@b807233` e contém somente seu delta.
Alterar esta âncora, encontrar hash divergente ou editar fora de READ/EDIT/CREATE invalida o pacote e
exige sua regeneração. Estados registram apenas diff real, mutações, resumo do gate, SHA final,
desvio e próximo passo.

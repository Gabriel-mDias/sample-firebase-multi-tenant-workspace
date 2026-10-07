# Medição dos gates — S-def, T056

Data: 2026-09-14. Base de código: `0459526`; definições: `411156d`.
Este é relatório bruto, não baseline autorizado nem conformação do código.
Fontes completas: [backend](medicao-backend.json), [Checkstyle](medicao-checkstyle.json),
[frontend](medicao-frontend.json). O congelamento ocorre em T057.

| Gate | Resultado bruto |
| :--- | :--- |
| Checkstyle | 314 ocorrências: {'ParenPad': 290, 'AvoidStarImport': 24} |
| ArchUnit/fonte | 189 entradas: 4 repositories sem fragmento, 2 controllers sem BaseController, 6 ordens de service, 177 nomes de teste |
| Frontend | 174 ocorrências: {'visual-standards.md#2': 59, 'testing.md#7': 110, 'signals-standards.md#1': 1, 'organization.md#1': 1, 'organization.md#6': 3} |
| Spotless | `spotless:check -fae` vermelho em todos os cinco módulos com Java; nenhum arquivo foi formatado |
| Exceções escritas | 0 exceções; inventário gerado localmente, sem copiar o do Meduc |
| Silent check-sso | vermelho: `ENOENT .../frontend/public/silent-check-sso.html`; HTML e JS ausentes, embora configurados em keycloak.init.ts |

Reator em modo relatório: **BUILD SUCCESS**, **203 testes** (13 security + 132 core + 58 deploy),
sem falhas/erros, 01:02 min; boot e validação JPA executados. A base antes dos gates passou com 179.
Frontend: `ng test` **21 arquivos / 118 casos**; `ng build` **724,68 kB**, ambos verdes.
`npm test` encadeado permanece vermelho por ausência do asset silent check-sso;
os comandos diretos foram usados para medir a suíte e o bundle, não equivalem ao gate completo.
Avisos preexistentes de budget e CommonJS SweetAlert mantidos.

O levantamento estimava 3 repositories, 179 nomes e 70 hex; os gates reais medem
4 repositories, 177 nomes e o conjunto completo acima. Não completar as divergências por suposição.
Ratchet proposto para Spotless: referência fixa `411156d`, que preserva a dívida
dos arquivos existentes e exige formato correto nos arquivos alterados/novos.
Nenhum `spotless:apply` sobre o código de domínio pelo orquestrador.

**Bloqueio de T057:** decidir o tratamento dos assets de autenticação ausentes;
ajuste concreto no roteiro da rodada 010 do Meduc, `ajuste-s-def.md`.
O catálogo de ações foi copiado desligado conforme contrato, até S-09.

## T057 — congelamento aprovado em 2026-09-14

Aprovação do ajuste registrada na §12 da rodada 010 do Meduc. Assets HTML/JS instalados
pelo orquestrador; contribuição explicitada em OK-1. Checkstyle: 314 ocorrências em
22 supressões específicas; arquitetura/fonte: 189 identidades; frontend: 174 ocorrências.
Catraca Checkstyle recusa supressões fora do conjunto congelado. Frontend e Java recusam
crescimento dos totais. S-codex deve reduzir também constantes e conjunto congelado.
Spotless ratchet `411156d`; apenas infraestrutura dos gates formatada.
POM em reprovação por padrão; frontend sem variável de relatório.

Gate final T057: BUILD SUCCESS, 204 testes (13 security + 132 core + 59 deploy),
zero falhas/erros, 01:02 min; contexto JPA com validate verde. npm test: 21/118;
build: 724,68 kB; prettier e contexto verdes. Nove mutações rejeitadas/restauradas.
Avisos preexistentes de budget e CommonJS mantidos. CI recebe fetch-depth=0 para o ratchet.

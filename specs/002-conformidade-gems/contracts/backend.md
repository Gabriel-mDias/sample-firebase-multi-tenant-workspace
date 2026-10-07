# Contrato backend — Conformidade GEMS

## Contratos preservados

- Rotas/payloads/status de parceiro, solicitação, localidade, normalização, usuário e S3.
- `GET /api/auditoria/{entidade}/{id}` e `POST /api/auditoria/search`, ambos somente leitura.
- Auditoria: autor estável, nome/tipo, correlação, campos, agregados, paginação e data final inclusiva.
- Usuário/Keycloak: criar, consultar, alterar, habilitar, suspender, compensar/remover e trocar grupo.
- Autorização: 21 ações, perfis compostos, endpoints por ação e autorização de campo.

## Decisões bloqueantes

Java SDK 3.1.0 não satisfaz integralmente auditoria nem Keycloak. Até D-P01/D-P02 e eventual release
publicada, as implementações locais permanecem e não há adoção parcial. Não será criado wrapper que
preencha por fora os dados omitidos nem cliente paralelo.

## Rollback integrado

Os novos testes vivem em `app-deploy`, chamam bean Spring proxied, usam PostgreSQL real e não anotam o
método de teste com transação. Uma exceção checked ocorre após a primeira escrita. As asserções abrem
nova leitura e exigem ausência de raiz, filhos, efeito de evento e auditoria. Remover `rollbackFor`
precisa deixar dado visível e tornar a mutação vermelha.

## Zero Java

O contrato de fechamento é zero no relatório Checkstyle, zero nas quatro identidades repository, zero
nos dois controllers, zero nas seis ordens e zero nos 177 nomes. `suppressions.xml` não conserva
supressão de dívida, arquivos baseline deixam de tolerar dívida e o POM não contém `ratchetFrom`.

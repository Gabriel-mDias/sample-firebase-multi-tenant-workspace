# Modelo de dados — Conformidade GEMS

## Estado atual e garantias

`REGISTRO_AUDITORIA` contém UUID da operação, entidade, UUID afetado, operação em português, instante,
`ID_AUTOR`, `NM_AUTOR` e `CD_CORRELACAO`. `CAMPO_ALTERADO` contém FK, campo, valores e sigilo. A
consulta filtra `autorId`; correlação preserva vínculo entre gravações de um mesmo save.

## Contrato publicado 3.1.0

`AUDIT_OPERATION` recebe UUID, operação inglesa, entidade, id textual, nome/tipo do ator e instante.
`AUDIT_CHANGE` recebe mudança e sigilo. Não existe coluna nem argumento para identidade estável ou
correlação. Coluna extra no banco não resolve: o writer não a preenche.

## D-P01 — dois caminhos mutuamente exclusivos

### A. Preservar (recomendado)

Antes da implementação, publicar SDK com identidade estável e correlação no modelo/writer e aferir a
nova fonte. Só então definir nomes/tipos finais e changesets. Este plano não inventa a API futura.

### B. Emendar contrato

O product owner pode aceitar que novas linhas tenham apenas nome/tipo e não tenham id/correlação. A
migração manteria os valores antigos, mas filtro por `autorId` e correlação seriam completos apenas
para legado. Essa perda deve constar nominalmente na assinatura e nos contratos; resolução id→login
não é aceita como equivalência.

## Bloco atômico após decisão

1. Teste de upgrade com dados legados e testes de writer falhando.
2. Changesets com backfill/renames/constraints e preconditions sem truncamento.
3. Read model/DTO/repositories/service ajustados ao contrato selecionado.
4. Provider de ator/configuração e ativação SDK.
5. Remoção simultânea de writer/anotações/testes órfãos locais.
6. Testes de commit, rollback, sigilo, ator, correlação, filtro e `ddl-auto: validate` verdes.

Nenhum desses passos fecha checkpoint ou commit isolado.

## Catálogo e realm

O enum possui 21 ações e os perfis continuam no mapa atual. Se D-P03 for acionada, somente roles de
ação e compostas de perfil são derivadas. O gate também faz snapshot/paridade de `clients`, protocol
mappers, users, groups, defaults e client roles para provar que o gerador não reescreveu o realm todo.

## Residual frontend

O inventário final é uma entidade de decisão: `regra`, `arquivo`, `linha/identidade`, `motivo`,
`alternativa`, `status humano`. O agente pode medir e recomendar; somente humano muda o status para
aceito.

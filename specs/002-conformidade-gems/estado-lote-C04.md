# Estado — Lote C04, rodada 002

## 1. Rollback deste lote

Na branch de trabalho, restaurar somente os arquivos rastreados tocados pelo C04:

```powershell
git checkout -- `
  'backend/app-core/src/main/java/br/com/gems/app/auditoria/repository/CampoAlteradoRepository.java' `
  'backend/app-core/src/main/java/br/com/gems/app/commons/exception/ProvedorExternoException.java' `
  'backend/app-core/src/main/java/br/com/gems/app/commons/exception/ProvedorExternoExceptionHandler.java' `
  'backend/app-core/src/main/java/br/com/gems/app/localidade/controller/LocalidadeController.java' `
  'backend/app-core/src/main/java/br/com/gems/app/localidade/repository/EstadoRepository.java' `
  'backend/app-core/src/main/java/br/com/gems/app/localidade/repository/PaisRepository.java' `
  'backend/app-core/src/main/java/br/com/gems/app/normalizacao/controller/CepController.java' `
  'backend/app-core/src/main/java/br/com/gems/app/normalizacao/service/PessoaService.java' `
  'backend/app-core/src/main/java/br/com/gems/app/solicitacao/repository/EtapaSolicitacaoRepository.java' `
  'backend/app-deploy/src/test/java/br/com/gems/app/autorizacao/AutorizacaoPorAcaoIntegrationTest.java' `
  'backend/app-security/src/main/java/br/com/gems/app/security/AccessDeniedExceptionHandler.java' `
  'backend/app-security/src/main/java/br/com/gems/app/security/IdentityProviderExceptionHandler.java' `
  'backend/app-security/src/main/java/br/com/gems/app/security/RequisicaoInvalidaExceptionHandler.java' `
  'backend/app-security/src/main/java/br/com/gems/app/security/service/KeycloakUserService.java' `
  'specs/002-conformidade-gems/roteiro.md' `
  'specs/002-conformidade-gems/tasks.md'
Remove-Item -Force -ErrorAction SilentlyContinue -LiteralPath `
  'backend/app-deploy/src/test/java/br/com/gems/app/security/ErrorEnvelopeIntegrationTest.java', `
  'specs/002-conformidade-gems/estado-lote-C04.md'
```

Preservar o pacote C04 não rastreado, que já consta da âncora.

## 2. Concluído

T052 concluída: os quatro repositories estendem `JpaRepository` antes de
`BaseCustomJpaRepository`; gate focado `ConvencoesArquiteturaTest` terminou exit 0/BUILD SUCCESS,
8 testes, zero falhas/erros/skips.
T053 concluída: `LocalidadeController` e `CepController` estendem `BaseController`; repetição do
gate focado terminou exit 0/BUILD SUCCESS, 8 testes, zero falhas/erros/skips.
T054 concluída: teste-first produziu 3 falhas; `ProvedorExternoException` adotou
`ExternalServiceException`, falhas Admin API do Keycloak passaram a preservar mensagem/causa e os
quatro handlers locais foram removidos. Gate pré-mutação terminou exit 0/BUILD SUCCESS, 11 testes.
Mutação real retirou a herança SDK e produziu 1 falha exata, 502 esperado contra 500 recebido;
restauração por patch inverso terminou exit 0/BUILD SUCCESS, 4 testes.
T055 concluída: medição nominal dos seis services terminou com zero ordem; o residual real em
`PessoaService` e o residual de `KeycloakUserService` foram movidos depois dos públicos.
`ConvencoesFonteTest` terminou exit 0/BUILD SUCCESS, 9 testes. Portão final C04 terminou exit 0/
BUILD SUCCESS, 28 testes, zero falhas/erros/skips; repository/controller/ordem nominais em 0/0/0.
Após fixar também mensagens e `codigo` de 400/404 no teste, o mesmo portão final foi repetido no
estado definitivo: exit 0/BUILD SUCCESS, 28 testes, zero falhas/erros/skips.

## 3. Em curso

- **Tarefa:** nenhuma; C04 concluído
- **Arquivo:** nenhum com mutação ativa
- **O que já está feito nele:** T052–T055, mutação, restauração, medição nominal e portão final verdes
- **O que falta:** nada neste pacote; parar antes de C05

## 4. Não iniciado

Nenhuma.

## 5. Arquivos tocados

`git status --porcelain --untracked-files=all`

```text
 M backend/app-core/src/main/java/br/com/gems/app/auditoria/repository/CampoAlteradoRepository.java
 M backend/app-core/src/main/java/br/com/gems/app/commons/exception/ProvedorExternoException.java
 D backend/app-core/src/main/java/br/com/gems/app/commons/exception/ProvedorExternoExceptionHandler.java
 M backend/app-core/src/main/java/br/com/gems/app/localidade/controller/LocalidadeController.java
 M backend/app-core/src/main/java/br/com/gems/app/localidade/repository/EstadoRepository.java
 M backend/app-core/src/main/java/br/com/gems/app/localidade/repository/PaisRepository.java
 M backend/app-core/src/main/java/br/com/gems/app/normalizacao/controller/CepController.java
 M backend/app-core/src/main/java/br/com/gems/app/normalizacao/service/PessoaService.java
 M backend/app-core/src/main/java/br/com/gems/app/solicitacao/repository/EtapaSolicitacaoRepository.java
 M backend/app-deploy/src/test/java/br/com/gems/app/autorizacao/AutorizacaoPorAcaoIntegrationTest.java
 D backend/app-security/src/main/java/br/com/gems/app/security/AccessDeniedExceptionHandler.java
 D backend/app-security/src/main/java/br/com/gems/app/security/IdentityProviderExceptionHandler.java
 D backend/app-security/src/main/java/br/com/gems/app/security/RequisicaoInvalidaExceptionHandler.java
 M backend/app-security/src/main/java/br/com/gems/app/security/service/KeycloakUserService.java
 M specs/002-conformidade-gems/roteiro.md
 M specs/002-conformidade-gems/tasks.md
?? backend/app-deploy/src/test/java/br/com/gems/app/security/ErrorEnvelopeIntegrationTest.java
?? specs/002-conformidade-gems/pacote-de-execucao-C04.md
```

## 6. Decisões fora do pacote

Diferenças observáveis do envelope SDK registradas: 403 muda de “Você não tem permissão para
executar esta ação.” para “Você não possui acesso para este serviço!”; 404 muda de “O endereço
solicitado não existe nesta API.” para “Recurso não encontrado.”; 400 preserva “O parâmetro 'id'
foi informado em um formato inválido.”. Em 404/400, `RECURSO_NAO_ENCONTRADO` e
`PARAMETRO_INVALIDO` passam de `errorType` para `codigo`; os novos `errorType` são `FALHA` e
`VALIDACAO`. Status e o envelope obrigatório permanecem provados.
O pacote enumerou seis services em T055, mas a fonte pós-C03 já deixava cinco deles em ordem; só
`PessoaService` ainda tinha privado antes de público. A medição nominal foi executada nos seis e
terminou em zero sem criar edições artificiais. `KeycloakUserService`, já alterado por T054, tinha o
mesmo residual e também foi ordenado.

## 7. O que está quebrado agora

Nada — nenhuma mutação ativa; portão final C04 verde, 28 testes, zero falhas/erros/skips.

## 8. Retomada

- **Executor:** Codex
- **Token de retomada:** não exposto ao executor nesta sessão

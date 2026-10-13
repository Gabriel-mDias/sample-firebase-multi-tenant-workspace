# Quickstart — validar a 004

Guia de execução das provas. O detalhe de comportamento está no [data-model](data-model.md) e no
[contrato](contracts/sdk-gems-firebase-auth-3.5.0.md).

## 1. SDK (Java-GEMS-SDK)

Pré-requisitos: JDK 21 e Docker para o Testcontainers.

```powershell
mvn -pl gems-firebase-auth -am verify -Dtest=JdbcSecurityPostgresTest
mvn --batch-mode clean verify
```

Esperado: casos novos verdes (devolução, idempotência, catálogo sem a ação, ENABLE obsoleto,
cancelamento, entrega única de evento e reentrega depois de falha) e reator inteiro verde.
Mutação: remover o passo 2 do ACK, ver os casos de devolução falharem e restaurar; remover a
reivindicação do `afterCommit`, ver o caso de entrega única falhar e restaurar.

## 2. Publicação

Tag `v3.5.0` aprovada pelo usuário e workflow `release.yml` verde. Antes disso, o sample não adota
(Constituição, "Adoção de Módulo da SDK GEMS", condição 1).

## 3. Sample

```powershell
cd backend; .\mvnw.cmd dependency:tree -pl app-deploy   # antes e depois da troca para 3.5.0
.\mvnw.cmd clean install                                  # com ADC do projeto Firebase de testes
```

Esperado: o E2E de lifecycle com o Firebase real afirma `/auth/me` 200, nenhuma empresa e exatamente
uma concessão vigente da ação padrão depois do ACK do `ENABLE`. O contrato de lifecycle afirma que o
caso obsoleto não concede. O job `identity-worker` emite cada evento uma vez. Mutação no consumidor:
voltar o worker de teste ao construtor antigo e ver o E2E falhar.

## 4. Verificação humana

No ambiente local da homologação da 003: suspender o Tester, rodar o worker, reativar, rodar o worker
e entrar com ele. A tela deve abrir com "Nenhuma empresa disponível", e não falhar no login.

# Evidências — 006 Formatter Java do GEMS AI SDK 3.1.0

Registro do que foi observado na execução. O que ainda não rodou não aparece aqui.

## T001 — CLI

Em 2026-10-12, o SHA-256 do `gems_ai-3.1.0-py3-none-any.whl` deu
`1a7027d10cb1041c6fff52367d7dac15f365fb650f3abe965afddee1cedba887`, igual à linha do `SHA256SUMS` da
release `v3.1.0`. A CLI está num venv isolado fora dos repositórios, e `gems-ai --version` = `3.1.0`.

## T002 — Linha de base do Java-GEMS-SDK

A branch `feature/006-formatter-gems-ai` foi criada a partir de `origin/main` `e830533`. Com JDK 21,
`mvn --batch-mode clean verify -DgenerateBackupPoms=false` terminou em `BUILD SUCCESS`, nos 18 módulos
do reator, com **402 testes**, 0 falhas, 0 erros e 0 ignorados.

| Módulo | Testes |
| :--- | --: |
| GEMS SDK Utils | 38 |
| GEMS SDK Model Mapper | 5 |
| GEMS SDK MapStruct | 4 |
| GEMS SDK Exception | 29 |
| GEMS SDK JPA | 4 |
| GEMS SDK JPA Multi-Tenant | 41 |
| GEMS SDK Auditing | 35 |
| GEMS SDK Keycloak Admin | 33 |
| GEMS SDK Security Authorization | 25 |
| GEMS Firebase Auth | 174 |
| GEMS SDK AWS | 11 |
| GEMS SDK Validation | 3 |

Os outros 6 módulos (raiz, BOM, AWS Web, REST Common, OpenAPI e Observability) não têm testes.

## Linha de base dos testes Python do sample (R11)

Em 2026-10-12, na branch da 005 rebaseada sobre `origin/main` `5b849a3`,
`python -m unittest discover -s scripts -p "test_*.py"` rodou 60 testes, com 50 falhas e 7 erros.
Em 48 deles a causa é `Protected metadata integrity conflict: backend/app-security/pom.xml`. O
`clean-demo --root <checkout> --dry-run` real recusa pelo mesmo motivo.

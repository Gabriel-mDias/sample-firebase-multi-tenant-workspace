# Benchmark de tenants (T030)

Mede criação e provisionamento sequencial de até **100 tenants/schemas**, sem SLA,
com o SDK 3.4.1, `JdbcTenantService`, `JdbcTenantProvisioner` e `SampleTenantMigration`
reais. PostgreSQL 16 Alpine descartável via Testcontainers; nenhuma credencial ou
chamada ao Firebase. As portas determinísticas do fixture existente são reutilizadas.

Pré-requisitos de execução: Java 21, Docker disponível e dependências Maven do projeto.
O executor T030 não executa Maven/Docker; VBM1 (100 tenants) e VBM2 (Spotless) são
executados pelo coordenador no espelho.

```powershell
.\scripts\benchmark-tenants\run.ps1 -N 100
.\scripts\benchmark-tenants\run.ps1 -N 100 -Output "$PWD\benchmark-tenants.json"
```

O script executa a partir de `backend/`:

```powershell
.\mvnw.cmd -B -pl app-deploy -am test -Dtest=TenantProvisioningBenchmark -Dbenchmark.tenants=100 -Dsurefire.failIfNoSpecifiedTests=false
```

Sem `benchmark.tenants`, a condição JUnit desabilita a classe. O nome da classe
também fica fora dos padrões habituais de testes do Surefire no `clean install`.
O JSON padrão fica em `backend/app-deploy/target/benchmark-tenants.json`.
`benchmark.output` aceita outro caminho; o script resolve `-Output` a partir do
diretório de invocação.

O JSON registra N, tempo total e p50/p95/max por tenant (criação + provisionamento),
tempos de `TenantSchemaSource.expectedSchemas()` e listagem READY, heap antes e
depois de `System.gc()`, statements e versões efetivas do SDK e PostgreSQL.
Percentis usam a posição de ordem mais próxima por teto; tempos usam relógio
monotônico e milissegundos. Heap é uma observação do processo JVM, não do container;
`System.gc()` é uma solicitação à JVM, sem garantia de coleta completa.

`pg_stat_statements` é pré-carregado no container e sua extensão é criada antes
da medição. O contador é zerado após o bootstrap e soma chamadas do banco de teste
(incluindo SQL interno com `track=all`), excluindo consultas de instrumentação.
Inclui criação, provisionamento e inventário; não inclui bootstrap nem asserts
posteriores. Não equivale a quantidade de viagens de rede JDBC. Falha ao carregar
a extensão é falha de infraestrutura, nunca resultado de desempenho.

Não há aquecimento ou asserts de desempenho. Os asserts verificam N READY
distintos, N schemas físicos `tenant_` distintos, equivalência do inventário e
versão do SDK. O resumo é impresso como
`BENCHMARK N=... total=...ms p95=...ms inventario=...ms sql=...`.

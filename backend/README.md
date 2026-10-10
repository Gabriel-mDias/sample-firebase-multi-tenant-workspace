# Backend Firebase multi-tenant

Java 21, Spring Boot 4.1 e SDK GEMS publicada. Os módulos `app-model`, `app-security`,
`app-events`, `app-core` e `app-deploy` preservam o grafo unidirecional. Configuração e
Liquibase ficam em `app-deploy`; Hibernate valida o schema e OSIV permanece desligado.

O ambiente local está descrito no [README principal](../README.md). Configure Maven e o
registry privado externamente e execute `mvnw.cmd clean install` sob JDK 21. O Dockerfile
consome apenas `app-deploy/target/app-deploy-*.jar` gerado por esse build. Não recebe
credencial de registry, settings.xml, chave Firebase ou argumentos de segredo.

Para executar no host com PostgreSQL e Auth Emulator locais, forneça no processo:
`SPRING_PROFILES_ACTIVE=demo`, `DB_URL=jdbc:postgresql://localhost:5432/gems_demo`,
`DB_USERNAME=gems_demo`, `DB_PASSWORD` lida do arquivo externo usado pelo Compose,
`FIREBASE_PROJECT_ID=demo-gems`, `FIREBASE_EMULATOR_ENABLED=true` e
`FIREBASE_AUTH_EMULATOR_HOST=localhost:9099`. Então execute
`mvnw.cmd -pl app-deploy spring-boot:run`. O backend em Docker usa `auth-emulator:9099`.
O emulator exige opt-in e todos os perfis ativos em `local`, `test`, `ci` ou `demo`;
um projeto real ou perfil de produção com emulator falha no startup.

`GET /actuator/health` e `GET /api/demo/public` são públicos. `GET /api/auth/me` e
`GET /api/auth/context` exigem autenticação e ações SQL concretas. Ausência de token
retorna 401; nenhuma primeira autenticação concede administração. S3 permanece negado.

Bootstrap central é um job explícito, sem endpoint HTTP: exige UID Google verificado
pelo Admin e concede o catálogo GLOBAL ao operador designado, sem grant TENANT implícito.
Não imprime nem persiste token. Provisionamento de tenant e processamento da outbox
também são jobs separados; use os contratos da spec para operá-los. A execução local
do ambiente não cria operador nem empresas.

Homologação Firebase real exige ADC externo, projeto isolado e testers confirmados.
O emulator não comprova Google real, revogação real nem entrega de e-mail do provedor.

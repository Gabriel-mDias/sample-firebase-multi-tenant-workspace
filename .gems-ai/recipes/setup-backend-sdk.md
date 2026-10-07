---
origem: local
precedencia: local-vence
versao: 2.1.0
---

# Recipe: Setup Backend — Java GEMS SDK

**Objetivo:** Configurar um novo projeto Spring Boot com a GEMS SDK via Maven BOM, GitHub Packages e módulos opcionais.

**Rules relacionadas:**
- [architecture.md](../rules/backend-java/architecture.md)
- [rest-common.md](../rules/backend-java/rest-common.md)
- [persistence-db.md](../rules/backend-java/persistence-db.md)

---

## Passo 1: Autenticação no GitHub Packages

```xml
<!-- ~/.m2/settings.xml -->
<settings>
  <servers>
    <server>
      <id>github-gems</id>
      <username>${env.GITHUB_ACTOR}</username>
      <password>${env.GITHUB_TOKEN}</password>
    </server>
  </servers>
</settings>
```

Variáveis de ambiente: `GITHUB_ACTOR` (seu usuário GitHub) e `GITHUB_TOKEN` (PAT com `read:packages`).

## Passo 2: BOM no pom.xml raiz

```xml
<dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>br.com.gems</groupId>
      <artifactId>gems-bom</artifactId>
      <version>LATEST</version>    <!-- Substituir pela versão do BOM -->
      <type>pom</type>
      <scope>import</scope>
    </dependency>
  </dependencies>
</dependencyManagement>

<repositories>
  <repository>
    <id>github-gems</id>
    <url>https://maven.pkg.github.com/Gabriel-mDias/Java-GEMS-SDK</url>
  </repository>
</repositories>
```

## Passo 3: Módulos por Funcionalidade

Adicionar **apenas os módulos necessários** — sem versão (gerenciada pelo BOM):

```xml
<!-- SEMPRE incluir (núcleo) -->
<dependency>
    <groupId>br.com.gems</groupId>
    <artifactId>gems-utils</artifactId>
</dependency>
<dependency>
    <groupId>br.com.gems</groupId>
    <artifactId>gems-exception</artifactId>
</dependency>
<dependency>
    <groupId>br.com.gems</groupId>
    <artifactId>gems-model-mapper</artifactId>
</dependency>

<!-- Persistence (quando há banco de dados) -->
<dependency>
    <groupId>br.com.gems</groupId>
    <artifactId>gems-jpa</artifactId>
</dependency>

<!-- REST (quando expõe endpoints HTTP) -->
<dependency>
    <groupId>br.com.gems</groupId>
    <artifactId>gems-rest-common</artifactId>
</dependency>

<!-- Validação BR (CPF, CNPJ, e-mail) -->
<dependency>
    <groupId>br.com.gems</groupId>
    <artifactId>gems-validation</artifactId>
</dependency>

<!-- Opcionais -->
<dependency>
    <groupId>br.com.gems</groupId>
    <artifactId>gems-jpa-multi-tenant</artifactId>   <!-- Multi-tenancy schema-based -->
</dependency>
<dependency>
    <groupId>br.com.gems</groupId>
    <artifactId>gems-aws</artifactId>                <!-- S3 Service -->
</dependency>
<dependency>
    <groupId>br.com.gems</groupId>
    <artifactId>gems-aws-web</artifactId>            <!-- S3Controller (presigned URLs) -->
</dependency>
<dependency>
    <groupId>br.com.gems</groupId>
    <artifactId>gems-openapi</artifactId>            <!-- Swagger/springdoc auto-config -->
</dependency>
<dependency>
    <groupId>br.com.gems</groupId>
    <artifactId>gems-observability</artifactId>      <!-- @Observed + Micrometer -->
</dependency>
```

## Passo 4: Estrutura de Módulos Maven (Multi-módulo)

```
{app}/
├── pom.xml                 # Parent: herda Spring Boot + GEMS BOM
├── {app}-model/
│   ├── pom.xml             # DTOs, interfaces, enums — sem dependências de infraestrutura
│   └── src/
└── {app}-core/
    ├── pom.xml             # Spring Boot, JPA, gems-jpa, gems-rest-common
    └── src/
```

```xml
<!-- {app}-core/pom.xml — depende do model -->
<dependency>
    <groupId>br.com.gems.{app}</groupId>
    <artifactId>{app}-model</artifactId>
    <version>${project.version}</version>
</dependency>
```

## Passo 5: application.yml Base

```yaml
spring:
  application:
    name: {app}
  datasource:
    url: jdbc:postgresql://localhost:5432/{app}
    username: ${DB_USER}
    password: ${DB_PASS}
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect

  liquibase:
    change-log: classpath:db/changelog/db.changelog-master.yaml

gems:
  rest:
    correlation-id:
      header-name: X-Correlation-Id
      include-in-response: true
  openapi:
    title: "{App} API"
    version: "1.0"
    description: "API do sistema {App}"

server:
  port: 8080
```

## Passo 6: Estrutura de Pacotes

```
br.com.gems.{app}/
├── config/              # @Configuration: Security, observabilidade, etc.
├── {modulo}/
│   ├── controller/
│   ├── service/
│   ├── repository/
│   ├── mapper/          # @Mapper(config = GemsMappingConfig.class)
│   ├── entity/
│   └── dto/
└── {modulo2}/
    └── ...
```

---

## Checklist de Conformidade

- [ ] GitHub Packages configurado em `~/.m2/settings.xml`.
- [ ] BOM importado no `<dependencyManagement>`.
- [ ] Apenas módulos necessários incluídos (sem versão explícita).
- [ ] Estrutura multi-módulo: `{app}-model` + `{app}-core`.
- [ ] Pacote base: `br.com.gems.{app}`.
- [ ] `ddl-auto: validate` (nunca `update` ou `create` em produção).
- [ ] Credenciais de banco via variáveis de ambiente (`${DB_USER}`, `${DB_PASS}`).
- [ ] Liquibase configurado com `change-log` apontando para `db.changelog-master.yaml`.

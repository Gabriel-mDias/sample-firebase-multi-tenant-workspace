---
stack: backend-java
module: gems-jpa-multi-tenant
severity: mandatory
see-also: [security.md, persistence-db.md]
origem: local
precedencia: local-vence
versao: 2.1.0
---

> **MECANISMO IMPLEMENTADO** (rodada 007, C-04). O Meduc é multitenant por organização.
> `administracao` e `security` são globais; negócio e sua auditoria vivem em `tenant_<alias>`.
> Resolução, provider e trilha vêm do `gems-jpa-multi-tenant` e do `gems-auditing`; o identificador
> que o Hibernate recebe é o **alias**, nunca o nome do schema. Continua proibido introduzir fallback
> para `public` ou uma solução paralela — o estado real está em `backend/AGENTS.md`.

# Multi-Tenancy — gems-jpa-multi-tenant

> **TL;DR:**
> - Schema-based multi-tenancy via Hibernate connection providers.
> - `JpaTenantContext.setCurrentTenant(tenant)`: popule em um `RequestFilter` **antes** de qualquer acesso ao banco.
> - Cada tenant tem seu próprio schema no PostgreSQL — isolamento completo de dados.
> - **Obrigatório:** `JpaTenantContext.clear()` no bloco `finally` para evitar vazamento entre requisições.

## 1. Conceito

O `gems-jpa-multi-tenant` implementa multi-tenancy baseado em **schemas PostgreSQL**. Cada tenant tem seu próprio schema e o Hibernate troca automaticamente de schema em cada requisição com base no contexto.

```
Requisição HTTP
   │
   ▼
RequestFilter
   └── JpaTenantContext.setCurrentTenant("tenant-abc")
          │
          ▼
      Hibernate usa schema "tenant-abc" para todas as queries desta thread
```

---

## 2. Populando o Contexto do Tenant

Crie um `Filter` ou `HandlerInterceptor` que popule o contexto antes de qualquer acesso ao banco:

```java
@Component
@Order(1)
public class TenantContextFilter implements Filter {

    @Override
    public void doFilter( ServletRequest request, ServletResponse response, FilterChain chain )
            throws IOException, ServletException {

        var httpRequest = (HttpServletRequest) request;

        // Extraia o tenant do header, JWT, subdomínio ou parâmetro — depende da estratégia do projeto
        var tenantId = extractTenantId( httpRequest );

        JpaTenantContext.setCurrentTenant( tenantId );
        try {
            chain.doFilter( request, response );
        } finally {
            JpaTenantContext.clear();  // Limpa após a requisição — obrigatório
        }
    }

    private String extractTenantId( HttpServletRequest request ) {
        // Estratégias comuns:
        // 1. Header: request.getHeader("X-Tenant-Id")
        // 2. JWT claim: extrair do token no SecurityContext
        // 3. Subdomínio: request.getServerName().split("\\.")[0]
        return request.getHeader( "X-Tenant-Id" );
    }
}
```

---

## 3. Configuração do Módulo

```xml
<!-- pom.xml -->
<dependency>
    <groupId>br.com.gems</groupId>
    <artifactId>gems-jpa-multi-tenant</artifactId>
</dependency>
```

```yaml
# application.yml
gems:
  jpa:
    multi-tenant:
      enabled: true
      default-schema: public   # Schema padrão para dados compartilhados
```

---

## 4. Regras

- **Sempre limpe o contexto** no `finally` — evita vazamento entre requisições no pool de threads.
- O `JpaTenantContext.setCurrentTenant()` deve ser chamado **antes** de qualquer operação JPA na thread.
- Dados administrativos globais ficam explicitamente em `administracao`; identidade persistida fica
  em `security`. `public` nunca é fallback de operação tenant.
- Dado escolar e sua auditoria ficam no schema `tenant_<alias>` da organização comprovada.
- `ADMIN` global não herda acesso a dado escolar; autorização tenant vem do contexto da organização.
- Testes de integração precisam configurar o contexto do tenant explicitamente.
- Não armazene o `tenantId` em variáveis de instância — use apenas o `JpaTenantContext` (ThreadLocal).

---

## Anti-Patterns (NUNCA)

| Proibido | Correto |
| :--- | :--- |
| Omitir `JpaTenantContext.clear()` no `finally` | Sempre limpar no `finally` — evita vazamento entre requisições no pool |
| Setar `JpaTenantContext` depois de qualquer operação JPA | Setar o contexto **antes** de qualquer operação JPA na thread |
| `private String tenantId` como campo de instância | Usar apenas `JpaTenantContext` (ThreadLocal por requisição) |

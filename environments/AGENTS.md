# Ambientes

- Configuração de ambiente é código versionado.
- Realm do Keycloak: `shared/keycloak/app-realm.yaml`; alterações no console são drift.
- `IMPORT_CACHE_ENABLED=false` em todos os ambientes.
- Segredos vêm por variável; somente `.env.example` é versionado.
- Em produção apenas o Caddy expõe porta.
- Nunca execute `docker compose down -v`.
- Subida local ou de produção deve ser `docker compose up -d`, sem script manual posterior.
- Mudança de infraestrutura usa ciclo spec-driven e exige validação de restore quando tocar backup.
- Três ambientes: `local`, `staging` e `production`. Staging pode dividir a máquina com produção,
  desde que com rede, volume, banco, identidade e credencial próprios — `vps/08-staging.md`.
  **Dado real nunca entra nele.**
- A rede `app_staging_network` é externa às duas stacks; crie-a uma vez com `docker network create`.
- Teto de heap é `JAVA_TOOL_OPTIONS` no backend (o ENTRYPOINT é forma exec, `JAVA_OPTS` seria inerte)
  e `JAVA_OPTS_KC_HEAP` no Keycloak. Sem `mem_limit`, as JVMs dimensionam heap pelo host — e num host
  compartilhado elas somam mais memória do que existe.

> **O ensaio de restore é gate, não documentação.** O Princípio IX exige backup *verificado*: um
> mecanismo de backup entregue sem um restore executado atende metade da cláusula. Quem alterar
> backup herda essa pendência e a declara aqui.

Regras GEMS vencem código consolidado; referência não autoriza desvio. Consulte a constituição 2.0.0 e a seção Adoção de Módulo da SDK GEMS.

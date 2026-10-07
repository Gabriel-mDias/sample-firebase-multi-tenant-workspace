# Backend

## Arquitetura

- Reator Maven: `app-model`, `app-security`, `app-events`, `app-core`, `app-deploy`.
- Dependências são unidirecionais; somente `app-deploy` executa e guarda ambiente/Liquibase.
- `app-model` é camada-primeiro; `app-core` é domínio-primeiro.
- `app-events` contém somente POJOs; listener que chama regra de negócio fica em `app-core`.
- Leitura entre domínios pode usar repository; escrita/regra de outro domínio usa evento.
- Sem cascade.

## Domínios de `app-core`

- `parceiro`, `solicitacao`, `localidade`, `normalizacao`, `usuario`, `auditoria`, `autorizacao`,
  `commons`.
- Cada domínio segue `{controller,service,repository,listener}`; nem todo domínio usa os quatro.

## Persistência e serviços

- IDs `UUID`; entidades de negócio estendem `EntidadeAuditavel` e usam soft delete.
- `ManyToOne` é LAZY; associação usada fora da transação exige entity graph ou carga no service.
- Schema muda somente por Liquibase em `app-deploy`; `ddl-auto: validate`.
- Ordem de service: `findById > search > delete > insert > save > validate`.
- Validação acumula mensagens em `BusinessException`; mutações usam
  `@Transactional(rollbackFor = Exception.class)` do Spring`.
- Agregados são persistidos/excluídos explicitamente: agregado-primeiro no insert, dono-primeiro no
  delete.

## REST e segurança

- Sucesso retorna tipo direto; busca paginada é `POST /search`.
- Controller chama `setBodyToExceptionLog(dto, request)` em POST/PUT.
- Cada endpoint usa uma ação concreta de `AcaoSistema`; nunca acrescente `ADMIN` manualmente.
- Situação de fluxo nunca entra pelo corpo: cada transição é um endpoint de ação próprio.
- Erro do Keycloak é 502; autorização negada é 403.
- Auditoria é automática pelo Hibernate; domínio nunca chama serviço de auditoria.

## Referência e gate

- Espelhe o domínio `parceiro` e leia apenas as regras GEMS da camada tocada.
- Iteração: `.\mvnw.cmd -pl <modulo> -am test`.
- Fechamento: `.\mvnw.cmd clean install` uma vez.

Transição aprovada na constituição 2.0.0: SDK transversal substitui implementações locais com prova de equivalência. A anotação Jakarta, handlers, auditoria e autorização locais ainda existem e serão conformados na rodada 002.
Todo controller estende BaseController, inclusive GET; não estender exige motivo no Javadoc.

Regras GEMS vencem código consolidado; referência não autoriza desvio. Consulte a constituição 2.0.0 e a seção Adoção de Módulo da SDK GEMS.

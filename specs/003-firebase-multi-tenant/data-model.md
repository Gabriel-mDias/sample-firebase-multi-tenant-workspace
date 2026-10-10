# Data model — segurança e schemas

## Convenções e vigência

15 tabelas de autorização propostas abaixo, mais suporte técnico durável descrito separadamente. PK/FK UUID, timestamps Instant/TIMESTAMPTZ obtidos por Clock injetado. Não há FL_ATIVO concorrente. Vigente em t: DT_INICIO<=t e (DT_FIM IS NULL ou DT_FIM>t). CHECK DT_FIM IS NULL OR DT_FIM>=DT_INICIO: igualdade cria intervalo vazio, permitindo revogar no instante da criação. Grants têm início imediato, não se agendam. Encerrar vínculo não limpa DT_FIM; reatribuição cria nova linha/histórico.

| Tabela security | Chaves e conteúdo | Constraints/índices obrigatórios |
| :--- | :--- | :--- |
| TENANT | ID_TENANT; CD_ALIAS imutável; NM_TENANT; vigência | alias permanente único, normalização ASCII LocaleROOT; max56bytes; reservados negados; schema tenant_ derivado |
| PROVEDOR_IDENTIDADE | ID_PROVEDOR_IDENTIDADE; código; emissor/projeto configurado; vigência | código único; provider também deve estar vigente; 1projeto por DB; mismatch startup falha |
| USUARIO | ID_USUARIO; NM_USUARIO; CD_EMAIL normalizado; vigência; criação/alteração | elegibilidade técnica googlePending/passwordProvisioning/bound explícita; não equivale a ativo; lookup Google exige exatamente um pending elegível |
| USUARIO_IDENTIDADE | ID_USUARIO_IDENTIDADE; ID_USUARIO; ID_PROVEDOR_IDENTIDADE; CD_EMISSOR; CD_SUJEITO_EXTERNO(UID); CD_EMAIL_IDENTIDADE; vigência | UNIQUE(provider,issuer,uid) permanente; UNIQUE(user,provider,issuer) parcial DT_FIM IS NULL; dono/UID/emissor imutáveis; e-mail informativo |
| USUARIO_TENANT | ID_USUARIO_TENANT; ID_USUARIO; ID_TENANT; vigência | UNIQUE(user,tenant) parcial aberto; UNIQUE(ID_TENANT,ID_USUARIO_TENANT) para FKcomposta; fim nunca reabre |
| ROLE_SEGURANCA | ID_ROLE; CD_ACAO; CD_ESCOPO GLOBAL/TENANT; delegabilidade; vigência | ação permanente única; CHECK escopo; UNIQUE(ID_ROLE,CD_ESCOPO) para integridade; deriva catálogo e drift não autoriza |
| PERFIL | ID_PERFIL; ID_TENANT; CD_PERFIL; nome/descrição; vigência | UNIQUE(tenant,code) parcial aberto; UNIQUE(ID_TENANT,ID_PERFIL) |
| PERFIL_ROLE | ID_PERFIL_ROLE; ID_TENANT; ID_PERFIL; ID_ROLE; CD_ESCOPO; vigência | FK(tenant,perfil), FK(role,scope), CHECK scope=TENANT; unique aberto(perfil,role) |
| USUARIO_TENANT_PERFIL | ID; ID_TENANT; ID_USUARIO_TENANT; ID_PERFIL; vigência | FKscompostas tenant em ambos os lados; unique aberto(membership,perfil) |
| USUARIO_TENANT_ROLE | ID; ID_TENANT; ID_USUARIO_TENANT; ID_ROLE; CD_ESCOPO; vigência | FKcomposta membership/tenant; role/scope+CHECK TENANT; unique aberto(membership,role) |
| USUARIO_ROLE_GLOBAL | ID; ID_USUARIO; ID_ROLE; CD_ESCOPO; vigência | FKrole/scope+CHECK GLOBAL; unique aberto(user,role) |
| GRUPO | ID_GRUPO; ID_TENANT; CD_GRUPO; nome/descrição; vigência | UNIQUE(tenant,code) parcial aberto; UNIQUE(ID_TENANT,ID_GRUPO) |
| GRUPO_USUARIO | ID; ID_TENANT; ID_GRUPO; ID_USUARIO_TENANT; vigência | FKscompostas tenant nos dois lados; unique aberto(group,membership) |
| GRUPO_PERFIL | ID; ID_TENANT; ID_GRUPO; ID_PERFIL; vigência | FKscompostas tenant nos dois lados; unique aberto(group,profile) |
| GRUPO_ROLE | ID; ID_TENANT; ID_GRUPO; ID_ROLE; CD_ESCOPO; vigência | FKgroup/tenant; role/scope+CHECK TENANT; unique aberto(group,role) |

Todas as tabelas com vigência têm CHECK de intervalo. Constraints são provadas por INSERT/UPDATE diretos no banco, não somente via services. Partial uniques não substituem rowlock para comandos concorrentes. Scope helper/coluna redundante é controlado por compositeFK/CHECK, nunca campo livre confiado ao chamador.

## Autorização efetiva

Um instante t de Clock por snapshot/request. CTEs vigentes para usuário/provider/identidade; tenant/membership obrigatório quando selecionado; grants diretos global ou tenant; usuário→perfil→perfil_role; usuário→grupo→grupo_role e usuário→grupo→grupo_perfil→perfil→perfil_role. Todo elo, incluindo role, precisa vigência. UNION deduplica; final intersecciona catálogo de código e scope. Grupo/perfil suspenso interrompe todas as heranças. Drift gera evento sem delete de histórico e nunca authority; mudança de scope no catálogo falha startup. Índices cobrindo FK+DT_INICIO/DT_FIM são medidos com EXPLAIN; sem cache.

## Identidade e lifecycle durável

Google: procurar (provider,issuer,uid) primeiro, incluindo histórico. Se existe encerrada, negar; não fallback. Somente UID jamais ligado pode tentar e-mail, token válido sign_in_provider=google.com e email_verified=true, exatamente um USUARIO googlePending elegível, provider/usuário vigentes. Rowlock em usuário + uniques; CAS para bound na mesma transação. UID de outro usuário/projeto não é apropriável.

Password: transação cria usuário passwordProvisioning, UID planejado e comando CREATE por UID; Admin cria aquele UID. ACK+CAS latestversion registra bound/identidade conforme resultado verificado. Não assume conta existente por e-mail. Primeiro acesso/reset pelo cliente Firebase envia e-mail nativo. Admin generate link não envia; links não são persistidos nem logados.

Support técnico: security.EVENTO_SEGURANCA append-only e outbox/COMANDO_IDENTIDADE com UUID, issuer/project, UID, tipo CREATE/DISABLE/REVOKE/ENABLE, versão monotônica, idempotency-key, estado pending/retry/acked/obsolete, tentativas, timestamps/resultado sanitizado; nenhum password/token/key/action link. Mutação SQL+evento+outbox atômicos sob manager JDBC qualificado separado de JPA. Worker reclama comando duravelmente, reprocessa depois de restart e compara latestversion antes/após chamada externa.

Desativação global: commit de DT_FIM local antes de disable/revoke Firebase; falha externa conserva bloqueio. Reativação global explicitamente autorizada solicita ENABLE; somente ACK da versão mais recente e CAS podem tornar usuário vigente, sem reabrir identidades/memberships/grants encerrados. Enable antigo nunca derrota disable novo. Encerrar membership só fecha cadeia daquele tenant. Não se guarda segredo sequer como payload temporário no SQL.

## Schemas e provisionamento

security: JDBC compartilhado explicitamente qualificado; administracao: metadados globais com TenantScope.global deliberado; auditoria: trilha global separada; modelo: schema técnico de validação; tenant_alpha/tenant_beta: negócio e auditoria empresarial sem schema fixo de anotação.

Metadados de provisionamento por tenant: estado PENDING/FAILED/READY, revisão aplicada, erro sanitizado, timestamps e alias/schema imutáveis. Inventário `TenantSchemaSource.expectedSchemas` retorna NOMES DE SCHEMA READY inclusive suspensos; autorização de tráfego exige ainda tenant/usuário/membership vigentes e minimumAppliedMigration. Suspendidos continuam migrando para permitir reativação.

CLI/job em lote fora HTTP usa advisory lock por alias com conexão de sessão pinada, unlock em finally; falha de unlock invalida/evict conexão do pool. Cria schema validado, executa Liquibase, verifica revisão, só depois READY+appliedrevision. Falha→FAILED; retry idempotente; nenhum DDL oculto no filtro/request. Migration modelo acontece ANTES de EntityManagerFactory com ddlvalidate.

DemoRegistro por tenant: UUID, NM_REGISTRO, DS_REGISTRO e VL_RESTRITO, EntidadeAuditavel/softdelete empresarial conforme regras GEMS; leitura/gravação do campo restrito exige ação de service. Fixtures Alpha/Beta compartilham PK para provar isolamento. Nenhuma tabela de negócio pesada adicional faz parte do molde.

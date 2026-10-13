# Backend — Firebase multi-tenant

A constituição 3.1.1 é autoridade. A spec aprovada é contrato de comportamento; código/testes
mostram o estado real. Regras GEMS vencem código consolidado sob constituição, segurança e API
publicada. Nenhum waiver/exceção ou baseline é criado pelo agente para passar gate. Referências
anteriores servem à forma de código. Adoção SDK exige publicação, alcance medido, garantia nomeada,
equivalência/mutação no consumidor e remoção da classe duplicada na mesma alteração.

Reator app-model/ app-security/ app-events/ app-core/ app-deploy com grafo unidirecional.
Model camada-primeiro; core domínio-primeiro. App-events só POJOs; listeners com regra em core.
Semcascade; evento aciona outro domínio, repository pode consultar. Toda config/migration/resource
de aplicação só app-deploy. SDK fornece changelog reutilizável e JDBC security explicitamente opt-in.

UUID/Clock/Instant; Liquibase/ddlvalidate; JPA negócio EntidadeAuditavel/softdelete, LAZY/entitygraph.
Services findById>search>delete>insert>save>validate; validação acumuladaBusinessException;
Spring @Transactional(rollbackFor=Exception.class). RepositoryJpa+BaseCustom/trincaHQL/parâmetrosnomeados,
busca POST/search/projeçãoDTO. ControllerBaseController inclusiveGET; setBodyToExceptionLog emPOST/PUT,
mas nunca registrar senha/token/action link. Status201/204/200 e ações concretas do catálogo gerado.

Firebase Google+e-mail/senha; SQL security vigente/catalog/scope, sem cache/claims de roles.
Tenant admin só membership/grants próprios com subset do ator∩catálogo delegável; central cria
identidade/tenants/primeiro admin. UID+issuer+provider e ownership imutáveis; Google somente pending elegível
com e-mail verificado e rowlock. Lifecycle outbox durável/CAS de versão/bloqueio local antes da chamada remota.

Authentication implementsAuthorizationContextAware/JwtAuthorizationContext real; scannerassertProtected
registrado e TenantAuthorizationInterceptor obrigatório. Me éGlobalEndpoint+CONSULTAR_PROPRIA_CONTA;
context éTenantEndpoint+CONSULTAR_PROPRIO_CONTEXTO_TENANT. SemclassificaçãoPublic de conveniência.
Filtro stateless uma só registration. Token ausente/inválido/revogado/disabled401, negação local403,
indisponibilidade do provider503falha fechada; check-revoked=true real. Authports semJpaDep/DS; manager JDBC qualificado.

Schema negócio tenant_ validado; header→membership→Authenticationalias→TenantScope. Limparbegin/finally
porque close restaura anterior. OSIV false/nenhum EM antes de auth/nenhuma troca de scope dentro de EM/tx; globalJPATenantScope.global
deliberado; asyncquando usado security/tenant/actor. READY e revisão mínima antes tráfego;
expectedSchemas fornece nomesREADY inclui suspensos; provisionamentoCLI/job fora de HTTP/locksconexão pinada.
Auditoria empresarial por tenant semschemafix; global/security separado. Tests mesma PK/constraints/races/restart da outbox.

Gates de fechamento: Java SDK clean verify/gate oficial/publicação/consumer M2 isolado;
Angular core/adapter publicados/consumer npm isolado; sample GEMS strict+conformance efetiva de código
novo E preservado baseline zero, backend mvnw.cmd clean install, frontend npm ci/test/build/prettier check,
E2E PostgreSQL descartável+Firebase real de testes (ADC/FIREBASE_TEST_SERVICE_ACCOUNT_JSON; PR de fork pula com aviso; Emu só demo/offline)/constraints/races/outbox/thread/mesma PK. Planejado é
NOT_RUN até execução observada; installlocal não comprova publicação.

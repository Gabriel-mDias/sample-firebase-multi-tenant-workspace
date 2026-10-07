# Pesquisa técnica — Conformidade GEMS

**Data**: 2026-09-14
**Fontes**: Java GEMS SDK 3.1.0 (`137f18f`), Angular GEMS SDK 2.1.0 (`e452ba0`), ambas publicadas.

## R-01 — Versões e alcance

Declarar a versão Java uma vez no POM pai, medir `dependency:tree` antes/depois e consumir cada módulo
somente onde sua API é importada. No frontend, adotar 2.1.0 com overrides do release. A atualização
remove invocações/stale guidance de `--legacy-peer-deps` nos arquivos operacionais autorizados; menções
normativas imutáveis são reportadas, não alteradas silenciosamente.

## R-02 — Conversão estrita

Os seis alvos reais são `LocalidadeService`, `ContatoService`, `EnderecoService`, `PessoaService`,
`ParceiroService` e `SolicitacaoService`. Cada um recebe mapper nominal com `GemsMappingConfig`.
O compilador mantém Lombok e acrescenta `mapstruct-processor`/`lombok-mapstruct-binding`.

## R-03 — Rollback provado na fronteira real

Mockito prova colaboração, não transação. Parceiro e solicitação terão testes Spring integrados no
`app-deploy`, chamando bean proxied contra PostgreSQL real. Cada caso lança exceção checked depois da
primeira escrita e afirma ausência de dado, evento/efeito persistido e trilha. O método de teste não
será `@Transactional`; limpeza ocorre antes/depois, de modo que retirar `rollbackFor` torna o teste
vermelho em vez de produzir falso rollback pelo framework de teste.

## R-04 — Auditoria 3.1.0: garantias presentes e ausentes

O writer publicado usa a conexão do chamador, grava `AUDIT_OPERATION`/`AUDIT_CHANGE`, apaga valores
sigilosos e registra `NM_ACTOR`/`CD_ACTOR_TYPE`. Ele não grava `ID_AUTOR` nem `CD_CORRELACAO`, hoje
persistidos pela implementação local. Nome/login não é identidade estável: rename, remoção e reuso
podem perder ou misturar o filtro histórico. Resolver id atual para login não reconstitui a garantia.

**Decisão D-P01 — aprovada em 2026-09-14**: preservar e bloquear o bloco até release publicada que
carregue identidade estável e correlação. A alternativa de perda semântica foi recusada.

Quando habilitado, o bloco de adoção é único e indivisível: testes primeiro; migration/read adapter;
provider; configuração; ativação; remoção de `AuditoriaGravador`, `AuditoriaInterceptor`,
`RegistroPendente`, anotações e testes órfãos; gate completo verde. Não existe checkpoint em que writer
antigo veja tabela nova ou writer novo veja tabela ausente.

## R-05 — Keycloak 3.1.0: superfície insuficiente

`KeycloakAdminGateway` 3.1.0 publica `findUserByEmail`, `createUser`, organizações, grupos, membros,
lista de membros e associação de role existente a grupo. A implementação atual também lê/atualiza,
habilita/desabilita/exclui usuário, consulta grupos para compensação, cria realm roles e compõe perfis.
Essas operações não existem no gateway aferido.

**Decisão D-P02 — aprovada em 2026-09-14**: preservar e bloquear a remoção das quatro classes locais
até release publicada com a superfície completa. Criar adapter local continua sendo a duplicata
proibida; a alternativa de perda de garantias foi recusada.

## R-06 — Realm declarativo é alternativa, não pressuposto

Local, staging e produção já montam o mesmo `environments/shared/keycloak/app-realm.yaml` em
`keycloak-config-cli`; o one-shot espera Keycloak saudável, usa `IMPORT_CACHE_ENABLED=false` e o backend
depende da conclusão onde necessário. Isso atualiza ambiente existente, ao contrário de import apenas
no primeiro boot.

Se D-P02 escolher a alternativa declarativa, D-P03 precisa ampliar o escopo de escrita. Somente os
blocos derivados de ações/compostas podem ser gerados; clients, protocol mappers, users, groups,
defaults e client roles permanecem escritos/preservados. Um gate compara enum + perfis + seções
derivadas e também prova que o restante do realm não foi substituído.

## R-07 — Autorização

`AcaoSistema` implementa `AuthorizationAction`; as 21 ações atuais são globais no sample single tenant.
`AuthorizationCatalog.of` deriva o catálogo; `EndpointAuthorizationScan` cobre mappings publicados; o
frontend recebe JSON produzido/verificado pelo gerador SDK. A anotação `PublicEndpoint` local e seu
scanner saem junto dos órfãos quando as anotações SDK assumirem todos os usos.

## R-08 — Quatro handlers e órfãos

Saem atomicamente: `ProvedorExternoExceptionHandler`, `AccessDeniedExceptionHandler`,
`IdentityProviderExceptionHandler` e `RequisicaoInvalidaExceptionHandler`. `ProvedorExternoException`
passa a estender `ExternalServiceException`. Depois de migrar usos, remover também tipos sem consumidor,
incluindo `CreateUserDTO` se o gateway publicado final o substituir e `PublicRoute`/`PublicEndpoint`
quando a configuração SDK eliminar seus únicos usos. `SecurityRoles` e `PasswordUtil` permanecem se
a busca nominal provar consumidores reais.

## R-09 — Baseline final

Java termina em zero: 314 Checkstyle, quatro repositories, dois controllers, seis ordens e 177 nomes
de teste são corrigidos; as 22 supressões de dívida, os dois arquivos baseline e `ratchetFrom=411156d`
saem. Frontend termina no mínimo medido após correções. Cada residual traz identidade, arquivo, regra,
motivo e alternativa; D-P04 é aceite humano e não será marcado pelo agente.

## R-10 — Angular e flag de peers

2.1.0 expõe `provideGemsKeycloak`, field-error, password e navigation. A adoção exige testes locais de
equivalência. O inventário atual encontrou comandos/stale guidance ativos em `frontend/Dockerfile`,
`.github/workflows/ci.yml`, `README.md` e `.github/dependabot.yml`; os três últimos estão fora do escopo
de implementação original e exigem expansão humana. Referências em constituição/regras que explicam a
transição são imutáveis neste lote.

## R-11 — Revisão externa

O escopo é elegível para Opus por arquitetura, migração, autenticação e ambiente. Não foi chamado:
o pacote proíbe executores adicionais e não concede autorização específica.

## R-12 — Rodada produtora única da Java GEMS SDK

A fonte publicada 3.1.0 confirma duas lacunas exatas. Em `gems-auditing`, `AuditActor` contém apenas
nome/tipo e `TransactionalAuditWriter` grava `NM_ACTOR`/`CD_ACTOR_TYPE`, sem identidade estável nem
correlação. Em `gems-keycloak-admin`, o gateway cobre criação e busca por e-mail, organizações,
grupos/membros e associação de role já existente, mas não cobre snapshot/alteração/enable/disable/
delete de usuário, consulta de seus grupos, compensação da senha, criação de role nem composição.

A evolução será uma rodada SDD produtora no repositório `Java-GEMS-SDK`, com dois blocos
independentes e uma release conjunta:

- **SDK-AUD**: novos providers/tipos aditivos para identidade estável e correlação; os providers
  antigos e defaults continuam válidos; o writer acrescenta os dois valores sem derivar identidade
  do nome;
- **SDK-KC**: subinterfaces/tipos públicos aditivos para o ciclo completo, snapshots e roles; o
  `KeycloakAdminGateway` 3.1.0 permanece compilável e o cliente REST implementa a superfície nova;
- **SDK-REL**: compatibilidade, reator, revisão, BOM, release notes, PR, merge, workflow de publicação
  e resolução limpa no GitHub Packages.

SemVer: a forma aditiva torna a próxima release **3.2.0 MINOR**. Alterar/remover assinatura pública,
inclusive acrescentar método abstrato obrigatório ao gateway existente, é quebra e interrompe o
bloco para novo portão MAJOR; não se ajusta o número depois em silêncio.

Os módulos de produção afetados são `gems-auditing` e `gems-keycloak-admin`.
`gems-security-authorization` recebe apenas prova de integração entre catálogo e composição de roles,
sem mudança pública prevista; `gems-bom`, POM pai, guias e release notes mudam no bloco de release.

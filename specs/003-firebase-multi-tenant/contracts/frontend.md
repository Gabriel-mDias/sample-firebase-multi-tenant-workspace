# Contrato frontend e publicação Angular

Core `@gabriel-mdias/angular-gems-sdk`3.0.0 MAJOR remove entrypoint/auth do provedor anterior para adapter `@gabriel-mdias/angular-gems-keycloak-auth`1.0.0, preservando implementação2.3.4 no adapter. UI/forms/exports core permanecem; metadata e transitive dependencies do core não incluem adapter. Ambos publicados e smoke npm isolado antes de consumo sample. Base oficial SHA8a571633e18fff2f537e1fa203b6ef7cae9a2a35; clone antigo2.1.0 permanece histórico sem modificação.

O sample usa fachada LOCAL Firebase: signInWithEmailAndPassword, signInWithGoogle, signOut, authenticatedUser, idToken, me, tenants,currentTenant,effectiveActions,isAuthorized. Components não importam firebase/auth nem HttpClient; stores/GemsBaseStore/fachada concentram chamadas. Google usa provider oficial; e-mail/senha e primeiro reset usam APIs nativas Firebase para envio, sem SMTP/GmailAPI. FirebaseWeb config não é segredo Admin, mas domains/project/isolamento precisam validação.

Guard de PRODUTO local com `data.roles` tipado, `roleMode:any|all` e `authorizationScope:global|tenant`. Roles ausentes/vazias negam. Nenhum AuthorizationRequirement/data.authorization/providerFirebase inventado como export da SDK publicada. Scope local exige spec/contrato/testes; ações retornadas do backend servem UX e não autorizam backend. Rotas/menu/botões/campo usam ações concretas geradas pelo catálogo oficial.

## Interceptor e geração monotônica

Capturar ANTES de await de getIdToken: generation monotônica, UID, sessão, alias, boundary de destino e payload vinculado ao contexto de origem. Bearer somente origem exata de apiBaseUrl E boundary de path (não prefixo /api-evil); renovação por getIdToken oficial. Após await, revalidar geração/UID/alias/sessão antes de enviar; headers são snapshot capturado, não alias relido. Context requests para destino têm geração explícita; nenhuma action empresarial liberada durante loading.

Troca de tenant/logout/UID cancela pending requests, incrementa geração e limpa stores/menus/forms/actions antes de buscar novo contexto. Contexto destino validado libera negócio. Falha deixa tenant actions vazias/sem tenant operacional, sem restaurar Alpha implicitamente. Responses de geração antiga nunca mudam UI nem navegam; erro antigo não derruba sessão nova. Payload Alpha jamais é enviado a Beta.

## Testes determinísticos

Promises deferred controlam token e responses: Alpha→Beta com mesmas PK; token await Alpha antes de switch; response Alpha depois de Beta pronto; logout enquanto token pendente; outro UID entra; switch failure; session expiration; bearer terceiros/origin parecida/path boundary; roles vazias; roleMode all/any; scopeglobal vs tenant; menu/botões/actions/catalog. Assert chamadas de rede e payload/context de origem, não só render. Guard coverage percorre loadChildren e detecta metadata ausente.

## Demo/UI GEMS

DemoRegistro tem list/form/view autocontidos, .ts/.html/.css separados, standalone/inject/signals/input/output/@if/@for, theme provider/var(--gems-*), FontAwesome6Solid, classes globais de botão, feedback SDK e validação GEMS. CRUD/DTO/service/repository/controller preservam forma de código; domínios parceiro/solicitacao são referências anteriores, não escopo pesado. Cleanup remove feature, nav, catalogue/demo fixtures/testes por manifesto e mantém auth/tenants/security fundamentos.

## Login próprio G&Ms — US4 / FR-015 / SC-005

Feature `frontend/src/app/features/login/login.routes.ts` lazy via `app.routes.ts`, rota pública `/login` dentro do módulo de autenticação de apresentação, sem guard que redirecione novamente ao login. Componentes standalone e `.ts/.html/.css` separados; formulário usa `gems-input-password` e `gems-field-error` publicados. Login não valida senha existente por política de cadastro nem exige confirmação de senha. Componentes chamam somente fachada local; Firebase/HTTP continuam encapsulados em core/services/stores apropriados.

Google, e-mail/senha, recuperação com envio nativo Firebase e logout passam pela fachada T023. Não oferecer criar conta; pré-provisionamento/elegibilidade e autoridade SQL são preservados. Reset retorna confirmação genérica, sem revelar se existe conta; nenhum token/senha/action link nos logs. Textos em português, labels associados, autocomplete adequado, foco visível, erros anunciados e ordem de teclado coerente. Loading impede submits duplicados e ações incompatíveis; cancelamento de popup, indisponibilidade e credenciais inválidas têm mensagens sanitizadas.

Somente path interno permitido pode compor retorno, validado por allowlist/rotas autorizadas; rejeitar URLs externas, protocol-relative e caminhos que causem loops. Sessão Firebase isolada não basta: sessão ativa ignora o formulário apenas após `me` e contexto necessário válidos. Após entrada, preservar seleção validada ou solicitar tenant entre vínculos autorizados; negócio permanece bloqueado até contexto T024 válido. Negar contas não elegíveis sem enumerar vínculos. Logout, novo UID, token/response antigos e falha de contexto obedecem generation já contratada, sem navegação tardia.

Aplicar wordmark oficial/creme/preto/dourado e tipografia de gems.tec.br por tema central `provideGemsTheme`, tokens nos componentes, sem overrides/hex locais. Assets públicos da marca serão incorporados na implementação, não nesta tarefa. Layout sugerido não é mockup aprovado. Testar 320/768/1440 px sem overflow, contraste 4,5:1 normal/3:1 grande, teclado, labels e anúncios. T033 cobre UI antes de T034; T029 integra E2E, T031 gates e T032 homologação real. Referência medida e contraste: [research.md](../research.md#referência-de-marca-e-decisão-de-acesso--2026-10-08).

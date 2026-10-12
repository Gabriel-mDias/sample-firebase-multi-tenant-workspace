# Checklist de conteúdo — ux

Esta checklist avalia completude documental; não atesta implementação. Revisão independente pendente.

- [ ] guard localroles tipadas, vazio nega/authorizationScope
- [ ] loadingbloqueiaatécontexto destino válido
- [ ] swap/logoutUIDlimpaformsstoresmenusactions
- [ ] response antiganãonaveganãomuta/falha no switchsemimplicitrollback
- [ ] UI/formsfeedbacktheme GEMS preservados

- [ ] US4/FR-015/SC-005: marca G&Ms observada distinguida de layout sugerido/mockup não aprovado
- [ ] Tema central provideGemsTheme/tokens e tipografia por mecanismo publicado, sem hex/overrides locais
- [ ] Login 320/768/1440 sem overflow; teclado/foco/labels/erros anunciados e contraste 4,5:1 normal/3:1 grande
- [ ] Google/senha/reset nativo por fachada; senha existente sem política de cadastro; sem autocadastro
- [ ] Loading bloqueia submit duplicado; erro/recuperação genéricos em português; retorno só interno permitido
- [ ] Sessão ativa e retorno aguardam me/context válidos; seletor/negação/logout/races preservam isolamento

Cobertura pendente T033/T034/T029/T031/T032; [contrato](../contracts/frontend.md), [research](../research.md).

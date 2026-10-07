# Contrato frontend — Conformidade GEMS

## Rotas e autorização

Parceiro e solicitação expõem, cada um, `list`, `form`, `form/:id`, `view/:id`. Guard, menu e comandos
usam `isAuthorized(action)` com catálogo derivado; ausência ou ação inválida falha fechada.

## SDK e forms

`provideGemsKeycloak` conserva check-sso/interceptor DI. Field-error, password e navigation substituem
equivalentes locais somente depois de testes de inputs, outputs, validação, acessibilidade e estado.
Domínio não é transportado como dado efêmero de navegação.

## Instalação

O gate futuro exige `npm ci` sem flag em execução local, Docker e CI. Documentação operacional
autorizada também deve deixar de instruir a flag. Arquivos fora do escopo original são expansão humana,
não edição implícita.

## Residual

O gate mede cada ocorrência de visual, testes, signals e organização. O objetivo é zero onde a regra é
aplicável. Qualquer mínimo não zero é listado nominalmente e permanece pendente até aceite humano; a
contagem agregada não autoriza nada.

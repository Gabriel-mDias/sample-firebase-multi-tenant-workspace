# Quickstart de validação — Conformidade GEMS

Executar somente depois de D-P01–D-P03 resolvidas, implementação concluída e antes de D-P04 final.

## Auditoria

1. Aplicar upgrade sobre schema antigo populado; confirmar parada diante de dado incompatível e
   `ddl-auto: validate` verde no caso válido.
2. Confirmar commit com identidade estável/correlação, rollback sem dado/efeito/trilha, sigilo e ator
   técnico.
3. Alterar/remover usuário e reutilizar login; confirmar filtro histórico sem mistura.
4. Exercitar histórico agregado e busca com data final inclusiva.

## Keycloak e ambiente

1. Subir apenas com `docker compose up -d`; confirmar one-shot após health, cache false e backend após
   conclusão em local/staging/produção.
2. Validar 21 ações, compostas, grupos e idempotência em realm já existente.
3. Criar, alterar, suspender, reativar e mover usuário; induzir falhas para provar compensações/502.
4. Se D-P03 for escolhida, comparar seções não derivadas do realm byte/semanticamente antes/depois.

## Frontend

1. Rodar `npm ci` sem flag em host, Dockerfile e CI.
2. Exercitar oito rotas com ação presente/ausente, create/edit/view, erros acessíveis e navegação.
3. Gerar inventário residual com identidade nominal e submeter D-P04.

## Gates finais

```powershell
cd backend
.\mvnw.cmd clean install

cd ..\frontend
npm test
npm run build
npx prettier --check .
```

O backend fecha somente com zero dívida Java, `suppressions.xml` sem dívida, baselines sem tolerância e
sem `ratchetFrom`. O frontend fecha somente depois da decisão humana sobre cada residual.

## Execução C14 — 2026-09-23

- **Auditoria:** a base local preexistente tinha 32 changesets; o primeiro boot aplicou
  `migrate-auditoria-to-gems-sdk`, chegou a 33, manteve `ddl-auto: validate` e iniciou com sucesso. O
  segundo boot executou zero changesets. O gate completo C13 cobre commit com identidade/correlação,
  rollback sem dado/trilha, sigilo, ator técnico, id estável, histórico agregado e data final inclusiva.
- **Keycloak/ambiente:** os compose files local, staging e production passaram `docker compose config
  --quiet`. `docker compose up -d` local deixou Postgres, Keycloak e LocalStack saudáveis; o one-shot
  encerrou duas vezes com exit 0 e cache false. No primeiro boot do backend, o bootstrap confirmou 21
  ações, 21 filhos e 3/3 grupos; no segundo, 21 ações, zero filhos novos e 3/3 grupos. Os cenários
  de lifecycle, compensação e 502 estão no gate completo C13. D-P03 não se aplica. Ao fim, somente os
  contêineres e a rede criados nesta execução foram removidos; volumes foram preservados.
- **Frontend:** `npm ci` passou sem flag; o gate completo passou 26 arquivos/123 testes, build e
  Prettier. O gate focal de rotas resolveu os `loadChildren`, afirmou as oito rotas guardadas e passou
  9/9; a suíte completa cobre ação presente/ausente, create/edit/view, erros e navegação.
- **Inventário D-P04:** a desativação temporária do baseline revelou exatamente 63 violações e foi
  revertida imediatamente; o verificador restaurado voltou a zero violações. O inventário nominal
  está em `estado-lote-C14.md` e aguarda decisão humana.

## Fechamento C14-R1 — 2026-09-23

D-P04 foi devolvida para correção. As 63 ocorrências foram eliminadas e o baseline foi esvaziado.
O verificador passou com 13 regras/126 arquivos e zero violações; `npm ci`, 27 arquivos/125
testes, build, Prettier e `git diff --check` ficaram verdes.

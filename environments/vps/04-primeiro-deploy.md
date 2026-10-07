# 04 — Primeiro deploy

Pré-requisitos: capítulos 01 a 03 concluídos, e os três subdomínios **já resolvendo** para o IP da
VPS.

---

## 4.1 Construir as imagens

Faça um push na `main` (ou dispare o workflow pela interface do GitHub). O
`build-images.yml` constrói backend e frontend em paralelo e publica no GHCR.

Acompanhe em **Actions**. Se ele falhar na resolução de dependências, é o registro privado — volte ao
item 3.3.

Ao terminar, confirme em **Packages** que existem `app-backend` e `app-frontend`.

## 4.2 Subir

Pelo workflow manual (**Actions → Deploy → Run workflow**), ou direto na VPS:

```bash
cd ~/app-project/environments/production
docker compose pull
docker compose up -d
```

> O deploy **nunca** é automático a partir de um merge. Publicar em produção é decisão, não
> consequência de aprovar um PR.

## 4.3 Acompanhar a ordem de subida

A ordem é imposta por prontidão real, não por ordem de declaração:

```
postgres (saudável) → keycloak (saudável) → keycloak-config (concluído) → backend
```

```bash
docker compose ps
docker compose logs -f app-keycloak-config
```

O Keycloak leva cerca de **80 segundos** no primeiro boot — ele migra o próprio schema antes de
atender. É esperado ver o `keycloak-config` aguardando nesse período.

**No log do `app-keycloak-config`, você quer ver:**

```
Server info available. Using version: 26.0.8
keycloak-config-cli ran in 00:0X.XXX
```

Se ele falhar, o backend **não sobe** — e isso é intencional: um backend no ar com identidade meio
configurada criaria usuários sem permissão nenhuma.

**No log do backend**, confirme a linha do bootstrap dos grupos:

```
Perfis de atuação sincronizados no Keycloak: 7/7 grupos garantidos com a role correspondente.
```

Se vier `WARN` com menos grupos que perfis, **pare**: usuários dos perfis faltantes conseguiriam logar sem
receber permissão alguma — falha silenciosa, que aparece só como "acesso negado" muito depois.

## 4.4 Certificado

```bash
docker compose logs caddy | grep -i certificate
curl -sSI https://app.seudominio.com.br | head -1
```

Esperado: `HTTP/2 200`, sem aviso de certificado. Se a emissão falhou, a causa quase sempre é DNS —
volte ao capítulo 02 e **não** fique repetindo o `up`: cada tentativa conta no limite semanal do
Let's Encrypt.

## 4.5 Verificação pós-deploy

Percorra os cinco. Cada um cobre um defeito que já aconteceu neste projeto.

**1. Login ponta a ponta.** Acesse `https://app.seudominio.com.br`, entre com `APP_ADMIN_EMAIL` e a
senha do `.env`. Deve abrir sem exigir troca de senha.

**2. Emissor público no token.** Na aba Network do navegador, no token, o campo `iss` precisa ser
`https://auth.seudominio.com.br/realms/app-realm` — **não** um endereço interno. Se vier interno, a
API rejeita toda requisição com 401 e a causa é o `KC_HOSTNAME`.

**3. Os 7 grupos.** No console do Keycloak (`https://auth.seudominio.com.br`), realm `app-realm` →
Groups. Os grupos de perfil, e `ADMIN` mapeada **apenas** em `ADMINISTRADOR`.

**4. Superfície de rede.** De fora da máquina:

```bash
nmap -Pn SEU_IP
docker compose ps --format '{{.Service}} {{.Ports}}'
```

Apenas 22, 80 e 443 abertas; e apenas o `caddy` com porta publicada.

**5. Falha do provedor de identidade responde 502, não 401.** Cadastre um usuário com uma
senha fraca de propósito. A resposta deve ser **502** com a mensagem da causa. Um **401** aqui seria
lido como sessão expirada — o operador seria mandado para o login no meio do wizard, e a causa real
ficaria escondida.

**6. A superfície de diagnóstico está fechada.** Quatro verificações, todas **de fora** da
máquina. Cada uma cobre uma porta que costuma ficar aberta por omissão:

```bash
# O actuator: só o health responde. `metrics` enumerava TODOS os URIs da API, anonimamente.
curl -sS -o /dev/null -w '%{http_code}
' https://api.SEUDOMINIO/actuator/health    # 200
curl -sS -o /dev/null -w '%{http_code}
' https://api.SEUDOMINIO/actuator/metrics   # 401 (NUNCA 200)

# O contrato da API não é publicado.
curl -sS https://api.SEUDOMINIO/v3/api-docs | grep -c '"openapi"'                   # 0

# O console administrativo do Keycloak está fechado.
curl -sS -o /dev/null -w '%{http_code}
' https://auth.SEUDOMINIO/admin/            # 404
```

**7. Os cabeçalhos de segurança chegam ao navegador.**

```bash
curl -sSI https://app.SEUDOMINIO | grep -iE 'strict-transport|x-frame|nosniff|referrer|content-security'
```

Esperado: `Strict-Transport-Security`, `X-Frame-Options: DENY`, `X-Content-Type-Options: nosniff`,
`Referrer-Policy` e `Content-Security-Policy-Report-Only`.

> **A CSP começa em `Report-Only` de propósito** — ela relata violação no console do navegador sem
> bloquear nada. Antes de promovê-la a `Content-Security-Policy` bloqueante no `Caddyfile`, abra a
> aplicação, percorra login, wizard de cadastro, upload de arquivo, modal de chancela e a esteira de
> um serviço, e confirme que o console **não** acusou nenhuma violação. Promover sem observar é
> aplicar um bloqueio sem teste.

**8. O login continua funcionando depois de fechar o console.** Fechar `/admin` no Caddy **não** deve
afetar o fluxo de autenticação — o backend fala com a Admin API pelo endereço interno. Se o login
quebrou, o padrão do `@console` no `Caddyfile` está capturando mais do que devia.

Para administrar o Keycloak a partir de agora, use um túnel:

```bash
ssh -L 8080:app-keycloak:8080 app@SEU_IP
# e então abra http://localhost:8080/admin no seu navegador
```

## 4.6 Se precisar recomeçar

```bash
docker compose down          # derruba os contêineres, PRESERVA os volumes
```

**Nunca** acrescente `-v`. O volume guarda o banco da aplicação **e** o do Keycloak — e as senhas dos
usuários existem apenas no segundo, sem forma de recriá-las.

---

**Próximo:** [05 — Backup e restauração](./05-backup-e-restore.md)

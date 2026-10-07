# 08 — Ambiente de staging

Segunda stack completa na **mesma VPS** da produção, servindo três subdomínios `staging.*`. É onde a
entrega é homologada antes de ir para produção.

Entregue pela rodada `specs/022-ambiente-de-staging`.

---

## 8.1 O que existe, e o que é compartilhado

| Recurso | Produção | Staging |
| :--- | :--- | :--- |
| Projeto Compose | `app` | `app-staging` |
| Rede | `app_network` | `app_staging_network` |
| Volume do Postgres | `app_postgres_data` | `app_staging_postgres_data` |
| Servidor Postgres | `app-postgres` | `app-staging-postgres` |
| Keycloak | `app-keycloak` | `app-staging-keycloak` |
| Bucket S3 | `<prefixo>-acervo` | `<prefixo>-acervo-staging` |
| Senhas e client secret | próprios | **próprios, nenhum reaproveitado** |

**Três coisas são compartilhadas, e só três:**

1. **O Caddy.** Um único proxy na máquina serve os seis domínios.
2. **O arquivo de realm** — `shared/keycloak/app-realm.yaml`, montado nas duas stacks sem cópia. As
   instâncias de Keycloak são separadas; o arquivo é o mesmo. Um segundo arquivo divergiria em
   silêncio, e o ambiente que deveria provar a entrega passaria a provar outra coisa.
3. **As imagens.** Staging roda exatamente a imagem que vai para produção, repontada em runtime.

> ⚠️ **Derrubar a stack de produção derruba o acesso a staging.** A borda mora em `app`. É a
> consequência aceita de manter uma borda única na máquina, e ela é real: `docker compose -f
> environments/production/docker-compose.yml down` deixa os dois ambientes inacessíveis de fora,
> ainda que os contêineres de staging continuem de pé. Separar a borda numa terceira stack é a
> correção, e está registrada como fora de escopo até haver um terceiro ambiente.

---

## 8.2 Ordem de instalação

A rede é **externa às duas stacks** — nenhuma das duas a cria. Crie-a **uma vez**, antes de qualquer
`up`:

```bash
docker network create app_staging_network
```

Por que assim: se o compose de staging criasse a rede, um `docker compose down` em staging a levaria
junto, e o Caddy de produção a perderia na reinicialização seguinte — em silêncio, com produção no
ar. Fazer as duas stacks apenas consumirem uma rede de terceiro remove o acoplamento de ordem.

Depois:

```bash
cd "$VPS_REPO_PATH/environments/staging"
cp .env.example .env && chmod 600 .env
# preencha o .env — ver 8.3
docker compose up -d --wait
```

---

## 8.3 Os dois `.env`

### O que ACRESCENTAR ao `.env` de produção

Nada é removido. Estas seis linhas entram porque o Caddy e o backup vivem na stack de produção:

```dotenv
# --- Domínios de staging (o Caddy desta stack serve os seis) ---
STAGING_APP_DOMAIN=
STAGING_API_DOMAIN=
STAGING_AUTH_DOMAIN=

# --- Bucket de backup (cap. 03 §3.2) ---
# ⚠️ NUNCA reutilize aqui a credencial da aplicação. Ela é usada por código exposto à internet; se
# ela alcançar o bucket de backup, o vazamento que a separação existe para conter passa a alcançar
# as cópias.
AWS_BACKUP_BUCKET=
AWS_BACKUP_ACCESS_KEY=
AWS_BACKUP_SECRET_KEY=
```

### O `.env` de staging, inteiro

⚠️ **Nenhum segredo é compartilhado com produção.** Toda senha, credencial e client secret é próprio.
Staging é onde se erra de propósito; um segredo compartilhado transforma um erro de homologação em um
incidente de produção.

```dotenv
# --- Domínios ---
# Precisam resolver para o IP da VPS ANTES do primeiro `up` (ver 8.4).
APP_DOMAIN=
API_DOMAIN=
AUTH_DOMAIN=

# --- Postgres ---
# Os NOMES de banco repetem os de produção de propósito: os servidores são distintos, então não
# colidem, e repetir evita parametrizar o caminho do realm em três lugares. As SENHAS não repetem.
POSTGRES_SUPERUSER=
POSTGRES_SUPERUSER_PASSWORD=
APP_DB_NAME=app-db
APP_DB_USER=app-access
APP_DB_PASSWORD=
KEYCLOAK_DB_NAME=keycloak-db
KEYCLOAK_DB_USER=keycloak-access
KEYCLOAK_DB_PASSWORD=

# --- Keycloak ---
KEYCLOAK_ADMIN_USER=
KEYCLOAK_ADMIN_PASSWORD=
KEYCLOAK_BACKEND_CLIENT_SECRET=

# --- Administrador da aplicação ---
# ⚠️ A senha precisa passar na política do realm: 12 caracteres, minúscula, maiúscula, caractere
# especial, e não pode conter o nome de usuário nem o e-mail. Se não passar, o keycloak-config-cli
# ABORTA e o backend não sobe — ele depende de service_completed_successfully. O sintoma é um
# backend que nunca inicia, sem erro próprio.
APP_ADMIN_EMAIL=
APP_ADMIN_PASSWORD=

# --- AWS S3 ---
# Bucket e credencial PRÓPRIOS. Esta credencial não pode alcançar o bucket de produção nem o de
# backup — o gate G5 verifica exatamente isso.
AWS_REGION=us-east-1
AWS_ENDPOINT=https://s3.us-east-1.amazonaws.com
AWS_S3_BUCKET=
AWS_ACCESS_KEY=
AWS_SECRET_KEY=

# --- Imagens ---
# GHCR_OWNER é o mesmo de produção: staging roda AS MESMAS imagens, não uma variante.
# IMAGE_TAG é injetada pelo workflow a cada publicação; o default serve para subida manual.
GHCR_OWNER=
IMAGE_TAG=latest
```

**Ausentes de propósito**: `ACME_EMAIL` (o Caddy vive na stack de produção) e `AWS_BACKUP_*` (staging
não é copiado — dado sintético é recriável).

Conferência antes de subir, do capítulo 03 §3.4:

```bash
grep -c '=$' .env    # precisa dar 0
ls -l .env           # precisa mostrar -rw------- (600)
```

---

## 8.4 DNS: seis registros, não três

| Papel | Produção | Staging |
| :--- | :--- | :--- |
| Interface | `APP_DOMAIN` | `STAGING_APP_DOMAIN` |
| API | `API_DOMAIN` | `STAGING_API_DOMAIN` |
| Identidade | `AUTH_DOMAIN` | `STAGING_AUTH_DOMAIN` |

Todos os seis apontam para o **mesmo IP** — é a mesma máquina. Confirme que resolvem **antes** do
primeiro `up`: o desafio HTTP-01 do Let's Encrypt falha com DNS errado, e o limite de emissões é
semanal. Errar aqui custa dias de espera, não um retry. Ver capítulo 02.

---

## 8.5 O que NUNCA pode entrar em staging

**Dado pessoal real.** Nem carga, nem importação, nem restore de um dump de produção "só para
testar". É requisito desta decisão.

A conferência é **recorrente**, não de entrega única — nada impede alguém fazer isso depois:

```bash
docker compose -f environments/staging/docker-compose.yml exec -T app-staging-postgres \
  psql -U "$APP_DB_USER" -d app-db -c "SELECT COUNT(*) FROM PESSOA;"
```

O número precisa corresponder ao que foi digitado à mão durante a homologação — nunca a um volume que
só uma carga explicaria.

**A exceção legítima**: restaurar em staging um dump gerado pelo `pg-backup.sh` para provar que o
arquivo é íntegro e legível. Isso prova o **mecanismo** de backup. Não é o ensaio de restore de
produção, e o capítulo 05 não pode chamar uma coisa de outra. Feito o teste, o banco de staging é
recriado.

---

## 8.6 Publicação

| Ambiente | Gatilho | Versão |
| :--- | :--- | :--- |
| Staging | automático, a cada `main` publicado | `sha-<curto>` do commit |
| Produção | `workflow_dispatch`, deliberado | tag escolhida no disparo |

Staging publica sozinho porque não é ambiente externo no sentido da regra: não tem dado real, não tem
usuário final e não tem compromisso de disponibilidade. Produção continua exigindo alguém decidir.

> ⏳ Os workflows de publicação são a **etapa B** desta decisão e ainda não existem. Até lá, staging
> sobe à mão pelo `docker compose up -d --wait` de 8.2.

---

## 8.7 Memória: por que os limites não são opcionais

Quatro JVMs passam a dividir 8 GB. Sem `mem_limit`, cada uma dimensiona o heap pela memória **do
host** — as quatro se acham sozinhas na máquina, e a soma estoura muito antes de qualquer uma sentir
pressão.

| Serviço | `mem_limit` | Heap |
| :--- | ---: | ---: |
| `app-postgres` | 512 MB | — |
| `app-keycloak` | 1024 MB | ~768 MB |
| `app-backend` | 1024 MB | ~768 MB |
| `app-frontend` | 64 MB | — |
| `app-caddy` | 128 MB | — |
| `app-staging-postgres` | 384 MB | — |
| `app-staging-keycloak` | 768 MB | ~576 MB |
| `app-staging-backend` | 768 MB | ~576 MB |
| `app-staging-frontend` | 64 MB | — |
| **Soma em regime** | **4736 MB** | |

Com o sistema operacional e o Docker, cerca de 5,2 GB — **~35% livre**. Os dois `keycloak-config` são
transitórios (256 MB cada) e somam apenas no pico de uma publicação, que é o momento de maior pressão
da máquina e a razão de a folga existir.

**No backend o teto entra por `JAVA_TOOL_OPTIONS`, nunca por `JAVA_OPTS`**: o `ENTRYPOINT` da imagem
é a forma *exec* `["java","-jar","/app/app.jar"]`, sem shell para expandir `JAVA_OPTS` — defini-la
seria configuração inerte, que falha em silêncio. No Keycloak, a variável é `JAVA_OPTS_KC_HEAP`.

Conferência em regime:

```bash
docker stats --no-stream
free -m
```

---

## 8.8 Administrar o Keycloak de staging

O console está **fechado nos dois ambientes** (`/admin*` e `/realms/master/*` respondem 404), pelo
mesmo motivo: o realm `master` nasce com `bruteForceProtected` falso, e ser ambiente de teste não
torna um painel de administração de identidade sem proteção contra força bruta menos perigoso — ele
dá acesso à mesma máquina.

Para administrar, túnel SSH:

```bash
ssh -L 8081:app-staging-keycloak:8080 app@SEU_IP
```

Mas a regra vale aqui como em produção: **alteração feita pelo console é drift.** Com
`IMPORT_CACHE_ENABLED=false`, ela é desfeita na próxima publicação. O lugar de mudar o realm é
`shared/keycloak/app-realm.yaml`.

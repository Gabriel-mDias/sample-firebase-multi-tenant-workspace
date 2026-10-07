# 06 — Operação corrente

---

## 6.1 Publicar uma versão nova

1. Merge na `main` → o `build-images.yml` constrói e publica as imagens.
2. **Actions → Deploy → Run workflow** (ou, na VPS, `docker compose pull && docker compose up -d`).

O deploy derruba e sobe os contêineres: há uma janela de indisponibilidade de alguns segundos. Deploy
sem interrupção está fora do escopo desta entrega.

As migrações do banco rodam no boot do backend, pelo Liquibase. Se uma migração falhar, o backend não
sobe — e é o comportamento certo: `ddl-auto: validate` recusa subir com o schema divergente do
mapeamento.

## 6.2 Mudar configuração do Keycloak

**Nunca pelo console administrativo.** Edite
[`environments/shared/keycloak/app-realm.yaml`](../shared/keycloak/app-realm.yaml), commite, e
reaplique:

```bash
git pull
docker compose up -d --force-recreate app-keycloak-config
docker compose logs app-keycloak-config
```

A ferramenta roda com o cache de checksum **desligado**, então ela reconcilia de verdade: uma
alteração feita no console é **desfeita** na próxima aplicação. Isso é intencional — é o que torna o
arquivo a fonte de verdade em vez de uma sugestão.

**Exceção:** os 7 grupos de perfil de atuação **não** estão nesse arquivo. Eles derivam do enum
`PerfilAtuacao` no código Java e são convergidos pelo `PerfilAtuacaoGroupBootstrap` a cada boot do
backend. Para acrescentar um perfil, mexa no enum — não no YAML.

## 6.3 Repontar a interface sem reconstruir

A imagem do frontend gera o `env.js` no start, a partir das variáveis. Trocar o endereço da API não
exige build novo:

```bash
# edite API_DOMAIN no .env
docker compose up -d --force-recreate app-frontend
curl -s https://app.seudominio.com.br/env.js
```

## 6.4 Rotacionar um segredo

| Segredo | Como |
| :--- | :--- |
| `KEYCLOAK_BACKEND_CLIENT_SECRET` | edite o `.env`, recrie `app-keycloak-config` **e** `app-backend`. O YAML já lê do ambiente |
| `APP_ADMIN_PASSWORD` | edite o `.env` e recrie `app-keycloak-config` |
| `KEYCLOAK_ADMIN_PASSWORD` | troque pelo console do realm `master` **e** no `.env` — não é gerenciado pelo YAML |
| Senhas de banco | exigem `ALTER USER` no Postgres além do `.env`; a criação só roda em volume vazio |
| Chaves AWS | rotacione no IAM, atualize `.env` e `/etc/app-backup.env` |

Depois de qualquer rotação, confirme o login ponta a ponta antes de encerrar.

## 6.5 Diagnóstico

```bash
docker compose ps
docker compose logs -f --tail=100 app-backend
docker stats --no-stream
df -h && free -h
```

| Sintoma | Onde olhar primeiro |
| :--- | :--- |
| `502` do Caddy em tudo | o contêiner de destino não subiu — `docker compose ps` |
| `401` em toda chamada autenticada | `iss` do token divergindo do `KEYCLOAK_ISSUER_URI` — confira `KC_HOSTNAME` |
| Login funciona, mas o usuário não vê nada | grupo sem role mapeada — confira o log `7/7 grupos garantidos` do backend |
| `502` só ao cadastrar usuário | recusa do Keycloak. A mensagem no corpo diz a causa — é para isso que ela é preservada |
| Backend não sobe | veja se o `app-keycloak-config` concluiu; o backend depende dele |
| Certificado expirado | log do `caddy`; quase sempre a porta 80 fechada, que a renovação exige |

## 6.6 Manutenção periódica

- **Toda semana:** conferir que os lotes de backup estão chegando ao bucket.
- **A cada trimestre:** repetir o **ensaio de restauração** do item 5.5. Backup que não é restaurado
  há meses voltou a ser hipótese.
- **A cada trimestre:** `sudo apt update && sudo apt upgrade` e reiniciar em janela combinada.
- **Ao atualizar o Keycloak de versão maior:** a imagem da ferramenta de configuração é casada com a
  versão do Keycloak (`6.5.1-26.0.5`). Atualize as duas juntas.

## 6.7 O que nunca fazer

- **`docker compose down -v`.** Destrói o banco da aplicação e o do Keycloak juntos — e as senhas dos
  usuários existem apenas no segundo.
- **Alterar configuração pelo console do Keycloak.** Será desfeita na próxima aplicação, e você vai
  perder tempo procurando o motivo.
- **Publicar porta de banco ou de Keycloak no host.** Só o Caddy publica porta.
- **Commitar o `.env`.** O `.gitignore` protege, mas ele é a última linha, não a primeira.
- **Repetir o `up` quando o certificado falha.** Cada tentativa conta no limite semanal do Let's
  Encrypt. Corrija o DNS primeiro.

## 6.8 Dívida técnica herdada, a resolver antes de tráfego real

Itens que vinham do antigo `TODO-HUMANO.md` e continuam válidos:

- **`npm audit`** acusava 13 vulnerabilidades em dependências transitivas, uma delas crítica.
  Avaliar antes de expor a interface a usuários reais.
- **Bundle inicial em ~728 kB**, acima do orçamento de 500 kB configurado (é aviso, não erro). Vem da
  SDK e do sweetalert2.
- **`provideAnimationsAsync()` está depreciado** (remoção prevista no Angular 23) mas é exigido pela
  SDK. Acompanhar.
- **Dependências da SDK GEMS num runner limpo**: se o registro for privado, o build de CI depende dos
  secrets do item 3.3.

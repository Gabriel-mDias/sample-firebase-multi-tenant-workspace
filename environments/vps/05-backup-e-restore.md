# 05 — Backup e restauração

**Este é o capítulo mais importante do roteiro.** Desde a feature 007, as contas e senhas dos
usuários existem **apenas** no `keycloak-db` — o `security.USUARIO` guarda login e identificador
externo, nunca a senha. Perder esse banco sem backup significa que ninguém mais entra no sistema, e
não há como recriar as credenciais.

---

## 5.1 As duas cópias formam um par

`security.USUARIO.CD_KEYCLOAK_ID` é uma **chave estrangeira entre dois bancos**, e nenhum banco a
garante. `pg_dump` é por banco: dois dumps são dois instantes distintos. Um usuário criado
entre eles deixaria um `USUARIO` apontando para uma conta inexistente, ou uma conta órfã no realm — o
mesmo defeito que a compensação transacional evita em runtime, voltando pela porta do backup.

Por isso o `scripts/pg-backup.sh` tira os dois em sequência imediata e dá a ambos o **mesmo carimbo de
lote** no nome do arquivo.

> **Restaure sempre o par, nunca um só.** Cópias de lotes diferentes não são combináveis.

## 5.2 Credencial e configuração

Crie o arquivo de ambiente da rotina, com a credencial IAM **do backup** (a de escrita no bucket de
backup, criada no item 3.2 — não a da aplicação):

```bash
sudo install -m 600 /dev/null /etc/app-backup.env
sudo tee /etc/app-backup.env >/dev/null <<'EOF'
PGCONTAINER=app-postgres
POSTGRES_SUPERUSER=postgres
APP_DB_NAME=app-db
KEYCLOAK_DB_NAME=keycloak-db
BACKUP_DIR=/var/backups/app
BACKUP_RETENTION_DAYS=14
BACKUP_S3_BUCKET=seu-bucket-de-backup
AWS_ACCESS_KEY_ID=
AWS_SECRET_ACCESS_KEY=
AWS_DEFAULT_REGION=us-east-1
EOF
sudo nano /etc/app-backup.env   # preencha as chaves
```

A rotina usa a CLI da AWS:

```bash
sudo apt install -y awscli
```

## 5.3 Agendar

Timer do systemd no host, não um contêiner: menos uma peça e mais fácil de auditar.

`/etc/systemd/system/app-backup.service`:

```ini
[Unit]
Description=Backup dos bancos do App
After=docker.service

[Service]
Type=oneshot
User=app
EnvironmentFile=/etc/app-backup.env
ExecStart=/home/app/app-project/scripts/pg-backup.sh
```

`/etc/systemd/system/app-backup.timer`:

```ini
[Unit]
Description=Backup diario do App

[Timer]
OnCalendar=*-*-* 03:30:00
Persistent=true

[Install]
WantedBy=timers.target
```

O `ExecStart` do systemd **executa** o arquivo, e não o passa a um shell: sem o bit de execução, a
unidade falha com `Permission denied`. O bit precisa estar no git (modo `100755`), então o
`chmod` abaixo é redundante num clone normal — fica como cinto de segurança para o caso de os
arquivos terem chegado à VPS por `scp` ou `rsync` sem preservar o modo.

```bash
chmod +x ~/app-project/scripts/pg-backup.sh
sudo systemctl daemon-reload
sudo systemctl enable --now app-backup.timer
sudo systemctl list-timers app-backup.timer
```

> **O horário importa.** Rode em janela de baixo movimento: é entre os dois dumps que existe a chance
> de o par sair desemparelhado.

## 5.4 Primeira execução

```bash
sudo systemctl start app-backup.service
journalctl -u app-backup.service -n 40 --no-pager
aws s3 ls s3://seu-bucket-de-backup/ --recursive | tail -5
```

Esperado: **dois** arquivos, com o mesmo carimbo de lote no nome.

## 5.5 Ensaio de restauração — obrigatório

**Backup nunca restaurado é hipótese, não backup.** Este ensaio é parte da entrega, não da
documentação. Faça-o agora, enquanto o sistema ainda não tem dados que você teme perder.

Ele roda num Postgres descartável, **sem tocar no que está no ar**:

```bash
LOTE=20260810T033000Z   # o carimbo do lote que você quer testar
mkdir -p /tmp/restore && cd /tmp/restore
aws s3 cp s3://seu-bucket-de-backup/$LOTE/ . --recursive

docker run -d --name pg-teste -e POSTGRES_PASSWORD=teste -p 5433:5432 postgres:16
sleep 10
docker exec -i pg-teste psql -U postgres -c 'CREATE DATABASE "app-db";'
docker exec -i pg-teste psql -U postgres -c 'CREATE DATABASE "keycloak-db";'

zcat ${LOTE}__app-db.sql.gz    | docker exec -i pg-teste psql -U postgres -d app-db
zcat ${LOTE}__keycloak-db.sql.gz | docker exec -i pg-teste psql -U postgres -d keycloak-db
```

**A verificação que importa** não é "o restore rodou sem erro" — é provar que uma credencial
sobreviveu:

```bash
# 1. Um usuário que existia no momento do backup continua lá, com o vínculo entre os dois bancos
#    íntegro:
docker exec -i pg-teste psql -U postgres -d app-db -c \
  "SELECT NM_LOGIN, CD_KEYCLOAK_ID FROM security.USUARIO;"

# 2. E a conta correspondente existe no dump do Keycloak, com credencial gravada:
docker exec -i pg-teste psql -U postgres -d keycloak-db -c \
  "SELECT u.username, count(c.id) AS credenciais
     FROM user_entity u LEFT JOIN credential c ON c.user_id = u.id
    GROUP BY u.username;"
```

Cada `CD_KEYCLOAK_ID` da primeira consulta precisa ter conta correspondente na segunda, e a conta
precisa ter ao menos uma credencial. **Se algum identificador não casar, o par estava desemparelhado**
— revise o horário do agendamento.

Limpe o ensaio:

```bash
docker rm -f pg-teste && rm -rf /tmp/restore
```

## 5.6 Restauração de verdade

Depois de um desastre, com a stack **parada**:

```bash
cd ~/app-project/environments/production
docker compose down                    # sem -v
docker volume rm app_postgres_data   # deliberado: o volume vai ser recriado do backup
docker compose up -d app-postgres    # recria os bancos vazios pelo init-postgres.sh
```

Restaure **o par completo**, do mesmo lote, e só então suba o resto:

```bash
zcat LOTE__app-db.sql.gz    | docker exec -i app-postgres psql -U postgres -d app-db
zcat LOTE__keycloak-db.sql.gz | docker exec -i app-postgres psql -U postgres -d keycloak-db
docker compose up -d
```

Repita a verificação do item 4.5 antes de considerar o sistema recuperado.

---

**Próximo:** [06 — Operação](./06-operacao.md)

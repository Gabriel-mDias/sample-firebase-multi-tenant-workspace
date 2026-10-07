#!/usr/bin/env bash
#
# Cria os dois bancos e seus donos, no primeiro boot do volume do Postgres.
#
# POR QUE É UM .sh E NÃO O ANTIGO .sql
# ------------------------------------
# O entrypoint do Postgres executa tanto `.sql` quanto `.sh` de `/docker-entrypoint-initdb.d`. Só o
# `.sh` enxerga variáveis de ambiente — e é isso que permite que ESTE MESMO arquivo sirva ao ambiente
# local e à produção, com senhas diferentes. O `.sql` obrigava a senha literal no repositório.
#
# QUANDO ISTO RODA
# ----------------
# Apenas quando o diretório de dados está VAZIO. Num volume já inicializado o entrypoint ignora este
# script inteiro. Alterá-lo não afeta ambiente existente — para isso, use SQL manual.
#
# POR QUE OS DOIS BANCOS NO MESMO SERVIDOR
# ----------------------------------------
# Num host único, duas instâncias dobrariam o piso de memória sem
# ganho de disponibilidade — se o host cai, as duas caem. O que protege o dado é backup por banco,
# não isolamento de volume. E é por isso que `docker compose down -v` continua proibido: ele destrói
# o banco da aplicação E as senhas dos usuários, que existem apenas no keycloak-db.

set -euo pipefail

: "${APP_DB_NAME:?variável obrigatória}"
: "${APP_DB_USER:?variável obrigatória}"
: "${APP_DB_PASSWORD:?variável obrigatória}"
: "${KEYCLOAK_DB_NAME:?variável obrigatória}"
: "${KEYCLOAK_DB_USER:?variável obrigatória}"
: "${KEYCLOAK_DB_PASSWORD:?variável obrigatória}"

# --set + :'variavel' faz o psql escapar o literal, em vez de interpolar a senha no texto do SQL.
# Sem isso, uma senha com aspas simples quebraria o comando — e as senhas deste projeto têm.
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" \
     --set app_user="$APP_DB_USER" \
     --set app_password="$APP_DB_PASSWORD" \
     --set keycloak_user="$KEYCLOAK_DB_USER" \
     --set keycloak_password="$KEYCLOAK_DB_PASSWORD" <<-'EOSQL'
    CREATE USER :"app_user" WITH ENCRYPTED PASSWORD :'app_password';
    CREATE USER :"keycloak_user" WITH ENCRYPTED PASSWORD :'keycloak_password';
EOSQL

# CREATE DATABASE não pode rodar dentro do bloco acima em algumas configurações de transação, e o
# nome do banco precisa de identificador citado — daí a chamada separada.
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" \
     --set app_db="$APP_DB_NAME" --set app_user="$APP_DB_USER" \
     --set keycloak_db="$KEYCLOAK_DB_NAME" --set keycloak_user="$KEYCLOAK_DB_USER" <<-'EOSQL'
    CREATE DATABASE :"app_db" OWNER :"app_user";
    GRANT ALL PRIVILEGES ON DATABASE :"app_db" TO :"app_user";

    CREATE DATABASE :"keycloak_db" OWNER :"keycloak_user";
    GRANT ALL PRIVILEGES ON DATABASE :"keycloak_db" TO :"keycloak_user";
EOSQL

echo "Bancos '${APP_DB_NAME}' e '${KEYCLOAK_DB_NAME}' criados."

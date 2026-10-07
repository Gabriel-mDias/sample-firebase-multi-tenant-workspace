#!/usr/bin/env bash
#
# Backup dos dois bancos da aplicação, enviado para fora da máquina.
#
# POR QUE OS DOIS DUMPS SÃO UMA UNIDADE
# -------------------------------------
# `security.USUARIO.CD_KEYCLOAK_ID` é chave estrangeira ENTRE DOIS BANCOS, e nenhum banco a garante.
# `pg_dump` é por banco, então dois dumps são dois instantes distintos: um usuário criado
# entre eles deixaria um USUARIO apontando para uma conta que não está no dump do Keycloak, ou uma
# conta órfã no realm. É o mesmo defeito que a compensação transacional evita em runtime, voltando
# pela porta do backup.
#
# Mitigação: os dois dumps saem em sequência imediata e compartilham o MESMO carimbo de lote no nome
# do arquivo. Restaure sempre o par — nunca um só. Se um dia a janela entre eles incomodar, a saída
# é backup físico do cluster (`pg_basebackup` + WAL), atômico entre bancos.
#
# POR QUE AS SENHAS DOS USUARIOS DEPENDEM DISTO
# --------------------------------------------------
# As contas e senhas da equipe existem APENAS no keycloak-db. O `security.USUARIO`
# guarda login e identificador externo, nunca a senha. Perder o keycloak-db sem backup significa que
# ninguém mais entra no sistema, e não há como recriar as credenciais.
#
# USO
#     ./scripts/pg-backup.sh
#
# Agendamento: systemd timer no host — ver environments/vps/05-backup-e-restore.md.

set -euo pipefail

# --- Configuração -------------------------------------------------------------------------------
# Lidas do ambiente; o timer as fornece por EnvironmentFile.
: "${PGCONTAINER:=app-postgres}"
: "${POSTGRES_SUPERUSER:?defina POSTGRES_SUPERUSER}"
: "${APP_DB_NAME:=app-db}"
: "${KEYCLOAK_DB_NAME:=keycloak-db}"
: "${BACKUP_DIR:=/var/backups/app}"
: "${BACKUP_RETENTION_DAYS:=14}"
# Bucket e credencial SEPARADOS dos da aplicação: um vazamento da credencial de upload não pode
# alcançar as cópias de segurança.
: "${BACKUP_S3_BUCKET:?defina BACKUP_S3_BUCKET}"

LOTE="$(date -u +%Y%m%dT%H%M%SZ)"
mkdir -p "${BACKUP_DIR}"

dump() {
    local banco="$1"
    local destino="${BACKUP_DIR}/${LOTE}__${banco}.sql.gz"

    echo "==> Dump de '${banco}' ..."
    # -Fp comprimido por gzip: legível com zcat e restaurável com psql, sem depender de pg_restore
    # da mesma versão. --clean para que o restore não exija banco vazio.
    docker exec "${PGCONTAINER}" pg_dump \
        --username "${POSTGRES_SUPERUSER}" \
        --dbname "${banco}" \
        --clean --if-exists --no-owner \
        | gzip -9 > "${destino}"

    # Um dump que saiu vazio por erro de conexão é pior que dump nenhum: dá falsa sensação de
    # cobertura. Falhar aqui é o comportamento correto.
    if [ ! -s "${destino}" ]; then
        echo "ERRO: dump de '${banco}' saiu vazio." >&2
        exit 1
    fi

    echo "    ${destino} ($(du -h "${destino}" | cut -f1))"
}

# A ordem importa pouco; a proximidade no tempo é que importa. Nada de tarefa demorada entre as duas.
dump "${APP_DB_NAME}"
dump "${KEYCLOAK_DB_NAME}"

echo "==> Enviando o lote ${LOTE} para s3://${BACKUP_S3_BUCKET}/ ..."
# Backup guardado na mesma máquina que ele deveria proteger não é backup. O envio para fora é parte
# da rotina, não um extra.
aws s3 cp "${BACKUP_DIR}/${LOTE}__${APP_DB_NAME}.sql.gz"    "s3://${BACKUP_S3_BUCKET}/${LOTE}/"
aws s3 cp "${BACKUP_DIR}/${LOTE}__${KEYCLOAK_DB_NAME}.sql.gz" "s3://${BACKUP_S3_BUCKET}/${LOTE}/"

echo "==> Limpando cópias locais com mais de ${BACKUP_RETENTION_DAYS} dias ..."
find "${BACKUP_DIR}" -name '*.sql.gz' -mtime "+${BACKUP_RETENTION_DAYS}" -delete

echo "Lote ${LOTE} concluído: 2 bancos, enviados para fora da máquina."
echo "Lembre-se: as duas cópias formam um PAR e devem ser restauradas em conjunto."

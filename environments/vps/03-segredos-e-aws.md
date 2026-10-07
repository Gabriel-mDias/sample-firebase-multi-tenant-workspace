# 03 — Segredos e AWS

Nenhum valor real de segredo entra no Git. O `.env.example` de cada ambiente é o contrato versionado;
o `.env` de verdade existe só na VPS, com permissão restrita. O `.gitignore` da raiz bloqueia o
commit acidental, mas ele é a última linha de defesa, não a primeira.

---

## 3.1 Gerar as senhas

Na VPS:

```bash
cd ~/app-project/environments/production
cp .env.example .env
chmod 600 .env
```

Gere **uma senha diferente para cada campo** — reaproveitar transforma um vazamento em vários:

```bash
openssl rand -base64 24
```

Preencha:

| Variável | Observação |
| :--- | :--- |
| `POSTGRES_SUPERUSER_PASSWORD` | superusuário do Postgres |
| `APP_DB_PASSWORD` | dono do banco da aplicação |
| `KEYCLOAK_DB_PASSWORD` | dono do banco do Keycloak |
| `KEYCLOAK_ADMIN_USER` / `_PASSWORD` | administrador do realm `master`. **É a chave-mestra da identidade** — o segredo mais sensível do arquivo. Não use `admin` como nome |
| `KEYCLOAK_BACKEND_CLIENT_SECRET` | segredo do cliente confidencial que o backend usa na Admin API |
| `APP_ADMIN_EMAIL` / `_PASSWORD` | administrador **da aplicação**, com quem você entra na interface |

> **A senha do `APP_ADMIN_PASSWORD` precisa passar na política do realm**: mínimo 8 caracteres, com
> minúscula, maiúscula e caractere especial. Uma senha gerada por `openssl rand -base64` quase sempre
> passa, mas confira — se ela falhar, o `app-keycloak-config` aborta e o backend não sobe.

Preencha também os três domínios do capítulo 02, o `ACME_EMAIL` e o `GHCR_OWNER` (seu usuário do
GitHub, em minúsculas).

## 3.2 Dois buckets, duas credenciais

São **dois** buckets com finalidades e riscos diferentes:

| Bucket | Para quê | Quando é necessário |
| :--- | :--- | :--- |
| Aplicação | upload de arquivos pela interface | **a partir do momento em que houver upload de arquivo** — é nele que os documentos dos clientes ficam |
| **Backup** | cópias dos dois bancos | **desde o dia um** |

**Não use a mesma credencial nos dois.** A da aplicação é usada por código exposto à internet; se ela
vazar, não pode alcançar as cópias de segurança.

No console da AWS:

1. Crie os dois buckets, na mesma região, **sem acesso público**.
2. No bucket de backup, ative **versionamento** e uma regra de ciclo de vida expirando objetos com
   mais de 90 dias.
3. Crie **dois usuários IAM** com acesso programático:
   - o da aplicação, com permissão de leitura e escrita **apenas** no bucket da aplicação;
   - o do backup, com permissão de escrita **apenas** no bucket de backup.
4. Guarde as quatro chaves. A da aplicação vai para `AWS_ACCESS_KEY` / `AWS_SECRET_KEY` no `.env`; a
   do backup é configurada no capítulo 05.

> ⚠️ **Nunca aponte a aplicação para o bucket de backup**, nem "temporariamente". Este capítulo
> faz sentido só enquanto o sistema não tiver upload nenhum; a partir dele
> tem, e a sugestão passou a contradizer o parágrafo logo acima. A credencial da aplicação é usada
> por código exposto à internet — se ela alcançar o bucket de backup, o vazamento que a separação
> existe para conter passa a alcançar também as cópias de segurança.
>
> **Ligue versionamento no bucket da aplicação também.** O `pg-backup.sh` copia os dois bancos, mas
> **não** copia os arquivos do acervo: restaurar só o banco devolveria metadado apontando para PDF
> que não existe mais. Versionamento é a proteção mais barata contra exclusão acidental enquanto o
> backup do acervo não existir.

## 3.3 Secrets no GitHub

Em **Settings → Secrets and variables → Actions**, no repositório:

| Secret | Para quê |
| :--- | :--- |
| `GEMS_REGISTRY_USER` | credencial do registro privado da SDK, usada no build |
| `GEMS_REGISTRY_TOKEN` | idem |
| `VPS_HOST` | IP ou nome da VPS |
| `VPS_USER` | `app` |
| `VPS_SSH_KEY` | chave **privada** com acesso à VPS |

> ## ✅ O backend CONSTRÓI em CI
>
> Vale registrar o diagnóstico errado que já custou tempo aqui: concluiu-se, uma vez, que os módulos
> Maven `br.com.gems:gems-*` não estavam publicados em repositório nenhum e que a imagem do backend
> só podia ser construída numa máquina com os jars no `~/.m2`. **Estava errado**: os artefatos
> **já estavam publicados** em `https://maven.pkg.github.com/Gabriel-mDias/Java-GEMS-SDK`; o que
> faltava era o `<repositories>` no `backend/pom.xml` e o rótulo certo de `${gems.version}`.
>
> **O que vale:** as duas dependências GEMS — a npm do frontend e a Maven do backend — exigem
> credencial, porque o GitHub Packages autentica até para leitura. Com `GEMS_REGISTRY_USER` e
> `GEMS_REGISTRY_TOKEN` preenchidos, os dois builds passam. Sem eles, os dois falham com **401** —
> e o 401 é de credencial ausente, não de artefato inexistente.
>
> O `<id>` do `<server>` precisa ser **`github`**, idêntico ao `<repositories>` de
> `backend/pom.xml`. Id divergente faz o Maven não aplicar a credencial, e o sintoma é um 401
> indistinguível de senha errada.
>
> Detalhe completo em [`roteiro-PF3.md`](../../utils/documentação%20do%20projeto/plano-onda-1/roteiros/roteiro-PF3.md).

Para o `VPS_SSH_KEY`, gere um par **dedicado ao deploy**, em vez de reaproveitar a sua chave pessoal:

```bash
ssh-keygen -t ed25519 -f ~/.ssh/app_deploy -C "deploy app"
ssh-copy-id -i ~/.ssh/app_deploy.pub app@SEU_IP
```

A chave privada (`~/.ssh/app_deploy`) vai inteira para o secret, incluindo as linhas de início e
fim.

O registro de imagens não precisa de secret: o workflow usa o `GITHUB_TOKEN` que o Actions já
fornece. Confira apenas, em **Settings → Actions → General**, que as permissões de escrita de pacote
estão habilitadas.

## 3.4 Conferir antes de seguir

```bash
cd ~/app-project/environments/production
grep -c '=$' .env    # deve ser 0 — nenhuma variável obrigatória vazia
ls -l .env           # deve mostrar -rw------- (600)
```

---

**Próximo:** [04 — Primeiro deploy](./04-primeiro-deploy.md)

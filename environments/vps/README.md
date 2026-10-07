# Publicação do App numa VPS

Roteiro das tarefas que **dependem de você**. Tudo que dá para automatizar já está automatizado: as
imagens são construídas pelo GitHub Actions, a configuração do Keycloak é aplicada por arquivo
versionado, e o TLS é emitido e renovado sozinho. O que sobra aqui é o que exige uma decisão, uma
credencial ou um acesso que só você tem.

**Siga os capítulos na ordem.** Ela não é arbitrária: o DNS precisa estar propagado antes do primeiro
`up`, senão a emissão do certificado falha e o Let's Encrypt passa a recusar novas tentativas por um
período.

| # | Capítulo | O que você faz |
| :-- | :--- | :--- |
| 01 | [Provisionamento](./01-provisionamento.md) | criar a VPS, fechar o acesso, instalar Docker |
| 02 | [DNS e domínio](./02-dns-e-dominio.md) | três registros A e conferir a propagação |
| 03 | [Segredos e AWS](./03-segredos-e-aws.md) | gerar senhas, criar buckets, cadastrar secrets no GitHub |
| 04 | [Primeiro deploy](./04-primeiro-deploy.md) | construir as imagens, subir e verificar |
| 05 | [Backup e restauração](./05-backup-e-restore.md) | agendar a rotina e **ensaiar** o restore |
| 06 | [Operação](./06-operacao.md) | atualizar, rotacionar segredo, diagnosticar, o que nunca fazer |
| 07 | [Homologação](./07-homologacao.md) | **comece por aqui na primeira vez** — o ensaio com dado sintético, antes de existir dado que dói perder |

## Topologia

Um host, tudo em Docker Compose. **Apenas o Caddy publica porta**; banco, Keycloak, backend e
frontend conversam pela rede interna e não são alcançáveis de fora da máquina.

```
                    Internet
                       │
                 :80  :443          ← as ÚNICAS portas publicadas
                       │
                 ┌─────────┐
                 │  caddy  │  TLS automático (Let's Encrypt)
                 └────┬────┘
        ┌─────────────┼──────────────┐
        │             │              │
   app.dominio   api.dominio    auth.dominio
        │             │              │
   ┌────────┐   ┌──────────┐   ┌──────────┐
   │frontend│   │ backend  │   │ keycloak │
   │ nginx  │   │ :8080    │   │ :8080    │
   └────────┘   └────┬─────┘   └────┬─────┘
                     │              │
                     └──────┬───────┘
                       ┌──────────┐
                       │ postgres │  app-db + keycloak-db
                       └──────────┘
```

Fora do desenho, porque roda uma vez e morre: `app-keycloak-config`, que aplica
`environments/shared/keycloak/app-realm.yaml` assim que o Keycloak fica saudável, e sem o qual o
backend nem sobe.

## Pré-requisitos

- **VPS Linux** com pelo menos **4 GB de RAM** e 40 GB de disco. Abaixo disso o Keycloak e o Postgres
  competem por memória e o sistema fica instável sob carga. Não é preciso mais que isso porque a VPS
  não compila nada — as imagens chegam prontas.
- **Um domínio** com controle sobre os registros DNS.
- **Conta AWS** com permissão para criar buckets S3 e usuários IAM.
- **Acesso de administrador ao repositório no GitHub**, para cadastrar os *secrets*.

## O que este roteiro não cobre

Está deliberadamente fora de escopo: alta disponibilidade, réplica de banco, deploy sem
interrupção, monitoração com alerta e cofre de segredos dedicado. Um host, um contêiner por
serviço, diagnóstico por log.

O **ambiente de homologação** também constava dessa lista; ele deixou de constar com o
[capítulo 07](./07-homologacao.md), que não muda a topologia — descreve como subir uma **segunda
instância** dela, com domínios e segredos próprios, para ensaiar o procedimento antes de produção.

Continua valendo, e importa para quem for além do ensaio: **não há escopo horizontal de dado**.
A autorização é por ação, não por recorte de registro — quem tem a ação de consulta alcança todo o
acervo daquele recurso. É correto para o modelo single-tenant, e é aceitável com dado sintético;
com dado real, é a primeira coisa a reavaliar.

# 07 — Ambiente de homologação

Este capítulo é para o **ensaio**: uma VPS com dado sintético, para você exercitar o procedimento
inteiro antes de existir dado que dói perder. Produção nasce depois, numa máquina nova, seguindo os
mesmos capítulos 01 a 06.

**Ele não substitui os capítulos anteriores** — acrescenta o que muda.

---

## 7.1 Por que ensaiar antes

A publicação tem três passos que só falham na primeira vez, e todos custam caro quando falham com
dado real:

| Passo | O que dá errado na primeira vez | Custo se for direto em produção |
| :--- | :--- | :--- |
| Emissão do certificado | DNS ainda não propagou | O Let's Encrypt **limita tentativas por semana**. Errar não é repetir o comando: é esperar |
| Ordem de subida | `.env` incompleto, senha fora da política | O `app-keycloak-config` aborta e o **backend não sobe** |
| Restauração | ninguém nunca testou | Descobrir que o backup não presta no dia em que ele é necessário |

O ensaio existe para que os três aconteçam **quando não importam**.

---

## 7.2 A máquina

| | Recomendado | Por quê |
| :--- | :--- | :--- |
| **Provedor** | Hetzner Cloud, **CX22** (2 vCPU, 4 GB RAM, 40 GB NVMe), ~€4/mês | É o mais barato que atende o mínimo documentado no [README](./README.md). A região europeia acrescenta ~200 ms de latência, irrelevante para um ensaio |
| **Alternativa com latência BR** | Vultr São Paulo, 4 GB, ~US$20/mês | Escolha se quiser medir a experiência real do escritório, e não só validar o procedimento |
| **Sistema** | Ubuntu LTS ou Debian estável | O capítulo 01 assume `apt` |

**Não desça de 4 GB.** Abaixo disso o Keycloak e o Postgres competem por memória e o sistema fica
instável sob carga — e um ambiente instável ensina a lição errada no ensaio.

O swap do item 1.6 **não é opcional aqui**: com 4 GB e três serviços JVM/Postgres, é ele que evita
que o kernel mate um contêiner num pico.

---

## 7.3 Domínios separados

Use um conjunto próprio, para o ensaio não tomar o nome definitivo:

```
app-hml.seudominio.com.br
api-hml.seudominio.com.br
auth-hml.seudominio.com.br
```

Três registros A apontando para o IP da VPS de homologação, exatamente como no capítulo 02.

> **Por que não subdomínio de terceiro nível** (`app.hml.seudominio`): funciona, mas exige
> certificado curinga ou três emissões, e complica sem devolver nada num ensaio.

---

## 7.4 O `.env` de homologação

Copie de `.env.example` como manda o capítulo 03, com três diferenças:

```bash
APP_DOMAIN=app-hml.seudominio.com.br
API_DOMAIN=api-hml.seudominio.com.br
AUTH_DOMAIN=auth-hml.seudominio.com.br

# NUNCA `latest` em homologação — ver 7.5
IMAGE_TAG=sha-a1b2c3d
```

**As senhas são outras.** Reaproveitar as de produção num ambiente que existe para ser quebrado
anula o motivo de ele existir. Gere um conjunto novo com `openssl rand -base64 24`.

**A senha do `APP_ADMIN_PASSWORD` precisa passar na política do realm**: mínimo **12**, com
minúscula, maiúscula e caractere especial, diferente do login e do e-mail. Se não passar, o
`app-keycloak-config` aborta e o backend não sobe — e a mensagem aparece no log **daquele**
contêiner, não no do backend.

**Buckets S3 próprios**, separados dos de produção. Dois, como no capítulo 03: um de aplicação, um de
backup, com credenciais IAM distintas. Mesmo com dado sintético, é o ensaio da separação que importa.

---

## 7.5 `IMAGE_TAG` é uma SHA, não `latest`

Em produção o `latest` é defensável: você publica e quer o que acabou de publicar. **Em homologação
ele destrói o propósito do ambiente** — você homologa uma imagem, alguém faz merge em `main`, o
`latest` muda, e o que você aprovou não é mais o que está no ar.

O `build-images.yml` já publica uma tag `sha-<curta>` em todo push para `main`
(`type=sha,format=short`). Use ela:

```bash
# a tag existe em Packages, ao lado de `latest`
IMAGE_TAG=sha-a1b2c3d docker compose -f environments/production/docker-compose.yml up -d
```

Promover para produção passa a ser **repetir a mesma SHA** na outra máquina — não reconstruir e
torcer para dar igual.

---

## 7.6 Dado sintético — e por que a regra é dura

Este ambiente recebe **apenas dado fictício**. Não é preciosismo:

- O cadastro guarda **identidade civil de terceiros** — CPF, RG, endereço de pessoas que não são
  usuárias do ambiente de teste e nunca consentiram com isso.
- **Não existe escopo horizontal**: a autorização é por ação, e quem tem a ação de consulta alcança
  todos os registros daquele recurso. Correto para o modelo single-tenant de hoje, e inofensivo com
  dado inventado — não com dado real.
- **A trilha registra alteração, não leitura.** Ela responde "quem mudou isto?"; não responde "quem
  leu isto?", e é a leitura que vaza dado de terceiro sem deixar rastro.

Se um dia este ambiente precisar de dado realista, use **massa anonimizada** — nunca uma cópia da
produção.

---

## 7.7 O que verificar, e nesta ordem

1. Capítulos **01 a 04 na íntegra**, incluindo o `nmap -Pn` **de outra máquina** (só 22, 80, 443).
2. Os itens **6, 7 e 8** do §4.5 — a superfície de diagnóstico fechada e os cabeçalhos de segurança.
3. **A CSP.** Ela sobe em `Report-Only`. Percorra a aplicação inteira com o console do navegador
   aberto — login, wizard de cadastro, upload de arquivo, modal de chancela, esteira de um serviço —
   e confirme que **não há violação relatada**. Só então promova a bloqueante no `Caddyfile`.
   Este é o passo que o ensaio existe para cobrir: uma CSP promovida sem observação quebra a
   aplicação em silêncio, e só em produção.
4. **O ensaio de restauração do capítulo 05.** Obrigatório, e este é o melhor momento da vida do
   projeto para fazê-lo.
5. **Relogar todo mundo.** As roles compostas mudaram de tamanho nas rodadas 011 a 013; um token
   antigo não carrega as ações novas, e não há como invalidá-lo retroativamente.

---

## 7.8 Encerrando o ensaio

Quando a homologação terminar e produção nascer noutra máquina:

```bash
# na VPS de homologação
cd ~/app-project/environments/production
docker compose down            # sem -v, por hábito — ver 4.6
```

E então **destrua a VPS pelo painel do provedor**, junto com os buckets do ensaio. Um ambiente de
homologação esquecido no ar é uma superfície exposta que ninguém monitora, com uma cópia do sistema e
credenciais válidas.

---

**Anterior:** [06 — Operação](./06-operacao.md)

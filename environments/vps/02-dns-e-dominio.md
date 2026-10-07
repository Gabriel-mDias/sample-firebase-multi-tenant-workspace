# 02 — DNS e domínio

**Este capítulo vem antes do deploy de propósito.** O Caddy pede o certificado assim que sobe, e o
Let's Encrypt confirma a posse do domínio conectando de volta no endereço que o DNS aponta. Se o DNS
ainda não resolve, ou resolve para outro lugar, a emissão falha — e o Let's Encrypt **limita o número
de falhas por domínio por semana**. Errar aqui custa espera, não um retry.

---

## 2.1 Três registros A

No painel DNS do seu domínio, apontando todos para o **IP da VPS**:

| Tipo | Nome | Valor | Serve |
| :--- | :--- | :--- | :--- |
| A | `app` | IP da VPS | interface |
| A | `api` | IP da VPS | API |
| A | `auth` | IP da VPS | Keycloak |

Use TTL baixo (300s) enquanto estiver configurando — se precisar corrigir, a correção propaga rápido.

> **Por que três subdomínios, e não caminhos num só.** O Keycloak monta URLs absolutas a partir do
> hostname configurado, e servi-lo sob um prefixo de caminho exige `KC_HTTP_RELATIVE_PATH` e um
> conjunto de ajustes de proxy que quebram de formas sutis no fluxo de login. Subdomínio é o caminho
> em que ele menos briga.

## 2.2 Conferir a propagação — obrigatório

**De fora da VPS:**

```bash
dig +short app.seudominio.com.br
dig +short api.seudominio.com.br
dig +short auth.seudominio.com.br
```

Os três precisam devolver o IP da VPS. Se algum vier vazio ou com outro endereço, **pare aqui** e
espere a propagação.

Confirme também que a porta 80 chega na máquina — é por ela que o desafio acontece:

```bash
curl -sS -o /dev/null -w '%{http_code}\n' http://app.seudominio.com.br
```

Qualquer resposta HTTP serve (inclusive erro), desde que **não** seja timeout ou recusa de conexão.
Timeout significa firewall bloqueando; recusa significa que nada escuta na 80 ainda — o que é
esperado antes do deploy, e é o único caso aceitável para prosseguir.

## 2.3 Se você usa CDN ou proxy na frente

Se o seu provedor de DNS oferece proxy (a "nuvem laranja" do Cloudflare, por exemplo),
**desligue-o para os três registros** durante o primeiro deploy. Com o proxy ligado, o desafio
HTTP-01 é interceptado e a emissão falha. Depois que o certificado estiver emitido, você pode
reavaliar — mas note que aí passam a existir dois certificados na cadeia e o `auth.` exige cuidado
extra com cabeçalhos encaminhados.

## 2.4 Anote para o próximo capítulo

Os três nomes vão para `APP_DOMAIN`, `API_DOMAIN` e `AUTH_DOMAIN` no `.env`. Escreva-os **sem**
`https://` e **sem** barra final — o compose monta as URLs a partir deles.

---

**Próximo:** [03 — Segredos e AWS](./03-segredos-e-aws.md)

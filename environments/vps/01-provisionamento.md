# 01 — Provisionamento e proteção da VPS

Objetivo: sair de uma máquina recém-criada para uma máquina pronta a receber o deploy, com a
superfície de rede fechada.

---

## 1.1 Criar a máquina

Qualquer provedor serve. O que importa:

- **Linux**, distribuição com suporte a longo prazo (Ubuntu LTS ou Debian estável).
- **4 GB de RAM ou mais.** Com 2 GB o Keycloak e o Postgres brigam por memória e o sistema fica
  instável sob carga.
- **40 GB de disco.** O banco cresce devagar; quem ocupa espaço são as imagens e os backups locais.
- Anote o **IP público** — ele é usado no capítulo 02.

## 1.2 Criar um usuário sem privilégio

Trabalhar como `root` faz com que qualquer descuido seja irreversível.

```bash
adduser app
usermod -aG sudo app
```

Daqui em diante, tudo é feito como `app`.

## 1.3 Fechar o acesso remoto

**Primeiro copie sua chave, depois desabilite a senha.** Invertendo a ordem, você se tranca do lado
de fora.

```bash
# Da SUA máquina, não da VPS:
ssh-copy-id app@SEU_IP

# Confirme que entra sem digitar senha ANTES de seguir:
ssh app@SEU_IP
```

Só então, na VPS, em `/etc/ssh/sshd_config`:

```
PermitRootLogin no
PasswordAuthentication no
```

```bash
sudo systemctl restart ssh
```

> Deixe a sessão atual aberta e teste uma segunda conexão numa janela nova. Se algo deu errado, você
> ainda tem a primeira sessão para desfazer.

## 1.4 Firewall

**Três portas, e nenhuma a mais.** Nenhum serviço além do Caddy publica porta — o banco, o Keycloak e
o backend só existem dentro da rede do Docker.

```bash
sudo ufw default deny incoming
sudo ufw default allow outgoing
sudo ufw allow 22/tcp      # SSH
sudo ufw allow 80/tcp      # HTTP — necessária para o desafio do Let's Encrypt
sudo ufw allow 443/tcp     # HTTPS
sudo ufw enable
sudo ufw status numbered
```

> **Atenção com Docker e UFW.** O Docker escreve regras direto no netfilter e, para portas
> publicadas, pode furar o UFW. No nosso caso só o Caddy publica porta, e ele publica justamente 80 e
> 443 — que já estão liberadas. Ainda assim, confirme de fora da máquina (item 1.7) em vez de confiar
> na configuração.

## 1.5 Proteção contra força bruta no SSH

```bash
sudo apt install -y fail2ban
sudo systemctl enable --now fail2ban
```

## 1.6 Docker

```bash
curl -fsSL https://get.docker.com | sudo sh
sudo usermod -aG docker app
```

Saia e entre de novo para o grupo valer. Confirme que o plugin de Compose veio junto:

```bash
docker compose version
```

**Swap**, se a máquina tiver 4 GB ou menos — evita que o kernel mate um contêiner sob pico:

```bash
sudo fallocate -l 2G /swapfile && sudo chmod 600 /swapfile
sudo mkswap /swapfile && sudo swapon /swapfile
echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab
```

## 1.7 Conferir a superfície exposta

**De outra máquina**, não da própria VPS:

```bash
nmap -Pn SEU_IP
```

Esperado: **apenas 22, 80 e 443**. Qualquer outra porta aberta — em especial **5432** — é um defeito
a corrigir antes de seguir.

## 1.8 Clonar o repositório

```bash
cd ~ && git clone git@github.com:SEU_USUARIO/app-project.git
```

A VPS precisa do repositório porque o `docker-compose.yml`, o `Caddyfile` e o arquivo de realm moram
nele. Ela **não** compila nada: as imagens vêm prontas do registro.

---

**Próximo:** [02 — DNS e domínio](./02-dns-e-dominio.md)

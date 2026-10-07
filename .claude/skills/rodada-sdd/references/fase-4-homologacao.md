# Fase 4 — Homologação

**Objetivo:** provar que a rodada funciona, no maior grau de automação que este projeto permite, e
entregar ao usuário um relatório do que sobrou para ele.

**Termina quando:** o usuário dá o OK e a §12 carrega o bloco de homologação com data e a tabela de
números medidos.

> **Por que este portão existe.** Uma suíte verde prova que o código faz o que os testes pedem, não
> que o sistema funciona. Defeito de integração — endpoint que responde 500 para toda entrada real,
> modal que abre atrás da barra lateral, filtro que só busca depois do Enter — aparece no primeiro
> minuto de uso e não aparece em nenhuma suíte, por maior que ela seja. Esta fase é o único ponto do
> ciclo em que alguém liga o sistema e olha.

> **Fontes desta fase**, e as seções são citadas por letra ao longo do texto:
> `references/checklist-de-fechamento.md` (§C, §D, §F), a §6 e a §6.4 do `roteiro.md` da rodada, e o
> `quickstart.md` dela.

---

## Camada 1 — Automatizada

Rode, na raiz de cada projeto, e **leia a saída**:

| Comando | Onde | O que fecha |
| :--- | :--- | :--- |
| `.\mvnw.cmd clean install` | `backend/` | reator completo, com testes. Nunca `-pl` isolado quando a mudança é transversal |
| `npm test` | `frontend/` | a suíte Vitest |
| `npm run build` | `frontend/` | o build de produção — pega o que o teste não pega |
| `npx prettier --write .` | `frontend/` | formatação, se tocou frontend |

Os testes de integração exigem **Docker rodando** (Testcontainers).

**Anote os números medidos**, por módulo. Eles vão para a tabela da §12 e **nunca são herdados de
documento anterior** — a exigência é do §D do checklist de fechamento, e existe porque documento
compilado que diverge da fonte é pior que ausência.

---

## Camada 2 — Prova por mutação

Para cada rede nova que a rodada criou (varredura, guarda, trava, validação): **quebre o alvo, rode
o teste, confirme que ele falha, restaure.**

Um teste que passa com e sem o defeito não é rede. O caso clássico é o teste de integração cujo
efeito observável é idêntico com e sem a guarda: ele fica verde nos dois mundos. A correção é atacar
a regra num teste unitário, sem o framework no caminho.

Registre no relatório **o que foi mutado e o que falhou**. Mutação não registrada não aconteceu.

---

## Camada 3 — Browser

### Subir o ambiente

```bash
cd environments/local && docker compose up -d        # postgres, keycloak, realm, localstack
cd backend  && ./mvnw clean install -DskipTests && ./mvnw -pl meduc-deploy spring-boot:run
cd frontend && npm start
```

Rode a API e a SPA **em background**. O Keycloak leva ~80s no primeiro boot — espere
`Listening on: http://0.0.0.0:8080` em `docker compose logs -f meduc-keycloak`; pollar antes disso dá
falso negativo.

⚠️ `docker compose down -v` é **proibido**: o volume é compartilhado com o `keycloak-db`, e as senhas
dos usuários existem só lá.

Portas: API `8080` · SPA `4200` · Keycloak `8443` (HTTP puro, apesar do número).
As credenciais da aplicação estão em `environments/local/.env.example`.

### Percorrer

Com o Chrome DevTools MCP: navegue, tire `take_snapshot`, preencha, clique e capture as telas do
`quickstart.md` da rodada. Ao fim de cada fluxo, leia `list_console_messages` e
`list_network_requests` — **erro de console e status inesperado são achado**, mesmo com a tela
parecendo certa.

Capture as telas que a fase 5 vai usar em `business/imagens/rf0NN/`.

### Limites honestos, e o que fazer com eles

| Limite | Consequência |
| :--- | :--- |
| Login é Authorization Code + PKCE, com `directAccessGrants` **desligado** no client do frontend | não há como obter token por usuário/senha fora do navegador. A automação precisa passar pela tela de login do Keycloak, ou o usuário faz o login e você segue na sessão dele |
| Empilhamento de modal não é observável em jsdom | conferir modal sobre a barra lateral é olho humano, sempre |
| Aparência e legibilidade | screenshot prova que renderizou, não que está bom |

Onde a automação não alcançar, **diga o que falta** e passe o item para a camada 4. Não relate como
verificado o que você não viu.

---

## Camada 4 — Manual

Monte a lista do que só o usuário pode conferir, a partir da §6.4 do roteiro e do `quickstart.md`.
Cada item precisa dizer **o que fazer** e **o que deve acontecer** — não "conferir a tela de
parceiros".

O §D do checklist de fechamento exige que o critério de aceitação seja exercitado **com dado real de
negócio, num navegador**. Isso é dele, não seu.

---

## Relatório e portão

Entregue o relatório no formato do §F do checklist de fechamento:

1. o que foi feito;
2. o que não foi, e por quê;
3. o que ficou frouxo, e em que rodada fecha;
4. as divergências encontradas entre documentação e código;
5. **os números medidos**, por módulo;
6. **o que está pendente da aprovação dele**, item a item.

**Se algo falhou, mostre a saída.** Relatório que omite o que não funcionou custa mais que a falha.

Então **pare**. Nenhum documento de negócio é escrito antes do OK.

### Recebido o OK

Marque a lista "Para considerar a rodada concluída" da §12 e acrescente o bloco:

```markdown
> ✅ **HOMOLOGADA pelo product owner em <AAAA-MM-DD>**, em tela, com dado real de negócio.
> <o que foi percorrido, e o que a rodada libera.>
```

Mais a tabela **Números medidos em \<data\>**, com a coluna "antes da rodada" e "depois" —
`specs/001-parceiro-e-solicitacao/roteiro.md` §12 é o modelo literal de forma.

Recomende `/compact` antes da fase 5. Você não tem ferramenta para compactar; recomende, e deixe a
decisão com ele.

### Se o usuário reprovar

Reprovação é achado, não fim de fase. Registre o defeito, volte para a fase 3 para corrigir, e
retorne aqui. O defeito encontrado na homologação vai para a §12 do roteiro, com o que ele revelou —
é assim que a próxima rodada aprende sem repetir a conta.

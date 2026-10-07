# Quickstart: validação manual da rodada 001

Os cenários que só uma pessoa consegue conferir. Cada item diz **o que fazer** e **o que deve
acontecer** — "conferir a tela de parceiros" não é gate.

## Preparar

```powershell
cd environments/local
docker compose up -d
# Espere `keycloak-config-cli ran in` em: docker compose logs -f app-keycloak-config
```

```powershell
cd backend
.\mvnw.cmd install -DskipTests
.\mvnw.cmd -pl app-deploy spring-boot:run
```

```powershell
cd frontend
npm install --legacy-peer-deps
npm start
```

API em `http://localhost:8080` · SPA em `http://localhost:4200` · Keycloak em `http://localhost:8443`
(HTTP puro, apesar do número). Credenciais em `environments/local/.env.example`.

⚠️ Nunca `docker compose down -v`: o volume guarda o banco da aplicação **e** o do Keycloak, e as
senhas dos usuários existem só no segundo.

## G1 — O ambiente sobe sem passo manual

- [ ] `docker compose up -d` e nada depois. O realm é aplicado pelo serviço one-shot.
- [ ] O backend sobe com `ddl-auto: validate` sem reclamar de schema.
- [ ] O log traz `Autorização sincronizada no Keycloak: 21 ações ... 3/3 grupos mapeados`.

## G2 — Cadastro de parceiro, ponta a ponta

- [ ] Entre com o usuário administrador. O menu mostra Parceiros e Solicitações.
- [ ] **Novo parceiro** → preencha os quatro passos → Confirmar. **Deve** aparecer o alerta de
      sucesso e voltar para a listagem, com o registro na lista.
- [ ] Reabra em **Visualizar**. **Nenhum** campo é editável, e o passo de Resumo oferece Fechar, não
      Confirmar.
- [ ] **Novo parceiro** com nome vazio e comissão `-1` → Confirmar. **Deve** exibir as **duas**
      mensagens de uma vez.
- [ ] Cadastre um segundo parceiro com o mesmo nome do primeiro. **Deve** ser recusado.
- [ ] Exclua o primeiro e cadastre outro com o mesmo nome. **Deve** ser aceito.

## G3 — O fluxo da solicitação

- [ ] **Nova solicitação** para o parceiro criado → Salvar. O badge diz **Rascunho**.
- [ ] Reabra. Aparece **Publicar**; não aparecem Iniciar nem Concluir.
- [ ] Publicar → badge **Aberta**; agora aparece **Iniciar**.
- [ ] Iniciar → badge **Em andamento**; agora aparece **Concluir**.
- [ ] Concluir → badge **Concluída**. Nenhum botão de fluxo aparece mais.
- [ ] Abra o parceiro na listagem: a coluna **Concluídas** subiu em um.
- [ ] Numa solicitação nova, tente **Cancelar** → confirme. Badge **Cancelada**, com o motivo
      exibido.

## G4 — O sigilo da comissão

Crie no console do Keycloak um usuário no grupo `OPERADOR` e outro no grupo `CONSULTA`.

- [ ] Com o **administrador**: a coluna Comissão aparece na listagem, e o campo aparece no wizard.
- [ ] Com o **operador**: a coluna **não** aparece, e o campo **não** aparece.
- [ ] Com o operador, edite o parceiro e salve. Volte com o administrador: **o valor continua lá**.
      Este é o item mais fácil de quebrar sem ninguém ver.
- [ ] Com o administrador, altere a comissão e abra o **histórico** do registro. A trilha diz que o
      campo mudou, e **não** mostra os valores.

## G5 — A autorização morde

- [ ] Com o perfil **CONSULTA**: o botão "Novo parceiro" não aparece, e as ações de editar e excluir
      não aparecem na tabela.
- [ ] Com o perfil **CONSULTA**, abra `/parceiros/novo` digitando na barra de endereço. **Deve** cair
      em `/acesso-negado`.
- [ ] Com o perfil **OPERADOR**: os botões de fluxo da solicitação aparecem; o de excluir parceiro,
      não.

## G6 — O que a automação não alcança

| Item | Por que é manual |
| :--- | :--- |
| Login pela tela do Keycloak | PKCE com `directAccessGrants` desligado: não há token por usuário/senha fora do navegador |
| Modal sobre a barra lateral | Empilhamento não é observável em jsdom |
| Legibilidade e aparência | Screenshot prova que renderizou, não que está bom |
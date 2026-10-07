# Métrica mínima de custo de agentes

O repositório não versiona telemetria bruta. Registre cada lote com:

```powershell
.\scripts\registrar-custo-agente.ps1 `
  -Agent codex -Model sol -Lote 001-A -Session resumed `
  -InputTokens 100000 -CachedTokens 85000 -OutputTokens 5000 `
  -BilledCost 0 -Outcome accepted -Gate "mvn test" -ReviewMinutes 10
```

A métrica de decisão é `BilledCost / accepted`, complementada por retrabalho e minutos de revisão.
O CSV padrão fica ignorado em `.agents/metrics/custos.csv`.

## Pré-voo obrigatório do Kilo

O `kilo.json` limita o projeto ao DeepSeek V4 Flash, mas não substitui controles mantidos pelo
cliente ou pelo provedor. Antes de cada sessão:

1. abra um worktree novo e atualizado; nunca retome worktree criado antes da configuração econômica;
2. use **Reset model** no seletor do agente e confirme `deepseek-v4-flash-0731`;
3. confirme no painel do Kilo que Memory, Codebase Indexing, Autocomplete e todos os MCPs estão
   desligados;
4. use uma chave OpenRouter exclusiva, sem segredo versionado, com allowlist somente do modelo acima
   e limite rígido de **US$ 2 por dia**;
5. inicie em `ask`; aprove explicitamente cada edição e comando de shell;
6. no primeiro uso após mudança de configuração, execute um smoke test somente leitura de no
   máximo cinco passos e aceite apenas custo inferior a **US$ 0,10**.

O seletor de modelo persistido, o estado da Memory e o teto da chave são externos ao repositório.
Uma sessão não começa enquanto esses três controles não forem conferidos. Registre o smoke test
com `registrar-custo-agente.ps1`; nunca registre chave, prompt integral ou identificador sensível.

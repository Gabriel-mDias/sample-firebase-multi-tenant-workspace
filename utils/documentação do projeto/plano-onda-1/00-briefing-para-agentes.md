# Briefing para agentes — onda 1

**Curto de propósito.** Só entra aqui o que já custou caro nesta onda: convenções que produziram
defeito silencioso, e armadilhas que a suíte não pega.

Leia antes de implementar. Não substitui os `AGENTS.md` — acrescenta o que é específico desta onda.

## O que esta onda aprendeu do jeito caro

| # | Lição | Como ela se manifestou |
| :-- | :--- | :--- |
| 1 | | |

## Convenções que mais escapam

[As que um executor delegado erra sem perceber, e que o portão não pega:]

- validação **acumulada**, nunca fail-fast;
- `ObjectUtil.isNullOrEmpty()` no lugar de `!= null` / `.isEmpty()`;
- ordem fixa dos métodos do service, `validate` sempre por último;
- `@NamedEntityGraph` pelo **sentido de navegação**, não por entidade;
- no frontend, `signal`/`computed`, `@if`/`@for`, `inject()`, e nada de hex no CSS.

## O que sempre entregar

1. o que foi feito;
2. o que não foi, e por quê;
3. o comando do portão, com a saída real das falhas;
4. divergências entre a documentação e o código — quando divergirem, o **código vence**, e reporte.
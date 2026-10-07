# 2. Especificações por requisito

O que o levantamento vira quando alguém o organiza. Aqui já há decisão: o que é um requisito, onde
ele começa e termina, e em que ordem de valor ele entra.

## Numeração

`RF001`, `RF002`, … — **ordem de levantamento**, não de entrega. Um requisito mantém o número dele
para sempre, mesmo que seja entregue por último.

⚠️ **`RF001` não é `specs/001`.** A pasta `specs/` numera **rodadas**, e a ordem lá é de entrega. Os
dois números divergem, e é normal — uma rodada pode fechar dois requisitos, e um requisito pode se
partir em duas rodadas.

## Estrutura

```
000-*.md              documentos transversais: modelo de dados consolidado, modelo de autorização,
                      dúvidas em aberto, déficits técnicos conhecidos
onda-1-<nome>/        RF001..RF00N — os requisitos da primeira leva
onda-2-<nome>/        RF00N+1..    — os da segunda
```

O agrupamento em ondas é **de valor**, não técnico: cada onda deve ser algo que o cliente reconheça
como uma entrega.

## Template

Use [`TEMPLATE-RF.md`](TEMPLATE-RF.md).

## O que fazer com o que ficou em aberto

Cada `⛔` num RF é uma decisão que falta ao cliente. Ele **não** é lacuna para o desenvolvedor
preencher: a fase 1 da `/rodada-sdd` transforma cada um numa pergunta obrigatória antes de abrir a
rodada. Um `⛔` que atravessa a fase 1 sem resposta reaparece na fase 3 como código escrito sobre
suposição.
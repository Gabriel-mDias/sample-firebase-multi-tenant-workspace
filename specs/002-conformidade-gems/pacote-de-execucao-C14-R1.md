# Pacote de execução — rodada 002, C14-R1

> Sublote corretivo aberto após rejeição nominal de D-P04. Sem delegação, commit ou push.
> Backend, ambientes e repositório Meduc ficam fora da fronteira.

Âncora: `evidencias/sample-c14-r1-anchor-porcelain.txt` no ledger de controle. Gate: residual
frontend zero, baseline vazio e gates completos do frontend verdes.

## Fronteira nominal

- Renomear somente os 57 títulos `it(...)` medidos nos 13 specs, sem mudar corpos.
- Trocar os dois `rgba(...)` de `acesso-negado.component.css` por `--meduc-shadow-card`.
- Criar `shared/parceiro/` com model, filtro, store de lookup e teste; remover o import
  `solicitacao → parceiro` e provar requisição/opções.
- Mover e renomear o state service para `form/parceiro-state.service.ts`, conservando os símbolos.
- Incorporar `ParceiroResponseModel` em `parceiro.model.ts` e remover o arquivo fora do padrão.
- Esvaziar `.gems-ai/conformance-baseline.json`; nenhuma exceção ou baseline novo é autorizado.
- Atualizar somente os artefatos de fechamento C14/T078 e o ledger externo correspondente.

## Gates

```powershell
cd frontend
node scripts/verify-conventions.mjs
npm ci
npm test
npm run build
npx prettier --check .
git -C .. diff --check
```

O backend completo não é repetido: o delta é exclusivamente frontend/documental e o gate C13
de 213 testes permanece a evidência vigente.

# Checklist de conteúdo — security

Esta checklist avalia completude documental; não atesta implementação. Revisão independente pendente.

- [ ] UID/provider/issuer permanente e ownership imutável
- [ ] Google somente pending elegível/rowlock/unique/histórico sem fallback
- [ ] scope/tenant compositeFK e toda cadeia vigente
- [ ] outboxversionada/restart/CAS da última versão/bloqueio local primeiro
- [ ] scanner/endpointsmecontext/contextclear/filtro stateless único
- [ ] revogação real/check de token emu/401403503
- [ ] generation antes de await/payload Alpha nunca Beta
- [ ] grant direto/indiretosubconjunto ator ∩ catálogo delegável

- [ ] FR-015: rota pública login não torna me/context públicos nem concede roles por claims/UI
- [ ] Login/recuperação negam enumeração de existência/elegibilidade e não logam senha/token/action link
- [ ] Retorno aceita apenas path interno permitido; rejeita externo/protocol-relative/loop
- [ ] Firebase/HTTP só fachada/stores; sem autocadastro/auto-admin; me/context validados antes do negócio
- [ ] Logout/novo UID/respostas antigas do login/reset não navegam ou restauram contexto revogado

Testes pendentes T033/T029 e homologação T032; [contrato frontend](../contracts/frontend.md), [diagnóstico](../development-status-20261008.md).

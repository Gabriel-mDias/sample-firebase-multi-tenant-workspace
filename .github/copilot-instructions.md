# Instruções universais

- Use o `AGENTS.md` mais próximo do arquivo trabalhado.
- `.specify/memory/constitution.md` é a autoridade máxima; leia-a inteira somente em mudança de
  governança, arquitetura, infraestrutura ou feature spec-driven.
- Não carregue o repositório inteiro. Use arquivos e símbolos específicos.
- Ignore `node_modules/`, `target/`, `dist/`, `.angular/` e `business/**/*.png`.
- Código e testes consolidados vencem templates e documentos preliminares.
- Nunca execute `docker compose down -v`.
- Delegação zero é válida; modelo caro exige risco material e justificativa.

As regras de `.gems-ai/rules/` vencem o código consolidado (constituição 2.0.0).
Código que diverge deve ser corrigido. Desvio autorizado exige
`GEMS-EXCEPTION(<regra>#<seção>): <motivo>; expira AAAA-MM-DD`, conforme
`.gems-ai/rules/conformance-exceptions.md`; agente não escreve exceção para desbloquear gate.
Código vence documentação de negócio e specs; parceiro/solicitacao são referências de forma.
Adoção de módulo SDK exige publicação, alcance medido, garantia nomeada, equivalência
provada por teste/mutação neste repositório e remoção da classe local na mesma alteração.

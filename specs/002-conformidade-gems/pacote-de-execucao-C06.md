# Pacote de Execução — Rodada 002, Lote C06

| Controle | Valor |
| :-- | :-- |
| Checkpoint | C06 — T058/T060/T061, após C05 verde em `d0703e3` |
| Sessão | `nova`; nenhum token anterior é reutilizado |
| Capacidade / esforço | Codex, alto; a rede de conformidade e o reator completo exigem julgamento e leitura do resultado |
| Admissão 1–5 | sim / sim / sim / sim / **não**: quatro arquivos e um gate integral não amortizam autoria, revisão e repetição do gate |
| Decisão | **sem delegação**; o orquestrador executa e verifica |
| Âncora | `C:/Users/gabri/AppData/Local/Temp/sample-c06-pre-dispatch-20260916.porcelain` |

## 1. Fronteira fechada do lote

Fecha a tolerância residual da rede Java para zero, prova cada rede por mutação e executa o reator completo. Não altera regras de domínio, frontend, Docker, SDKs nem os blocos C11/C12 bloqueados.

### READ

- `AGENTS.md`, `backend/AGENTS.md`, `.gems-ai/rules/backend-java/testing.md` — instruções aplicáveis.
- `utils/documentação do projeto/plano-onda-1/00-briefing-para-agentes.md` — briefing da onda.
- `specs/002-conformidade-gems/tasks.md`, `estado-lote-C05.md`, este pacote — contrato e retomada.
- `backend/app-deploy/src/test/java/br/com/gems/app/convencoes/ConvencoesArquiteturaTest.java`, `ConvencoesFonteTest.java`, `RegrasDeFonte.java`, `RelatorioDeConformidade.java` — redes C06.
- `backend/app-deploy/src/test/resources/archunit-baseline.txt`, `checkstyle-baseline.txt`, `backend/config/checkstyle/suppressions.xml` — tolerâncias que devem permanecer vazias.

### EDIT / CREATE

| # | Ação | Arquivo |
| :-- | :--- | :-- |
| T060 | EDIT | `backend/app-deploy/src/test/java/br/com/gems/app/convencoes/ConvencoesArquiteturaTest.java` |
| T060 | EDIT | `backend/app-deploy/src/test/java/br/com/gems/app/convencoes/ConvencoesFonteTest.java` |
| T060 | EDIT | `backend/app-deploy/src/test/java/br/com/gems/app/convencoes/RegrasDeFonte.java` |
| T060 | EDIT | `backend/app-deploy/src/test/java/br/com/gems/app/convencoes/RelatorioDeConformidade.java` |
| C06 | EDIT | `specs/002-conformidade-gems/tasks.md` |
| C06 | EDIT | `specs/002-conformidade-gems/roteiro.md` |

## 3. Arquivos de referência

| Para escrever | Copie de |
| :-- | :-- |
| Relato de violação | `RelatorioDeConformidade.java` — caminho, regra, violação e esperado são obrigatórios |
| Regra de fonte | `RegrasDeFonte.java` — JavaParser e `RelatorioDeConformidade`, sem regex |

## 4. Regras aplicáveis

1. O baseline e as supressões terminam sem entrada de dívida; não crie exceção GEMS.
2. Cada rede alterada recebe mutação temporária, falha observada e restauração por edição inversa.
3. Não altere comportamento de domínio, nem paths fora da tabela.
4. Não comite antes de revisar o diff, rodar o verificador e repetir o gate.

## 5. Portão

Em `backend`:

```powershell
.\mvnw.cmd clean install
```

Exit 0, `BUILD SUCCESS`, zero dívida Java e todos os testes executados.

## 6. Relatório

Registrar as quatro mutações, restaurações, totais do reator e toda divergência. Falha mantém C06 em recuperação.

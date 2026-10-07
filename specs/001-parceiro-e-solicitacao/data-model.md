# Data Model: Parceiro e Solicitação

## Entidades

### PARCEIRO

| Coluna | Tipo | Nulo | Observação |
| :--- | :--- | :--- | :--- |
| `ID_PARCEIRO` | UUID | não | PK, `defaultValueComputed` |
| `NM_PARCEIRO` | VARCHAR(255) | não | Sem `UNIQUE` — ver R5 do research |
| `DS_PARCEIRO` | VARCHAR(4000) | sim | |
| `VL_COMISSAO` | NUMERIC(5,2) | sim | `@CampoSigiloso` |
| `NR_SOLICITACOES_CONCLUIDAS` | INTEGER | não | `DEFAULT 0`; mantido pelo listener |
| `ID_PESSOA` | UUID | não | FK → `PESSOA` |
| `ID_CONTATO` | UUID | não | FK → `CONTATO` |
| `ID_ENDERECO` | UUID | não | FK → `ENDERECO` |
| `DT_CRIACAO` | TIMESTAMP | não | de `EntidadeAuditavel` |
| `DT_EXCLUSAO` | TIMESTAMP | sim | soft delete |

Índices: `IDX_PARCEIRO_NOME`, `IDX_PARCEIRO_PESSOA`.

### SOLICITACAO

| Coluna | Tipo | Nulo | Observação |
| :--- | :--- | :--- | :--- |
| `ID_SOLICITACAO` | UUID | não | PK |
| `NM_SOLICITACAO` | VARCHAR(255) | não | o título |
| `DS_SOLICITACAO` | VARCHAR(4000) | sim | |
| `CD_SITUACAO` | VARCHAR(20) | não | nome do enum, **nunca ordinal** |
| `DS_MOTIVO_CANCELAMENTO` | VARCHAR(4000) | sim | só depois do cancelamento |
| `DT_CONCLUSAO` | TIMESTAMP | sim | só depois da conclusão |
| `ID_PARCEIRO` | UUID | não | FK → `PARCEIRO` |

Índices: `IDX_SOLICITACAO_SITUACAO`, `IDX_SOLICITACAO_PARCEIRO`.

**`CD_SITUACAO` é o nome do enum e não o ordinal.** Ordinal quebra em silêncio quando alguém
acrescenta uma constante no meio: o banco passa a dizer outra coisa sem que nada falhe.

### ETAPA_SOLICITACAO

| Coluna | Tipo | Nulo | Observação |
| :--- | :--- | :--- | :--- |
| `ID_ETAPA_SOLICITACAO` | UUID | não | PK |
| `NM_ETAPA` | VARCHAR(255) | não | |
| `NR_ORDEM` | INTEGER | não | **coluna**, não posição na lista |
| `FL_CONCLUIDA` | BOOLEAN | não | `DEFAULT false` |
| `DT_CONCLUSAO` | TIMESTAMP | sim | |
| `ID_SOLICITACAO` | UUID | não | FK → `SOLICITACAO` |

Índice composto `IDX_ETAPA_SOLICITACAO_ORDEM` em `(ID_SOLICITACAO, NR_ORDEM)`: a consulta é sempre
"etapas desta solicitação, na ordem".

## Transições de estado

```
RASCUNHO ──abrir──▶ ABERTA ──iniciar──▶ EM_ANDAMENTO ──concluir──▶ CONCLUIDA
    │                  │                     │
    └──────────────────┴─────────cancelar────┴─────────────────────▶ CANCELADA
```

| Origem | Destinos possíveis |
| :--- | :--- |
| `RASCUNHO` | `ABERTA`, `CANCELADA` |
| `ABERTA` | `EM_ANDAMENTO`, `CANCELADA` |
| `EM_ANDAMENTO` | `CONCLUIDA`, `CANCELADA` |
| `CONCLUIDA` | — terminal |
| `CANCELADA` | — terminal |

A tabela vive no enum `SituacaoSolicitacao`, e o service tem um único ponto que a consulta.

## Validações

| # | Regra | Onde |
| :-- | :--- | :--- |
| V1 | Nome do parceiro obrigatório | `ParceiroService.validate` |
| V2 | Nome único entre ativos | idem, via `existsByNomeIgnoreCase` |
| V3 | Comissão não negativa | idem |
| V4 | Identidade civil válida | `PessoaService.validar` |
| V5 | Contato válido | `ContatoService.validar` |
| V6 | Endereço válido; UF e CEP só no Brasil | `EnderecoService.validar` |
| V7 | Título da solicitação obrigatório | `SolicitacaoService.validate` |
| V8 | Parceiro existente na abertura | idem |
| V9 | Transição permitida pela origem | `SituacaoSolicitacao.permiteTransicaoPara` |
| V10 | Sem etapa pendente ao concluir | `SolicitacaoService.concluir` |
| V11 | Motivo obrigatório no cancelamento | `SolicitacaoService.cancelar` |
| V12 | Exclusão só de rascunho ou cancelada | `SolicitacaoService.delete` |

## Projeções

`ParceiroResponseDTO` e `SolicitacaoResponseDTO` são construídos por `SELECT new` na HQL. **A ordem
dos argumentos do construtor é o contrato**: trocar dois compila e falha em runtime. Os dois andam
juntos, sempre.
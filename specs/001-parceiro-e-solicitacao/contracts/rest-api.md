# Contrato REST — rodada 001

Toda rota exige JWT válido. Erro de negócio sai **400** com corpo do handler da SDK; ação negada sai
**403**; falha do provedor de identidade sai **502** — nunca 500, para que uma queda do Keycloak não
se apresente como defeito da aplicação.

## Parceiros

### `GET /api/parceiros/{id}` — `CONSULTAR_PARCEIRO`

**200** com o parceiro completo. `comissao` vem `null` para quem não tem `VISUALIZAR_COMISSAO`.

```json
{
  "id": "uuid",
  "nome": "Parceiro Alfa",
  "descricao": "texto livre",
  "pessoa": { "id": "uuid", "primeiroNome": "Ana", "sobrenome": "Silva", "cpf": "12345678909",
              "rg": "MG1234567", "dataNascimento": "1990-01-01", "sexo": "FEMININO" },
  "contato": { "id": "uuid", "email": "ana@exemplo.com", "telefone": "+55 27 99999-0000" },
  "endereco": { "id": "uuid", "pais": { "id": "uuid", "nome": "Brasil" },
                "estado": { "id": "uuid", "nome": "Espírito Santo", "siglaEstado": "ES" },
                "cep": "29000-000", "cidade": "Vitória", "bairro": "Centro",
                "logradouro": "Rua X", "numero": "100", "complemento": null },
  "comissao": 12.50,
  "solicitacoesConcluidas": 3
}
```

**400** — parceiro inexistente ou excluído.

### `POST /api/parceiros/search` — `CONSULTAR_PARCEIRO`

Filtro no corpo, paginação em query params (`page`, `size`, `sort`). Campo ausente não filtra.

```json
{ "nome": "alfa", "cpf": "12345678909", "estadoId": "uuid" }
```

**200** com `Page<ParceiroResponseDTO>`. `comissao` é apagada **linha a linha** para quem não tem a
ação — filtrar só no `findById` deixaria o valor vazando pela listagem.

Ordenação aceita: `nome`, `cpf`, `email`, `estadoSigla`, `solicitacoesConcluidas`. Propriedade
desconhecida é ignorada e cai no padrão `nome,asc`.

### `POST /api/parceiros` — `INSERIR_PARCEIRO`

Corpo igual ao do `GET`, sem `id` e sem `solicitacoesConcluidas` (somente leitura). **201** com o
recurso criado. `comissao` é **ignorada** para quem não tem `VISUALIZAR_COMISSAO`.

### `PUT /api/parceiros/{id}` — `ALTERAR_PARCEIRO`

**200**. O `id` da rota é a autoridade; o do corpo é ignorado. Omitir `comissao` **não** a apaga
quando o chamador não pode vê-la.

### `DELETE /api/parceiros/{id}` — `EXCLUIR_PARCEIRO`

**204**. Exclusão lógica do parceiro e dos agregados dele.

---

## Solicitações

### `GET /api/solicitacoes/{id}` — `CONSULTAR_SOLICITACAO`

```json
{
  "id": "uuid",
  "titulo": "Revisar contrato",
  "descricao": "texto livre",
  "parceiroId": "uuid",
  "parceiroNome": "Parceiro Alfa",
  "situacao": "EM_ANDAMENTO",
  "motivoCancelamento": null,
  "dataConclusao": null,
  "etapas": [ { "id": "uuid", "nome": "Coletar documentos", "ordem": 1, "concluida": true,
                "dataConclusao": "2026-09-05T10:00:00" } ]
}
```

### `POST /api/solicitacoes/search` — `CONSULTAR_SOLICITACAO`

```json
{ "titulo": "revisar", "situacao": "ABERTA", "parceiroId": "uuid" }
```

Ordenação aceita: `titulo`, `situacao`, `parceiroNome`, `dataCriacao`, `dataConclusao`. Padrão
`dataCriacao,desc`.

### `POST /api/solicitacoes` — `INSERIR_SOLICITACAO`

**201**. **`situacao` no corpo é ignorada**: toda solicitação nasce `RASCUNHO`.

### `PUT /api/solicitacoes/{id}` — `ALTERAR_SOLICITACAO`

**200**. Altera **apenas** título e descrição. `situacao` no corpo é ignorada — não é erro, é campo
somente leitura. **400** se a solicitação estiver em situação terminal.

### `DELETE /api/solicitacoes/{id}` — `EXCLUIR_SOLICITACAO`

**204**. **400** se estiver em andamento ou concluída.

### As quatro transições

| Rota | Ação | De | Para | 400 quando |
| :--- | :--- | :--- | :--- | :--- |
| `POST /{id}/abrir` | `ABRIR_SOLICITACAO` | `RASCUNHO` | `ABERTA` | origem não permite |
| `POST /{id}/iniciar` | `INICIAR_SOLICITACAO` | `ABERTA` | `EM_ANDAMENTO` | origem não permite |
| `POST /{id}/concluir` | `CONCLUIR_SOLICITACAO` | `EM_ANDAMENTO` | `CONCLUIDA` | origem não permite, **ou** há etapa pendente |
| `POST /{id}/cancelar` | `CANCELAR_SOLICITACAO` | qualquer não terminal | `CANCELADA` | origem é terminal, **ou** motivo ausente |

Só o cancelamento tem corpo:

```json
{ "motivo": "Cliente desistiu." }
```

Todas devolvem **200** com a solicitação atualizada. **Esta é a razão de existirem quatro endpoints e
não um `PUT`**: cada transição tem a sua permissão, e um campo no corpo não teria.

---

## Endpoints públicos

Apenas `OPTIONS`, `/actuator/health`, `/api/auth/**` e o que estiver anotado com `@PublicEndpoint`.
`/actuator/**` como um todo **não** é público. A documentação OpenAPI é pública só enquanto
`SPRINGDOC_ENABLED` permitir, e fica desligada em ambiente exposto.
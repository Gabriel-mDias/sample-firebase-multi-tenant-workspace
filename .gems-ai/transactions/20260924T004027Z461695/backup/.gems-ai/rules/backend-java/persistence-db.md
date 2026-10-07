---
stack: backend-java
module: gems-jpa
severity: mandatory
see-also: [architecture.md, repositories.md]
origem: local
precedencia: local-vence
versao: 2.1.0
---

# Persistência & Banco de Dados — PostgreSQL + Liquibase

> **TL;DR:**
> - Prefixos obrigatórios: `ID_` (UUID), `CD_` (enum/cód), `DS_` (varchar≤4k), `TX_` (text longo), `VL_` (numérico), `DT_` (data).
> - Banco: PostgreSQL + Liquibase (`changelog-master.xml` para `administracao`, `changelog-tenant-master.xml` para `tenant_<alias>`). Tabelas em MAIÚSCULO, schema explícito.
> - Toda tabela inclui `DT_CRIACAO` e `DT_EXCLUSAO`. Soft delete via `@SQLDelete + @SQLRestriction` — nunca DELETE físico.
> - UUID: `ID_X UUID PRIMARY KEY` **sem default no banco** — quem gera é o Hibernate (`@GeneratedValue(strategy = GenerationType.UUID)`).

## 1. Tecnologias

- **Banco:** PostgreSQL.
- **Versionamento:** Liquibase. `changelog-master.xml` inclui um `changelog-<dominio>.xml` por domínio do schema `administracao`; `changelog-tenant-master.xml` é aplicado a cada `tenant_<alias>` pelo provisionamento. Changeset em **SQL puro** dentro de `<sql><![CDATA[ ... ]]></sql>`, id `NNN-SSS-<verbo>-<tabela>` (rodada e sequência), `author="meduc"`.

---

## 2. Prefixos de Colunas (Obrigatórios)

| Prefixo | Significado | Tipo Recomendado | Exemplo |
| :--- | :--- | :--- | :--- |
| **ID_** | Identificadores e FKs | UUID | `ID_ENTIDADE`, `ID_RELACIONADO` |
| **CD_** | Códigos, Enums, Documentos | VARCHAR / Enum | `CD_TIPO`, `CD_SITUACAO` |
| **DS_** | Descritivos e Strings (até 4000 chars) | VARCHAR(4000) | `DS_NOME`, `DS_EMAIL` |
| **TX_** | Textos longos (>4000 chars) | TEXT | `TX_OBSERVACAO`, `TX_DESCRICAO` |
| **VL_** | Valores numéricos e monetários | NUMERIC(10,2) / INT | `VL_VALOR`, `VL_QUANTIDADE` |
| **DT_** | Datas e Timestamps | DATE / TIMESTAMPTZ | `DT_CRIACAO`, `DT_EXCLUSAO`, `DT_NASCIMENTO` |

---

## 3. Tipagem de Dados (PostgreSQL)

| Caso | Tipo SQL |
| :--- | :--- |
| Strings curtas | `VARCHAR(N)` |
| Textos longos | `TEXT` |
| Monetário/Decimal | `NUMERIC(10,2)` |
| Inteiro | `INT4` (Integer) ou `INT8` (Long) |
| Identificadores | `UUID` |
| Data sem hora (`LocalDate`) | `DATE` |
| Instante (`LocalDateTime`) | `TIMESTAMPTZ` |

---

## 4. Liquibase — Estrutura de ChangeSet

A forma real do projeto é SQL puro — `changelog-organizacao.xml` é o exemplo de referência:

```xml
<changeSet id="008-001-create-entidade" author="meduc">
    <sql><![CDATA[
        CREATE TABLE administracao.ENTIDADE (
          ID_ENTIDADE UUID PRIMARY KEY, DS_NOME VARCHAR(255) NOT NULL,
          CD_TIPO VARCHAR(32) NOT NULL, DT_CRIACAO TIMESTAMPTZ NOT NULL,
          DT_EXCLUSAO TIMESTAMPTZ,
          CONSTRAINT CK_ENTIDADE_TIPO CHECK (CD_TIPO IN ('A','B'))
        );
    ]]></sql>
</changeSet>
```

- `ID_` sem `DEFAULT`: o UUID vem do Hibernate.
- `DT_CRIACAO TIMESTAMPTZ NOT NULL` e `DT_EXCLUSAO TIMESTAMPTZ` em toda tabela; `DT_ATUALIZACAO` e `NR_VERSAO` quando a entidade estende `EntidadeAuditavel`.
- `ddl-auto: validate` confere a entidade contra a tabela no subir — coluna a mais ou tipo errado derruba o contexto.

---

## 5. Convenções de Tabelas

- Nomes em **MAIÚSCULO** (SNAKE_CASE): `ORGANIZACAO`, `INSTITUICAO`.
- Toda tabela inclui `DT_CRIACAO` e `DT_EXCLUSAO`.
- Nenhuma tabela usa DELETE físico — soft delete apenas.

---

## 6. Soft Delete na Entidade JPA

O soft delete é gerenciado automaticamente via Hibernate — nunca implemente manualmente:

```java
@SQLDelete(sql = "UPDATE NOME_TABELA SET DT_EXCLUSAO = NOW() WHERE ID_ENTIDADE = ?")
@SQLRestriction("DT_EXCLUSAO IS NULL")
public class {Entidade} {
    @Column(name = "DT_EXCLUSAO")
    private LocalDateTime dataExclusao;
}
```

- `@SQLDelete` substitui o DELETE físico por um UPDATE na coluna `DT_EXCLUSAO`.
- `@SQLRestriction` filtra automaticamente registros excluídos em todas as queries.
- Na Service, basta chamar `repository.deleteById(id)` — o soft delete é transparente.

---

## Anti-Patterns (NUNCA)

| Proibido | Correto |
| :--- | :--- |
| `nome VARCHAR(100)` sem prefixo | `DS_NOME VARCHAR(100)` |
| `id BIGINT AUTO_INCREMENT` ou `DEFAULT gen_random_uuid()` | `ID_ENTIDADE UUID PRIMARY KEY`, gerado pelo Hibernate |
| `DELETE FROM TB_ENTIDADE WHERE ID_ENTIDADE = ?` | Soft delete via `@SQLDelete` + `@SQLRestriction` |
| `VARCHAR(10000)` para textos longos | `TEXT` (prefixo `TX_`) para conteúdo longo |
| Tabela sem `DT_CRIACAO` e `DT_EXCLUSAO` | Toda tabela inclui as duas colunas de auditoria |

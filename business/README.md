# Apresentação do projeto

**Documento para o cliente e para a análise de requisitos** · Atualizado em 5 de setembro de 2026

> **Como este documento se distingue dos outros.** Aqui não há nome de classe, endpoint nem tabela.
> O leitor é o cliente e o analista de requisitos, e uma decisão técnica só aparece quando ela **muda
> o que o cliente pode fazer**. A pasta `specs/` fala com quem constrói; esta fala com quem contrata.

---

## Em uma frase

O sistema leva a operação de planilhas, pastas de e-mail e conversas soltas para um lugar só — onde
o cadastro, o que precisa ser feito e o andamento ficam ligados entre si, visíveis para quem precisa
e invisíveis para quem não precisa.

---

## Como ler esta pasta

Cada requisito entregue tem um arquivo `RF0NN-<slug>.md`, e todos seguem **as mesmas sete seções, na
mesma ordem** — é o que os torna comparáveis entre si:

1. **A dor que resolve** — o que acontecia antes;
2. **Resumo do requisito** — o que foi construído;
3. **Quem usa** — a matriz de perfis;
4. **O que ele controla** — as informações que a área gere;
5. **Como funciona** — o passo a passo, ilustrado com as telas reais;
6. **O que ele ainda não faz** — a fronteira, para não gerar expectativa errada;
7. **De onde vem** — a origem no levantamento e a rodada que entregou.

As imagens ficam em `imagens/rf0NN/`, com nomes que descrevem a tela.

Um documento escrito **antes** da entrega descreve o desenho proposto. A fase 5 da rodada o converte
para o que **foi** construído — a diferença entre os dois é justamente o que a homologação encontrou.

---

## Como o sistema está estruturado hoje

| Área | O que faz | Estado |
| :--- | :--- | :--- |
| **Parceiros** | Cadastro de referência: identidade, contato, endereço e a comissão | ✅ entregue |
| **Solicitações** | O que precisa ser feito, e por onde já passou | ✅ entregue |
| **Acessos e permissões** | Quem pode o quê, por perfil | ✅ entregue |
| **Trilha de alterações** | Quem mudou o quê, e quando | ✅ entregue (mecanismo; a tela de consulta ainda não) |

## O que ainda não existe

- Tela de consulta da trilha de alterações — o registro acontece, a listagem não.
- Anexo de arquivo. A infraestrutura existe e está fechada.
- Cadastro das etapas de uma solicitação pela tela; hoje elas só são exibidas.
- Painéis e indicadores.

---

## Quem usa, e o que cada um alcança

| Perfil | Parceiros | Solicitações | Comissão | Trilha |
| :--- | :--- | :--- | :--- | :--- |
| **Administrador** | tudo | tudo | vê | vê |
| **Operador** | consulta, cadastra, altera | tudo, incluindo o fluxo | **não vê** | não vê |
| **Consulta** | só lê | só lê | **não vê** | não vê |

Duas coisas dessa tabela merecem atenção:

**A comissão some para quem não pode vê-la — de verdade.** Não é um campo escondido na tela: o
sistema não entrega o valor a quem não tem a permissão, por caminho nenhum. Nem pela listagem, nem
por quem consultar o sistema por fora, nem pela trilha de alterações — que registra **que** o valor
mudou, sem dizer para quanto.

**Quem pode editar não necessariamente pode concluir.** Cada passagem de uma solicitação — publicar,
iniciar, concluir, cancelar — é uma permissão separada. Corrigir o título de uma solicitação e
encerrá-la são atos diferentes, e o sistema trata como tais.

---

## Os requisitos entregues

| Requisito | O que faz |
| :--- | :--- |
| [**RF001** Parceiros e Solicitações](RF001-parceiro-e-solicitacao.md) | O cadastro de parceiros, o fluxo de solicitações e a base de permissões e trilha sobre a qual tudo o mais é construído |

/**
 * Os perfis de atuação — espelho de `PerfilAtuacao.java` do `app-model`.
 *
 * <h2>Por que existe uma cópia no frontend</h2>
 * O backend é a fonte de verdade e a expõe num endpoint de opções. Esse endpoint, porém, exige uma
 * ação de consulta que nem todo perfil tem — e telas que apenas **exibem** o rótulo de um perfil
 * são abertas por gente sem essa ação. Buscar a lista de lá daria 403 numa tela que a pessoa tem
 * permissão de ver.
 *
 * <p>Por isso a exibição de rótulo usa esta tabela local, e o **cadastro** de usuário continua
 * consultando o backend. O que não pode acontecer é a lista existir duplicada dentro do próprio
 * frontend, em mais de uma feature: é assim que o mesmo perfil passa a aparecer escrito de dois
 * jeitos em duas telas.
 *
 * <p>O spec ao lado fixa os pares contra o enum do backend. Perfil novo entra nos dois lugares, e o
 * teste é o que avisa quando entrar em só um.
 */
export interface PerfilAtuacaoOpcao {
  value: string;
  label: string;
}

export const PERFIL_ATUACAO_OPTIONS: PerfilAtuacaoOpcao[] = [
  { value: 'ADMINISTRADOR', label: 'Administrador' },
  { value: 'OPERADOR', label: 'Operador' },
  { value: 'CONSULTA', label: 'Consulta' },
];

/** Rótulo do perfil, ou string vazia quando não houver perfil atribuído. */
export function perfilAtuacaoLabel(valor?: string | null): string {
  return PERFIL_ATUACAO_OPTIONS.find((opcao) => opcao.value === valor)?.label ?? '';
}

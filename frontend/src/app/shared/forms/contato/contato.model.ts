/**
 * Espelha `ContatoDTO` do backend.
 *
 * <p>Um dono só, em `shared/`: `Parceiro`, `Usuário` e `Parceiro` gravam o **mesmo** `Contato`
 * no backend. Duplicar a interface dentro de cada feature é o defeito que este arquivo evita:
 * duas cópias byte a byte divergem no dia em que só uma é editada.
 * três estratégias simultâneas para uma estrutura só.
 */
export interface ContatoModel {
  id?: string;
  email: string;
  telefone: string;
}

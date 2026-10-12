/** Operação de uma sessão anterior: o consumidor encerra sem alterar UI ou navegação. */
export class StaleSessionError extends Error {
  constructor() {
    super('stale-session');
  }
}

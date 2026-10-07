import { GemsTableLabels } from '@gabriel-mdias/angular-gems-sdk';

/** Textos pt-BR para a paginação do `gems-table`. Modelo compartilhado entre as listas. */
export const GEMS_TABLE_LABELS_PT_BR: GemsTableLabels = {
  showing: (from, to, total) => `Mostrando ${from} até ${to} de ${total} registros`,
  perPage: 'Por página:',
  page: (current, total) => `Página ${current} de ${total}`,
  previous: 'Anterior',
  next: 'Próxima',
};

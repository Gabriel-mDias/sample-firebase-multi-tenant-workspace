import { EstadoModel } from '../../../core/localidade/models/estado.model';
import { PaisModel } from '../../../core/localidade/models/pais.model';

/**
 * Espelha `EnderecoDTO` do backend.
 *
 * <p>`pais` é **sempre obrigatório**; `estado` e `cep` só quando o país é o Brasil. Antes
 * da feature 009 o país era derivado de `estado.pais` — o que tornava impossível um endereço sem
 * estado, e portanto impossível um endereço fora do Brasil. `numero` e `complemento` são os únicos
 * outros campos opcionais.
 *
 * <p>Dono único em `shared/`, pelo mesmo motivo do {@link ContatoModel}.
 */
export interface EnderecoModel {
  id?: string;
  pais: PaisModel;
  estado?: EstadoModel;
  cep?: string;
  cidade: string;
  bairro: string;
  logradouro: string;
  numero?: string;
  complemento?: string;
}

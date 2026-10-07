package br.com.gems.app.normalizacao.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import br.com.gems.utils.ObjectUtil;

/**
 * Resposta do ViaCEP, reduzida ao que o sistema consome.
 *
 * <p>
 * <b>A armadilha do provedor está no campo {@code erro}.</b> Para um CEP que não existe, o ViaCEP
 * responde <b>HTTP 200</b> com o corpo {@code {"erro": "true"}} — não 404. Tratar a resposta apenas
 * pelo status devolveria "sucesso" com um endereço inteiramente vazio, e a tela preencheria os
 * campos com nada. Daí {@link #naoEncontrado()} existir e ser checado sempre.
 *
 * <p>
 * O tipo do {@code erro} é {@link String} de propósito: o ViaCEP já devolveu tanto {@code "true"}
 * quanto {@code true} em versões diferentes, e desserializar como {@code boolean} quebraria na
 * primeira mudança.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ViaCepResponse(String cep,
        String logradouro,
        String complemento,
        String bairro,
        String localidade,
        String uf,
        String erro) {

    public boolean naoEncontrado() {
        return ObjectUtil.isNotNullAndNotEmpty( erro ) && !"false".equalsIgnoreCase( erro );
    }

}

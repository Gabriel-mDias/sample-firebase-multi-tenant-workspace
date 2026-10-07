package br.com.gems.app.commons.exception;

import br.com.gems.exception.exception.ExternalServiceException;

/**
 * Falha de um provedor externo do qual o sistema depende, mas que não controla.
 *
 * <p>
 * Existe para separar "o serviço de fora caiu" de "o sistema tem um defeito" e de "quem chamou
 * mandou algo errado". Sem ela, uma indisponibilidade do ViaCEP chegaria ao cliente como <b>500</b>,
 * indistinguível de um bug do sistema — e a monitoração acusaria erro da aplicação onde não há.
 *
 * <p>
 * Traduzida para <b>502 Bad Gateway</b> pelo envelope compartilhado da GEMS SDK.
 */
public class ProvedorExternoException extends ExternalServiceException {

    public ProvedorExternoException( String message, Throwable cause ) {
        super( "Provedor externo", message, cause );
    }

}

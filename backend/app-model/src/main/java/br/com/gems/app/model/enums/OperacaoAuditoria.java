package br.com.gems.app.model.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * A natureza da operação registrada na trilha de auditoria.
 *
 * <p>
 * Persistida na coluna {@code CD_OPERACAO} por nome ({@code @Enumerated(STRING)}), como todo
 * enum de domínio do projeto.
 *
 * @see br.com.gems.app.model.entity.auditoria.RegistroAuditoria
 */
@Getter
@RequiredArgsConstructor
public enum OperacaoAuditoria {

    /** O registro passou a existir. */
    INSERCAO("INSERT", "Inserção"),

    /** Um ou mais campos mudaram. */
    ALTERACAO("UPDATE", "Alteração"),

    /** Exclusão lógica do registro de negócio. */
    EXCLUSAO("DELETE", "Exclusão");

    private final String codigoSdk;
    private final String descricao;

    /** Converte o contrato público em português para os códigos persistidos pela SDK. */
    @Converter
    public static class PersistenciaConverter implements AttributeConverter<OperacaoAuditoria, String> {

        @Override
        public String convertToDatabaseColumn( OperacaoAuditoria operacao ) {
            return operacao == null ? null : operacao.getCodigoSdk();
        }

        @Override
        public OperacaoAuditoria convertToEntityAttribute( String codigo ) {
            if ( codigo == null ) {
                return null;
            }

            for ( var operacao : values() ) {
                if ( operacao.getCodigoSdk().equals( codigo ) ) {
                    return operacao;
                }
            }

            throw new IllegalArgumentException( "Operação de auditoria desconhecida: " + codigo );
        }
    }

}

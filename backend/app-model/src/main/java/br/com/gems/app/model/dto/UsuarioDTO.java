package br.com.gems.app.model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * Dados de acesso de um usuário.
 *
 * <p>
 * {@code senha} é campo de <b>entrada apenas</b>: chega no cadastro, segue até o Keycloak e nunca
 * é persistida. Toda leitura devolve {@code null} nele, zerado explicitamente em
 * {@code UsuarioService.toDTO}.
 *
 * @implNote Seria natural marcar {@code senha} com {@code @JsonProperty(access = WRITE_ONLY)}, mas
 *           {@code app-model} declara apenas {@code hibernate-core} — não há Jackson no classpath deste
 *           módulo. O zeramento é manual e coberto por teste.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UsuarioDTO {

    private UUID id;
    private String login;
    private String senha;
    private Boolean ativo;

    /**
     * Representação textual <b>sem a senha</b>.
     *
     * <p>
     * Existe por segurança, não por conveniência de depuração. O {@code BaseController} guarda o
     * DTO inteiro num atributo da requisição em todo POST e PUT ({@code setBodyToExceptionLog}), e o
     * {@code UsuarioDTO} pode viajar aninhado num DTO maior — então a senha em texto claro fica ao
     * alcance de qualquer código que venha a serializar o corpo da requisição para log.
     *
     * <p>
     * Hoje nada a imprime: a SDK passa o corpo como argumento extra de uma mensagem SLF4J sem
     * placeholder, e o argumento é descartado. Mas isso é acidente de implementação de terceiro, e a
     * proteção não pode depender dele. Declarar {@code toString()} explicitamente também impede que
     * um {@code @Data} ou {@code @ToString} acrescentado num refactor futuro gere uma versão que
     * inclua o campo — o Lombok não sobrescreve método já declarado.
     */
    @Override
    public String toString() {
        return "UsuarioDTO(id=" + id + ", login=" + login + ", ativo=" + ativo + ", senha=<omitida>)";
    }

}

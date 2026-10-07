package br.com.gems.app.security;

import br.com.gems.app.support.AbstractIntegrationTest;
import br.com.gems.aws.web.S3Controller;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prova, em nível HTTP, que {@code /api/aws/s3/**} está fechado para todo mundo — inclusive
 * ADMIN — pelo bloqueio deliberado declarado em {@code SecurityConfig}.
 *
 * <p>
 * <b>Por que este teste existe.</b> O {@code S3Controller} pertence ao {@code gems-aws-web}
 * (jar de terceiro) e nunca teve {@code @PreAuthorize}: qualquer usuário autenticado conseguia
 * assinar upload/download de qualquer chave do bucket, e {@code DELETE /delete-file} é exclusão
 * física via {@code s3Client.deleteObject} — sem versionamento, irreversível. Como a anotação
 * teria de estar na classe da SDK, o bloqueio foi feito na cadeia de segurança do sistema, com
 * {@code auth.requestMatchers("/api/aws/s3/**").denyAll()} antes de {@code anyRequest()}.
 *
 * <p>
 * <b>Por que o 403 aqui prova a regra de segurança, e não a ausência do controller.</b> Um
 * endpoint inexistente também devolveria uma resposta de erro, e {@code denyAll()} casa por
 * padrão de caminho — não distinguiria "rota bloqueada" de "rota que nunca existiu" sozinho. Por
 * isso {@link #s3ControllerContinuaPublicadoNoContexto()} verifica, à parte, que o
 * {@code S3Controller} da SDK está de fato registrado no contexto Spring (ele é dependência
 * declarada de {@code app-deploy}): o controller existe e está mapeado, e mesmo assim toda
 * chamada aos seus três endpoints volta 403 — a negativa só pode vir da regra de autorização.
 *
 * <p>
 * É o primeiro teste de autorização em nível HTTP do projeto: primeiro uso de {@link MockMvc}
 * e do {@code spring-security-test}. Serve de modelo para os próximos.
 *
 * <p>
 * <b>Atenção ao import de {@code @AutoConfigureMockMvc}:</b> no Spring Boot 4 a anotação saiu do
 * {@code spring-boot-test-autoconfigure} — que hoje carrega apenas as slices de {@code jdbc} e
 * {@code json} — e passou a viver em {@code org.springframework.boot.webmvc.test.autoconfigure},
 * entregue pelo starter {@code spring-boot-starter-webmvc-test}. Sem essa dependência o import não
 * compila, e o sintoma engana: parece jar corrompido.
 *
 * <p>
 * O {@link MockMvc} montado aqui carrega a cadeia de filtros real do {@code SecurityConfig} do
 * sistema — é o {@code denyAll()} de verdade que responde, não uma simulação dele.
 */
@AutoConfigureMockMvc
class S3EndpointsSecurityIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private S3Controller s3Controller;

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("O S3Controller da SDK continua publicado no contexto — o 403 dos testes abaixo vem da regra de segurança, não de rota inexistente")
    void deveManterS3ControllerPublicado_quandoContextoInicia() {
        assertThat( s3Controller ).isNotNull();
    }

    @Test
    @DisplayName("POST /api/aws/s3/generate-upload-url retorna 403 para usuário autenticado USER")
    void deveRetornar403_quandoUsuarioAutenticadoTentaGerarUrlDeUpload() throws Exception {
        mockMvc.perform( post( "/api/aws/s3/generate-upload-url" )
                .with( jwt().authorities( new SimpleGrantedAuthority( "ROLE_USER" ) ) )
                .contentType( MediaType.APPLICATION_JSON )
                .content( "{}" ) )
                .andExpect( status().isForbidden() );
    }

    @Test
    @DisplayName("GET /api/aws/s3/generate-download-url retorna 403 para usuário autenticado USER")
    void deveRetornar403_quandoUsuarioAutenticadoTentaGerarUrlDeDownload() throws Exception {
        mockMvc.perform( get( "/api/aws/s3/generate-download-url" )
                .param( "fileKey", "qualquer-chave.pdf" )
                .with( jwt().authorities( new SimpleGrantedAuthority( "ROLE_USER" ) ) ) )
                .andExpect( status().isForbidden() );
    }

    @Test
    @DisplayName("DELETE /api/aws/s3/delete-file retorna 403 para usuário autenticado USER")
    void deveRetornar403_quandoUsuarioAutenticadoTentaExcluirArquivo() throws Exception {
        mockMvc.perform( delete( "/api/aws/s3/delete-file" )
                .param( "fileKey", "qualquer-chave.pdf" )
                .with( jwt().authorities( new SimpleGrantedAuthority( "ROLE_USER" ) ) ) )
                .andExpect( status().isForbidden() );
    }

    @Test
    @DisplayName("D2 — DELETE /api/aws/s3/delete-file retorna 403 mesmo para ADMIN: denyAll() não abre exceção por role")
    void deveRetornar403_quandoAdminTentaExcluirArquivo() throws Exception {
        mockMvc.perform( delete( "/api/aws/s3/delete-file" )
                .param( "fileKey", "qualquer-chave.pdf" )
                .with( jwt().authorities( new SimpleGrantedAuthority( "ROLE_ADMIN" ) ) ) )
                .andExpect( status().isForbidden() );
    }
}

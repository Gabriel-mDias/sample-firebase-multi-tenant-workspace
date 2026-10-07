package br.com.gems.app.auditoria.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A trilha é somente escrita e leitura, e a <b>ausência</b> tem teste.
 *
 * <h2>Por que testar uma ausência</h2>
 * Porque ela é a garantia, não uma consequência. Quem grava a trilha é o mecanismo de persistência;
 * um endpoint de escrita permitiria <b>forjar</b> registro, e um de exclusão faria da trilha algo
 * apagável — trilha apagável não é trilha, e uma trilha em que se pode inserir linha à mão não prova
 * nada em auditoria nenhuma.
 *
 * <p>
 * Sem este teste, a proibição existe só como comentário no controller. E este projeto tem a lição
 * escrita no próprio checklist de fechamento: <i>toda regra que só existe como texto será
 * violada</i> — normalmente por alguém de boa-fé, resolvendo um problema legítimo.
 *
 * <p>
 * A única exceção é {@code POST /search}, que é consulta com corpo — a convenção do projeto para
 * busca paginada. Ela é reconhecida pelo caminho, não dispensada por tipo de verbo.
 */
class AuditoriaControllerSemEscritaTest {

    /** Busca paginada é POST por convenção do projeto. É consulta, não escrita. */
    private static final List<String> CAMINHOS_DE_CONSULTA_COM_CORPO = List.of( "/search" );

    @Test
    @DisplayName("o controller não expõe nenhum método mapeado de escrita")
    void deveComprovarNenhumMetodoDeEscrita_quandoExecutaOCenario() {
        var escritas = Arrays.stream( AuditoriaController.class.getDeclaredMethods() )
                .filter( AuditoriaControllerSemEscritaTest::ehEscrita )
                .map( Method::getName )
                .toList();

        assertThat( escritas )
                .as( """
                        O AuditoriaController expõe método de escrita. Isso viola o requisito.

                        Quem grava a trilha é o writer do GEMS Auditing, na camada de persistência. Um \
                        endpoint de escrita permite forjar registro; um de exclusão faz da trilha \
                        algo apagável. Se a necessidade for legítima — expurgo por retenção, por \
                        exemplo — ela é rotina de banco, não API, e passa por decisão registrada.""" )
                .isEmpty();
    }

    @Test
    @DisplayName("A varredura de fato examina o controller — não passa por olhar para o vazio")
    void deveComprovarAVarreduraExaminaOsMetodos_quandoExecutaOCenario() {
        var mapeados = Arrays.stream( AuditoriaController.class.getDeclaredMethods() )
                .filter( metodo -> metodo.getAnnotations().length > 0 )
                .count();

        assertThat( mapeados )
                .as( "se o controller ficar sem métodos anotados, o teste acima passaria examinando "
                        + "nada e diria que está tudo certo" )
                .isGreaterThanOrEqualTo( 2 );
    }

    private static boolean ehEscrita( Method metodo ) {
        if ( metodo.isAnnotationPresent( PutMapping.class )
                || metodo.isAnnotationPresent( DeleteMapping.class )
                || metodo.isAnnotationPresent( PatchMapping.class ) ) {
            return true;
        }

        if ( metodo.isAnnotationPresent( PostMapping.class ) ) {
            return !ehConsultaComCorpo( metodo.getAnnotation( PostMapping.class ).value() );
        }

        if ( metodo.isAnnotationPresent( RequestMapping.class ) ) {
            var mapping = metodo.getAnnotation( RequestMapping.class );

            return Arrays.stream( mapping.method() )
                    .anyMatch( verbo -> verbo != RequestMethod.GET && verbo != RequestMethod.HEAD
                            && verbo != RequestMethod.OPTIONS
                            && !(verbo == RequestMethod.POST && ehConsultaComCorpo( mapping.value() )) );
        }

        return false;
    }

    private static boolean ehConsultaComCorpo( String[] caminhos ) {
        return Arrays.stream( caminhos ).anyMatch( CAMINHOS_DE_CONSULTA_COM_CORPO::contains );
    }
}

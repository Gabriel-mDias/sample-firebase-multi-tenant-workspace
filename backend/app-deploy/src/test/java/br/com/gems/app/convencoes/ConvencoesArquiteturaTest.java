package br.com.gems.app.convencoes;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mapstruct.Mapper;
import org.springframework.data.jpa.repository.Query;

/**
 * N-04 — o gate de arquitetura da rodada 008: o que o <strong>bytecode</strong> sabe afirmar sobre a
 * assinatura que {@code .gems-ai/rules/backend-java/} prescreve.
 *
 * <p>
 * <strong>O que este gate NÃO consegue afirmar, e por quê</strong> (R-03, e está aqui para não ser
 * redescoberto): ordem de qualquer espécie — de campos, de métodos, de interfaces no {@code extends}
 * —, presença de anotação que o compilador descarta, e conteúdo de método. O bytecode não preserva a
 * ordem de declaração de forma confiável nem guarda o corpo em forma legível. Tudo isso vive no
 * {@link ConvencoesFonteTest}, que lê a fonte com o JavaParser.
 *
 * <p>
 * A formatação das reprovações é do {@link RelatorioDeConformidade}, pela razão escrita lá: o §2 de
 * {@code contracts/gates.md} exige caminho relativo, regra com âncora e o <em>esperado</em>.
 */
class ConvencoesArquiteturaTest {

    private static final String BASE = "br.com.gems.app";
    private static JavaClasses classes;

    @BeforeAll
    static void importar() {
        classes = new ClassFileImporter()
                .withImportOption( ImportOption.Predefined.DO_NOT_INCLUDE_TESTS )
                .importPackages( BASE );
    }

    @Test
    void deveRecusarQualquerEntrada_quandoOBaselineEstaZerado() {
        RelatorioDeConformidade.verificarTamanhoDoBaseline();
    }

    @Test
    void deveAnotarComRepository_quandoOTipoEUmRepositorioDeDominio() {
        var relatorio = new RelatorioDeConformidade( "ArchUnit · @Repository" );

        for ( JavaClass repositorio : repositorios() ) {
            if ( !repositorio.isAnnotatedWith( "org.springframework.stereotype.Repository" ) ) {
                relatorio.registrar( chave( repositorio, "repository-annotation" ), LocalizadorDeFonte.caminhoRelativo( repositorio.getName() ), 0, "repositories.md#1", repositorio.getSimpleName() + " estende JpaRepository sem @Repository", "@Repository na declaração da interface" );
            }
        }

        relatorio.concluir();
    }

    @Test
    void deveEstenderBaseCustomJpaRepository_quandoOTipoEUmRepositorioDeDominio() {
        var relatorio = new RelatorioDeConformidade( "ArchUnit · BaseCustomJpaRepository" );

        for ( JavaClass repositorio : repositorios() ) {
            var estende = repositorio.getAllRawInterfaces().stream()
                    .anyMatch( interfaceDoTipo -> interfaceDoTipo.getName()
                    .equals( "br.com.gems.jpa.repository.BaseCustomJpaRepository" ) );

            if ( !estende ) {
                relatorio.registrar( chave( repositorio, "base-custom-jpa-repository" ), LocalizadorDeFonte.caminhoRelativo( repositorio.getName() ), 0, "repositories.md#1", repositorio.getSimpleName() + " não estende BaseCustomJpaRepository", "extends JpaRepository<Entidade, UUID>, BaseCustomJpaRepository<Entidade> — " + "é ele que entrega executeHql e executeCountHql" );
            }
        }

        relatorio.concluir();
    }

    /**
     * A tétrade de {@code repositories.md} §2, e o modificador de cada membro dela. Só se aplica ao
     * repositório que <strong>tem</strong> listagem paginada: a regra prescreve a estrutura de quem
     * tem {@code search}, não obriga todo repositório a ter um.
     */
    @Test
    void deveDeclararATetradeCompleta_quandoORepositorioTemListagemPaginada() {
        var relatorio = new RelatorioDeConformidade( "ArchUnit · tétrade de consulta" );

        for ( JavaClass repositorio : repositorios() ) {
            if ( metodo( repositorio, "search" ) == null ) {
                continue;
            }

            var caminho = LocalizadorDeFonte.caminhoRelativo( repositorio.getName() );
            exigirMetodoPrivado( relatorio, repositorio, caminho, "countQuery" );
            exigirMetodoPrivado( relatorio, repositorio, caminho, "searchQuery" );
            exigirMetodoPrivado( relatorio, repositorio, caminho, "appendFilters" );

            var search = metodo( repositorio, "search" );
            var ehDefaultPublico = search.getModifiers().contains( JavaModifier.PUBLIC )
                    && !search.getModifiers().contains( JavaModifier.ABSTRACT )
                    && !search.getModifiers().contains( JavaModifier.STATIC );
            if ( !ehDefaultPublico ) {
                relatorio.registrar( chave( repositorio, "search-default" ), caminho, 0, "repositories.md#2.1", "search não é um método default público", "default Page<EntidadeResponseDTO> search( EntidadeFilterParams filterParams, " + "Pageable pageable )" );
            }
        }

        relatorio.concluir();
    }

    @Test
    void deveRecusarSqlNativo_quandoOMetodoDeclaraQuery() {
        var relatorio = new RelatorioDeConformidade( "ArchUnit · HQL sempre" );

        for ( JavaClass classe : classes ) {
            for ( JavaMethod metodo : classe.getMethods() ) {
                var query = metodo.tryGetAnnotationOfType( Query.class );
                if ( query.isPresent() && query.get().nativeQuery() ) {
                    relatorio.registrar( chave( classe, "native-query-" + metodo.getName() ), LocalizadorDeFonte.caminhoRelativo( classe.getName() ), 0, "repositories.md#anti-patterns", metodo.getName() + " usa @Query( nativeQuery = true )", "HQL sempre — portabilidade e type-safety" );
                }
            }
        }

        relatorio.concluir();
    }

    /**
     * Princípio V da constituição, na forma que a rodada 007 tornou incondicional: <strong>todo</strong>
     * controller estende {@code BaseController}. Não estender exige motivo escrito no Javadoc, e o
     * projeto não tem hoje nenhum caso.
     */
    @Test
    void deveEstenderBaseController_quandoAClasseEUmController() {
        var relatorio = new RelatorioDeConformidade( "ArchUnit · BaseController" );

        for ( JavaClass classe : classes ) {
            if ( !classe.isAnnotatedWith( "org.springframework.web.bind.annotation.RestController" ) ) {
                continue;
            }
            var estende = classe.getAllRawSuperclasses().stream()
                    .anyMatch( pai -> pai.getName().equals( "br.com.gems.exception.base.BaseController" ) );

            if ( !estende ) {
                relatorio.registrar( chave( classe, "base-controller" ), LocalizadorDeFonte.caminhoRelativo( classe.getName() ), 0, "controllers.md#1", classe.getSimpleName() + " é @RestController e não estende BaseController", "extends BaseController (br.com.gems.exception.base.BaseController)" );
            }
        }

        relatorio.concluir();
    }

    /**
     * O {@code config = GemsMappingConfig.class} é o que traz {@code unmappedTargetPolicy = ERROR}:
     * sem ele, campo novo no DTO deixa de reprovar a compilação e passa a chegar nulo em produção. A
     * anotação tem retenção {@code CLASS}, e por isso o bytecode ainda a enxerga.
     */
    @Test
    void deveDeclararOGemsMappingConfig_quandoAInterfaceEUmMapper() {
        var relatorio = new RelatorioDeConformidade( "ArchUnit · @Mapper( config = GemsMappingConfig.class )" );

        for ( JavaClass classe : classes ) {
            var mapper = classe.tryGetAnnotationOfType( Mapper.class );
            if ( mapper.isEmpty() ) {
                continue;
            }

            var config = mapper.get().config();
            if ( !config.getName().equals( "br.com.gems.mapstruct.GemsMappingConfig" ) ) {
                relatorio.registrar( chave( classe, "mapper-config" ), LocalizadorDeFonte.caminhoRelativo( classe.getName() ), 0, "mapping.md#2", classe.getSimpleName() + " é @Mapper com config = " + config.getSimpleName(), "@Mapper( config = GemsMappingConfig.class ) — é ele que traz unmappedTargetPolicy = ERROR" );
            }
        }

        relatorio.concluir();
    }

    /**
     * O controller fala com a service, e só. Dependência direta de repository é a fronteira que some
     * primeiro quando alguém tem pressa, e o efeito é regra de negócio escapando para a camada web.
     */
    @Test
    void deveManterAsFronteirasEntreCamadas_quandoUmaClasseDependeDeOutra() {
        var relatorio = new RelatorioDeConformidade( "ArchUnit · fronteiras entre camadas" );

        for ( JavaClass classe : classes ) {
            var caminho = LocalizadorDeFonte.caminhoRelativo( classe.getName() );

            if ( classe.getPackageName().endsWith( ".controller" ) ) {
                for ( JavaClass dependencia : classe.getDirectDependenciesFromSelf().stream()
                        .map( dependencia -> dependencia.getTargetClass() ).toList() ) {
                    if ( dependencia.getPackageName().startsWith( BASE )
                            && dependencia.getPackageName().endsWith( ".repository" ) ) {
                        relatorio.registrar( chave( classe, "controller-repository" ), caminho, 0, "controllers.md#1", classe.getSimpleName() + " depende diretamente de " + dependencia.getSimpleName(), "UMA única dependência de Service — o controller não enxerga repository" );
                    }
                }
            }

            if ( classe.getPackageName().startsWith( BASE + ".model" ) ) {
                for ( JavaClass dependencia : classe.getDirectDependenciesFromSelf().stream()
                        .map( dependencia -> dependencia.getTargetClass() ).toList() ) {
                    var pacote = dependencia.getPackageName();
                    var ehCamadaDeAplicacao = pacote.startsWith( BASE )
                            && (pacote.endsWith( ".controller" ) || pacote.endsWith( ".service" )
                            || pacote.endsWith( ".repository" ));
                    if ( ehCamadaDeAplicacao ) {
                        relatorio.registrar( chave( classe, "model-aplicacao" ), caminho, 0, "architecture.md#1", classe.getSimpleName() + " (app-model) depende de " + dependencia.getSimpleName(), "app-model contém apenas Entities, DTOs e Enums — a dependência vai de " + "model/security para events/core, nunca ao contrário" );
                    }
                }
            }
        }

        relatorio.concluir();
    }

    private static Set<JavaClass> repositorios() {
        return classes.stream()
                .filter( JavaClass::isInterface )
                .filter( classe -> classe.getAllRawInterfaces().stream()
                .anyMatch( interfaceDoTipo -> interfaceDoTipo.getName()
                .equals( "org.springframework.data.jpa.repository.JpaRepository" ) ) )
                .collect( java.util.stream.Collectors.toSet() );
    }

    private static void exigirMetodoPrivado( RelatorioDeConformidade relatorio, JavaClass repositorio, String caminho, String nome ) {
        var metodo = metodo( repositorio, nome );

        if ( metodo == null ) {
            relatorio.registrar( chave( repositorio, "tetrade-ausente-" + nome ), caminho, 0, "repositories.md#2", repositorio.getSimpleName() + " tem search e não declara " + nome, "a tétrade completa: search > countQuery > searchQuery > appendFilters" );
            return;
        }

        if ( !metodo.getModifiers().contains( JavaModifier.PRIVATE ) ) {
            relatorio.registrar( chave( repositorio, "tetrade-visibilidade-" + nome ), caminho, 0, "repositories.md#2", nome + " não é privado", nome + " privado — a montagem do HQL não é interface pública do repositório" );
        }
    }

    private static JavaMethod metodo( JavaClass classe, String nome ) {
        return classe.getMethods().stream()
                .filter( candidato -> candidato.getName().equals( nome ) )
                .findFirst()
                .orElse( null );
    }

    private static String chave( JavaClass classe, String regra ) {
        return String.join( "|", List.of( classe.getName(), regra ) );
    }

}

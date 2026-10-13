package br.com.gems.app.convencoes;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.NullLiteralExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

/**
 * As regras do gate de fonte (N-05), cada uma escrevendo no {@link RelatorioDeConformidade} que
 * recebe. Separadas do teste para que o gate de exceções as reavalie em {@code Modo.BRUTO} — é a
 * comparação entre os dois modos que revela a exceção órfã.
 *
 * <p>
 * <strong>Nenhuma regra usa expressão regular</strong> (FR-029, T074): a estrutura vem da árvore
 * sintática do JavaParser, e o pouco de HQL que precisa ser lido dentro das strings é tokenizado
 * por espaço em branco, com {@link StringTokenizer}.
 */
final class RegrasDeFonte {

    private static final Set<String> PREFIXOS_DE_JOIN = Set.of( "LEFT", "RIGHT", "INNER", "OUTER", "FULL" );
    private static final Set<String> ANOTACOES_DE_TESTE = Set.of( "Test", "ParameterizedTest", "RepeatedTest" );
    /** A ordem canônica de {@code services.md} §2 (FR-017a). Métodos fora dela não têm posição prescrita. */
    private static final List<String> ORDEM_CANONICA = List.of( "findById", "findAll", "search", "delete", "insert", "save" );

    private RegrasDeFonte() {
    }

    // ---------------------------------------------------------------- 1. JOINs idênticos (repositories.md §5)

    /**
     * A única regra do conjunto que pega defeito de <em>comportamento</em>: {@code countQuery} e
     * {@code searchQuery} com JOINs diferentes devolvem um total que não corresponde à página, sem
     * erro nenhum.
     */
    static void joinsIdenticos( RelatorioDeConformidade relatorio ) {
        for ( var repositorio : repositorios() ) {
            var count = metodo( repositorio.tipo(), "countQuery" );
            var search = metodo( repositorio.tipo(), "searchQuery" );
            if ( count == null || search == null ) {
                continue;
            }

            var joinsDoCount = joins( tokensDeHql( count ) );
            var joinsDoSearch = joins( tokensDeHql( search ) );
            if ( !joinsDoCount.equals( joinsDoSearch ) ) {
                relatorio.registrar( chave( repositorio.caminho(), "joins-identicos" ), repositorio.caminho(), linha( count ), "repositories.md#5", "countQuery declara " + joinsDoCount + " e searchQuery declara " + joinsDoSearch, "JOINs idênticos em countQuery e searchQuery — sem isso o total da página não corresponde " + "à lista, e nenhum erro é lançado" );
            }
        }
    }

    // ---------------------------------------------------------------- 2. != null em appendFilters (§2.4)

    static void semComparacaoComNuloEmAppendFilters( RelatorioDeConformidade relatorio ) {
        for ( var repositorio : repositorios() ) {
            var appendFilters = metodo( repositorio.tipo(), "appendFilters" );
            if ( appendFilters == null ) {
                continue;
            }

            for ( var comparacao : appendFilters.findAll( BinaryExpr.class ) ) {
                var operador = comparacao.getOperator();
                var comparaComNulo = (operador == BinaryExpr.Operator.NOT_EQUALS
                        || operador == BinaryExpr.Operator.EQUALS)
                        && (comparacao.getLeft() instanceof NullLiteralExpr
                        || comparacao.getRight() instanceof NullLiteralExpr);
                if ( comparaComNulo ) {
                    relatorio.registrar( chave( repositorio.caminho(), "null-em-appendFilters|" + comparacao ), repositorio.caminho(), linha( comparacao ), "repositories.md#2.4", "appendFilters compara com null: " + comparacao, "ObjectUtil.isNotNullAndNotEmpty( ... ) — nunca != null nem == null" );
                }
            }
        }
    }

    // ---------------------------------------------------------------- 3. Primeiro campo da service (services.md §1)

    /**
     * Âncora <strong>condicional</strong>: só a service que tem um mapper do domínio tem primeiro
     * campo prescrito. Sem mapper, a regra não inventa um.
     */
    static void mapperComoPrimeiroCampo( RelatorioDeConformidade relatorio ) {
        for ( var service : services() ) {
            var campos = camposDeInstancia( service.tipo() );
            var mapper = campos.stream().filter( RegrasDeFonte::ehMapper ).findFirst();
            if ( mapper.isEmpty() || campos.isEmpty() ) {
                continue;
            }

            var primeiro = campos.get( 0 );
            if ( primeiro != mapper.get() ) {
                relatorio.registrar( chave( service.caminho(), "mapper-primeiro-campo" ), service.caminho(), linha( primeiro ), "services.md#1", service.tipo().getNameAsString() + " tem " + nomeDoTipo( mapper.get() ) + " mas o primeiro campo é " + nomeDoTipo( primeiro ), "o mapper do domínio como primeiro campo, e o repository da entidade como segundo" );
            }
        }
    }

    // ---------------------------------------------------------------- 4. WHERE 1=1 (§2.4)

    static void clausulaDePartida( RelatorioDeConformidade relatorio ) {
        for ( var repositorio : repositorios() ) {
            var appendFilters = metodo( repositorio.tipo(), "appendFilters" );
            if ( appendFilters == null ) {
                continue;
            }

            var appends = appendsDe( appendFilters );
            var tokens = appends.isEmpty() ? List.<String>of() : tokens( appends.get( 0 ).asString() );
            var comecaComWhereUmIgualUm = tokens.size() == 2 && tokens.get( 0 ).equalsIgnoreCase( "WHERE" )
                    && tokens.get( 1 ).equals( "1=1" );
            if ( !comecaComWhereUmIgualUm ) {
                relatorio.registrar( chave( repositorio.caminho(), "where-1-1" ), repositorio.caminho(), appends.isEmpty() ? linha( appendFilters ) : linha( appends.get( 0 ) ), "repositories.md#2.4", "appendFilters não começa com \" WHERE 1=1 \"", "hql.append( \" WHERE 1=1 \" ) como primeira linha — é o que permite todo filtro " + "ser um AND condicional" );
            }
        }
    }

    // ---------------------------------------------------------------- 5. Ordem do extends (§1)

    static void ordemDaHeranca( RelatorioDeConformidade relatorio ) {
        for ( var repositorio : repositorios() ) {
            var nomes = repositorio.tipo().getExtendedTypes().stream().map( ClassOrInterfaceType::getNameAsString )
                    .toList();
            var jpa = nomes.indexOf( "JpaRepository" );
            var base = nomes.indexOf( "BaseCustomJpaRepository" );
            if ( jpa >= 0 && base >= 0 && base < jpa ) {
                relatorio.registrar( chave( repositorio.caminho(), "ordem-do-extends" ), repositorio.caminho(), linha( repositorio.tipo().getExtendedTypes().get( 0 ) ), "repositories.md#1", "extends " + String.join( ", ", nomes ), "extends JpaRepository<Entidade, UUID>, BaseCustomJpaRepository<Entidade> — nesta ordem" );
            }
        }
    }

    // ---------------------------------------------------------------- 6. Projeção por construtor (§2.3)

    /**
     * Quando {@code searchQuery} devolve DTO, o HQL projeta por {@code SELECT new <fqn>( ... )} e o
     * tipo construído é o mesmo que o método declara. A variante que devolve a entidade (§3) fica
     * de fora.
     */
    static void projecaoPorConstrutor( RelatorioDeConformidade relatorio ) {
        for ( var repositorio : repositorios() ) {
            var searchQuery = metodo( repositorio.tipo(), "searchQuery" );
            if ( searchQuery == null ) {
                continue;
            }

            var elemento = tipoDoElemento( searchQuery );
            if ( elemento == null || !elemento.endsWith( "DTO" ) ) {
                continue;
            }

            var tokens = tokensDeHql( searchQuery );
            var construido = tokens.size() > 2 && tokens.get( 0 ).equalsIgnoreCase( "SELECT" )
                    && tokens.get( 1 ).equals( "new" ) ? nomeSimples( tokens.get( 2 ) ) : null;
            if ( construido == null || !construido.equals( elemento ) ) {
                relatorio.registrar( chave( repositorio.caminho(), "projecao-por-construtor" ), repositorio.caminho(), linha( searchQuery ), "repositories.md#2.3", "searchQuery devolve List<" + elemento + "> e o HQL " + (construido == null ? "não projeta por construtor" : "constrói " + construido), "hql.append( \" SELECT new <pacote>." + elemento + "( \" ) — a projeção constrói o DTO que " + "o método devolve" );
            }
        }
    }

    // ---------------------------------------------------------------- 7. Ordem dos métodos da service (§2, FR-017a)

    /**
     * Duas afirmações: os métodos de nome canônico aparecem na ordem de {@code services.md} §2, e
     * todo método privado vem depois do último público — {@code validate} e os auxiliares fecham a
     * classe.
     */
    static void ordemDosMetodosDaService( RelatorioDeConformidade relatorio ) {
        for ( var service : services() ) {
            var metodos = service.tipo().getMethods();
            var ultimaPosicao = -1;
            MethodDeclaration ultimoPublico = null;

            for ( var metodo : metodos ) {
                if ( metodo.isPublic() ) {
                    ultimoPublico = metodo;
                    var posicao = ORDEM_CANONICA.indexOf( metodo.getNameAsString() );
                    if ( posicao >= 0 && posicao < ultimaPosicao ) {
                        relatorio.registrar( chave( service.caminho(), "ordem-canonica|" + metodo.getNameAsString() ), service.caminho(), linha( metodo ), "services.md#2", metodo.getNameAsString() + " vem depois de " + ORDEM_CANONICA.get( ultimaPosicao ), "findById > findAll > search > delete > insert > save, e os privados por último" );
                    }
                    ultimaPosicao = Math.max( ultimaPosicao, posicao );
                }
            }

            for ( var metodo : metodos ) {
                if ( metodo.isPrivate() && ultimoPublico != null && linha( metodo ) < linha( ultimoPublico ) ) {
                    relatorio.registrar( chave( service.caminho(), "privado-antes-de-publico|" + metodo.getNameAsString() ), service.caminho(), linha( metodo ), "services.md#2", "o privado " + metodo.getNameAsString() + " vem antes do público " + ultimoPublico.getNameAsString(), "métodos privados (validate e auxiliares) depois de todos os públicos" );
                }
            }
        }
    }

    // ---------------------------------------------------------------- 8. Nome de método de teste (testing.md §4)

    /**
     * A convenção {@code deveX_quandoY}, que saiu do Checkstyle (achado N-01-A: ele não lê a
     * anotação e reprovava auxiliares corretos). Só o método anotado como teste tem nome prescrito.
     */
    static void nomeDeMetodoDeTeste( RelatorioDeConformidade relatorio ) {
        for ( var entrada : FontesJava.teste().entrySet() ) {
            for ( var metodo : entrada.getValue().findAll( MethodDeclaration.class ) ) {
                var ehTeste = metodo.getAnnotations().stream()
                        .anyMatch( anotacao -> ANOTACOES_DE_TESTE.contains( anotacao.getNameAsString() ) );
                var nome = metodo.getNameAsString();
                if ( ehTeste && !nomeDeTesteSegueOPadrao( nome ) ) {
                    relatorio.registrar( chave( entrada.getKey(), "nome-de-teste|" + nome ), entrada.getKey(), linha( metodo ), "testing.md#4", "método de teste chamado " + nome, "deve{Acao}_quando{Condicao}, em camelCase e português" );
                }
            }
        }
    }

    private static boolean nomeDeTesteSegueOPadrao( String nome ) {
        return (nome.startsWith( "deve" ) || nome.startsWith( "naoDeve" )) && nome.contains( "_quando" );
    }

    // ---------------------------------------------------------------- leitura da árvore

    record Declaracao(String caminho, ClassOrInterfaceDeclaration tipo) {

    }

    static List<Declaracao> repositorios() {
        return declaracoes( FontesJava.producao(), tipo -> tipo.isInterface() && tipo.getExtendedTypes().stream()
                .anyMatch( estendido -> estendido.getNameAsString().equals( "JpaRepository" ) ) );
    }

    static List<Declaracao> services() {
        return declaracoes( FontesJava.producao(), tipo -> !tipo.isInterface()
                && tipo.getNameAsString().endsWith( "Service" ) && tipo.isAnnotationPresent( "Service" ) );
    }

    private static List<Declaracao> declaracoes( Map<String, CompilationUnit> unidades, java.util.function.Predicate<ClassOrInterfaceDeclaration> filtro ) {
        var encontradas = new ArrayList<Declaracao>();
        for ( var entrada : unidades.entrySet() ) {
            for ( var tipo : entrada.getValue().findAll( ClassOrInterfaceDeclaration.class ) ) {
                if ( filtro.test( tipo ) ) {
                    encontradas.add( new Declaracao( entrada.getKey(), tipo ) );
                }
            }
        }
        return encontradas;
    }

    private static MethodDeclaration metodo( ClassOrInterfaceDeclaration tipo, String nome ) {
        var candidatos = tipo.getMethodsByName( nome );
        return candidatos.isEmpty() ? null : candidatos.get( 0 );
    }

    private static List<FieldDeclaration> camposDeInstancia( ClassOrInterfaceDeclaration tipo ) {
        return tipo.getFields().stream().filter( campo -> !campo.isStatic() ).toList();
    }

    private static boolean ehMapper( FieldDeclaration campo ) {
        return nomeDoTipo( campo ).endsWith( "Mapper" );
    }

    private static String nomeDoTipo( FieldDeclaration campo ) {
        return campo.getVariable( 0 ).getType().asString();
    }

    /** Os literais passados a {@code hql.append( "..." )} no corpo do método, em ordem de fonte. */
    private static List<StringLiteralExpr> appendsDe( MethodDeclaration metodo ) {
        var literais = new ArrayList<StringLiteralExpr>();
        for ( var chamada : metodo.findAll( MethodCallExpr.class ) ) {
            var ehAppendNoHql = chamada.getNameAsString().equals( "append" ) && chamada.getScope().isPresent()
                    && chamada.getScope().get() instanceof NameExpr escopo && escopo.getNameAsString().equals( "hql" )
                    && chamada.getArguments().size() == 1
                    && chamada.getArgument( 0 ) instanceof StringLiteralExpr;
            if ( ehAppendNoHql ) {
                literais.add( (StringLiteralExpr) chamada.getArgument( 0 ) );
            }
        }
        return literais;
    }

    private static List<String> tokensDeHql( MethodDeclaration metodo ) {
        var hql = new StringBuilder();
        for ( var literal : appendsDe( metodo ) ) {
            hql.append( ' ' ).append( literal.asString() );
        }
        return tokens( hql.toString() );
    }

    private static List<String> tokens( String texto ) {
        var tokens = new ArrayList<String>();
        var tokenizador = new StringTokenizer( texto );
        while ( tokenizador.hasMoreTokens() ) {
            tokens.add( tokenizador.nextToken() );
        }
        return tokens;
    }

    /** Cada JOIN como {@code [LEFT] JOIN [FETCH] caminho alias}, normalizado, na ordem em que aparece. */
    private static List<String> joins( List<String> tokens ) {
        var joins = new ArrayList<String>();
        for ( int i = 0; i < tokens.size(); i++ ) {
            if ( !tokens.get( i ).equalsIgnoreCase( "JOIN" ) ) {
                continue;
            }
            var clausula = new StringBuilder();
            if ( i > 0 && PREFIXOS_DE_JOIN.contains( tokens.get( i - 1 ).toUpperCase() ) ) {
                clausula.append( tokens.get( i - 1 ).toUpperCase() ).append( ' ' );
            }
            clausula.append( "JOIN" );
            var proximo = i + 1;
            if ( proximo < tokens.size() && tokens.get( proximo ).equalsIgnoreCase( "FETCH" ) ) {
                clausula.append( " FETCH" );
                proximo++;
            }
            for ( int j = proximo; j < Math.min( proximo + 2, tokens.size() ); j++ ) {
                clausula.append( ' ' ).append( tokens.get( j ) );
            }
            joins.add( clausula.toString() );
        }
        return joins;
    }

    /** O {@code T} de {@code List<T>} no retorno do método, ou {@code null} quando não há argumento de tipo. */
    private static String tipoDoElemento( MethodDeclaration metodo ) {
        if ( !(metodo.getType() instanceof ClassOrInterfaceType retorno) || retorno.getTypeArguments().isEmpty() ) {
            return null;
        }
        var argumentos = retorno.getTypeArguments().get();
        return argumentos.isEmpty() ? null : argumentos.get( 0 ).asString();
    }

    /** {@code br.com.x.Tipo(} → {@code Tipo}. */
    private static String nomeSimples( String token ) {
        var abreParenteses = token.indexOf( '(' );
        var semParenteses = abreParenteses < 0 ? token : token.substring( 0, abreParenteses );
        return semParenteses.substring( semParenteses.lastIndexOf( '.' ) + 1 );
    }

    private static int linha( Node no ) {
        return no.getBegin().map( posicao -> posicao.line ).orElse( 0 );
    }

    private static String chave( String caminho, String regra ) {
        return caminho + "|" + regra;
    }

}

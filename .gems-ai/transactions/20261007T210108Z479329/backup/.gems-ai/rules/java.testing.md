<!-- gems-ai-rule: {"id":"java.testing","version":"2.0.0","status":"candidate"} -->
---
stack: backend-java
module: junit, mockito
severity: mandatory
see-also: [services.md, repositories.md, controllers.md]
origem: local
precedencia: local-vence
versao: 2.1.0
---

# Testes — Backend Java / Spring Boot

> **TL;DR:**
> - Padrão AAA: **Arrange → Act → Assert** em todo teste. Separação com linha em branco.
> - Services: `@ExtendWith(MockitoExtension.class)` + `@Mock` Repository + `@InjectMocks` Service.
> - Repositories: `@DataJpaTest` + banco em memória (H2) ou Testcontainers (PostgreSQL real).
> - Controllers: `@WebMvcTest` + `MockMvc` + `@MockBean` Service.
> - Nomenclatura: `deveFazerAlgo_quandoCondicao()` (camelCase, português).
> - **NUNCA** teste lógica do framework — teste a lógica de negócio.

## 1. Testes de Service (Unitários)

```java
@ExtendWith(MockitoExtension.class)
class {Entidade}ServiceTest {

    @Mock
    private {Entidade}Mapper mapper;

    @Mock
    private {Entidade}Repository repository;

    @InjectMocks
    private {Entidade}Service service;

    // --- findById ---

    @Test
    void deveLancarExcecao_quandoIdNulo() {
        // Arrange — nenhuma configuração necessária

        // Act + Assert
        assertThrows( BusinessException.class, () -> service.findById( (UUID) null ) );
    }

    @Test
    void deveRetornarDTO_quandoEntidadeEncontrada() {
        // Arrange
        var id = UUID.randomUUID();
        var entity = new {Entidade}();
        var dto = new {Entidade}DTO();

        when( repository.findById( id ) ).thenReturn( Optional.of( entity ) );
        when( mapper.toDTO( entity ) ).thenReturn( dto );

        // Act
        var result = service.findById( id );

        // Assert
        assertNotNull( result );
        verify( repository ).findById( id );
    }

    // --- insert ---

    @Test
    void deveLancarExcecao_quandoCampoObrigatorioVazio() {
        // Arrange
        var dto = new {Entidade}DTO();
        dto.setCampoObrigatorio( null );  // Campo inválido

        // Act + Assert
        var exception = assertThrows( BusinessException.class, () -> service.insert( dto ) );
        assertTrue( exception.getDetalhes().contains( "O campo obrigatório é obrigatório." ) );
    }

    @Test
    void deveInserir_quandoDadosValidos() {
        // Arrange
        var dto = new {Entidade}DTO();
        dto.setCampoObrigatorio( "valor" );
        var entity = new {Entidade}();
        var savedEntity = new {Entidade}();
        var responseDto = new {Entidade}DTO();

        when( mapper.toEntity( dto ) ).thenReturn( entity );
        when( repository.save( entity ) ).thenReturn( savedEntity );
        when( mapper.toDTO( savedEntity ) ).thenReturn( responseDto );

        // Act
        var result = service.insert( dto );

        // Assert
        assertNotNull( result );
        verify( repository ).save( entity );
    }
}
```

---

## 2. Testes de Repository (Integração)

```java
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)  // Use Testcontainers ou H2
class {Entidade}RepositoryTest {

    @Autowired
    private {Entidade}Repository repository;

    @Test
    void deveBuscarPorNome_quandoFiltroInformado() {
        // Arrange
        var entity = new {Entidade}();
        entity.setCampo( "Valor de Teste" );
        repository.save( entity );

        var filter = new {Entidade}FilterParams();
        filter.setTextoLivre( "Valor" );
        var pageable = PageRequest.of( 0, 10 );

        // Act
        var result = repository.search( filter, pageable );

        // Assert
        assertFalse( result.getContent().isEmpty() );
        assertEquals( 1, result.getTotalElements() );
    }

    @Test
    void naoDeveRetornarEntidadeExcluida() {
        // Arrange
        var entity = new {Entidade}();
        entity.setCampo( "valor" );
        var saved = repository.save( entity );
        repository.deleteById( saved.getId() );  // Soft delete

        // Act
        var result = repository.findById( saved.getId() );

        // Assert
        assertTrue( result.isEmpty() );  // @SQLRestriction filtra automaticamente
    }
}
```

---

## 3. Testes de Controller (Integração com MockMvc)

```java
@WebMvcTest({Entidade}Controller.class)
@Import(SecurityConfig.class)
class {Entidade}ControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private {Entidade}Service service;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser(roles = "INSERIR_{ENTIDADE}")  // a ação concreta de AcaoSistema que o endpoint exige
    void deveRetornar201_quandoInsertValido() throws Exception {
        // Arrange
        var dto = new {Entidade}DTO();
        dto.setCampoObrigatorio( "valor" );

        when( service.insert( any() ) ).thenReturn( dto );

        // Act + Assert
        mockMvc.perform( post( "/api/{recurso}" )
                .contentType( MediaType.APPLICATION_JSON )
                .content( objectMapper.writeValueAsString( dto ) ) )
                .andExpect( status().isCreated() )
                .andExpect( jsonPath( "$.campoObrigatorio" ).value( "valor" ) );
    }

    @Test
    @WithMockUser(roles = "INSERIR_{ENTIDADE}")  // a ação concreta de AcaoSistema que o endpoint exige
    void deveRetornar400_quandoBusinessException() throws Exception {
        // Arrange
        var dto = new {Entidade}DTO();
        when( service.insert( any() ) ).thenThrow( new BusinessException( "Campo obrigatório." ) );

        // Act + Assert
        mockMvc.perform( post( "/api/{recurso}" )
                .contentType( MediaType.APPLICATION_JSON )
                .content( objectMapper.writeValueAsString( dto ) ) )
                .andExpect( status().isBadRequest() );
    }
}
```

---

## 4. Padrões de Nomenclatura

```
deve{Acao}_{quandoCondicao}()
→ deveLancarExcecao_quandoIdNulo()
→ deveRetornarDTO_quandoEntidadeEncontrada()
→ deveInserir_quandoDadosValidos()
→ naoDeveRetornarEntidadeExcluida()
```

Alternativa em inglês: `should_{action}_when_{condition}()`.

---

## 5. Regras

- **Padrão AAA**: Arrange (preparar) → Act (executar) → Assert (verificar) em todo teste.
- Um Assert principal por teste (múltiplos `verify` são OK como complemento).
- Não teste comportamento do framework (ex.: que `@Transactional` faz rollback) — teste lógica de negócio.
- Mocks apenas para dependências externas (Repository, outras Services) — nunca mock da classe sendo testada.
- Use `assertThrows` para verificar exceções — nunca `try/catch` manual.
- Para `BusinessException` com lista de erros: `exception.getDetalhes()` para verificar mensagens específicas — é o acessor que a `br.com.gems.exception.exception.BusinessException` (gems-exception 3.0.0) expõe; `getErrors()` não existe.

---

## Anti-Patterns (NUNCA)

| Proibido | Correto |
| :--- | :--- |
| `try { service.method(); } catch (Exception e) { assertTrue(true); }` | `assertThrows(BusinessException.class, () -> service.method())` |
| Mock da própria classe sendo testada | Mock apenas de dependências externas (Repository, outras Services) |
| Testar que `@Transactional` faz rollback | Testar lógica de negócio — comportamento do framework não é responsabilidade do teste |
| Múltiplos asserts sem separação clara de AAA | Separar Arrange / Act / Assert com linha em branco entre blocos |

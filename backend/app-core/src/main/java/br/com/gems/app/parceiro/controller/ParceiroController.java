package br.com.gems.app.parceiro.controller;

import br.com.gems.security.authorization.GlobalEndpoint;
import br.com.gems.app.model.dto.ParceiroDTO;
import br.com.gems.app.model.params.ParceiroFilterParams;
import br.com.gems.app.model.response.ParceiroResponseDTO;
import br.com.gems.app.parceiro.service.ParceiroService;
import br.com.gems.exception.base.BaseController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints do cadastro de parceiros — <b>o controller de referência</b>.
 *
 * <p>
 * Sem lógica de negócio: delega tudo ao {@link ParceiroService}. Respostas sem envelope — o tipo
 * vai direto no corpo. Erros de negócio saem como 400 pelo handler da SDK; a negação de ação vira
 * 403 no {@code AccessDeniedExceptionHandler} do {@code app-security}.
 *
 * <p>
 * Cada método declara <b>uma</b> ação concreta de {@code AcaoSistema}. Nunca
 * {@code hasAnyRole(..., 'ADMIN')}: a composta já expande, e o {@code CoberturaPreAuthorizeTest}
 * falha se algum método mapeado ficar sem anotação.
 */
@RestController
@RequestMapping("/api/parceiros")
@RequiredArgsConstructor
@Tag(name = "Parceiros", description = "Cadastro de parceiros")
@GlobalEndpoint
public class ParceiroController extends BaseController {

    private final ParceiroService service;

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('CONSULTAR_PARCEIRO')")
    @Operation(summary = "Buscar parceiro por id", description = "Devolve o parceiro com pessoa, contato e endereço. A comissão só é "
            + "preenchida para quem tem VISUALIZAR_COMISSAO.")
    @ApiResponse(responseCode = "200", description = "Parceiro encontrado")
    @ApiResponse(responseCode = "400", description = "Parceiro inexistente ou excluído")
    public ResponseEntity<ParceiroDTO> findById( @PathVariable String id ) {
        return ResponseEntity.ok( service.findById( id ) );
    }

    @PostMapping("/search")
    @PreAuthorize("hasRole('CONSULTAR_PARCEIRO')")
    @Operation(summary = "Pesquisar parceiros", description = "Busca paginada. Filtros de nome (LIKE case-insensitive), CPF e estado são combináveis.")
    @ApiResponse(responseCode = "200", description = "Página de parceiros")
    public ResponseEntity<Page<ParceiroResponseDTO>> search(
            @RequestBody ParceiroFilterParams filterParams,
            @PageableDefault(page = 0, size = 10, sort = "nome", direction = Sort.Direction.ASC) Pageable pageable ) {
        return ResponseEntity.ok( service.search( filterParams, pageable ) );
    }

    @PostMapping
    @PreAuthorize("hasRole('INSERIR_PARCEIRO')")
    @Operation(summary = "Cadastrar parceiro", description = "Grava parceiro, pessoa, contato e endereço em uma única transação.")
    @ApiResponse(responseCode = "201", description = "Parceiro criado")
    @ApiResponse(responseCode = "400", description = "Validação de negócio")
    public ResponseEntity<ParceiroDTO> insert( @RequestBody ParceiroDTO dto, HttpServletRequest request ) {
        super.setBodyToExceptionLog( dto, request );
        return ResponseEntity.status( HttpStatus.CREATED ).body( service.insert( dto ) );
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ALTERAR_PARCEIRO')")
    @Operation(summary = "Alterar parceiro")
    @ApiResponse(responseCode = "200", description = "Parceiro alterado")
    @ApiResponse(responseCode = "400", description = "Validação de negócio, ou parceiro inexistente")
    public ResponseEntity<ParceiroDTO> save( @PathVariable String id, @RequestBody ParceiroDTO dto,
            HttpServletRequest request ) {
        super.setBodyToExceptionLog( dto, request );
        return ResponseEntity.ok( service.save( id, dto ) );
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('EXCLUIR_PARCEIRO')")
    @Operation(summary = "Excluir parceiro", description = "Exclusão lógica do parceiro e dos agregados dele.")
    @ApiResponse(responseCode = "204", description = "Parceiro excluído")
    @ApiResponse(responseCode = "400", description = "Parceiro inexistente")
    public ResponseEntity<Void> delete( @PathVariable String id ) {
        service.delete( id );
        return ResponseEntity.noContent().build();
    }

}

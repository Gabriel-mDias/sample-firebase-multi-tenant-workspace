package br.com.gems.app.demo.controller;

import br.com.gems.app.demo.service.DemoRegistroService;
import br.com.gems.app.model.dto.DemoRegistroDTO;
import br.com.gems.app.model.params.DemoRegistroFilterParams;
import br.com.gems.app.model.response.DemoRegistroResponseDTO;
import br.com.gems.exception.base.BaseController;
import br.com.gems.security.authorization.TenantEndpoint;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
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

@RestController
@RequestMapping("/api/demo-registros")
@RequiredArgsConstructor
@TenantEndpoint
@Tag(name = "DemoRegistro")
public class DemoRegistroController extends BaseController {
    private final DemoRegistroService service;
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('CONSULTAR_DEMO')")
    @Operation(summary = "Consultar registro")
    @ApiResponse(responseCode = "200", description = "Registro encontrado")
    public DemoRegistroDTO findById( @PathVariable UUID id ) {
        return service.findById( id );
    }
    @PostMapping("/search")
    @PreAuthorize("hasRole('CONSULTAR_DEMO')")
    @Operation(summary = "Pesquisar registros")
    @ApiResponse(responseCode = "200", description = "Página")
    public Page<DemoRegistroResponseDTO> search( @RequestBody DemoRegistroFilterParams dto,
            @PageableDefault(size = 10) Pageable pageable, HttpServletRequest request ) {
        super.setBodyToExceptionLog( dto, request );
        return service.search( dto, pageable );
    }
    @PostMapping
    @PreAuthorize("hasRole('INSERIR_DEMO')")
    @Operation(summary = "Inserir registro")
    @ApiResponse(responseCode = "201", description = "Registro criado")
    public ResponseEntity<DemoRegistroDTO> insert( @RequestBody DemoRegistroDTO dto, HttpServletRequest request ) {
        super.setBodyToExceptionLog( new DemoRegistroDTO( dto.id(), dto.nome(), dto.descricao(), null ), request );
        return ResponseEntity.status( 201 ).body( service.insert( dto ) );
    }
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ALTERAR_DEMO')")
    @Operation(summary = "Alterar registro")
    @ApiResponse(responseCode = "200", description = "Registro alterado")
    public DemoRegistroDTO save( @PathVariable UUID id, @RequestBody DemoRegistroDTO dto, HttpServletRequest request ) {
        super.setBodyToExceptionLog( new DemoRegistroDTO( dto.id(), dto.nome(), dto.descricao(), null ), request );
        return service.save( id, dto );
    }
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('EXCLUIR_DEMO')")
    @Operation(summary = "Excluir registro logicamente")
    @ApiResponse(responseCode = "204", description = "Registro excluído")
    public ResponseEntity<Void> delete( @PathVariable UUID id ) {
        service.delete( id );
        return ResponseEntity.noContent().build();
    }
}

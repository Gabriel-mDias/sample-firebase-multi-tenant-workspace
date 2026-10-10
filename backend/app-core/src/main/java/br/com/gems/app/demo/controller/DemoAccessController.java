package br.com.gems.app.demo.controller;

import br.com.gems.app.demo.service.DemoRegistroService;
import br.com.gems.app.model.dto.DemoRegistroDTO;
import br.com.gems.app.model.params.DemoRegistroFilterParams;
import br.com.gems.app.model.response.DemoRegistroResponseDTO;
import br.com.gems.exception.base.BaseController;
import br.com.gems.security.authorization.GlobalEndpoint;
import br.com.gems.security.authorization.PublicEndpoint;
import br.com.gems.security.authorization.TenantEndpoint;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/demo")
@RequiredArgsConstructor
@Tag(name = "Demonstrações de acesso")
public class DemoAccessController extends BaseController {
    private final DemoRegistroService service;
    @GetMapping("/public")
    @PublicEndpoint
    @Operation(summary = "Acesso público deliberado")
    @ApiResponse(responseCode = "200", description = "Mensagem pública")
    public Map<String, String> publicAccess() {
        return Map.of( "message", "Demonstração pública GEMS" );
    }
    @GetMapping("/global")
    @GlobalEndpoint
    @PreAuthorize("hasRole('CONSULTAR_TENANT')")
    @Operation(summary = "Acesso global")
    @ApiResponse(responseCode = "200", description = "Contexto global")
    public Map<String, String> globalAccess() {
        return Map.of( "scope", "global" );
    }
    @GetMapping("/tenant/read")
    @TenantEndpoint
    @PreAuthorize("hasRole('CONSULTAR_DEMO')")
    @Operation(summary = "Leitura empresarial")
    @ApiResponse(responseCode = "200", description = "Página isolada")
    public Page<DemoRegistroResponseDTO> read() {
        return service.search( new DemoRegistroFilterParams( null ), PageRequest.of( 0, 10 ) );
    }
    @PostMapping("/tenant/write")
    @TenantEndpoint
    @PreAuthorize("hasRole('ALTERAR_DEMO')")
    @Operation(summary = "Gravação empresarial")
    @ApiResponse(responseCode = "200", description = "Registro alterado")
    public DemoRegistroDTO write( @RequestBody DemoRegistroDTO dto, HttpServletRequest request ) {
        super.setBodyToExceptionLog( new DemoRegistroDTO( dto.id(), dto.nome(), dto.descricao(), null ), request );
        return service.save( dto.id(), dto );
    }
}

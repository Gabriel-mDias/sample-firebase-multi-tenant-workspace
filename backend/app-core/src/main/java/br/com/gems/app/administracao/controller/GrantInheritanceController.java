package br.com.gems.app.administracao.controller;

import br.com.gems.app.administracao.service.AdministracaoService;
import br.com.gems.app.model.dto.ContainerCadastroDTO;
import br.com.gems.app.model.dto.GrantCadastroDTO;
import br.com.gems.app.model.dto.GrantRelacionamentoDTO;
import br.com.gems.exception.base.BaseController;
import br.com.gems.firebase.auth.AdministrationViews;
import br.com.gems.firebase.auth.GrantAdministration;
import br.com.gems.security.authorization.TenantEndpoint;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Heranças permanecem limitadas por ator, catálogo, delegabilidade e tenant na SDK. */
@RestController
@RequestMapping("/api/admin/tenants/{tenant}")
@TenantEndpoint
@RequiredArgsConstructor
@Tag(name = "Heranças de concessões")
public class GrantInheritanceController extends BaseController {
    private final AdministracaoService service;
    @PostMapping("/profiles/search")
    @PreAuthorize("hasRole('CONSULTAR_GRANT')")
    @Operation(summary = "Consultar perfis")
    @ApiResponse(responseCode = "200", description = "Perfis")
    public AdministrationViews.Page<AdministrationViews.Profile> profiles( @PathVariable UUID tenant,
            @RequestBody AdministrationViews.Query dto, HttpServletRequest request ) {
        super.setBodyToExceptionLog( dto, request );
        return service.profiles( tenant, dto );
    }
    @PostMapping("/groups/search")
    @PreAuthorize("hasRole('CONSULTAR_GRANT')")
    @Operation(summary = "Consultar grupos")
    @ApiResponse(responseCode = "200", description = "Grupos")
    public AdministrationViews.Page<AdministrationViews.Group> groups( @PathVariable UUID tenant,
            @RequestBody AdministrationViews.Query dto, HttpServletRequest request ) {
        super.setBodyToExceptionLog( dto, request );
        return service.groups( tenant, dto );
    }
    @PostMapping("/profiles")
    @PreAuthorize("hasRole('CONCEDER_GRANT')")
    @Operation(summary = "Criar perfil de concessões")
    @ApiResponse(responseCode = "201", description = "Perfil")
    public ResponseEntity<Map<String, UUID>> profile( @PathVariable UUID tenant, @RequestBody ContainerCadastroDTO dto,
            HttpServletRequest request ) {
        super.setBodyToExceptionLog( dto, request );
        return ResponseEntity.status( 201 )
                .body( Map.of( "id", service.createProfile( tenant, dto.code(), dto.name() ) ) );
    }
    @PostMapping("/groups")
    @PreAuthorize("hasRole('CONCEDER_GRANT')")
    @Operation(summary = "Criar grupo de concessões")
    @ApiResponse(responseCode = "201", description = "Grupo")
    public ResponseEntity<Map<String, UUID>> group( @PathVariable UUID tenant, @RequestBody ContainerCadastroDTO dto,
            HttpServletRequest request ) {
        super.setBodyToExceptionLog( dto, request );
        return ResponseEntity.status( 201 )
                .body( Map.of( "id", service.createGroup( tenant, dto.code(), dto.name() ) ) );
    }
    @PostMapping("/grants/targets/{target}")
    @PreAuthorize("hasRole('CONCEDER_GRANT')")
    @Operation(summary = "Conceder ação direta ou herdada")
    @ApiResponse(responseCode = "201", description = "Grant")
    public ResponseEntity<Map<String, UUID>> grant( @PathVariable UUID tenant,
            @PathVariable GrantAdministration.Target target, @RequestBody GrantCadastroDTO dto,
            HttpServletRequest request ) {
        super.setBodyToExceptionLog( dto, request );
        return ResponseEntity.status( 201 )
                .body( Map.of( "id", service.grantTo( target, tenant, dto.ownerId(), dto.action() ) ) );
    }
    @PostMapping("/grants/links/{link}")
    @PreAuthorize("hasRole('CONCEDER_GRANT')")
    @Operation(summary = "Vincular concessões herdadas")
    @ApiResponse(responseCode = "201", description = "Link")
    public ResponseEntity<Map<String, UUID>> link( @PathVariable UUID tenant,
            @PathVariable GrantAdministration.Link link, @RequestBody GrantRelacionamentoDTO dto,
            HttpServletRequest request ) {
        super.setBodyToExceptionLog( dto, request );
        return ResponseEntity.status( 201 )
                .body( Map.of( "id", service.link( link, tenant, dto.leftId(), dto.rightId() ) ) );
    }
    @DeleteMapping("/grants/targets/{target}/{id}")
    @PreAuthorize("hasRole('REVOGAR_GRANT')")
    @Operation(summary = "Revogar grant direto ou herdado")
    @ApiResponse(responseCode = "204", description = "Revogado")
    public ResponseEntity<Void> revoke( @PathVariable UUID tenant, @PathVariable GrantAdministration.Target target,
            @PathVariable UUID id ) {
        service.revokeTarget( target, tenant, id );
        return ResponseEntity.noContent().build();
    }
    @DeleteMapping("/grants/links/{link}/{id}")
    @PreAuthorize("hasRole('REVOGAR_GRANT')")
    @Operation(summary = "Revogar relacionamento herdado")
    @ApiResponse(responseCode = "204", description = "Revogado")
    public ResponseEntity<Void> revokeLink( @PathVariable UUID tenant, @PathVariable GrantAdministration.Link link,
            @PathVariable UUID id ) {
        service.revokeLink( link, tenant, id );
        return ResponseEntity.noContent().build();
    }
}

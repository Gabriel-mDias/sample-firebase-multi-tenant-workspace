package br.com.gems.app.administracao.controller;

import br.com.gems.app.administracao.service.AdministracaoService;
import br.com.gems.app.model.dto.UsuarioCadastroDTO;
import br.com.gems.app.model.dto.TenantCadastroDTO;
import br.com.gems.app.model.dto.NomeCadastroDTO;
import br.com.gems.app.model.dto.PrimeiroAdminDTO;
import br.com.gems.app.model.dto.MembershipCadastroDTO;
import br.com.gems.app.model.dto.GrantCadastroDTO;
import br.com.gems.exception.base.BaseController;
import br.com.gems.firebase.auth.AdministrationViews;
import br.com.gems.security.authorization.GlobalEndpoint;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/tenants/{tenant}")
@TenantEndpoint
@RequiredArgsConstructor
@Tag(name = "Administração da empresa")
public class TenantAdministrationController extends BaseController {

    private final AdministracaoService service;

    @PostMapping("/memberships/search")
    @PreAuthorize("hasRole('CONSULTAR_MEMBERSHIP')")
    @Operation(summary = "Consultar memberships")
    @ApiResponse(responseCode = "200", description = "Consultar memberships")
    public AdministrationViews.Page<AdministrationViews.Membership> memberships( @PathVariable UUID tenant, @RequestBody AdministrationViews.Query dto, HttpServletRequest request ) {
        super.setBodyToExceptionLog( dto, request );
        return service.memberships( tenant, dto );
    }

    @PostMapping("/grants/search")
    @PreAuthorize("hasRole('CONSULTAR_GRANT')")
    @Operation(summary = "Consultar grants")
    @ApiResponse(responseCode = "200", description = "Consultar grants")
    public AdministrationViews.Page<AdministrationViews.Grant> grants( @PathVariable UUID tenant, @RequestBody AdministrationViews.Query dto, HttpServletRequest request ) {
        super.setBodyToExceptionLog( dto, request );
        return service.grants( tenant, dto );
    }

    @PostMapping("/memberships")
    @PreAuthorize("hasRole('INSERIR_MEMBERSHIP')")
    @Operation(summary = "Criar vínculo na própria empresa")
    @ApiResponse(responseCode = "201", description = "Criar vínculo na própria empresa")
    public ResponseEntity<Map<String, UUID>> addMember( @PathVariable UUID tenant, @RequestBody MembershipCadastroDTO dto, HttpServletRequest request ) {
        super.setBodyToExceptionLog( dto, request );
        return ResponseEntity.status( 201 ).body( Map.of( "id", service.addMember( tenant, dto.userId() ) ) );
    }

    @DeleteMapping("/memberships/{id}")
    @PreAuthorize("hasRole('ENCERRAR_MEMBERSHIP')")
    @Operation(summary = "Encerrar vínculo local")
    @ApiResponse(responseCode = "204", description = "Encerrar vínculo local")
    public ResponseEntity<Void> endMember( @PathVariable UUID tenant, @PathVariable UUID id ) {
        service.endMember( tenant, id );
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/grants")
    @PreAuthorize("hasRole('CONCEDER_GRANT')")
    @Operation(summary = "Conceder ação delegável")
    @ApiResponse(responseCode = "201", description = "Conceder ação delegável")
    public ResponseEntity<Map<String, UUID>> grant( @PathVariable UUID tenant, @RequestBody GrantCadastroDTO dto, HttpServletRequest request ) {
        super.setBodyToExceptionLog( dto, request );
        return ResponseEntity.status( 201 )
                .body( Map.of( "id", service.grant( tenant, dto.ownerId(), dto.action() ) ) );
    }

    @DeleteMapping("/grants/{id}")
    @PreAuthorize("hasRole('REVOGAR_GRANT')")
    @Operation(summary = "Revogar concessão")
    @ApiResponse(responseCode = "204", description = "Revogar concessão")
    public ResponseEntity<Void> revoke( @PathVariable UUID tenant, @PathVariable UUID id ) {
        service.revoke( tenant, id );
        return ResponseEntity.noContent().build();
    }

}

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
@RequestMapping("/api/admin/central")
@GlobalEndpoint
@RequiredArgsConstructor
@Tag(name = "Administração central")
public class CentralAdministrationController extends BaseController {
    private final AdministracaoService service;
    @PostMapping("/users/search")
    @PreAuthorize("hasRole('CONSULTAR_USUARIO')")
    @Operation(summary = "Consultar usuários")
    @ApiResponse(responseCode = "200", description = "Consultar usuários")
    public AdministrationViews.Page<AdministrationViews.User> users( @RequestBody AdministrationViews.Query dto,
            HttpServletRequest request ) {
        super.setBodyToExceptionLog( dto, request );
        return service.users( dto );
    }
    @PostMapping("/users")
    @PreAuthorize("hasRole('INSERIR_USUARIO')")
    @Operation(summary = "Pré-provisionar usuário")
    @ApiResponse(responseCode = "201", description = "Pré-provisionar usuário")
    public ResponseEntity<Map<String, UUID>> createUser( @RequestBody UsuarioCadastroDTO dto,
            HttpServletRequest request ) {
        super.setBodyToExceptionLog( dto, request );
        return ResponseEntity.status( 201 ).body( Map.of( "id", service.createUser( dto ) ) );
    }
    @PutMapping("/users/{id}")
    @PreAuthorize("hasRole('ALTERAR_USUARIO')")
    @Operation(summary = "Alterar usuário")
    @ApiResponse(responseCode = "200", description = "Alterar usuário")
    public ResponseEntity<Void> updateUser( @PathVariable UUID id, @RequestBody NomeCadastroDTO dto,
            HttpServletRequest request ) {
        super.setBodyToExceptionLog( dto, request );
        service.updateUser( id, dto.name(), dto.email() );
        return ResponseEntity.ok().build();
    }
    @PostMapping("/users/{id}/suspend")
    @PreAuthorize("hasRole('SUSPENDER_USUARIO')")
    @Operation(summary = "suspend usuário")
    @ApiResponse(responseCode = "200", description = "suspend usuário")
    public ResponseEntity<Void> suspendUser( @PathVariable UUID id, HttpServletRequest request ) {
        super.setBodyToExceptionLog( Map.of( "id", id ), request );
        service.suspendUser( id );
        return ResponseEntity.ok().build();
    }
    @PostMapping("/users/{id}/reactivate")
    @PreAuthorize("hasRole('REATIVAR_USUARIO')")
    @Operation(summary = "reactivate usuário")
    @ApiResponse(responseCode = "200", description = "reactivate usuário")
    public ResponseEntity<Void> reactivateUser( @PathVariable UUID id, HttpServletRequest request ) {
        super.setBodyToExceptionLog( Map.of( "id", id ), request );
        service.reactivateUser( id );
        return ResponseEntity.ok().build();
    }
    @PostMapping("/tenants/search")
    @PreAuthorize("hasRole('CONSULTAR_TENANT')")
    @Operation(summary = "Consultar empresas")
    @ApiResponse(responseCode = "200", description = "Consultar empresas")
    public AdministrationViews.Page<AdministrationViews.Tenant> tenants( @RequestBody AdministrationViews.Query dto,
            HttpServletRequest request ) {
        super.setBodyToExceptionLog( dto, request );
        return service.tenants( dto );
    }
    @PostMapping("/tenants")
    @PreAuthorize("hasRole('INSERIR_TENANT')")
    @Operation(summary = "Criar empresa pendente de provisionamento")
    @ApiResponse(responseCode = "201", description = "Criar empresa pendente de provisionamento")
    public ResponseEntity<Map<String, UUID>> createTenant( @RequestBody TenantCadastroDTO dto,
            HttpServletRequest request ) {
        super.setBodyToExceptionLog( dto, request );
        return ResponseEntity.status( 201 ).body( Map.of( "id", service.createTenant( dto ) ) );
    }
    @PutMapping("/tenants/{id}")
    @PreAuthorize("hasRole('ALTERAR_TENANT')")
    @Operation(summary = "Renomear empresa mantendo alias")
    @ApiResponse(responseCode = "200", description = "Renomear empresa mantendo alias")
    public ResponseEntity<Void> renameTenant( @PathVariable UUID id, @RequestBody NomeCadastroDTO dto,
            HttpServletRequest request ) {
        super.setBodyToExceptionLog( dto, request );
        service.renameTenant( id, dto.name() );
        return ResponseEntity.ok().build();
    }
    @PostMapping("/tenants/{id}/suspend")
    @PreAuthorize("hasRole('SUSPENDER_TENANT')")
    @Operation(summary = "Suspender empresa")
    @ApiResponse(responseCode = "200", description = "Suspender empresa")
    public ResponseEntity<Void> suspendTenant( @PathVariable UUID id, HttpServletRequest request ) {
        super.setBodyToExceptionLog( Map.of( "id", id ), request );
        service.suspendTenant( id );
        return ResponseEntity.ok().build();
    }
    @PostMapping("/users/{id}/global-grants/search")
    @PreAuthorize("hasRole('CONSULTAR_ACAO_GLOBAL')")
    @Operation(summary = "Consultar concessões globais")
    @ApiResponse(responseCode = "200", description = "Concessões")
    public AdministrationViews.Page<AdministrationViews.Grant> globalGrants( @PathVariable UUID id,
            @RequestBody AdministrationViews.Query dto, HttpServletRequest request ) {
        super.setBodyToExceptionLog( dto, request );
        return service.globalGrants( id, dto );
    }
    @PostMapping("/users/{id}/global-grants")
    @PreAuthorize("hasRole('CONCEDER_ACAO_GLOBAL')")
    @Operation(summary = "Conceder ação global")
    @ApiResponse(responseCode = "201", description = "Concessão")
    public ResponseEntity<Map<String, UUID>> globalGrant( @PathVariable UUID id, @RequestBody GrantCadastroDTO dto,
            HttpServletRequest request ) {
        super.setBodyToExceptionLog( dto, request );
        return ResponseEntity.status( 201 ).body( Map.of( "id", service.grantGlobal( id, dto.action() ) ) );
    }
    @DeleteMapping("/users/{id}/global-grants/{grant}")
    @PreAuthorize("hasRole('REVOGAR_ACAO_GLOBAL')")
    @Operation(summary = "Revogar ação global")
    @ApiResponse(responseCode = "204", description = "Revogada")
    public ResponseEntity<Void> revokeGlobal( @PathVariable UUID id, @PathVariable UUID grant ) {
        service.revokeGlobal( id, grant );
        return ResponseEntity.noContent().build();
    }
    @PostMapping("/tenants/{id}/first-administrator")
    @PreAuthorize("hasRole('CRIAR_ADMINISTRADOR_TENANT')")
    @Operation(summary = "Criar primeiro administrador da empresa")
    @ApiResponse(responseCode = "201", description = "Criar primeiro administrador da empresa")
    public ResponseEntity<Map<String, UUID>> firstAdministrator( @PathVariable UUID id,
            @RequestBody PrimeiroAdminDTO dto, HttpServletRequest request ) {
        super.setBodyToExceptionLog( dto, request );
        return ResponseEntity.status( 201 )
                .body( Map.of( "id", service.firstAdministrator( id, dto.userId(), dto.actions() ) ) );
    }
}

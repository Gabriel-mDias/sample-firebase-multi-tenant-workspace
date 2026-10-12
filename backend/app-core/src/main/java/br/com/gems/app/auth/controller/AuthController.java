package br.com.gems.app.auth.controller;

import br.com.gems.app.auth.service.AuthService;
import br.com.gems.app.model.response.AuthMeResponse;
import br.com.gems.app.model.response.AuthContextResponse;
import br.com.gems.exception.base.BaseController;
import br.com.gems.security.authorization.GlobalEndpoint;
import br.com.gems.security.authorization.TenantEndpoint;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Conta e contexto")
public class AuthController extends BaseController {

    private final AuthService service;

    @GetMapping("/me")
    @GlobalEndpoint
    @PreAuthorize("hasRole('CONSULTAR_PROPRIA_CONTA')")
    @Operation(summary = "Consultar a própria conta")
    @ApiResponse(responseCode = "200", description = "Snapshot da conta")
    public AuthMeResponse me() {
        return service.me();
    }

    @GetMapping("/context")
    @TenantEndpoint
    @PreAuthorize("hasRole('CONSULTAR_PROPRIO_CONTEXTO_TENANT')")
    @Operation(summary = "Consultar contexto da empresa selecionada")
    @ApiResponse(responseCode = "200", description = "Snapshot do tenant")
    public AuthContextResponse context() {
        return service.context();
    }

}

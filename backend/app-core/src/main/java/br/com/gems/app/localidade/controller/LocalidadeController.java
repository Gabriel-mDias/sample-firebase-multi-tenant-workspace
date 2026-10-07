package br.com.gems.app.localidade.controller;

import br.com.gems.security.authorization.GlobalEndpoint;
import br.com.gems.app.localidade.service.LocalidadeService;
import br.com.gems.app.model.dto.EstadoDTO;
import br.com.gems.app.model.dto.PaisDTO;
import br.com.gems.exception.base.BaseController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Dados de referência de localidade. */
@RestController
@RequestMapping("/api/localidades")
@RequiredArgsConstructor
@Tag(name = "Localidades", description = "Dados de referência de países e estados")
@GlobalEndpoint
public class LocalidadeController extends BaseController {

    private final LocalidadeService service;

    @GetMapping("/paises")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Listar países", description = "Países disponíveis para seleção, ordenados por nome.")
    @ApiResponse(responseCode = "200", description = "Lista de países")
    public ResponseEntity<List<PaisDTO>> listarPaises() {
        return ResponseEntity.ok( service.listarPaises() );
    }

    @GetMapping("/paises/{paisId}/estados")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Listar estados de um país", description = "Estados do país informado, ordenados por nome. País sem estados devolve lista vazia.")
    @ApiResponse(responseCode = "200", description = "Lista de estados")
    public ResponseEntity<List<EstadoDTO>> listarEstadosPorPais( @PathVariable String paisId ) {
        return ResponseEntity.ok( service.listarEstadosPorPais( paisId ) );
    }

}

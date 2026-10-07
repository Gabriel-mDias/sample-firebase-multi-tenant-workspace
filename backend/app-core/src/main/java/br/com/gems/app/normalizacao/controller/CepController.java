package br.com.gems.app.normalizacao.controller;

import br.com.gems.security.authorization.GlobalEndpoint;
import br.com.gems.app.model.dto.EnderecoDTO;
import br.com.gems.app.normalizacao.service.CepService;
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

/**
 * Consulta de endereço por CEP.
 *
 * <p>
 * Disponível a todos os cadastros que coletam endereço, não apenas ao de clientes.
 */
@RestController
@RequestMapping("/api/enderecos")
@RequiredArgsConstructor
@Tag(name = "Endereços", description = "Consulta de endereço por CEP")
@GlobalEndpoint
public class CepController extends BaseController {

    private final CepService service;

    @GetMapping("/cep/{cep}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Consultar endereço por CEP", description = "Devolve cidade, bairro, logradouro e o estado já resolvido para o cadastro "
            + "de referência. Número e complemento nunca vêm preenchidos: são do endereço, não do CEP.")
    @ApiResponse(responseCode = "200", description = "Endereço encontrado")
    @ApiResponse(responseCode = "400", description = "CEP fora do formato ou inexistente")
    @ApiResponse(responseCode = "502", description = "Provedor de CEP indisponível — preencha o endereço manualmente")
    public ResponseEntity<EnderecoDTO> buscarPorCep( @PathVariable String cep ) {
        return ResponseEntity.ok( service.buscarPorCep( cep ) );
    }

}

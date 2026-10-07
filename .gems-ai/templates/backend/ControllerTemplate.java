// origem: local; precedencia: local-vence; versao: 2.1.0
package br.com.gems.{app}.{dominio}.controller;

import br.com.gems.exception.base.BaseController;
import br.com.gems.{app}.model.dto.{dominio}.{Entidade}DTO;
import br.com.gems.{app}.model.params.{dominio}.{Entidade}FilterParams;
import br.com.gems.{app}.model.response.{dominio}.{Entidade}ResponseDTO;
import br.com.gems.{app}.{dominio}.service.{Entidade}Service;
import io.swagger.v3.oas.annotations.Operation;
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

import java.util.UUID;

// Forma: rules/backend-java/controllers.md. Estende BaseController; uma Service; o corpo é devolvido
// direto (o advice do gems-exception envelopa); setBodyToExceptionLog( dto, request ) em todo POST/PUT.
// Autorização por AÇÃO concreta do enum AcaoSistema ({app}-security), nunca por nível ("ADMIN"): a ação
// entra em hasRole() com o nome exato da constante — ação inexistente não compila o catálogo.
@RestController
@RequestMapping("/api/{recurso-kebab-case}")
@RequiredArgsConstructor
@Tag(name = "{Entidade}", description = "Gerenciamento de {entidades}")
public class {Entidade}Controller extends BaseController {

    private final {Entidade}Service service;

    @GetMapping("/{id}")
    @Operation(summary = "Busca {entidade} por id")
    @PreAuthorize("hasRole('CONSULTAR_{ENTIDADE}')")
    public ResponseEntity<{Entidade}DTO> findById( @PathVariable UUID id ) {
        return ResponseEntity.ok( service.findById( id ) );
    }

    @PostMapping("/search")
    @Operation(summary = "Pesquisa paginada de {entidades}")
    @PreAuthorize("hasRole('CONSULTAR_{ENTIDADE}')")
    public ResponseEntity<Page<{Entidade}ResponseDTO>> search(
            @RequestBody {Entidade}FilterParams filterParams,
            @PageableDefault(page = 0, size = 10, sort = "dataCriacao", direction = Sort.Direction.DESC)
            Pageable pageable ) {
        return ResponseEntity.ok( service.search( filterParams, pageable ) );
    }

    @PostMapping
    @Operation(summary = "Cria {entidade}")
    @PreAuthorize("hasRole('MANTER_{ENTIDADE}')")
    public ResponseEntity<{Entidade}DTO> insert( @RequestBody {Entidade}DTO dto, HttpServletRequest request ) {
        super.setBodyToExceptionLog( dto, request );
        return ResponseEntity.status( HttpStatus.CREATED ).body( service.insert( dto ) );
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza {entidade}")
    @PreAuthorize("hasRole('MANTER_{ENTIDADE}')")
    public ResponseEntity<{Entidade}DTO> save( @PathVariable UUID id, @RequestBody {Entidade}DTO dto,
            HttpServletRequest request ) {
        super.setBodyToExceptionLog( dto, request );
        return ResponseEntity.ok( service.save( id, dto ) );
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Exclui {entidade}")
    @PreAuthorize("hasRole('MANTER_{ENTIDADE}')")
    public ResponseEntity<Void> delete( @PathVariable UUID id ) {
        service.delete( id );
        return ResponseEntity.noContent().build();
    }
}

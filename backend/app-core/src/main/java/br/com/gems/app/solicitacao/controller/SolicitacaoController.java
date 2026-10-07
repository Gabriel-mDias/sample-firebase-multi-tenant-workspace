package br.com.gems.app.solicitacao.controller;

import br.com.gems.security.authorization.GlobalEndpoint;
import br.com.gems.app.model.dto.SolicitacaoDTO;
import br.com.gems.app.model.params.SolicitacaoFilterParams;
import br.com.gems.app.model.response.SolicitacaoResponseDTO;
import br.com.gems.app.solicitacao.service.SolicitacaoService;
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

import java.util.UUID;

/**
 * Endpoints da solicitação.
 *
 * <p>
 * <b>A situação nunca entra pelo corpo.</b> Cada transição é um endpoint de ação próprio, com a
 * sua ação de {@code AcaoSistema} — é o que torna a autorização por transição real em vez de
 * decorativa. Um {@code PUT} genérico que aceitasse a situação daria a quem edita o cadastro o poder
 * de concluir qualquer solicitação.
 */
@RestController
@RequestMapping("/api/solicitacoes")
@RequiredArgsConstructor
@Tag(name = "Solicitações", description = "Fluxo de solicitações")
@GlobalEndpoint
public class SolicitacaoController extends BaseController {

    private final SolicitacaoService service;

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('CONSULTAR_SOLICITACAO')")
    @Operation(summary = "Buscar solicitação por id")
    @ApiResponse(responseCode = "200", description = "Solicitação encontrada")
    @ApiResponse(responseCode = "400", description = "Solicitação inexistente ou excluída")
    public ResponseEntity<SolicitacaoDTO> findById( @PathVariable String id ) {
        return ResponseEntity.ok( service.findById( id ) );
    }

    @PostMapping("/search")
    @PreAuthorize("hasRole('CONSULTAR_SOLICITACAO')")
    @Operation(summary = "Pesquisar solicitações", description = "Busca paginada. Filtros de título, situação e parceiro são combináveis.")
    @ApiResponse(responseCode = "200", description = "Página de solicitações")
    public ResponseEntity<Page<SolicitacaoResponseDTO>> search(
            @RequestBody SolicitacaoFilterParams filterParams,
            @PageableDefault(page = 0, size = 10, sort = "dataCriacao", direction = Sort.Direction.DESC) Pageable pageable ) {
        return ResponseEntity.ok( service.search( filterParams, pageable ) );
    }

    @PostMapping
    @PreAuthorize("hasRole('INSERIR_SOLICITACAO')")
    @Operation(summary = "Abrir solicitação", description = "Nasce sempre em RASCUNHO. A situação do corpo, se vier, é ignorada.")
    @ApiResponse(responseCode = "201", description = "Solicitação criada")
    @ApiResponse(responseCode = "400", description = "Validação de negócio")
    public ResponseEntity<SolicitacaoDTO> insert( @RequestBody SolicitacaoDTO dto, HttpServletRequest request ) {
        super.setBodyToExceptionLog( dto, request );
        return ResponseEntity.status( HttpStatus.CREATED ).body( service.insert( dto ) );
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ALTERAR_SOLICITACAO')")
    @Operation(summary = "Alterar solicitação", description = "Altera apenas título e descrição. A situação do corpo é ignorada.")
    @ApiResponse(responseCode = "200", description = "Solicitação alterada")
    @ApiResponse(responseCode = "400", description = "Validação de negócio, ou solicitação terminal")
    public ResponseEntity<SolicitacaoDTO> save( @PathVariable String id, @RequestBody SolicitacaoDTO dto,
            HttpServletRequest request ) {
        super.setBodyToExceptionLog( dto, request );
        return ResponseEntity.ok( service.save( id, dto ) );
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('EXCLUIR_SOLICITACAO')")
    @Operation(summary = "Excluir solicitação", description = "Só rascunho e solicitação cancelada podem ser excluídos.")
    @ApiResponse(responseCode = "204", description = "Solicitação excluída")
    @ApiResponse(responseCode = "400", description = "Solicitação inexistente, ou em situação que não permite exclusão")
    public ResponseEntity<Void> delete( @PathVariable String id ) {
        service.delete( id );
        return ResponseEntity.noContent().build();
    }

    // ---- Transições --------------------------------------------------------------------------
    // Uma ação por transição. É aqui que a autorização por transição deixa de ser teoria.

    @PostMapping("/{id}/abrir")
    @PreAuthorize("hasRole('ABRIR_SOLICITACAO')")
    @Operation(summary = "Publicar a solicitação", description = "RASCUNHO -> ABERTA.")
    @ApiResponse(responseCode = "200", description = "Solicitação aberta")
    @ApiResponse(responseCode = "400", description = "Transição não permitida a partir da situação atual")
    public ResponseEntity<SolicitacaoDTO> abrir( @PathVariable UUID id ) {
        return ResponseEntity.ok( service.abrir( id ) );
    }

    @PostMapping("/{id}/iniciar")
    @PreAuthorize("hasRole('INICIAR_SOLICITACAO')")
    @Operation(summary = "Assumir a execução", description = "ABERTA -> EM_ANDAMENTO.")
    @ApiResponse(responseCode = "200", description = "Solicitação iniciada")
    @ApiResponse(responseCode = "400", description = "Transição não permitida a partir da situação atual")
    public ResponseEntity<SolicitacaoDTO> iniciar( @PathVariable UUID id ) {
        return ResponseEntity.ok( service.iniciar( id ) );
    }

    @PostMapping("/{id}/concluir")
    @PreAuthorize("hasRole('CONCLUIR_SOLICITACAO')")
    @Operation(summary = "Concluir a solicitação", description = "EM_ANDAMENTO -> CONCLUIDA. Recusa se houver etapa pendente. Publica o "
            + "evento consumido pelo domínio parceiro.")
    @ApiResponse(responseCode = "200", description = "Solicitação concluída")
    @ApiResponse(responseCode = "400", description = "Etapa pendente, ou transição não permitida")
    public ResponseEntity<SolicitacaoDTO> concluir( @PathVariable UUID id ) {
        return ResponseEntity.ok( service.concluir( id ) );
    }

    @PostMapping("/{id}/cancelar")
    @PreAuthorize("hasRole('CANCELAR_SOLICITACAO')")
    @Operation(summary = "Cancelar a solicitação", description = "Exige motivo. Situação terminal.")
    @ApiResponse(responseCode = "200", description = "Solicitação cancelada")
    @ApiResponse(responseCode = "400", description = "Motivo ausente, ou transição não permitida")
    public ResponseEntity<SolicitacaoDTO> cancelar( @PathVariable UUID id,
            @RequestBody CancelamentoRequest corpo ) {
        return ResponseEntity.ok( service.cancelar( id, corpo.motivo() ) );
    }

    /** Corpo do cancelamento. Só o motivo — a situação de destino é o próprio endpoint. */
    public record CancelamentoRequest(String motivo) {
    }

}

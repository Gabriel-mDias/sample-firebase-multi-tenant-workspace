package br.com.gems.app.auditoria.controller;

import br.com.gems.security.authorization.GlobalEndpoint;
import br.com.gems.app.auditoria.service.AuditoriaService;
import br.com.gems.app.model.params.AuditoriaFilterParams;
import br.com.gems.app.model.response.RegistroAuditoriaResponseDTO;
import br.com.gems.exception.base.BaseController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * A trilha de auditoria, do lado de quem lê.
 *
 * <h2>Só existem verbos de leitura, e a ausência é o requisito</h2>
 * Não há {@code POST} de registro, {@code PUT} nem {@code DELETE} — é exigência da trilha. Quem grava é o
 * mecanismo de persistência; um endpoint de escrita permitiria forjar trilha, e um de exclusão faria
 * dela algo apagável. Uma trilha apagável não é trilha.
 *
 * <p>
 * A ausência tem teste: {@code AuditoriaControllerSemEscritaTest} falha se um método mapeado de
 * escrita aparecer aqui. Regra que só existe como comentário é regra que será violada.
 */
@RestController
@RequestMapping("/api/auditoria")
@Tag(name = "Auditoria", description = "Consulta da trilha de alterações (somente leitura)")
@GlobalEndpoint
public class AuditoriaController extends BaseController {

    private final AuditoriaService service;

    public AuditoriaController( AuditoriaService service ) {
        this.service = service;
    }

    @GetMapping("/{entidade}/{id}")
    @PreAuthorize("hasRole('CONSULTAR_AUDITORIA')")
    @Operation(summary = "Histórico de um registro", description = "Quem alterou o quê e quando, em ordem cronológica decrescente. "
            + "O histórico sobrevive à exclusão lógica do registro.")
    @ApiResponse(responseCode = "200", description = "Página do histórico — vazia é resposta legítima")
    @ApiResponse(responseCode = "400", description = "Nome de entidade desconhecido")
    @ApiResponse(responseCode = "403", description = "Sem a ação CONSULTAR_AUDITORIA")
    public ResponseEntity<Page<RegistroAuditoriaResponseDTO>> historico(
            @PathVariable String entidade,
            @PathVariable UUID id,
            @PageableDefault(page = 0, size = 20, sort = "dataOperacao", direction = Sort.Direction.DESC) Pageable pageable ) {
        return ResponseEntity.ok( service.historico( entidade, id, pageable ) );
    }

    @PostMapping("/search")
    @PreAuthorize("hasRole('CONSULTAR_AUDITORIA')")
    @Operation(summary = "Pesquisar a trilha", description = "Busca ampla, para a investigação que não parte de um registro conhecido. "
            + "A data final é INCLUSIVA até o fim do dia.")
    @ApiResponse(responseCode = "200", description = "Página de registros")
    @ApiResponse(responseCode = "400", description = "Entidade ou operação inválida, ou data inicial posterior à final")
    @ApiResponse(responseCode = "403", description = "Sem a ação CONSULTAR_AUDITORIA")
    public ResponseEntity<Page<RegistroAuditoriaResponseDTO>> search(
            @RequestBody AuditoriaFilterParams filterParams,
            @PageableDefault(page = 0, size = 20, sort = "dataOperacao", direction = Sort.Direction.DESC) Pageable pageable ) {
        return ResponseEntity.ok( service.search( filterParams, pageable ) );
    }
}

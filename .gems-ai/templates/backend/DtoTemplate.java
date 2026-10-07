// origem: local; precedencia: local-vence; versao: 2.1.0
//
// Três arquivos, um por classe, em três pacotes do módulo {app}-model (architecture.md §3):
//   model/dto/{dominio}/{Entidade}DTO.java            — espelho (corpo de POST/PUT, resposta de GET)
//   model/response/{dominio}/{Entidade}ResponseDTO.java — linha da grid (resposta de POST /search)
//   model/params/{dominio}/{Entidade}FilterParams.java  — filtros (corpo de POST /search)
// Estão juntos aqui só para caberem num template. Id é UUID — o mesmo tipo da entidade.

package br.com.gems.{app}.model.dto.{dominio};

import br.com.gems.{app}.model.enums.{dominio}.{TipoEnum};
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class {Entidade}DTO {

    private UUID id;
    private String nome;
    private {TipoEnum} tipo;
    private String observacao;
    private UUID {relacionado}Id;
    private LocalDateTime dataCriacao;
    private LocalDateTime dataExclusao;
}

// ---- model/response/{dominio}/{Entidade}ResponseDTO.java ----------------------------------------
// Plano: campos de JOIN achatados. Construído no HQL por constructor expression — a ordem dos
// parâmetros do construtor é a ordem do SELECT (repositories.md §3).

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
class {Entidade}ResponseDTO {

    private UUID id;
    private String nome;
    private {TipoEnum} tipo;
    private String {relacionado}Nome;
    private LocalDateTime dataCriacao;
}

// ---- model/params/{dominio}/{Entidade}FilterParams.java ------------------------------------------
// Só primitivos, String, enum e datas — nunca objeto aninhado. Range com sufixo Min/Max.

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
class {Entidade}FilterParams {

    private String nome;
    private {TipoEnum} tipo;
    private LocalDate dataMin;
    private LocalDate dataMax;
}

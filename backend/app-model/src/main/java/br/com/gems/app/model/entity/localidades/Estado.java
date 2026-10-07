package br.com.gems.app.model.entity.localidades;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedAttributeNode;
import jakarta.persistence.NamedEntityGraph;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.Formula;

import java.util.UUID;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
@Entity
@Table(name = "ESTADO")
@NamedEntityGraph(name = "estado-entity-graph", attributeNodes = {
        @NamedAttributeNode(value = "pais"),
})
public class Estado {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "ID_ESTADO")
    private UUID id;

    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_PAIS", nullable = false)
    private Pais pais;

    @Column(name = "NM_ESTADO", nullable = false)
    private String nome;

    @Column(name = "NM_SIGLA_ESTADO", nullable = false, length = 2)
    private String siglaEstado;

}

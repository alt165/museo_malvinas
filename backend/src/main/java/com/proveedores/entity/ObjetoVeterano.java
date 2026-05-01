package com.proveedores.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(
        name = "objetos_veteranos",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_objeto_veterano_tipo",
                columnNames = {"objeto_museo_id", "veterano_id", "tipo_relacion"}
        ),
        indexes = {
            @Index(name = "idx_objeto_veterano_objeto", columnList = "objeto_museo_id"),
            @Index(name = "idx_objeto_veterano_veterano", columnList = "veterano_id")
        }
)
public class ObjetoVeterano extends EntidadBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "objeto_museo_id", nullable = false)
    private ObjetoMuseo objetoMuseo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "veterano_id", nullable = false)
    private Veterano veterano;

    @Column(name = "tipo_relacion", nullable = false, length = 100)
    private String tipoRelacion;

    @Column(columnDefinition = "TEXT")
    private String descripcion;
}

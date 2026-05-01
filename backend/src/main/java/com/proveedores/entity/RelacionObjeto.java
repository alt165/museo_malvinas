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
        name = "relaciones_objeto",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_relacion_objeto_direccional",
                columnNames = {"objeto_origen_id", "objeto_destino_id", "tipo_relacion"}
        ),
        indexes = {
            @Index(name = "idx_relacion_objeto_origen", columnList = "objeto_origen_id"),
            @Index(name = "idx_relacion_objeto_destino", columnList = "objeto_destino_id")
        }
)
public class RelacionObjeto extends EntidadBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "objeto_origen_id", nullable = false)
    private ObjetoMuseo objetoOrigen;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "objeto_destino_id", nullable = false)
    private ObjetoMuseo objetoDestino;

    @Column(name = "tipo_relacion", nullable = false, length = 80)
    private String tipoRelacion;

    @Column(columnDefinition = "TEXT")
    private String descripcion;
}

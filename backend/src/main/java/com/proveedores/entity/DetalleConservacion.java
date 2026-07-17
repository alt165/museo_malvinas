package com.proveedores.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(
        name = "detalles_estado_conservacion",
        indexes = {
                @Index(name = "idx_detalle_conservacion_nombre", columnList = "nombre"),
                @Index(name = "idx_detalle_conservacion_activo", columnList = "activo")
        }
)
public class DetalleConservacion extends EntidadBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 80)
    private String codigo;

    @Column(nullable = false, length = 160)
    private String nombre;

    @Column(columnDefinition = "TEXT")
    private String descripcion;
}

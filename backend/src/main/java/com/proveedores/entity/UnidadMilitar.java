package com.proveedores.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
        name = "unidades_militares",
        indexes = {
                @Index(name = "idx_unidad_militar_fuerza_activo", columnList = "fuerza, activo"),
                @Index(name = "idx_unidad_militar_nombre", columnList = "nombre"),
                @Index(name = "idx_unidad_militar_sigla", columnList = "sigla")
        }
)
public class UnidadMilitar extends EntidadBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private Fuerza fuerza;

    @Column(nullable = false, length = 180)
    private String nombre;

    @Column(length = 40)
    private String sigla;

    @Column(name = "tipo_unidad", length = 80)
    private String tipoUnidad;

    @Column(columnDefinition = "TEXT")
    private String descripcion;
}

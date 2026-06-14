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
        name = "rangos_militares",
        indexes = {
                @Index(name = "idx_rango_militar_fuerza_activo", columnList = "fuerza, activo"),
                @Index(name = "idx_rango_militar_orden", columnList = "fuerza, orden_jerarquico")
        }
)
public class RangoMilitar extends EntidadBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private Fuerza fuerza;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(name = "orden_jerarquico", nullable = false)
    private Integer ordenJerarquico;
}

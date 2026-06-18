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
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(
        name = "actuaciones_veteranos",
        indexes = @Index(name = "idx_actuacion_veterano", columnList = "veterano_id")
)
public class ActuacionVeterano extends EntidadBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "veterano_id", nullable = false)
    private Veterano veterano;

    @Column(length = 80)
    private String rango;

    @Column(length = 120)
    private String unidad;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rango_id")
    private RangoMilitar rangoMilitar;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unidad_id")
    private UnidadMilitar unidadMilitar;

    @Column(length = 120)
    private String rol;

    @Column(name = "fecha_inicio")
    private LocalDate fechaInicio;

    @Column(name = "fecha_fin")
    private LocalDate fechaFin;

    @Column(columnDefinition = "TEXT")
    private String descripcion;
}

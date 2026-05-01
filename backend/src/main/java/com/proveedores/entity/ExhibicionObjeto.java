package com.proveedores.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(
        name = "exhibicion_objeto",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_exhibicion_objeto",
                columnNames = {"exhibicion_id", "objeto_museo_id"}
        ),
        indexes = {
            @Index(name = "idx_exhibicion_objeto_exhibicion", columnList = "exhibicion_id"),
            @Index(name = "idx_exhibicion_objeto_objeto_estado", columnList = "objeto_museo_id, estado")
        }
)
public class ExhibicionObjeto extends EntidadBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exhibicion_id", nullable = false)
    private Exhibicion exhibicion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "objeto_museo_id", nullable = false)
    private ObjetoMuseo objetoMuseo;

    @Column(name = "fecha_inclusion", nullable = false)
    private LocalDate fechaInclusion;

    @Column(name = "fecha_retiro")
    private LocalDate fechaRetiro;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private EstadoExhibicionObjeto estado;

    @Column(name = "devolucion_verificada", nullable = false)
    private Boolean devolucionVerificada = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "verificado_por_usuario_id")
    private Usuario verificadoPor;

    @Column(name = "fecha_verificacion")
    private LocalDateTime fechaVerificacion;

    @Column(name = "observaciones_devolucion", columnDefinition = "TEXT")
    private String observacionesDevolucion;
}

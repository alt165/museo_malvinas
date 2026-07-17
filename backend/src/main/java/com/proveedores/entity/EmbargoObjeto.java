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
        name = "embargos_objeto",
        indexes = {
            @Index(name = "idx_embargos_objeto_objeto", columnList = "objeto_museo_id"),
            @Index(name = "idx_embargos_objeto_vigente", columnList = "fecha_finalizacion")
        }
)
public class EmbargoObjeto extends EntidadBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "objeto_museo_id", nullable = false)
    private ObjetoMuseo objetoMuseo;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    @Column(name = "fecha_finalizacion")
    private LocalDate fechaFinalizacion;

    @Column(columnDefinition = "TEXT")
    private String observaciones;
}

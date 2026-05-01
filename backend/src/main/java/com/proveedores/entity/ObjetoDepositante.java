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
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(
        name = "objeto_depositante",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_objeto_depositante",
                columnNames = {"objeto_museo_id", "depositante_id"}
        ),
        indexes = {
            @Index(name = "idx_objeto_depositante_objeto", columnList = "objeto_museo_id"),
            @Index(name = "idx_objeto_depositante_depositante", columnList = "depositante_id")
        }
)
public class ObjetoDepositante extends EntidadBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "objeto_museo_id", nullable = false)
    private ObjetoMuseo objetoMuseo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "depositante_id", nullable = false)
    private Depositante depositante;

    @Column(name = "fecha_deposito")
    private LocalDate fechaDeposito;

    @Column(name = "tipo_deposito", length = 80)
    private String tipoDeposito;

    @Column(columnDefinition = "TEXT")
    private String observaciones;
}

package com.proveedores.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(
        name = "objetos_museo",
        indexes = {
            @Index(name = "idx_objeto_museo_numero_inventario", columnList = "numero_inventario"),
            @Index(name = "idx_objeto_museo_denominacion", columnList = "denominacion_objeto")
        }
)
@Inheritance(strategy = InheritanceType.JOINED)
public class ObjetoMuseo extends EntidadBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "numero_inventario", nullable = false, unique = true, length = 80)
    private String numeroInventario;

    @Column(name = "denominacion_objeto", nullable = false, length = 160)
    private String denominacionObjeto;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "descripcion_tecnica", columnDefinition = "TEXT")
    private String descripcionTecnica;

    @Column(columnDefinition = "TEXT")
    private String materiales;

    @Column(columnDefinition = "TEXT")
    private String dimensiones;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_conservacion", length = 40)
    private EstadoConservacion estadoConservacion;

}

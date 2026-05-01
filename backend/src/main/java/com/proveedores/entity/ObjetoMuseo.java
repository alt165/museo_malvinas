package com.proveedores.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
            @Index(name = "idx_objeto_museo_nombre", columnList = "nombre")
        }
)
@Inheritance(strategy = InheritanceType.JOINED)
public class ObjetoMuseo extends EntidadBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "numero_inventario", nullable = false, unique = true, length = 80)
    private String numeroInventario;

    @Column(nullable = false, length = 160)
    private String nombre;

    @Column(name = "tipo_objeto", length = 100)
    private String tipoObjeto;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

}

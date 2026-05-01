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
        name = "objeto_categoria",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_objeto_categoria",
                columnNames = {"objeto_museo_id", "categoria_objeto_id"}
        ),
        indexes = {
            @Index(name = "idx_objeto_categoria_objeto", columnList = "objeto_museo_id"),
            @Index(name = "idx_objeto_categoria_categoria", columnList = "categoria_objeto_id")
        }
)
public class ObjetoCategoria extends EntidadBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "objeto_museo_id", nullable = false)
    private ObjetoMuseo objetoMuseo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "categoria_objeto_id", nullable = false)
    private CategoriaObjeto categoriaObjeto;

    @Column(columnDefinition = "TEXT")
    private String observaciones;
}

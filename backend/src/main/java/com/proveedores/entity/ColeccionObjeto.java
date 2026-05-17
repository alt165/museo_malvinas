package com.proveedores.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.HashSet;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "colecciones_objetos")
public class ColeccionObjeto extends EntidadBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 160)
    private String nombre;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @OneToMany(mappedBy = "coleccionObjeto")
    private Set<ObjetoMuseo> objetos = new HashSet<>();
}

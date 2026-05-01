package com.proveedores.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "objetos_digitales")
public class ObjetoDigital extends ObjetoMuseo {

    @Column(length = 80)
    private String formatoDigital;

    @Column(length = 120)
    private String identificadorDigital;

    @Column(columnDefinition = "TEXT")
    private String metadatos;
}

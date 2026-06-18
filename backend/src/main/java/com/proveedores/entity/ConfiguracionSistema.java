package com.proveedores.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "configuracion_sistema")
public class ConfiguracionSistema {

    @Id
    @Column(length = 120)
    private String clave;

    @Column(nullable = false, length = 255)
    private String valor;

    @Column(length = 255)
    private String descripcion;
}

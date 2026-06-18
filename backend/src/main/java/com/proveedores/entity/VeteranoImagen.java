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
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(
        name = "veterano_imagen",
        indexes = {
                @Index(name = "idx_veterano_imagen_veterano", columnList = "veterano_id"),
                @Index(name = "idx_veterano_imagen_activo", columnList = "activo"),
                @Index(name = "idx_veterano_imagen_orden", columnList = "orden")
        }
)
public class VeteranoImagen extends EntidadBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "veterano_id", nullable = false)
    private Veterano veterano;

    @Column(name = "nombre_archivo", nullable = false, length = 255)
    private String nombreArchivo;

    @Column(name = "nombre_archivo_almacenado", nullable = false, length = 255)
    private String nombreArchivoAlmacenado;

    @Column(name = "tipo_contenido", nullable = false, length = 120)
    private String tipoContenido;

    @Column(name = "tamanio_bytes", nullable = false)
    private Long tamanioBytes;

    @Column(name = "ruta_archivo", nullable = false, length = 500)
    private String rutaArchivo;

    @Column(name = "ruta_relativa", length = 500)
    private String rutaRelativa;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Column(nullable = false)
    private Integer orden = 0;

    @Column(name = "fecha_carga", nullable = false)
    private LocalDateTime fechaCarga;

    @Column(name = "cargado_por", length = 160)
    private String cargadoPor;
}

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
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(
        name = "fotos_objeto_museo",
        indexes = @Index(name = "idx_fotos_objeto_museo_objeto", columnList = "objeto_museo_id")
)
public class FotoObjetoMuseo extends EntidadBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "objeto_museo_id", nullable = false)
    private ObjetoMuseo objetoMuseo;

    @Column(name = "nombre_archivo", nullable = false, length = 255)
    private String nombreArchivo;

    @Column(name = "nombre_archivo_original", length = 255)
    private String nombreArchivoOriginal;

    @Column(name = "nombre_archivo_almacenado", length = 255)
    private String nombreArchivoAlmacenado;

    @Column(name = "content_type", nullable = false, length = 120)
    private String contentType;

    @Column(name = "tamanio_bytes", nullable = false)
    private Long tamanioBytes;

    @Column(name = "ruta_almacenamiento", nullable = false, length = 500)
    private String rutaAlmacenamiento;

    @Column(name = "ruta_relativa", length = 500)
    private String rutaRelativa;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private VisibilidadCampo visibilidad = VisibilidadCampo.PUBLICO;

    @Column(name = "fecha_carga", nullable = false)
    private LocalDateTime fechaCarga;

    @Column(name = "cargado_por", length = 160)
    private String cargadoPor;
}

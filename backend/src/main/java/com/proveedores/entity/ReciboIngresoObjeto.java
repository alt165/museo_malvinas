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
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(
        name = "recibos_ingreso_objeto",
        uniqueConstraints = @UniqueConstraint(name = "uk_recibos_ingreso_numero", columnNames = "numero_recibo"),
        indexes = {
            @Index(name = "idx_recibos_ingreso_objeto", columnList = "objeto_museo_id"),
            @Index(name = "idx_recibos_ingreso_depositante", columnList = "depositante_id")
        }
)
public class ReciboIngresoObjeto extends EntidadBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "numero_recibo", nullable = false, length = 80)
    private String numeroRecibo;

    @Column(name = "fecha_emision", nullable = false)
    private LocalDateTime fechaEmision;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "objeto_museo_id", nullable = false)
    private ObjetoMuseo objetoMuseo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "depositante_id", nullable = false)
    private Depositante depositante;

    @Column(name = "numero_inventario", nullable = false, length = 80)
    private String numeroInventario;

    @Column(name = "denominacion_objeto", nullable = false, length = 160)
    private String denominacionObjeto;

    @Column(name = "descripcion_breve", nullable = false, columnDefinition = "TEXT")
    private String descripcionBreve;

    @Column(name = "depositante_nombre", nullable = false, length = 160)
    private String depositanteNombre;

    @Column(name = "depositante_contacto", length = 160)
    private String depositanteContacto;

    @Column(length = 160)
    private String operador;

    @Column(name = "texto_constancia", nullable = false, columnDefinition = "TEXT")
    private String textoConstancia;

    @Column(name = "copia_firmada_nombre_archivo", length = 255)
    private String copiaFirmadaNombreArchivo;

    @Column(name = "copia_firmada_content_type", length = 120)
    private String copiaFirmadaContentType;

    @Column(name = "copia_firmada_tamanio_bytes")
    private Long copiaFirmadaTamanioBytes;

    @Column(name = "copia_firmada_ruta_almacenamiento", length = 500)
    private String copiaFirmadaRutaAlmacenamiento;

    @Column(name = "copia_firmada_fecha_carga")
    private LocalDateTime copiaFirmadaFechaCarga;

    @Column(name = "copia_firmada_cargado_por", length = 160)
    private String copiaFirmadaCargadoPor;
}

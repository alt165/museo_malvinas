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
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(
        name = "veterano_video",
        indexes = {
                @Index(name = "idx_veterano_video_veterano", columnList = "veterano_id"),
                @Index(name = "idx_veterano_video_activo", columnList = "activo"),
                @Index(name = "idx_veterano_video_orden", columnList = "orden")
        }
)
public class VeteranoVideo extends EntidadBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "veterano_id", nullable = false)
    private Veterano veterano;

    @Column(nullable = false, length = 180)
    private String titulo;

    @Column(name = "url_youtube", nullable = false, length = 500)
    private String urlYoutube;

    @Column(name = "video_id", nullable = false, length = 40)
    private String videoId;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "fecha_entrevista")
    private LocalDate fechaEntrevista;

    @Column(nullable = false)
    private Integer orden = 0;
}

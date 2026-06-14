package com.proveedores.service;

import com.proveedores.dto.VeteranoVideoRequestDTO;
import com.proveedores.dto.VeteranoVideoResponseDTO;
import com.proveedores.entity.Veterano;
import com.proveedores.entity.VeteranoVideo;
import com.proveedores.exception.BusinessException;
import com.proveedores.exception.ResourceNotFoundException;
import com.proveedores.repository.VeteranoVideoRepository;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class VeteranoVideoService {

    private final VeteranoVideoRepository veteranoVideoRepository;
    private final VeteranoService veteranoService;

    public VeteranoVideoService(VeteranoVideoRepository veteranoVideoRepository, VeteranoService veteranoService) {
        this.veteranoVideoRepository = veteranoVideoRepository;
        this.veteranoService = veteranoService;
    }

    @Transactional
    public VeteranoVideoResponseDTO crear(Long veteranoId, VeteranoVideoRequestDTO dto) {
        Veterano veterano = veteranoService.buscarActivo(veteranoId);
        VeteranoVideo video = new VeteranoVideo();
        video.setVeterano(veterano);
        aplicarDatos(video, dto, veteranoId, true);
        return toResponse(veteranoVideoRepository.save(video));
    }

    @Transactional(readOnly = true)
    public List<VeteranoVideoResponseDTO> listar(Long veteranoId) {
        veteranoService.buscarActivo(veteranoId);
        return veteranoVideoRepository.findByVeteranoIdAndEliminadoFalseOrderByOrdenAscIdAsc(veteranoId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public VeteranoVideoResponseDTO actualizar(Long veteranoId, Long videoId, VeteranoVideoRequestDTO dto) {
        VeteranoVideo video = buscarVideo(veteranoId, videoId);
        aplicarDatos(video, dto, veteranoId, false);
        return toResponse(veteranoVideoRepository.save(video));
    }

    @Transactional
    public void eliminar(Long veteranoId, Long videoId) {
        VeteranoVideo video = buscarVideo(veteranoId, videoId);
        video.setActivo(false);
        video.setEliminado(true);
        video.setFechaEliminacion(LocalDateTime.now());
        veteranoVideoRepository.save(video);
    }

    private void aplicarDatos(VeteranoVideo video, VeteranoVideoRequestDTO dto, Long veteranoId, boolean nuevo) {
        video.setTitulo(dto.titulo().trim());
        video.setUrlYoutube(dto.urlYoutube().trim());
        video.setVideoId(extraerVideoId(dto.urlYoutube()));
        video.setDescripcion(StringUtils.hasText(dto.descripcion()) ? dto.descripcion().trim() : null);
        video.setFechaEntrevista(dto.fechaEntrevista());
        if (dto.orden() != null && dto.orden() >= 0) {
            video.setOrden(dto.orden());
        } else if (nuevo) {
            video.setOrden((int) veteranoVideoRepository.countByVeteranoIdAndEliminadoFalse(veteranoId));
        }
    }

    private VeteranoVideo buscarVideo(Long veteranoId, Long videoId) {
        veteranoService.buscarActivo(veteranoId);
        return veteranoVideoRepository.findByIdAndVeteranoIdAndEliminadoFalse(videoId, veteranoId)
                .orElseThrow(() -> new ResourceNotFoundException("Video del veterano no encontrado"));
    }

    String extraerVideoId(String urlYoutube) {
        if (!StringUtils.hasText(urlYoutube)) {
            throw new BusinessException("La URL de YouTube es obligatoria");
        }
        try {
            URI uri = new URI(urlYoutube.trim());
            String host = uri.getHost();
            if (!StringUtils.hasText(host)) {
                throw new BusinessException("La URL debe ser de YouTube");
            }
            String normalizedHost = host.toLowerCase().replaceFirst("^www\\.", "");
            String path = uri.getPath();

            if ("youtu.be".equals(normalizedHost)) {
                String id = firstPathSegment(path);
                return validarVideoId(id);
            }
            if ("youtube.com".equals(normalizedHost) || "m.youtube.com".equals(normalizedHost)) {
                if ("/watch".equals(path)) {
                    return validarVideoId(queryParam(uri.getRawQuery(), "v"));
                }
                if (path != null && path.startsWith("/embed/")) {
                    return validarVideoId(firstPathSegment(path.substring("/embed".length())));
                }
            }
            throw new BusinessException("La URL debe ser de YouTube");
        } catch (URISyntaxException ex) {
            throw new BusinessException("La URL de YouTube no es valida");
        }
    }

    private String firstPathSegment(String path) {
        if (!StringUtils.hasText(path)) {
            return null;
        }
        String normalized = path.startsWith("/") ? path.substring(1) : path;
        int slashIndex = normalized.indexOf('/');
        return slashIndex >= 0 ? normalized.substring(0, slashIndex) : normalized;
    }

    private String queryParam(String rawQuery, String name) {
        if (!StringUtils.hasText(rawQuery)) {
            return null;
        }
        for (String part : rawQuery.split("&")) {
            int eq = part.indexOf('=');
            String key = eq >= 0 ? part.substring(0, eq) : part;
            if (name.equals(key)) {
                return eq >= 0 ? part.substring(eq + 1) : "";
            }
        }
        return null;
    }

    private String validarVideoId(String videoId) {
        if (!StringUtils.hasText(videoId) || !videoId.matches("[A-Za-z0-9_-]{6,20}")) {
            throw new BusinessException("La URL de YouTube no contiene un video valido");
        }
        return videoId;
    }

    private VeteranoVideoResponseDTO toResponse(VeteranoVideo video) {
        return new VeteranoVideoResponseDTO(
                video.getId(),
                video.getVeterano().getId(),
                video.getTitulo(),
                video.getUrlYoutube(),
                video.getVideoId(),
                video.getDescripcion(),
                video.getFechaEntrevista(),
                video.getOrden()
        );
    }
}

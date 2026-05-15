package com.proveedores.service;

import com.proveedores.exception.BusinessException;
import com.proveedores.exception.ResourceNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ObjectFileStorageService {

    private final Path rootDir;

    public ObjectFileStorageService(@Value("${app.storage.object-files-dir}") String rootDir) {
        this.rootDir = Path.of(rootDir).toAbsolutePath().normalize();
    }

    public StoredObjectFile store(Long objetoId, String folder, MultipartFile archivo) {
        String originalName = StringUtils.hasText(archivo.getOriginalFilename()) ? archivo.getOriginalFilename() : "archivo";
        String safeName = originalName.replaceAll("[^A-Za-z0-9._-]", "_");
        String storedName = UUID.randomUUID() + "-" + safeName;
        Path relativePath = Path.of("objeto-" + objetoId, folder, storedName);
        Path destination = rootDir.resolve(relativePath).normalize();
        if (!destination.startsWith(rootDir)) {
            throw new BusinessException("Nombre de archivo invalido");
        }

        try {
            Files.createDirectories(destination.getParent());
            archivo.transferTo(destination);
        } catch (IOException ex) {
            throw new BusinessException("No se pudo almacenar el archivo del objeto");
        }

        return new StoredObjectFile(originalName, storedName, relativePath.toString().replace('\\', '/'), destination.toString());
    }

    public Resource load(String relativePath) {
        Path file = rootDir.resolve(relativePath).normalize();
        if (!file.startsWith(rootDir)) {
            throw new BusinessException("Ruta de archivo invalida");
        }
        Resource resource = new FileSystemResource(file);
        if (!resource.exists() || !resource.isReadable()) {
            throw new ResourceNotFoundException("Archivo del objeto no encontrado");
        }
        return resource;
    }

    public record StoredObjectFile(String originalName, String storedName, String relativePath, String absolutePath) {
    }
}

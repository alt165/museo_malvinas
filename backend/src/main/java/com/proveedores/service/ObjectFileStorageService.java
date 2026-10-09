package com.proveedores.service;

import com.proveedores.exception.BusinessException;
import com.proveedores.exception.ResourceNotFoundException;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ObjectFileStorageService {

    private final Path rootDir;

    public ObjectFileStorageService(@Value("${app.storage.object-files-dir}") String rootDir) {
        this.rootDir = Path.of(rootDir).toAbsolutePath().normalize();
    }

    public StoredObjectFile storeBytes(Long objetoId, String folder, byte[] content, String logicalName, String extension) {
        return storeBytesInOwnerFolder("objeto-" + objetoId, folder, content, logicalName, extension);
    }

    public StoredObjectFile storeBytesInOwnerFolder(
            String ownerFolder,
            String folder,
            byte[] content,
            String logicalName,
            String extension
    ) {
        String safeExtension = extension.replaceAll("[^A-Za-z0-9]", "");
        if (!StringUtils.hasText(safeExtension)) throw new BusinessException("Extension de archivo invalida");
        String storedName = UUID.randomUUID() + "." + safeExtension;
        String safeOwnerFolder = ownerFolder.replaceAll("[^A-Za-z0-9._-]", "_");
        String safeFolder = folder.replaceAll("[^A-Za-z0-9._/-]", "_");
        Path relativePath = Path.of(safeOwnerFolder, safeFolder, storedName);
        Path destination = rootDir.resolve(relativePath).normalize();
        if (!destination.startsWith(rootDir)) throw new BusinessException("Nombre de archivo invalido");
        Path temporary = null;
        try {
            Files.createDirectories(destination.getParent());
            temporary = Files.createTempFile(destination.getParent(), ".upload-", ".tmp");
            Files.write(temporary, content);
            moveIntoPlace(temporary, destination);
        } catch (IOException ex) {
            deletePath(temporary);
            throw new BusinessException("No se pudo almacenar el archivo");
        }
        return new StoredObjectFile(logicalName, storedName, relativePath.toString().replace('\\', '/'), destination.toString());
    }

    private void moveIntoPlace(Path temporary, Path destination) throws IOException {
        try {
            Files.move(temporary, destination, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(temporary, destination);
        }
    }

    public Resource load(String relativePath) {
        Path file = resolve(relativePath);
        Resource resource = new FileSystemResource(file);
        if (!resource.exists() || !resource.isReadable()) {
            throw new ResourceNotFoundException("Archivo del objeto no encontrado");
        }
        return resource;
    }

    public boolean exists(String relativePath) {
        if (!StringUtils.hasText(relativePath)) return false;
        Path path = resolve(relativePath);
        return Files.isRegularFile(path) && Files.isReadable(path);
    }

    public void delete(String relativePath) {
        if (StringUtils.hasText(relativePath)) deletePath(resolve(relativePath));
    }

    private Path resolve(String relativePath) {
        Path file = rootDir.resolve(relativePath).normalize();
        if (!file.startsWith(rootDir)) throw new BusinessException("Ruta de archivo invalida");
        return file;
    }

    private void deletePath(Path path) {
        if (path == null) return;
        try {
            Files.deleteIfExists(path);
        } catch (IOException ex) {
            throw new BusinessException("No se pudo eliminar el archivo almacenado");
        }
    }

    public record StoredObjectFile(String originalName, String storedName, String relativePath, String absolutePath) {
    }
}

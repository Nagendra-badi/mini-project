package com.ddms.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@Service
public class LocalStorageServiceImpl implements StorageService {

    private final Path rootLocation;

    public LocalStorageServiceImpl(@Value("${storage.local.dir:./document-storage}") String localDir) {
        this.rootLocation = Paths.get(localDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.rootLocation);
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize storage directory: " + localDir, e);
        }
    }

    @Override
    public String uploadFile(MultipartFile file, String uniqueFileName) throws IOException {
        if (file.isEmpty()) {
            throw new IOException("Failed to store empty file.");
        }
        Path destinationFile = this.rootLocation.resolve(Paths.get(uniqueFileName))
                .normalize().toAbsolutePath();
        if (!destinationFile.getParent().equals(this.rootLocation.toAbsolutePath())) {
            // Security check
            throw new IOException("Cannot store file outside current directory.");
        }
        Files.copy(file.getInputStream(), destinationFile, StandardCopyOption.REPLACE_EXISTING);
        
        // Returns the access url or relative path. For simplicity, we return the filename.
        // The controller will append the download URL base.
        return "/api/documents/download/" + uniqueFileName;
    }

    @Override
    public Resource downloadFile(String fileName) throws MalformedURLException {
        Path file = rootLocation.resolve(fileName).normalize();
        Resource resource = new UrlResource(file.toUri());
        if (resource.exists() || resource.isReadable()) {
            return resource;
        } else {
            throw new MalformedURLException("Could not read file: " + fileName);
        }
    }

    @Override
    public void deleteFile(String fileName) throws IOException {
        Path file = rootLocation.resolve(fileName).normalize();
        Files.deleteIfExists(file);
    }
}

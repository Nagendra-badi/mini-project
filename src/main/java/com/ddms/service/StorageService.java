package com.ddms.service;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.net.MalformedURLException;

public interface StorageService {
    String uploadFile(MultipartFile file, String uniqueFileName) throws IOException;
    Resource downloadFile(String fileName) throws MalformedURLException;
    void deleteFile(String fileName) throws IOException;
}

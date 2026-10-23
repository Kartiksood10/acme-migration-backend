package com.acme.migration.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;

@Service
@Slf4j
public class LocalFileStorageService {

    /**
     * Safely transfers an uploaded MultipartFile to the OS temporary directory.
     *
     * @param file The uploaded file from the HTTP request.
     * @param prefix A prefix ("source" or "target") to identify the file.
     * @return The absolute path to the saved file on disk.
     */
    public String storeTempFile(MultipartFile file, String prefix) {
        if (file == null || file.isEmpty()) {
            return null;
        }
        try {
            // File.createTempFile guarantees a unique filename in your local /tmp directory
            File tempFile = File.createTempFile(prefix + "_", "_" + file.getOriginalFilename());

            // Streams the binary data from memory directly to the physical disk
            file.transferTo(tempFile);

            log.info("Saved temporary file to: {}", tempFile.getAbsolutePath());
            return tempFile.getAbsolutePath();

        } catch (IOException e) {
            log.error("Failed to store uploaded file: {}", file.getOriginalFilename(), e);
            throw new RuntimeException("Could not store file on server", e);
        }
    }
}

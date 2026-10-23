package com.acme.migration.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MigrationTriggerRequest {

    // Core Routing Data
    private String repositoryUrl;
    private String sourceContractType;
    private String targetContractType;

    // Raw HTTP Form Inputs
    // Spring binds the form-data keys directly to these fields
    private String sourceUrl;
    private MultipartFile sourceFile;
    private String targetUrl;
    private MultipartFile targetFile;

    // Resolved Database Fields
    // These are populated by the Service layer after saving files to disk,
    // holding either the web URL or the absolute local file path
    private String sourceContractLocation;
    private String targetContractLocation;
}

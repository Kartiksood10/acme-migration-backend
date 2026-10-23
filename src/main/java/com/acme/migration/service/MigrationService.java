package com.acme.migration.service;

import com.acme.migration.dto.MigrationTriggerRequest;
import com.acme.migration.dto.MigrationTriggerResponse;
import com.acme.migration.entity.MigrationJobs;
import com.acme.migration.repository.MigrationJobsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.io.File;

@Service
@Slf4j
@RequiredArgsConstructor
public class MigrationService {

    private final MigrationJobsRepository migrationJobsRepository;
    private final LocalFileStorageService fileStorageService;

    // Create and instantiate Rest client once for the class
    private final RestClient restClient = RestClient.create();

    private static final String CALLBACK_URL = "http://localhost:8081/api/v1/webhook/migrations";
    private static final String FASTAPI_ENDPOINT = "http://localhost:8000/migrations";
    private static final String TEST_CALLBACK_URL = "https://webhook.site/04518c1e-f23e-4a73-9afc-0fa4afb8958e";
    private static final String UPLOAD =  "UPLOAD";

    /**
     * Method to send a Migration POST request to FastAPI migration worker
     * Multi part form data is used to send different types of data together from the UI (Upload, text etc.)
     * @param request Input Migration trigger request from the UI
     * @return JobId - unique migration ID
     */
    public MigrationTriggerResponse triggerMigration(MigrationTriggerRequest request) {

        // 1. Resolve Source Location
        if (UPLOAD.equalsIgnoreCase(request.getSourceContractType()) && request.getSourceFile() != null) {
            // Save Uploaded file location in DB
            String savedPath = fileStorageService.storeTempFile(request.getSourceFile(), "source");
            request.setSourceContractLocation(savedPath);
        } else {
            request.setSourceContractLocation(request.getSourceUrl());
        }

        // 2. Resolve Target Location
        if (UPLOAD.equalsIgnoreCase(request.getTargetContractType()) && request.getTargetFile() != null) {
            // Save Uploaded file location in DB
            String savedPath = fileStorageService.storeTempFile(request.getTargetFile(), "target");
            request.setTargetContractLocation(savedPath);
        } else {
            request.setTargetContractLocation(request.getTargetUrl());
        }

        // 3. Build the Multipart Form Data payload expected by FastAPI
        MultiValueMap<String, Object> multipartBody = new LinkedMultiValueMap<>();
        multipartBody.add("consumer_repo_url", request.getRepositoryUrl());
        multipartBody.add("callback_url", TEST_CALLBACK_URL);

        // 4. Attach File Streams or URL Strings
        if (UPLOAD.equalsIgnoreCase(request.getSourceContractType())) {
            multipartBody.add("source_file", new FileSystemResource(new File(request.getSourceContractLocation())));
        } else {
            multipartBody.add("source_url", request.getSourceContractLocation());
        }

        if (UPLOAD.equalsIgnoreCase(request.getTargetContractType())) {
            multipartBody.add("target_file", new FileSystemResource(new File(request.getTargetContractLocation())));
        } else {
            multipartBody.add("target_url", request.getTargetContractLocation());
        }

        // 5. Call the FastAPI Worker through rest client POST request
        MigrationTriggerResponse response = restClient.post()
                .uri(FASTAPI_ENDPOINT)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(multipartBody)
                .retrieve()
                .body(MigrationTriggerResponse.class);

        if (response == null) {
            log.error("FastAPI migration service returned null response");
            return null;
        }

        String jobId = response.jobId();

        // 6. Persist initial state to PostgreSQL
        MigrationJobs migrationJobs = new MigrationJobs();
        migrationJobs.setJobId(jobId);
        migrationJobs.setRepositoryUrl(request.getRepositoryUrl());
        migrationJobs.setSourceContractType(request.getSourceContractType());
        migrationJobs.setSourceContractLocation(request.getSourceContractLocation());
        migrationJobs.setTargetContractType(request.getTargetContractType());
        migrationJobs.setTargetContractLocation(request.getTargetContractLocation());
        migrationJobs.setStatus("PROCESSING");

        migrationJobsRepository.save(migrationJobs);
        log.info("Migration job id {} has been saved in the DB", jobId);

        return response;
    }
}

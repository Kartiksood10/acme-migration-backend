package com.acme.migration.service;

import com.acme.migration.dto.*;
import com.acme.migration.entity.EndpointExecutionLogs;
import com.acme.migration.entity.GeneratedPullRequests;
import com.acme.migration.entity.MigrationJobs;
import com.acme.migration.exception.JobNotFoundException;
import com.acme.migration.repository.MigrationJobsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class MigrationService {

    private final MigrationJobsRepository migrationJobsRepository;
    private final LocalFileStorageService fileStorageService;

    // Create and instantiate Rest client once for the class
    private final RestClient restClient = RestClient.create();
    private static final String UPLOAD =  "UPLOAD";

    @Value("${webhook.callbackUrl}")
    private String callbackUrl;

    @Value(("${webhook.test.callbackUrl}"))
    private String testCallbackUrl;

    @Value("${fastapi.endpoint}")
    private String fastApiEndpoint;

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
        multipartBody.add("callback_url", callbackUrl);

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
                .uri(fastApiEndpoint)
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

    /**
     * Front end polling API that checks migration completion status
     * Finds existing triggered migration from DB and returns the current response to the UI
     * @param jobId Migration JobID
     * @return Migration status response for the UI
     */
    @Transactional(readOnly = true)
    public MigrationStatusResponse getMigrationJobStatus(String jobId) {
        MigrationJobs job = migrationJobsRepository.findById(jobId)
                .orElseThrow(() -> new JobNotFoundException("Job not found: " + jobId));

        String status = job.getStatus();
        Double executionTimeMinutes = job.getExecutionTimeMinutes();
        String errorMessage = job.getErrorMessage();
        List<String> prUrls = new ArrayList<>();
        List<EndpointLogDTO> logDTOs = new ArrayList<>();

        MigrationStatusResponse response = new MigrationStatusResponse();
        response.setJobId(jobId);
        response.setStatus(status);
        response.setErrorMessage(errorMessage);
        response.setExecutionTimeMinutes(executionTimeMinutes);

        if(job.getPullRequests()!=null) {
            for(GeneratedPullRequests pr : job.getPullRequests()) {
                prUrls.add(pr.getPrUrl());
            }
        }

        if(job.getExecutionLogs()!=null) {
            for(EndpointExecutionLogs log : job.getExecutionLogs()) {
                logDTOs.add(new EndpointLogDTO(log.getEndpointName(), log.getStatus()));
            }
        }

        response.setGeneratedPrs(prUrls);
        response.setEndpointLogs(logDTOs);

        return response;
    }

    /**
     * Retrieves a lightweight summary of all migration jobs for the UI dashboard
     * @return List of Migration Summary DTO
     */
    @Transactional(readOnly = true)
    public List<MigrationSummaryDTO> getAllMigrations() {
        List<MigrationSummaryDTO> migrationSummaryList = new ArrayList<>();
        List<MigrationJobs> migrationJobs = migrationJobsRepository.findAll();

        for(MigrationJobs job : migrationJobs) {
            MigrationSummaryDTO migrationSummaryDTO = new MigrationSummaryDTO();
            migrationSummaryDTO.setJobId(job.getJobId());
            migrationSummaryDTO.setRepositoryUrl(job.getRepositoryUrl());
            migrationSummaryDTO.setStatus(job.getStatus());
            migrationSummaryDTO.setExecutionTimeMinutes(job.getExecutionTimeMinutes());

            migrationSummaryList.add(migrationSummaryDTO);
        }

        return migrationSummaryList;
    }
}

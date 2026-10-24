package com.acme.migration.service;

import com.acme.migration.dto.WebhookCallbackPayload;
import com.acme.migration.entity.EndpointExecutionLogs;
import com.acme.migration.entity.GeneratedPullRequests;
import com.acme.migration.entity.MigrationJobs;
import com.acme.migration.repository.MigrationJobsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class MigrationWebhookService {

    private final MigrationJobsRepository migrationJobsRepository;

    /**
     * This method handles the incoming webhook data and updates/inserts data into the DB
     * Updates status, exec time and error message in MigrationJobs
     * Inserts generated PRs in GeneratedPullRequests
     * Inserts executed endpoints in EndpointExecutionLogs
     * @param payload Webhook payload received from FastAPI migration worker
     */
    @Transactional
    public void handleMigrationWebhook(WebhookCallbackPayload payload) {

        // Find Existing Migration job in DB
        MigrationJobs job = migrationJobsRepository.findById(payload.getJobId())
                .orElseThrow(() -> new RuntimeException("Job not found: " + payload.getJobId()));

        // Update webhook status for the particular jobId
        job.setStatus(payload.getStatus());
        job.setExecutionTimeMinutes(payload.getExecutionTimeMinutes());
        job.setErrorMessage(payload.getErrorMessage());

        // Store generated PRs for the migration job in the DB
        if(payload.getGeneratedPrs() != null) {
            for(String prUrl : payload.getGeneratedPrs()) {
                GeneratedPullRequests pullRequests = new GeneratedPullRequests();
                pullRequests.setPrUrl(prUrl);
                // Set many to one parent migrationJob object
                pullRequests.setMigrationJob(job);
                // Add to the list of PRs defined in the parent entity
                job.getPullRequests().add(pullRequests);
            }
        }

        // Store the successful endpoints in the DB
        if(payload.getCompletedEndpoints() != null) {
            for(String completedEndpoint : payload.getCompletedEndpoints()) {
                EndpointExecutionLogs endpointExecutionLogs = new EndpointExecutionLogs();
                endpointExecutionLogs.setEndpointName(completedEndpoint);
                endpointExecutionLogs.setMigrationJob(job);
                endpointExecutionLogs.setStatus("SUCCESS");
                job.getExecutionLogs().add(endpointExecutionLogs);
            }
        }

        // Store the failed endpoints in the DB
        if(payload.getFailedEndpoints() != null) {
            for(String failedEndpoint : payload.getFailedEndpoints()) {
                EndpointExecutionLogs endpointExecutionLogs = new EndpointExecutionLogs();
                endpointExecutionLogs.setEndpointName(failedEndpoint);
                endpointExecutionLogs.setMigrationJob(job);
                endpointExecutionLogs.setStatus("FAILED");
                job.getExecutionLogs().add(endpointExecutionLogs);
            }
        }

        // Performs update in the MigrationJobs table and inserts data in the PR and Execution logs tables
        migrationJobsRepository.save(job);
        log.info("Migration Webhook data saved successfully in DB");
    }
}

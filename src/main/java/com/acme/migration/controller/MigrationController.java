package com.acme.migration.controller;

import com.acme.migration.dto.*;
import com.acme.migration.service.MigrationService;
import com.acme.migration.service.MigrationWebhookService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class MigrationController {

    private final MigrationService migrationService;
    private final MigrationWebhookService migrationWebhookService;

    /**
     * API to trigger a new API contract migration from the UI
     * '@ModelAttribute' automatically binds the multipart/form-data into the DTO properties
     * @param request Migration trigger request
     * @return Immediate 202 trigger response received from FastAPI python worker
     */
    @PostMapping(value = "/migrate", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MigrationTriggerResponse> triggerMigration(@ModelAttribute MigrationTriggerRequest request) {

        // Pass the single, populated DTO directly to the service layer
        MigrationTriggerResponse response = migrationService.triggerMigration(request);

        // Return 202 Accepted, signaling the background process has started
        return ResponseEntity.accepted().body(response);
    }

    /**
     * API to receive webhook callback payload from the FastAPI python worker
     * Updates latest status, generated PRs and executed endpoints in the DB
     * @param payload Webhook payload
     */
    @PostMapping("/webhook/migrations")
    public void handleWebhook(@RequestBody WebhookCallbackPayload payload) {
        migrationWebhookService.handleMigrationWebhook(payload);
    }

    /**
     * Front end polling API to fetch latest migration status from DB
     * @param jobId Migration JobID
     * @return Migration status response delivered to the UI
     */
    @GetMapping("/migrations/{jobId}")
    public ResponseEntity<MigrationStatusResponse> getMigrationDetails(@PathVariable("jobId") String jobId) {
        MigrationStatusResponse response = migrationService.getMigrationJobStatus(jobId);
        return ResponseEntity.ok(response);
    }

    /**
     * Front end Dashboard API to fetch a summary of all historical migration jobs
     * @return List of migration job summaries delivered to the UI
     */
    @GetMapping("/migrations")
    public ResponseEntity<List<MigrationSummaryDTO>> getAllMigrations() {
        List<MigrationSummaryDTO> response = migrationService.getAllMigrations();
        return ResponseEntity.ok(response);
    }
}

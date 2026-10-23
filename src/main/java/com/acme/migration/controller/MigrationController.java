package com.acme.migration.controller;

import com.acme.migration.dto.MigrationTriggerRequest;
import com.acme.migration.dto.MigrationTriggerResponse;
import com.acme.migration.service.MigrationService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class MigrationController {

    private final MigrationService migrationService;

    public MigrationController(MigrationService migrationService) {
        this.migrationService = migrationService;
    }

    // @ModelAttribute automatically binds the multipart/form-data into the DTO properties
    @PostMapping(value = "/migrate", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MigrationTriggerResponse> triggerMigration(@ModelAttribute MigrationTriggerRequest request) {

        // Pass the single, populated DTO directly to the service layer
        MigrationTriggerResponse response = migrationService.triggerMigration(request);

        // Return 202 Accepted, signaling the background process has started
        return ResponseEntity.accepted().body(response);
    }
}

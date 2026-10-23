package com.acme.migration.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

// Immediate response structure received from FastAPI post migration trigger
public record MigrationTriggerResponse(
        @JsonProperty("job_id") String jobId,
        @JsonProperty("status") String status,
        @JsonProperty("message") String message
) {}


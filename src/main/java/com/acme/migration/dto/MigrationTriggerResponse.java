package com.acme.migration.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

// Immediate response structure received from FastAPI post migration trigger
// We define this as a record since we get the response from FastAPI, and we want the response to be immutable
// record - immutable , DTO class - mutable
public record MigrationTriggerResponse(
        @JsonProperty("job_id") String jobId,
        @JsonProperty("status") String status,
        @JsonProperty("message") String message
) {}


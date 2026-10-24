package com.acme.migration.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder

// Webhook payload received from FastAPI migration worker after migration completion
public class WebhookCallbackPayload {

    @JsonProperty("job_id")
    private String jobId;

    @JsonProperty("status")
    private String status;

    @JsonProperty("execution_time_minutes")
    private Double executionTimeMinutes;

    @JsonProperty("error_message")
    private String errorMessage;

    @JsonProperty("generated_prs")
    private List<String> generatedPrs;

    @JsonProperty("completed_endpoints")
    private List<String> completedEndpoints;

    @JsonProperty("failed_endpoints")
    private List<String> failedEndpoints;
}

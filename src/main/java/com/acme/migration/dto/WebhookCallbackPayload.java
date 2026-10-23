package com.acme.migration.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WebhookCallbackPayload {

    private String jobId;
    private String status;
    private Double executionTimeMinutes;
    private String errorMessage;
    private List<String> generatedPrs;
    private List<String> completedEndpoints;
    private List<String> failedEndpoints;
}

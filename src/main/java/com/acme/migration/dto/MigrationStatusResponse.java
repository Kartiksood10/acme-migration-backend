package com.acme.migration.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor

// Migration status response sent to the UI
public class MigrationStatusResponse {
    private String jobId;
    private String status;
    private String errorMessage;
    private Double executionTimeMinutes;
    List<String> generatedPrs;
    List<EndpointLogDTO> endpointLogs;
}

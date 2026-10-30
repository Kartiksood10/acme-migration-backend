package com.acme.migration.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MigrationSummaryDTO {
    private String jobId;
    private String repositoryUrl;
    private String status;
    private Double executionTimeMinutes;
}


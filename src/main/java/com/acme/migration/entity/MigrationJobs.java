package com.acme.migration.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "migration_jobs")

// Table for initial migration trigger and updating final webhook status
public class MigrationJobs {

    @Id
    @Column(name = "job_id")
    private String jobId;

    @Column(name = "repository_url")
    private String repositoryUrl;

    @Column(name = "source_contract_type")
    private String sourceContractType;

    @Column(name = "source_contract_location")
    private String sourceContractLocation;

    @Column(name = "target_contract_type")
    private String targetContractType;

    @Column(name = "target_contract_location")
    private String targetContractLocation;

    @Column(name = "status")
    private String status;

    @Column(name = "execution_time_minutes")
    private Double executionTimeMinutes;

    @Column(name = "error_message")
    private String errorMessage;

    // @CreationTimestamp sets a field to the current time when Hibernate first inserts the entity
    @CreationTimestamp
    @Column(name = "created_at")
    private Instant createdAt;

    // @UpdateTimestamp sets a field to the current time when Hibernate updates the entity
    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    // One migration job can have multiple generated PRs
    @OneToMany(mappedBy = "migrationJob", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<GeneratedPullRequests> pullRequests;

    // One migration job can have multiple endpoints executed
    @OneToMany(mappedBy = "migrationJob", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EndpointExecutionLogs> executionLogs;
}

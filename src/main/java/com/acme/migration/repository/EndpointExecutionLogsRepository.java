package com.acme.migration.repository;

import com.acme.migration.entity.EndpointExecutionLogs;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface EndpointExecutionLogsRepository extends JpaRepository<EndpointExecutionLogs, Long> {

    @Query("select eel from EndpointExecutionLogs eel where eel.migrationJob.jobId = :jobId")
    List<EndpointExecutionLogs> findByMigrationJobId(String jobId);
}

package com.acme.migration.repository;

import com.acme.migration.entity.GeneratedPullRequests;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GeneratedPullRequestsRepository extends JpaRepository<GeneratedPullRequests, Long> {

    @Query("select gpr from GeneratedPullRequests gpr where gpr.migrationJob.jobId = :jobId")
    List<GeneratedPullRequests> findByMigrationJobId(String jobId);
}

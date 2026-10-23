package com.acme.migration.repository;

import com.acme.migration.entity.MigrationJobs;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MigrationJobsRepository extends JpaRepository<MigrationJobs, String> {
}

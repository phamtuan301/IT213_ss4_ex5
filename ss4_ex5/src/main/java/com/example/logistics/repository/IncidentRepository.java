package com.example.logistics.repository;

import com.example.logistics.model.IncidentReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA Repository cho IncidentReport.
 */
@Repository
public interface IncidentRepository extends JpaRepository<IncidentReport, Long> {
}

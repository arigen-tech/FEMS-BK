package com.dmsBackend.repository;

import com.dmsBackend.entity.DocumentActivityReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface DocumentActivityReportRopository extends
        JpaRepository<DocumentActivityReport,Long>,
        JpaSpecificationExecutor<DocumentActivityReport> {

}

package com.dmsBackend.repository;

import com.dmsBackend.entity.PreExamination;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PreExaminationRepository extends JpaRepository<PreExamination, Integer> {
    Optional<PreExamination> findByDocumentHeader_Id(Integer documentHeaderId);
}

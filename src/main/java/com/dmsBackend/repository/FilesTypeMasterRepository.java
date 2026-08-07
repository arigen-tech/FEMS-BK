package com.dmsBackend.repository;

import com.dmsBackend.entity.FilesTypeMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FilesTypeMasterRepository extends JpaRepository<FilesTypeMaster, Integer> {
    Optional<FilesTypeMaster> findByExtension(String extension);


    @Query("SELECT f.extension FROM FilesTypeMaster f WHERE f.isActive = 1")
    List<String> findActiveFileExtensions();

    @Query("SELECT f FROM FilesTypeMaster f WHERE f.isActive = 1")
    List<FilesTypeMaster> findActiveFileType();


    @Query(value = """
    SELECT *
    FROM file_type_master f
    ORDER BY
        CASE
            WHEN f.is_active = 1 AND CAST(f.updated_on AS DATE) = CURRENT_DATE THEN 0
            WHEN f.is_active = 1 THEN 1
            ELSE 2
        END ASC,
        f.updated_on DESC
    """, nativeQuery = true)
    List<FilesTypeMaster> findAllFilesTypeMasterOrdered();


    boolean existsByExtension(String extension);
}

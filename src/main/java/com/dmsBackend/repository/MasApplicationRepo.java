package com.dmsBackend.repository;

import com.dmsBackend.entity.MasApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MasApplicationRepo extends JpaRepository<MasApplication,String> {


    List<MasApplication> findByStatusIgnoreCase(String status);
    List<MasApplication> findByStatusInIgnoreCase(List<String> statuses);

    //One type Setup

    //Before this create a sequence as in My Sql Db-> CREATE SEQUENCE mas_application_order_seq START WITH 1 INCREMENT BY 1;
    //Before this create a sequence as in Postgres Db->

    //    CREATE SEQUENCE mas_application_order_seq
    //    START WITH 1      -- first number in the sequence
    //    INCREMENT BY 1    -- step size
    //    NO MINVALUE       -- no minimum limit
    //    NO MAXVALUE       -- no maximum limit
    //    CACHE 1;;


//    @Query(value = "INSERT INTO mas_application_order_seq VALUES (NULL); SELECT LAST_INSERT_ID();", nativeQuery = true)
//    Long getNextOrderNo();

    @Query(value = "SELECT nextval('mas_application_order_seq')", nativeQuery = true)        //Postgres Db
    Long getNextOrderNo();

    @Query(value = "SELECT COALESCE(MAX(app_sequence_no), 0) + 1 FROM mas_application WHERE parent_id = :parentId", nativeQuery = true)
    Long getNextAppSequenceNo(String parentId);

//    @Query(value = "SELECT COALESCE(MAX(app_sequence_no), 0) + 1 FROM mas_application WHERE parent_id = :parentId", nativeQuery = true)
//    Long getNextAppSequenceNo(@Param("parentId") String parentId);
    List<MasApplication> findByParentId(String parentId);
    List<MasApplication> findByParentIdIsNullOrParentId(String parentId);

    List<MasApplication> findByParentIdAndStatusIgnoreCase(String parentId, String status);
}

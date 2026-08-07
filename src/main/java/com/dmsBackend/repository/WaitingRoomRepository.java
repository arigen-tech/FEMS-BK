package com.dmsBackend.repository;

import com.dmsBackend.entity.WaitingRoom;
import com.dmsBackend.entity.WaitingRoomStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WaitingRoomRepository extends JpaRepository<WaitingRoom, Integer> {
    boolean existsByDocumentNameAndSourceNameAndYearAndVersionAndFileType(
            String documentName,
            String sourceName,
            String year,
            String version,
            String fileType
    );


    boolean existsByFilepath(String filepath);

    Optional<WaitingRoom> findByFilepath(String filepath);


    List<WaitingRoom> findByStatusIn(List<WaitingRoomStatus> statuses);


}

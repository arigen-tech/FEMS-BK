package com.dmsBackend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.sql.Timestamp;

@Data
@Table(name = "waiting_room")
@Entity
public class WaitingRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "document_name")
    private String documentName;

    @Column(name = "source_name")
    private String sourceName;

    @Column(name = "year")
    private String year;

    @Column(name = "version")
    private String version;

    @Column(name = "file_type")
    private String fileType;

    @NotBlank(message = "File path is required")
    @Column(name = "filepath", columnDefinition = "TEXT")
    private String filepath;

    @Column(name="createdOn")
    private Timestamp createdOn;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", columnDefinition = "ENUM('PENDING','MOVED','FAILED')")
    private WaitingRoomStatus status; // e.g. "PENDING", "MOVED", "FAILED"
}


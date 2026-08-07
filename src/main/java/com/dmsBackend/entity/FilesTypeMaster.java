package com.dmsBackend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.sql.Timestamp;



@Entity
@Table(name = "file_type_master", indexes = {
        @Index(name = "idx_file_type", columnList = "file_type"),
        @Index(name = "idx_extension", columnList = "extension")
})
@Getter
@Setter
public class FilesTypeMaster {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "file_type", nullable = false)
    private String filetype; // pdf, image, etc

    @Column(name = "extension", nullable = false, unique = true)
    private String extension; // .pdf, .jpg

    @Column(name = "is_active", nullable = false)
    private int isActive;

    @Column(name = "created_on", nullable = false, updatable = false)
    private Timestamp createdOn;

    @Column(name = "updated_on")
    private Timestamp updatedOn;

    @ManyToOne
    @JoinColumn(name = "updated_by_id", referencedColumnName = "id")
    private Employee updetedBy;

    @ManyToOne
    @JoinColumn(name = "created_by_id", referencedColumnName = "id")
    private Employee createdBy;
}

package com.dmsBackend.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Data;

import java.sql.Timestamp;

import jakarta.persistence.*;
import lombok.Data;
import java.sql.Timestamp;

@Entity
@Table(
        name = "document_metadata",
        indexes = {
                @Index(name = "idx_meta_key", columnList = "meta_key"),
                @Index(name = "idx_meta_value", columnList = "meta_value")
        }
)
@Data
public class DocumentMetadata {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_header_id", nullable = false)
    @JsonBackReference
    private DocumentHeader documentHeader;


    @Column(name = "meta_key", nullable = false)
    private String metaKey;

    @Column(name = "meta_value")
    private String metaValue;

    @Column(name = "created_on")
    private Timestamp createdOn;
}

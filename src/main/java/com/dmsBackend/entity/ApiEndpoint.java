package com.dmsBackend.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.sql.Timestamp;

@Entity
@Data
@Table(name = "apis_endpoints")
public class ApiEndpoint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @CreationTimestamp
    @Column(name = "created_on", nullable = false, updatable = false)
    private Timestamp createdOn;

    @Column(name = "created_by", nullable = false, length = 100)
    private String createdBy;

    @Column(name = "method", nullable = false, length = 10)
    private String method;

    @Column(name = "endpoint", nullable = false, length = 255)
    private String endpoint;

    @Column(name = "controller", nullable = false, length = 255)
    private String controller;

    @UpdateTimestamp
    @Column(name = "updated_on")
    private Timestamp updatedOn;

    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    @Column(name = "working", length = 255)
    private String working;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "endpoint_type_id", nullable = false)
    private ApiEndpointType endpointType;
}

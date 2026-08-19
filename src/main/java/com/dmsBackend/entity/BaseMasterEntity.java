package com.dmsBackend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import lombok.Data;

import java.util.Date;

/**
 * Common fields shared by every master/lookup table added for
 * Register Case & Evidence (Case Type, Crime Type, State, District,
 * City, Priority, Evidence Type, Forwarding Authority Type,
 * Mode of Submission, Package Type).
 *
 * Mirrors the shape of your existing LanguageMaster-style tables:
 * id, is_active, created_on, name, updated_on.
 */
@MappedSuperclass
@Data
public abstract class BaseMasterEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @Column(name = "created_on", nullable = false)
    private Date createdOn;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "updated_on")
    private Date updatedOn;
}

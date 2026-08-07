package com.dmsBackend.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.sql.Timestamp;

@Entity
@Data
@Table(
        name = "api_access_by_role",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"role_id", "api_id"})
        }
)
public class ApiAccessByRole {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @CreationTimestamp
    @Column(name = "created_on", nullable = false, updatable = false)
    private Timestamp createdOn;

    @Column(name = "created_by", nullable = false, length = 100)
    private String createdBy;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "role_id", nullable = false)
    private RoleMaster role;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "api_id", nullable = false)
    private ApiEndpoint api;

    @UpdateTimestamp
    @Column(name = "updated_on")
    private Timestamp updatedOn;

    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    @Column(
            name = "status",
            columnDefinition = "tinyint(1) default 1"
    )
    private Boolean status = true;
}

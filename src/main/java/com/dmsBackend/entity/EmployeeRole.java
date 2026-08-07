package com.dmsBackend.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.sql.Timestamp;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(
        name = "EmpRole",
        indexes = {
                @Index(name = "idx_emplo_empid", columnList = "emp_id"),
                @Index(name = "idx_emplo_roleid", columnList = "role_id"),
                @Index(name = "idx_emplo_isactive", columnList = "isActive"),
                @Index(name = "idx_emplo_updatedby", columnList = "updatedById"),
                @Index(name = "idx_emplo_createdby", columnList = "createdById")
        }
)
public class EmployeeRole {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "emp_id", referencedColumnName = "id")
    private Employee empId;

    @ManyToOne
    @JoinColumn(name = "role_id", referencedColumnName = "id")
    private RoleMaster roleId;

    @Column(name = "isActive")
    private boolean isActive;

    @ManyToOne(fetch = FetchType.EAGER) // Fetch updatedBy eagerly
    @JoinColumn(name = "updatedById")
    @JsonBackReference("employee-updatedBy")
    private Employee updatedBy;

    @ManyToOne(fetch = FetchType.EAGER) // Fetch createdBy eagerly
    @JoinColumn(name = "createdById")
    @JsonBackReference("employee-createdBy")
    private Employee createdBy;

    @Column(name = "createdOn")
    private Timestamp createdOn;

    @Column(name = "updatedOn")
    private Timestamp updatedOn;
}

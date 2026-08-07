package com.dmsBackend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

@Getter
@Setter
@Entity
@Table(name = "mas_template")
public class MasTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "template_id", nullable = false)
    private Long id;

    @Size(max = 48)
    @NotNull
    @Column(name = "template_code", nullable = false, length = 48)
    private String templateCode;

    @Size(max = 120)
    @NotNull
    @Column(name = "template_name", nullable = false, length = 120)
    private String templateName;

    @Size(max = 4)
    @Column(name = "status", length = 4)
    private String status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private DepartmentMaster department;
//
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "template_parent_id")
//    private MasTemplate templateParent;

    @Column(name = "last_chg_by")
    private Integer lastChgBy;

    @Column(name = "last_chg_date")
    private Instant lastChgDate;

    @Column(name = "branch_id")
    private Integer branchId;

//    @OneToMany(mappedBy = "templateParent")
//    private Set<MasTemplate> masTemplates = new LinkedHashSet<>();

    @OneToMany(mappedBy = "template")
    private Set<RoleTemplate> roleTemplates = new LinkedHashSet<>();

    @OneToMany(mappedBy = "template")
    private Set<TemplateApplication> templateApplications = new LinkedHashSet<>();

}

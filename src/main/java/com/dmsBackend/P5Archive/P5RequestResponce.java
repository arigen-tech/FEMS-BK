package com.dmsBackend.P5Archive;

import com.dmsBackend.ArchiveWithLTO9.LtoRetentionJob;
import com.dmsBackend.entity.RetentionPolicy;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "p5_request_responce")
@Getter
@Setter
@ToString
public class P5RequestResponce {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ================= PLAN =================

    @Column(name = "p5_plan_id")
    private String p5PlanId;

    @Column(name = "p5_plan_request_json", columnDefinition = "JSON")
    @JdbcTypeCode(SqlTypes.JSON)
    private JsonNode p5PlanrequestJson;

    @Column(name = "p5_plan_responce_json", columnDefinition = "JSON")
    @JdbcTypeCode(SqlTypes.JSON)
    private JsonNode p5PlanResponceJson;

    // ================= JOB =================

    @Column(name = "p5_job_id")
    private String p5JobId;

    @Column(name = "p5_job_request_json", columnDefinition = "JSON")
    @JdbcTypeCode(SqlTypes.JSON)
    private JsonNode p5JobrequestJson;

    @Column(name = "p5_job_responce_json", columnDefinition = "JSON")
    @JdbcTypeCode(SqlTypes.JSON)
    private JsonNode p5JobResponceJson;

    // ================= RELATIONS =================

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lto_retention_job_id", nullable = false)
    private LtoRetentionJob ltoRetentionJob;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "retention_policy_id", nullable = false)
    private RetentionPolicy retentionPolicy;
}

package com.dmsBackend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Getter
@Setter
@Table(name = "mas_application")
public class MasApplication {

    @Id
    @Column(name = "app_id", nullable = false, length = 50)
    private String appId;

    @Size(max = 200)
    @Column(name = "name", length = 200)
    private String name;

    @Column(name = "parent_id")
    private String parentId;

    @Column(name = "url")
    private String url;

    @Column(name = "order_no")
    private Long orderNo;

    @Size(max = 1)
    @Column(name = "status", length = 1)
    private String status;

    @UpdateTimestamp
    @Column(name = "last_chg_date")
    private Instant lastChgDate;

    @Column(name = "app_sequence_no")
    private Long appSequenceNo;

    @Column(name = "serial_no")
    private Long serialNo;

    @OneToMany(mappedBy = "app")
    private Set<TemplateApplication> templateApplications = new LinkedHashSet<>();

}

package com.dmsBackend.entity;


import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "template_application")
public class TemplateApplication {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "temp_app_id", nullable = false)
    private Long id;

    @Size(max = 1)
    @Column(name = "status", length = 1)
    private String status;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "template_id")
    private MasTemplate template;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "app_id")
    private MasApplication app;

    @Column(name = "last_chg_date")
    private Instant lastChgDate;

    @Column(name = "last_chg_by")
    private Long lastChgBy;

    @Column(name = "order_no")
    private Long orderNo;

}
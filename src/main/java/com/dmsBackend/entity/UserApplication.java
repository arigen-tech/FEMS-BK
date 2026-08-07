package com.dmsBackend.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "user_applications")
public class UserApplication {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 180)
    private String userAppName;

    @Column(length = Integer.MAX_VALUE)
    private String url;

    @Column(length = 1)
    private String status;

    private Integer lastChgBy;

    @UpdateTimestamp
    private LocalDateTime lastChgDate;
}

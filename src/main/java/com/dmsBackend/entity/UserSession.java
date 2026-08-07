package com.dmsBackend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.Date;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "user_session",
        indexes = {
                @Index(name = "idx_user_session_user", columnList = "employee_id"),
                @Index(name = "idx_user_session_access_jti", columnList = "access_token_jti"),
                @Index(name = "idx_user_session_device", columnList = "device_id"),
                @Index(name = "idx_user_session_role", columnList = "role_id")
        })
public class UserSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", nullable = false)
    private RoleMaster role;

    @Column(name = "access_token_jti", nullable = false, unique = true)
    private String accessTokenJti;

    @Column(name = "refresh_token_jti", nullable = false, unique = true)
    private String refreshTokenJti;

    @Column(name = "device_id", nullable = false)
    private String deviceId;

    @Column(name = "user_agent")
    private String userAgent;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "active", nullable = false)
    private Boolean active = true;

    @Column(name = "access_token_expiry")
    private Date accessTokenExpiry;

    @Column(name = "refresh_token_expiry")
    private Date refreshTokenExpiry;

    @Column(name = "created_at", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt = new Date();
}
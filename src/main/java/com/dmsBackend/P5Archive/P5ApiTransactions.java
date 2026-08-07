package com.dmsBackend.P5Archive;

import com.dmsBackend.entity.RetentionPolicy;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "p5_api_transactions")
@Getter
@Setter
public class P5ApiTransactions {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // API info
    @Column(name = "api_url", length = 500, nullable = false)
    private String apiUrl;

    @Column(name = "http_method", length = 10, nullable = false)
    private String httpMethod;

    // Request details
    @Lob
    @Column(name = "request_headers", columnDefinition = "LONGTEXT")
    private String requestHeaders;

    @Lob
    @Column(name = "request_body", columnDefinition = "LONGTEXT")
    private String requestBody;

    // Response details
    @Lob
    @Column(name = "response_headers", columnDefinition = "LONGTEXT")
    private String responseHeaders;

    @Lob
    @Column(name = "response_body", columnDefinition = "LONGTEXT")
    private String responseBody;

    @Column(name = "http_status")
    private Integer httpStatus;

    // Client info
    @Column(name = "client_ip", length = 45)
    private String clientIp;

    @Column(name = "user_agent", length = 255)
    private String userAgent;

    // Performance
    @Column(name = "execution_time_ms")
    private Long executionTimeMs;

    // Audit
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "retention_policy_id", nullable = true)
    private RetentionPolicy retentionPolicy;


    @Column(name = "api_type")
    private String apiType;

    @Column(name = "expected_size_kb")
    private Long expectedSizeKb;

}

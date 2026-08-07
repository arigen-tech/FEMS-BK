package com.dmsBackend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.sql.Timestamp;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(
        name = "notification",
        indexes = {
                @Index(name = "idx_notif_employee", columnList = "employee_id"),
                @Index(name = "idx_notif_type", columnList = "type"),
                @Index(name = "idx_notif_isread", columnList = "isRead"),
                @Index(name = "idx_notif_reference", columnList = "reference_id, reference_type")
        }
)
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id")
    private Employee employee;

    private String title;
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", columnDefinition = "VARCHAR(50)")  // ✅ Change from length to columnDefinition
    private NotificationType type;

    @Column(columnDefinition = "TEXT")
    private String detailedMessage;

    private boolean isRead;

    @CreationTimestamp
    private Timestamp createdOn;

    @Column(name = "reference_id")
    private Integer referenceId;

    @Column(name = "reference_type")
    private String referenceType;

    @Column(name = "read_on")
    private Timestamp readOn;
}
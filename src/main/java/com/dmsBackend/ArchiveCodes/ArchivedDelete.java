package com.dmsBackend.ArchiveCodes;

import com.dmsBackend.entity.Employee;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "archived_delete")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArchivedDelete {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "object_name", nullable = false)
    private String objectName;

    @Column(name = "collection_name", nullable = false)
    private String collectionName;

    @Column(name = "priority", nullable = false)
    private Integer priority;

    @Column(name = "instance", nullable = false)
    private Integer instance;

    @Column(name = "status_code")
    private Integer statusCode;

    @Column(name = "status_name")
    private String statusName;

    @Column(name = "status_description")
    private String statusDescription;

    @Column(name = "request_id")
    private Long requestId;

    @Column(name = "deleted_reason", length = 500)
    private String deletedReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deleted_by", nullable = false)
    private Employee deletedBy;

    @Column(name = "deleted_at", nullable = false)
    private LocalDateTime deletedAt;
}

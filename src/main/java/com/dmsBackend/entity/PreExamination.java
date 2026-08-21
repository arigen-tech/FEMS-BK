package com.dmsBackend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.sql.Timestamp;

@Entity
@Getter
@Setter
@Table(name = "pre_examination")
public class PreExamination {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @OneToOne
    @JoinColumn(name = "document_header_id", nullable = false, unique = true)
    private DocumentHeader documentHeader;

    @ManyToOne
    @JoinColumn(name = "purpose_id")
    private PurposeMaster purpose;

    @ManyToOne
    @JoinColumn(name = "nature_of_examination_id")
    private NatureOfExaminationMaster natureOfExamination;

    @Column(name = "no_of_parcels")
    private Integer noOfParcels;

    @Column(name = "no_of_exhibits")
    private Integer noOfExhibits;

    @Lob
    @Column(name = "nature_of_case")
    private String natureOfCase;

    @ManyToOne
    @JoinColumn(name = "crime_type_id")
    private CrimeTypeMaster crimeType;

    @ManyToOne
    @JoinColumn(name = "priority_id")
    private PriorityMaster priority;

    @ManyToOne
    @JoinColumn(name = "seal_status_id")
    private SealStatusMaster sealStatus;

    @Lob
    @Column(name = "seal_verification_remarks")
    private String sealVerificationRemarks;

    @ManyToOne
    @JoinColumn(name = "parcel_condition_id")
    private ParcelConditionMaster parcelCondition;

    @Column(name = "parcel_condition_other")
    private String parcelConditionOther;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private PreExamStatus status = PreExamStatus.PENDING;

    @Column(name = "examined_by")
    private String examinedBy;

    @Column(name = "examined_on")
    private Timestamp examinedOn;

    @Column(name = "created_on", nullable = false, updatable = false)
    private Timestamp createdOn;

    @Column(name = "updated_on")
    private Timestamp updatedOn;

    public enum PreExamStatus { PENDING, COMPLETED }
}
package com.dmsBackend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.sql.Timestamp;

@Entity
@Getter
@Setter
@Table(name = "document_forwarding_authority")
public class DocumentForwardingAuthority {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JsonIgnore
    @JoinColumn(name = "document_header_id", referencedColumnName = "id", nullable = false)
    private DocumentHeader documentHeader;

    @ManyToOne
    @JoinColumn(name = "forwarding_authority_type_id", referencedColumnName = "id")
    private ForwardingAuthorityTypeMaster forwardingAuthorityType;

    @Column(name = "authority_name")
    private String authorityName;

    @Column(name = "designation")
    private String designation;

    @Column(name = "organisation")
    private String organisation;

    @ManyToOne
    @JoinColumn(name = "district_id", referencedColumnName = "id")
    private DistrictMaster district;

    @ManyToOne
    @JoinColumn(name = "city_id", referencedColumnName = "id")
    private CityMaster city;

    @Column(name = "address", length = 500)
    private String address;

    @Column(name = "contact_number", length = 50)
    private String contactNumber;

    @Column(name = "email")
    private String email;

    @Column(name = "forwarding_letter_number")
    private String forwardingLetterNumber;

    @Column(name = "forwarding_date")
    private Timestamp forwardingDate;

    @Column(name = "forwarding_letter_path", length = 500)
    private String forwardingLetterPath;

    @ManyToOne
    @JoinColumn(name = "mode_of_submission_id", referencedColumnName = "id")
    private ModeOfSubmissionMaster modeOfSubmission;

    @Column(name = "courier_agency")
    private String courierAgency;

    @Column(name = "awb_consignment_number")
    private String awbConsignmentNumber;

    @Column(name = "booking_date")
    private Timestamp bookingDate;

    @Column(name = "dispatch_date")
    private Timestamp dispatchDate;

    @Column(name = "expected_delivery_date")
    private Timestamp expectedDeliveryDate;

    @Column(name = "actual_delivery_date")
    private Timestamp actualDeliveryDate;

    @Column(name = "parcel_id")
    private String parcelId;

    @Column(name = "parcel_number")
    private String parcelNumber;

    @Column(name = "number_of_exhibits")
    private Integer numberOfExhibits;

    @ManyToOne
    @JoinColumn(name = "package_type_id", referencedColumnName = "id")
    private PackageTypeMaster packageType;

    @Column(name = "seal_number")
    private String sealNumber;

    @Column(name = "seal_description", length = 500)
    private String sealDescription;

    @Column(name = "seal_condition")
    private String sealCondition;

    @Column(name = "package_condition")
    private String packageCondition;

    @Column(name = "received_date")
    private Timestamp receivedDate;

    @Column(name = "received_time", length = 20)
    private String receivedTime;

    @Column(name = "received_by")
    private String receivedBy;

    @Lob
    @Column(name = "remarks")
    private String remarks;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "created_on", nullable = false)
    private Timestamp createdOn;

    @Column(name = "updated_by")
    private String updatedBy;

    @Column(name = "updated_on")
    private Timestamp updatedOn;

    // =========================================================
    // MESSENGER / HANDOVER DETAILS
    // =========================================================

    @Column(name = "messenger_name")
    private String messengerName;

    @Column(name = "messenger_designation")
    private String messengerDesignation;

    @Column(name = "messenger_organization")
    private String messengerOrganization;

    @Column(name = "messenger_id_ref")
    private String messengerIdRef;

    @Column(name = "handover_date_time")
    private Timestamp handoverDateTime;

    @PrePersist
    protected void onCreate() {
        if (createdOn == null) {
            createdOn = new Timestamp(System.currentTimeMillis());
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedOn = new Timestamp(System.currentTimeMillis());
    }
}
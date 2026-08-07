package com.dmsBackend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.sql.Timestamp;

@Entity
@Table(name = "language_master")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LanguageMaster {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String code;      // en-IN, hi-IN

    @Column(nullable = false)
    private String name;      // English, हिंदी



    @Column(name = "is_active")
    private Boolean isActive ;

    @Column(name = "created_on")
    private Timestamp createdOn;

    @Column(name = "updated_on")
    private Timestamp updatedOn;


}

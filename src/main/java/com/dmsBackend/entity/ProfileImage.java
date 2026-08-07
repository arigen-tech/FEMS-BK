package com.dmsBackend.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProfileImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "dp_image_name")
    private String dpImageSrc;

    @Column(name = "dp_image_src")
    private String dpImageName;

    @OneToOne
    @JoinColumn(name = "employee_id")
    @JsonBackReference // Break the cycle here
    private Employee employee;
}

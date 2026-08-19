package com.dmsBackend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "master_state")
@Getter
@Setter
public class StateMaster extends BaseMasterEntity implements CodedMaster {

    @Column(name = "code")
    private String code;
}
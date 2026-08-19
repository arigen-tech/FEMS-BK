package com.dmsBackend.entity;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** City, depends on District (master_district.id). */
@Entity
@Table(name = "master_city")
@AttributeOverride(name = "parentId", column = @Column(name = "district_id"))
public class CityMaster extends ParentedMasterEntity {
}

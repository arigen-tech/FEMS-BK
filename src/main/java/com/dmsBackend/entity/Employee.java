package com.dmsBackend.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;

@Entity
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Table(name = "employee", indexes = {
        @Index(name = "idx_employee_email", columnList = "email"),
        @Index(name = "idx_employee_mobile", columnList = "mobile"),
        @Index(name = "idx_emp_employee_id", columnList = "employee_id"),
        @Index(name = "idx_employee_role", columnList = "role_id"),
        @Index(name = "idx_employee_department", columnList = "department_master_id"),
        @Index(name = "idx_employee_branch", columnList = "department_master_branch_master_id")
})
public class Employee implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "employee_Id")
    private String employeeId;

    @Column(name = "password", length = 64)
    private String password;

    @Column(name = "mobile")
    private String mobile;

    @Column(name = "email", unique = true)
    private String email;

    @Column(name = "name")
    private String name;

    @Column(name = "isActive")
    private boolean isActive;

    @Column(name = "otp")
    private Integer otp;

    @Column(name = "createdOn")
    private Timestamp createdOn;

    @Column(name = "updatedOn")
    private Timestamp updatedOn;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "department_master_branch_master_id", referencedColumnName = "id")
    private BranchMaster branch;


    @ManyToOne(fetch = FetchType.EAGER) // Fetch DepartmentMaster eagerly
    @JoinColumn(name = "department_master_id")
    private DepartmentMaster department;

    @ManyToOne(fetch = FetchType.EAGER) // Fetch RoleMaster eagerly
    @JoinColumn(name = "role_id")
    private RoleMaster role;


    @ManyToOne(fetch = FetchType.EAGER) // Fetch updatedBy eagerly
    @JoinColumn(name = "updated_by_id")
    @JsonBackReference("employee-updatedBy")
    private Employee updatedBy;

    @ManyToOne(fetch = FetchType.EAGER) // Fetch createdBy eagerly
    @JoinColumn(name = "created_by_id")
    @JsonBackReference("employee-createdBy")
    private Employee createdBy;

    @OneToOne(mappedBy = "employee", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private ProfileImage profileImage;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "language_id")
    private LanguageMaster language;


    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (this.role != null && this.role.getRole() != null) {
            return List.of(new SimpleGrantedAuthority(this.role.getRole()));
        }
        return List.of();
    }

    @Override
    public String getUsername() {
        return this.email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return this.isActive;
    }
}

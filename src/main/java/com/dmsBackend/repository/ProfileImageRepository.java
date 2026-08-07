package com.dmsBackend.repository;

import com.dmsBackend.entity.Employee;
import com.dmsBackend.entity.ProfileImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProfileImageRepository extends JpaRepository<ProfileImage, Long> {
    Optional<ProfileImage> findByEmployee(Employee employee);
}

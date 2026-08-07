package com.dmsBackend.repository;

import com.dmsBackend.entity.RoleMaster;
import com.dmsBackend.entity.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface UserSessionRepository extends JpaRepository<UserSession, Long> {

    // Find by employee
    List<UserSession> findByEmployeeIdAndActiveTrue(Integer employeeId);

    // Find by employee and role
    @Query("SELECT us FROM UserSession us WHERE us.employee.id = :employeeId AND us.role = :role AND us.active = true ORDER BY us.createdAt ASC")
    List<UserSession> findByEmployeeIdAndRoleAndActiveTrue(@Param("employeeId") Integer employeeId, @Param("role") RoleMaster role);

    // Find by access token JTI
    Optional<UserSession> findByAccessTokenJtiAndActiveTrue(String jti);

    // Find by refresh token JTI
    Optional<UserSession> findByRefreshTokenJtiAndActiveTrue(String jti);

    // NEW: Deactivate oldest session for employee and role (FIXED for MySQL)
    @Modifying
    @Transactional
    @Query(value = "UPDATE user_session us1 " +
            "JOIN (" +
            "  SELECT id FROM user_session " +
            "  WHERE employee_id = :employeeId AND role_id = :roleId AND active = true " +
            "  ORDER BY created_at ASC LIMIT 1" +
            ") us2 ON us1.id = us2.id " +
            "SET us1.active = false",
            nativeQuery = true)
    int deactivateOldestSession(@Param("employeeId") Integer employeeId, @Param("roleId") Integer roleId);

    // Check if session exists with employee, device and JTI
    @Query("SELECT COUNT(us) > 0 FROM UserSession us WHERE " +
            "us.employee.id = :employeeId AND " +
            "us.deviceId = :deviceId AND " +
            "us.accessTokenJti = :jti AND " +
            "us.active = true")
    boolean existsValidSession(@Param("employeeId") Integer employeeId,
                               @Param("deviceId") String deviceId,
                               @Param("jti") String jti);
}
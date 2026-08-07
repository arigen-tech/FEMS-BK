package com.dmsBackend.service;

import com.dmsBackend.entity.BranchMaster;
import com.dmsBackend.entity.DepartmentMaster;
import com.dmsBackend.entity.Employee;
import com.dmsBackend.entity.ProfileImage;
import com.dmsBackend.response.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

@Service
public interface EmployeeService {
    @Transactional
    Employee create(Employee employee);

    Employee save(Employee employee);

    ApiResponse<Employee> updateEmployeeDetails(Integer employeeId, String name, String email, String mobile,
                                                Integer branchId, Integer departmentIdz);

    Employee findByEmail(String email);
    Employee findById(Integer id);
    void deleteByIdEmployee(Integer id);
    List<Employee> findAllEmployee();
//    void updateEmployeeStatus(Integer id, boolean isActive, HttpServletRequest request);
    void updateEmployeeRoleByEmail(String email, Integer roleId);

//    Employee updateEmployeeRoleByName(Integer id, String roleName);
      public Employee updateEmployeeRoleByName(Integer id, String roleName, HttpServletRequest request) ;
    //List<Employee> findEmployeesByBranch(BranchMaster branch);

//    List<Employee> findEmployeesByBranch(BranchMaster branch);

    // New methods
    List<Employee> getEmployeesByRoleIsNullById(Integer id);
    List<Employee> findEmployeesByRole(String roleName);
    List<Employee> getEmployeesByRoleIsNull();
    List<Employee> getAllWithoutNullRole();

    List<Employee> findAllByRoleIsNotNullAndBranch(BranchMaster branchId);
    List<Employee> findAllByRoleIsNotNullAndDepartment(DepartmentMaster departmentId);
    long countEmployeesByRoleNull();
    long countEmployeesByRoleNotNull();
    long countEmployeesByRole(String roleName);

    void changePassword(String email, String currentPassword, String newPassword);


    List<EmployeeDTO> findEmployeesByBranch(BranchMaster branch);
    List<Employee> findEmployeesWithNullRoleByBranch(BranchMaster branch);

    EmployeeDTO mapToDTO(Employee employee);

    List<Employee> findEmployeesWithNullRole();

    ApiResponse<List<EmployeeDTO>> findEmployeesByDepartment(DepartmentMaster department);


    List<Employee> findEmployeesWithNullRoleByDepartment(DepartmentMaster department);

    ApiResponse<FileResponse> getFilteredEmployeesApiResponse(EmployeeFilterRequest employeeFilterRequest);

    ApiResponse<List<Employee>>findEmployeeByCreatedByEmp(Employee employee);

    Optional<ProfileImage> findByEmployee(Employee employee);

    void saveProfileImage(ProfileImage profileImage);

    @Transactional
    void saveProfileImage(Employee employee, MultipartFile file);

    @Transactional
    Employee updateProfile(Employee employee, Integer loggedInEmployeeId);

    ApiResponse<Employee> switchEmployeeRole(String employeeIdentifier, String targetRoleName, Employee currentEmployee);


    List<Employee> findByDepartmentAndRole(Integer id, String departmentAdmin);

    List<StatusCountByYearDto> getStatusCountsPerYear();

    Optional<Employee> findByEmailOrMobile(String identifier);

    void resetPassword(String email, String newPassword);
     Employee updateEmployeeStatus(Integer id, boolean isActive, HttpServletRequest request);

    void updateLanguageOnly(Integer id, Long languageId);



    public ApiResponse<List<EmployeeDTO>> getEmployeesOfCurrentUserBranchAndDepartment();
}

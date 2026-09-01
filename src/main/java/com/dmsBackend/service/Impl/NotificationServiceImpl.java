package com.dmsBackend.service.Impl;

import com.dmsBackend.entity.*;
import com.dmsBackend.payloads.Helper;
import com.dmsBackend.repository.DepartmentMasterRepository;
import com.dmsBackend.repository.EmployeeRepository;
import com.dmsBackend.repository.NotificationRepository;
import com.dmsBackend.response.NotificationDTO;
import com.dmsBackend.service.DepartmentMasterService;
import com.dmsBackend.service.EmployeeService;
import com.dmsBackend.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    @Autowired
    private NotificationRepository notificationRepository;

    @Lazy
    @Autowired
    private EmployeeService employeeService;

    @Autowired
    private DepartmentMasterService departmentMasterService;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    public NotificationServiceImpl(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    // ======================= CREATE DOCUMENT NOTIFICATION =======================
    @Override
    @Transactional
    public void createDocumentNotification(DocumentDetails detail) {

        log.info("API CALL → Create Document Notification | docId={} status={}",
                detail.getId(), detail.getStatus());

        DocumentHeader header = detail.getDocumentHeader();

        String title;
        String message;
        String detailedMessage;

        if (detail.getStatus() == DocApprovalStatus.APPROVED) {
            title = "Document Approved";
            message = "Your document " + header.getFileNo() + " has been approved";

            log.debug("Creating approval notification for document: {} (ID: {})",
                    header.getTitle(), detail.getId());

            detailedMessage = String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    .notification-card {
                        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
                        max-width: 600px;
                        margin: 20px auto;
                        background: white;
                        border-radius: 12px;
                        box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
                        overflow: hidden;
                    }
                    .header {
                        background: #4CAF50;
                        color: white;
                        padding: 20px;
                        text-align: center;
                        font-size: 1.25rem;
                        font-weight: 600;
                    }
                    .content { padding: 20px; }
                    .section { margin-bottom: 20px; }
                    .section-title {
                        font-size: 1.1rem;
                        font-weight: 600;
                        color: #333;
                        margin-bottom: 15px;
                        padding-bottom: 8px;
                        border-bottom: 2px solid #eee;
                    }
                    .info-row {
                        display: flex;
                        margin-bottom: 12px;
                        align-items: flex-start;
                    }
                    .label {
                        flex: 0 0 120px;
                        color: #666;
                        font-weight: 500;
                    }
                    .value {
                        flex: 1;
                        color: #333;
                        font-weight: 500;
                    }
                    .footer {
                        background: #f8f9fa;
                        padding: 15px;
                        text-align: center;
                        color: #666;
                        font-style: italic;
                    }
                    .emoji { margin-right: 8px; }
                </style>
            </head>
            <body>
                <div class="notification-card">
                    <div class="header">
                        <span class="emoji">📄</span> Document Approved
                    </div>
                    
                    <div class="content">
                        <div class="section">
                            <div class="section-title">Document Details</div>
                            <div class="info-row"><div class="label">📎 Title</div><div class="value">%s</div></div>
                            <div class="info-row"><div class="label">📋 File No</div><div class="value">%s</div></div>
                            <div class="info-row"><div class="label">📌 Subject</div><div class="value">%s</div></div>
                            <div class="info-row"><div class="label">📂 File Name</div><div class="value">%s (v%s)</div></div>
                        </div>

                        <div class="section">
                            <div class="section-title">✅ Approval Information</div>
                            <div class="info-row"><div class="label">👤 ApprovedBy</div><div class="value">%s</div></div>
                            <div class="info-row"><div class="label">🕒 ApprovedOn</div><div class="value">%s</div></div>
                        </div>
                    </div>

                    <div class="footer">✨ Thank you for your submission!</div>
                </div>
            </body>
            </html>
            """,
                    header.getTitle(),
                    header.getFileNo(),
                    header.getSubject(),
                    detail.getDocName(),
                    detail.getVersion() != null ? detail.getVersion() : "-",
                    detail.getApprovedBy() != null ? detail.getApprovedBy() : "System",
                    detail.getApprovedOn() != null ? detail.getApprovedOn().toString() : "N/A"
            );

        } else if (detail.getStatus() == DocApprovalStatus.REJECTED) {
            title = "Document Rejected";
            message = "Your document " + header.getFileNo() + " has been rejected";

            log.debug("Creating rejection notification for document: {} (ID: {})",
                    header.getTitle(), detail.getId());

            detailedMessage = String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    .notification-card {
                        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
                        max-width: 600px;
                        margin: 20px auto;
                        background: white;
                        border-radius: 12px;
                        box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
                        overflow: hidden;
                    }
                    .header {
                        background: #f44336;
                        color: white;
                        padding: 20px;
                        text-align: center;
                        font-size: 1.25rem;
                        font-weight: 600;
                    }
                    .content { padding: 20px; }
                    .section { margin-bottom: 20px; }
                    .section-title {
                        font-size: 1.1rem;
                        font-weight: 600;
                        color: #333;
                        margin-bottom: 15px;
                        padding-bottom: 8px;
                        border-bottom: 2px solid #eee;
                    }
                    .info-row {
                        display: flex;
                        margin-bottom: 12px;
                        align-items: flex-start;
                    }
                    .label {
                        flex: 0 0 120px;
                        color: #666;
                        font-weight: 500;
                    }
                    .value {
                        flex: 1;
                        color: #333;
                        font-weight: 500;
                    }
                    .footer {
                        background: #f8f9fa;
                        padding: 15px;
                        text-align: center;
                        color: #666;
                        font-style: italic;
                    }
                    .emoji { margin-right: 8px; }
                </style>
            </head>
            <body>
                <div class="notification-card">
                    <div class="header">
                        <span class="emoji">📄</span> Document Rejected
                    </div>
                    
                    <div class="content">
                        <div class="section">
                            <div class="section-title">Document Details</div>
                            <div class="info-row"><div class="label">📎 Title</div><div class="value">%s</div></div>
                            <div class="info-row"><div class="label">📋 File No</div><div class="value">%s</div></div>
                            <div class="info-row"><div class="label">📌 Subject</div><div class="value">%s</div></div>
                            <div class="info-row"><div class="label">📂 File Name</div><div class="value">%s (v%s)</div></div>
                        </div>

                        <div class="section">
                            <div class="section-title">❌ Rejection Information</div>
                            <div class="info-row"><div class="label">👤 RejectedBy</div><div class="value">%s</div></div>
                            <div class="info-row"><div class="label">❗ Reason</div><div class="value">%s</div></div>
                            <div class="info-row"><div class="label">🕒 RejectedOn</div><div class="value">%s</div></div>
                        </div>
                    </div>

                    <div class="footer">📝 Please review and resubmit if needed.</div>
                </div>
            </body>
            </html>
            """,
                    header.getTitle(),
                    header.getFileNo(),
                    header.getSubject(),
                    detail.getDocName(),
                    detail.getVersion() != null ? detail.getVersion() : "-",
                    detail.getUpdatedBy() != null ? detail.getUpdatedBy() : "System",
                    detail.getRejectionReason() != null ? detail.getRejectionReason() : "No reason provided",
                    detail.getUpdatedOn() != null ? detail.getUpdatedOn().toString() : "N/A"
            );

        } else {
            log.debug("Document status is neither approved nor rejected: {}", detail.getStatus());
            return;
        }

        Notification notification = Notification.builder()
                .employee(header.getEmployee())
                .title(title)
                .message(message)
                .detailedMessage(detailedMessage)
                .type(detail.getStatus() == DocApprovalStatus.APPROVED ?
                        NotificationType.DOCUMENT_APPROVAL : NotificationType.DOCUMENT_REJECTION)
                .isRead(false)
                .referenceId(detail.getId())
                .referenceType("DOCUMENT_DETAIL")
                .build();

        notificationRepository.save(notification);
        log.info("SUCCESS → Document Notification Created | docId={} status={} employeeId={}",
                detail.getId(), detail.getStatus(), header.getEmployee().getId());
    }

    // ======================= CREATE EMPLOYEE UPDATE NOTIFICATION =======================
    @Override
    @Transactional
    public void createEmployeeUpdateNotification(Employee employee, String updateType, Map<String, Boolean> changedFields) {

        log.info("API CALL → Create Employee Update Notification | employeeId={} updateType={}",
                employee.getId(), updateType);

        String title;
        String message;
        String detailedMessage;

        if (changedFields == null) {
            changedFields = new HashMap<>();
        }

        if ("STATUS_CHANGE".equals(updateType)) {
            title = "Account Status Updated";
            message = "Your account status has been " + (employee.isActive() ? "activated" : "deactivated");
            String statusColor = employee.isActive() ? "#28a745" : "#dc3545";

            log.debug("Creating status change notification for employee: {} (Active: {})",
                    employee.getName(), employee.isActive());

            detailedMessage = String.format("""
            <!DOCTYPE html>
            <html lang='en'>
            <head>
            <style>
                body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                table { width: 100%%; border-collapse: collapse; }
                th, td { padding: 10px; border-bottom: 1px solid #ddd; }
                th { background-color: #f2f2f2; }
            </style>
            </head>
            <body>
            <h2>Employee Status Update</h2>
            <p>Your employee status has been updated to: 
               <span style='font-weight: bold; color: %s;'>%s</span></p>
            <h3>Updated Details:</h3>
            <table>
                <tr><th>Name</th><td>%s</td></tr>
                <tr><th>Email</th><td>%s</td></tr>
                <tr><th>Mobile</th><td>%s</td></tr>
                <tr><th>Branch</th><td>%s</td></tr>
                <tr><th>Department</th><td>%s</td></tr>
            </table>
            <p><strong>If you did not request this change, please contact the administrator immediately.</strong></p>
            <div class='footer'>
                <p>Best regards,<br>The Company Team</p>
            </div>
            </body>
            </html>
            """,
                    statusColor,
                    employee.isActive() ? "Activated" : "Deactivated",
                    employee.getName(),
                    employee.getEmail(),
                    employee.getMobile(),
                    employee.getBranch() != null ? employee.getBranch().getName() : "N/A",
                    employee.getDepartment() != null ? employee.getDepartment().getName() : "N/A"
            );
        } else {
            title = "Profile Updated";
            message = "Your profile information has been updated";

            log.debug("Creating profile update notification for employee: {}", employee.getName());

            detailedMessage = String.format("""
            <!DOCTYPE html>
            <html lang='en'>
            <head>
            <style>
                body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                table { width: 100%%; border-collapse: collapse; }
                th, td { padding: 10px; border-bottom: 1px solid #ddd; }
                th { background-color: #f2f2f2; }
                .highlight { background-color: #fffacd; font-weight: bold; }
            </style>
            </head>
            <body>
            <h2>Profile Update Details</h2>
            <table>
                <tr><th>Field</th><th>Value</th></tr>
                <tr%s><td>Name</td><td>%s</td></tr>
                <tr%s><td>Email</td><td>%s</td></tr>
                <tr%s><td>Mobile</td><td>%s</td></tr>
                <tr%s><td>Branch</td><td>%s</td></tr>
                <tr%s><td>Department</td><td>%s</td></tr>
            </table>
            <p><strong>If you did not request this change, please contact the administrator immediately.</strong></p>
            <div class='footer'>
                <p>Best regards,<br>The Company Team</p>
            </div>
            </body>
            </html>
            """,
                    changedFields.getOrDefault("name", false) ? " class='highlight'" : "",
                    employee.getName(),
                    changedFields.getOrDefault("email", false) ? " class='highlight'" : "",
                    employee.getEmail(),
                    changedFields.getOrDefault("mobile", false) ? " class='highlight'" : "",
                    employee.getMobile(),
                    changedFields.getOrDefault("branch", false) ? " class='highlight'" : "",
                    employee.getBranch() != null ? employee.getBranch().getName() : "N/A",
                    changedFields.getOrDefault("department", false) ? " class='highlight'" : "",
                    employee.getDepartment() != null ? employee.getDepartment().getName() : "N/A"
            );
        }

        Notification notification = Notification.builder()
                .employee(employee)
                .title(title)
                .message(message)
                .detailedMessage(detailedMessage)
                .type("STATUS_CHANGE".equals(updateType) ?
                        NotificationType.EMPLOYEE_STATUS_CHANGE : NotificationType.EMPLOYEE_UPDATE)
                .isRead(false)
                .referenceId(employee.getId())
                .referenceType("EMPLOYEE")
                .build();

        notificationRepository.save(notification);
        log.info("SUCCESS → Employee Update Notification Created | employeeId={} type={}",
                employee.getId(), updateType);
    }

    // ======================= CREATE ROLE ASSIGNMENT NOTIFICATION =======================
    @Override
    @Transactional
    public void createRoleAssignmentNotification(Employee employee, RoleMaster newRole, Employee assignedBy) {

        log.info("API CALL → Create Role Assignment Notification | employeeId={} newRole={} assignedBy={}",
                employee.getId(), newRole.getRole(), assignedBy.getName());

        String title = "Role Assignment";
        String message = "You have been assigned a new role: " + newRole.getRole();

        String detailedMessage = String.format("""
        <!DOCTYPE html>
        <html lang='en'>
        <head>
        <style>
            body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
            table { width: 100%%; border-collapse: collapse; }
            th, td { padding: 10px; border-bottom: 1px solid #ddd; }
            th { background-color: #f2f2f2; }
            .highlight { color: #2196F3; font-weight: bold; }
        </style>
        </head>
        <body>
        <h2>Role Assignment Notification</h2>
        <table>
            <tr>
                <th>Employee Name</th>
                <td>%s</td>
            </tr>
            <tr>
                <th>New Role</th>
                <td class='highlight'>%s</td>
            </tr>
            <tr>
                <th>Assigned By</th>
                <td>%s</td>
            </tr>
            <tr>
                <th>Assignment Date</th>
                <td>%s</td>
            </tr>
            <tr>
                <th>Branch</th>
                <td>%s</td>
            </tr>
            <tr>
                <th>Department</th>
                <td>%s</td>
            </tr>
        </table>
        <p><strong>Note:</strong> This role change grants you new permissions and responsibilities in the system.</p>
        <p><strong>If you believe this role assignment was made in error, please contact the administrator immediately.</strong></p>
        <div class='footer'>
            <p>Best regards,<br>The Company Team</p>
        </div>
        </body>
        </html>
        """,
                employee.getName(),
                newRole.getRole(),
                assignedBy.getName(),
                Helper.getCurrentTimeStamp(),
                employee.getBranch() != null ? employee.getBranch().getName() : "N/A",
                employee.getDepartment() != null ? employee.getDepartment().getName() : "N/A"
        );

        Notification notification = Notification.builder()
                .employee(employee)
                .title(title)
                .message(message)
                .detailedMessage(detailedMessage)
                .type(NotificationType.ROLE_UPDATE)
                .isRead(false)
                .referenceId(employee.getId())
                .referenceType("EMPLOYEE")
                .build();

        notificationRepository.save(notification);
        log.info("SUCCESS → Role Assignment Notification Created | employeeId={} role={}",
                employee.getId(), newRole.getRole());
    }

    // ======================= CREATE NEW EMPLOYEE NOTIFICATION =======================
    @Override
    @Transactional
    public void createNewEmployeeNotification(Employee newEmployee) {

        log.info("API CALL → Create New Employee Notification | employeeId={} name={}",
                newEmployee.getId(), newEmployee.getName());

        DepartmentMaster department = newEmployee.getDepartment();
        if (department == null || department.getName() == null) {
            log.debug("Fetching department details for employee ID: {}", newEmployee.getId());
            department = departmentMasterService.findById(newEmployee.getDepartment().getId());
            newEmployee.setDepartment(department);
        }

        List<Employee> departmentAdmins = employeeService.findByDepartmentAndRole(
                newEmployee.getDepartment().getId(),
                "SCIENTIFIC OFFICER"
        );

        String title = "New Employee Added";
        String message = String.format(
                "%s is added in your %s. Please assign a role.",
                newEmployee.getName(),
                newEmployee.getDepartment().getName()
        );

        String detailedMessage = String.format("""
        <!DOCTYPE html>
        <html lang='en'>
        <head>
        <style>
            body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
            table { width: 100%%; border-collapse: collapse; }
            th, td { padding: 10px; border-bottom: 1px solid #ddd; }
            th { background-color: #f2f2f2; }
            .highlight { color: #2196F3; font-weight: bold; }
        </style>
        </head>
        <body>
        <h2>New Employee Notification</h2>
        <table>
            <tr>
                <th>Employee Name</th>
                <td class='highlight'>%s</td>
            </tr>
            <tr>
                <th>Employee ID</th>
                <td>%s</td>
            </tr>
            <tr>
                <th>Email</th>
                <td>%s</td>
            </tr>
            <tr>
                <th>Department</th>
                <td>%s</td>
            </tr>
            <tr>
                <th>Branch</th>
                <td>%s</td>
            </tr>
            <tr>
                <th>Joined Date</th>
                <td>%s</td>
            </tr>
        </table>
        <p><strong>Note:</strong> Please ensure to welcome the new employee and provide necessary assistance.</p>
        <div class='footer'>
            <p>Best regards,<br>The HR Team</p>
        </div>
        </body>
        </html>
        """,
                newEmployee.getName(),
                newEmployee.getId(),
                newEmployee.getEmail(),
                department.getName(),
                newEmployee.getBranch().getName(),
                newEmployee.getCreatedOn().toString()
        );

        log.info("Found {} SCIENTIFIC OFFICERs for department: {}",
                departmentAdmins.size(), department.getName());

        for (Employee admin : departmentAdmins) {
            Notification notification = Notification.builder()
                    .employee(admin)
                    .title(title)
                    .message(message)
                    .detailedMessage(detailedMessage)
                    .type(NotificationType.NEW_EMPLOYEE_ADDED)
                    .isRead(false)
                    .referenceId(newEmployee.getId())
                    .referenceType("EMPLOYEE")
                    .createdOn(new Timestamp(System.currentTimeMillis()))
                    .build();

            notificationRepository.save(notification);
            log.debug("Created new employee notification for admin ID: {}", admin.getId());
        }

        log.info("SUCCESS → New Employee Notifications Created | employeeId={} adminsNotified={}",
                newEmployee.getId(), departmentAdmins.size());
    }

    // ======================= CREATE NEW DOCUMENT SAVED NOTIFICATION =======================
    @Override
    @Transactional
    public void createNewDocumentSavedNotification(DocumentHeader document) {

        log.info("API CALL → Create New Document Saved Notification | docId={} title={}",
                document.getId(), document.getTitle());

        try {
            log.debug("Starting notification creation for document ID: {}", document.getId());

            Employee creator = document.getEmployee();
            if (creator == null) {
                log.error("FAILED → Document creator not found for document ID: {}", document.getId());
                return;
            }

            DepartmentMaster department = creator.getDepartment();
            if (department == null) {
                log.error("FAILED → Department not found for employee ID: {}", creator.getId());
                return;
            }

            List<Employee> departmentAdmins = employeeRepository.findByDepartmentIdAndRoleRole(
                    department.getId(),
                    "SCIENTIFIC OFFICER"
            );

            log.debug("Found {} SCIENTIFIC OFFICERs for department ID: {}",
                    departmentAdmins.size(), department.getId());

            if (departmentAdmins.isEmpty()) {
                log.warn("No SCIENTIFIC OFFICERs found - notification creation stopped");
                return;
            }

            int notificationsCreated = 0;
            for (Employee admin : departmentAdmins) {
                try {
                    Notification notification = Notification.builder()
                            .employee(admin)
                            .title("New Document Added")
                            .message("New document '" + document.getTitle() + "' added in " + department.getName())
                            .detailedMessage(createDetailedMessage(document, admin))
                            .type(NotificationType.NEW_DOCUMENT)
                            .isRead(false)
                            .referenceId(document.getId())
                            .referenceType("DOCUMENT")
                            .createdOn(new Timestamp(System.currentTimeMillis()))
                            .build();

                    Notification savedNotification = notificationRepository.save(notification);
                    notificationsCreated++;
                    log.debug("Created notification ID: {} for admin: {}",
                            savedNotification.getId(), admin.getName());
                } catch (Exception e) {
                    log.error("Error creating notification for admin {}: {}", admin.getName(), e.getMessage(), e);
                }
            }

            log.info("SUCCESS → New Document Notifications Created | docId={} adminsNotified={} totalCreated={}",
                    document.getId(), departmentAdmins.size(), notificationsCreated);

        } catch (Exception e) {
            log.error("FAILED → Document Notification Creation | docId={} error={}",
                    document.getId(), e.getMessage(), e);
        }
    }

    // ======================= CREATE CUSTOM NOTIFICATION =======================
    @Override
    @Transactional
    public void createCustomNotification(Employee employee, String title, String message,
                                         String detailedMessage, NotificationType type,
                                         Integer referenceId, String referenceType) {

        log.info("API CALL → Create Custom Notification | employeeId={} type={} title={}",
                employee.getId(), type, title);

        Notification notification = Notification.builder()
                .employee(employee)
                .title(title)
                .message(message)
                .detailedMessage(detailedMessage)
                .type(type)
                .isRead(false)
                .referenceId(referenceId)
                .referenceType(referenceType)
                .build();

        notificationRepository.save(notification);
        log.info("SUCCESS → Custom Notification Created | employeeId={} type={}",
                employee.getId(), type);
    }

    // ======================= GET USER NOTIFICATIONS =======================
    @Override
    @Transactional(readOnly = true)
    public List<NotificationDTO> getUserNotifications(Integer employeeId) {

        log.info("API CALL → Get User Notifications | employeeId={}", employeeId);

        List<NotificationDTO> notifications = notificationRepository
                .findByEmployeeIdOrderByCreatedOnDesc(employeeId)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        log.info("SUCCESS → Retrieved {} notifications for employee ID: {}",
                notifications.size(), employeeId);

        return notifications;
    }

    // ======================= MARK AS READ =======================
    @Override
    @Transactional
    public void markAsRead(Long notificationId) {

        log.info("API CALL → Mark Notification as Read | notificationId={}", notificationId);

        try {
            notificationRepository.findById(notificationId).ifPresent(notification -> {
                notification.setRead(true);
                notification.setReadOn(new Timestamp(System.currentTimeMillis()));
                notificationRepository.save(notification);
                log.info("SUCCESS → Notification Marked as Read | notificationId={}", notificationId);
            });
        } catch (Exception e) {
            log.error("FAILED → Mark Notification as Read | notificationId={} error={}",
                    notificationId, e.getMessage(), e);
            throw e;
        }
    }

    // ======================= GET UNREAD COUNT =======================
    @Override
    @Transactional(readOnly = true)
    public Map<String, Long> getUnreadCount(Integer employeeId, String role) {

        log.info("API CALL → Get Unread Count | employeeId={} role={}", employeeId, role);

        Map<String, Long> counts = new HashMap<>();

        if ("CASE & EVIDENCE OFFICER".equals(role)) {
            long userNotificationCount = notificationRepository.countByEmployee_IdAndTypeInAndIsReadFalse(
                    employeeId,
                    Arrays.asList(
                            NotificationType.EMPLOYEE_UPDATE,
                            NotificationType.EMPLOYEE_STATUS_CHANGE,
                            NotificationType.ROLE_UPDATE,
                            NotificationType.DOCUMENT_APPROVAL,
                            NotificationType.DOCUMENT_REJECTION,
                            NotificationType.DOCUMENT_SHARE,
                            NotificationType.DOCUMENT_SHARE_REVOKE
                    )
            );
            counts.put("unreadCount", userNotificationCount);
        } else if ("SCIENTIFIC OFFICER".equals(role)) {
            long newDocumentCount = notificationRepository.countByEmployee_IdAndTypeAndIsReadFalse(
                    employeeId,
                    NotificationType.NEW_DOCUMENT
            );

            long newEmployeeCount = notificationRepository.countByEmployee_IdAndTypeAndIsReadFalse(
                    employeeId,
                    NotificationType.NEW_EMPLOYEE_ADDED
            );

            long totalCount = newEmployeeCount + newDocumentCount;
            counts.put("unreadCount", totalCount);
        } else if ("SYSTEM ADMIN".equals(role) || "LABORATORY ADMIN / HOD".equals(role)) {
            long totalCount = notificationRepository.countByEmployee_IdAndIsReadFalse(employeeId);
            counts.put("unreadCount", totalCount);
        }

        log.info("SUCCESS → Unread Count Retrieved | employeeId={} role={} count={}",
                employeeId, role, counts.getOrDefault("unreadCount", 0L));

        return counts;
    }

    // ======================= CREATE DOCUMENT SHARE NOTIFICATION =======================
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createDocumentShareNotification(DocumentShare share) {

        log.info("API CALL → Create Document Share Notification | shareId={}", share.getId());

        try {
            log.debug("Creating DOCUMENT_SHARE notification for recipient: {}",
                    share.getSharedTo().getName());

            if (share.getSharedTo() == null) {
                log.error("FAILED → Recipient is null for share ID: {}", share.getId());
                return;
            }

            if (share.getDocumentDetails() == null) {
                log.error("FAILED → DocumentDetails is null for share ID: {}", share.getId());
                return;
            }

            String title = "Document Shared";
            String message = String.format(
                    "%s shared document '%s' with you%s",
                    share.getSharedBy().getName(),
                    share.getDocumentDetails().getDocName(),
                    share.getEndTime() != null ?
                            String.format(" (expires on %s)",
                                    share.getEndTime().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm"))) :
                            ""
            );

            String detailedMessage = createDocumentShareMessage(share);

            Notification notification = Notification.builder()
                    .employee(share.getSharedTo())
                    .title(title)
                    .message(message)
                    .detailedMessage(detailedMessage)
                    .type(NotificationType.DOCUMENT_SHARE)
                    .isRead(false)
                    .referenceId(share.getDocumentDetails().getId())
                    .referenceType("DOCUMENT_DETAIL")
                    .createdOn(new Timestamp(System.currentTimeMillis()))
                    .build();

            Notification savedNotification = notificationRepository.save(notification);
            log.info("SUCCESS → Document Share Notification Created | notificationId={} shareId={} recipientId={}",
                    savedNotification.getId(), share.getId(), share.getSharedTo().getId());

        } catch (Exception e) {
            log.error("FAILED → Document Share Notification Creation | shareId={} error={}",
                    share.getId(), e.getMessage(), e);
        }
    }

    // ======================= CREATE DOCUMENT SHARE REVOKE NOTIFICATION =======================
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createDocumentShareRevokeNotification(DocumentShare share, String reason) {

        log.info("API CALL → Create Document Share Revoke Notification | shareId={}", share.getId());

        try {
            log.debug("Creating DOCUMENT_SHARE_REVOKE notification for recipient: {}",
                    share.getSharedTo().getName());

            if (share.getSharedTo() == null) {
                log.error("FAILED → Recipient is null for share ID: {}", share.getId());
                return;
            }

            if (share.getDocumentDetails() == null) {
                log.error("FAILED → DocumentDetails is null for share ID: {}", share.getId());
                return;
            }

            String title = "Document Access Revoked";
            String message = String.format(
                    "%s revoked your access to document '%s'%s",
                    share.getSharedBy().getName(),
                    share.getDocumentDetails().getDocName(),
                    reason != null ? String.format(" (Reason: %s)", reason) : ""
            );

            String detailedMessage = createDocumentShareRevokeMessage(share, reason);

            Notification notification = Notification.builder()
                    .employee(share.getSharedTo())
                    .title(title)
                    .message(message)
                    .detailedMessage(detailedMessage)
                    .type(NotificationType.DOCUMENT_SHARE_REVOKE)
                    .isRead(false)
                    .referenceId(share.getDocumentDetails().getId())
                    .referenceType("DOCUMENT_DETAIL")
                    .createdOn(new Timestamp(System.currentTimeMillis()))
                    .build();

            Notification savedNotification = notificationRepository.save(notification);
            log.info("SUCCESS → Document Share Revoke Notification Created | notificationId={} shareId={} recipientId={}",
                    savedNotification.getId(), share.getId(), share.getSharedTo().getId());

        } catch (Exception e) {
            log.error("FAILED → Document Share Revoke Notification Creation | shareId={} error={}",
                    share.getId(), e.getMessage(), e);
        }
    }

    // ======================= HELPER METHODS =======================

    private String createDetailedMessage(DocumentHeader document, Employee admin) {
        StringBuilder message = new StringBuilder();
        message.append("<!DOCTYPE html>")
                .append("<html lang='en'>")
                .append("<head>")
                .append("<style>")
                .append("body { font-family: Arial, sans-serif; margin: 20px; line-height: 1.6; color: #333; }")
                .append("h2 { color: #4CAF50; text-align: center; }")
                .append("table { width: 100%; border-collapse: collapse; margin-top: 20px; }")
                .append("th, td { text-align: left; padding: 12px; border-bottom: 1px solid #ddd; }")
                .append("th { background-color: #f2f2f2; color: #333; font-weight: bold; }")
                .append("td { color: #555; }")
                .append("p { margin-top: 20px; font-size: 14px; }")
                .append(".action { font-weight: bold; color: #FF5722; }")
                .append("</style>")
                .append("</head>")
                .append("<body>")
                .append("<h2>New Document Added</h2>")
                .append("<table>")
                .append("<tr><th>Title</th><td>").append(document.getTitle()).append("</td></tr>")
                .append("<tr><th>File No</th><td>").append(document.getFileNo()).append("</td></tr>")
                .append("<tr><th>Subject</th><td>").append(document.getSubject()).append("</td></tr>");

        if (document.getEmployee() != null) {
            message.append("<tr><th>Created By</th><td>").append(document.getEmployee().getName()).append("</td></tr>");
        }

        if (admin.getDepartment() != null) {
            message.append("<tr><th>Department</th><td>").append(admin.getDepartment().getName()).append("</td></tr>");
        } else {
            message.append("<tr><th>Department</th><td>Not specified</td></tr>");
        }

        message.append("<tr><th>Created On</th><td>")
                .append(new java.text.SimpleDateFormat("dd MMM yyyy HH:mm:ss").format(document.getCreatedOn()))
                .append("</td></tr>")
                .append("</table>")
                .append("<p><span class='action'>Action Required:</span> Please review this document at your earliest convenience.</p>")
                .append("</body>")
                .append("</html>");

        return message.toString();
    }

    private String createDocumentShareMessage(DocumentShare share) {
        return String.format("""
        <!DOCTYPE html>
        <html lang='en'>
        <head>
        <style>
            body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; margin: 20px; }
            h2 { color: #2196F3; }
            table { width: 100%%; border-collapse: collapse; margin-top: 15px; }
            th, td { text-align: left; padding: 10px; border-bottom: 1px solid #ddd; }
            th { background-color: #f2f2f2; color: #333; font-weight: bold; }
            .highlight { color: #2196F3; font-weight: bold; }
            .expiry { color: #FF5722; }
        </style>
        </head>
        <body>
        <h2>📄 Document Shared With You</h2>
        <table>
            <tr><th>Document Name</th><td class='highlight'>%s</td></tr>
            <tr><th>Shared By</th><td>%s</td></tr>
            <tr><th>Shared On</th><td>%s</td></tr>
            %s
            <tr><th>Document Title</th><td>%s</td></tr>
            <tr><th>File Number</th><td>%s</td></tr>
        </table>
        <p><strong>Note:</strong> You can now access this document in your shared documents section.</p>
        </body>
        </html>
        """,
                share.getDocumentDetails().getDocName(),
                share.getSharedBy().getName(),
                Helper.getCurrentTimeStamp(),
                share.getEndTime() != null ?
                        String.format("<tr><th class='expiry'>Expiry Date</th><td class='expiry'>%s</td></tr>",
                                share.getEndTime().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm"))) :
                        "",
                share.getDocumentHeader() != null ? share.getDocumentHeader().getTitle() : "N/A",
                share.getDocumentHeader() != null ? share.getDocumentHeader().getFileNo() : "N/A"
        );
    }

    private String createDocumentShareRevokeMessage(DocumentShare share, String reason) {
        return String.format("""
        <!DOCTYPE html>
        <html lang='en'>
        <head>
        <style>
            body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; margin: 20px; }
            h2 { color: #f44336; }
            table { width: 100%%; border-collapse: collapse; margin-top: 15px; }
            th, td { text-align: left; padding: 10px; border-bottom: 1px solid #ddd; }
            th { background-color: #f2f2f2; color: #333; font-weight: bold; }
            .highlight { color: #f44336; font-weight: bold; }
            .reason { color: #FF5722; font-style: italic; }
        </style>
        </head>
        <body>
        <h2>🚫 Document Access Revoked</h2>
        <table>
            <tr><th>Document Name</th><td class='highlight'>%s</td></tr>
            <tr><th>Revoked By</th><td>%s</td></tr>
            <tr><th>Revoked On</th><td>%s</td></tr>
            %s
            <tr><th>Document Title</th><td>%s</td></tr>
            <tr><th>File Number</th><td>%s</td></tr>
        </table>
        <p><strong>Note:</strong> You no longer have access to this document.</p>
        </body>
        </html>
        """,
                share.getDocumentDetails().getDocName(),
                share.getSharedBy().getName(),
                Helper.getCurrentTimeStamp(),
                reason != null ?
                        String.format("<tr><th class='reason'>Reason</th><td class='reason'>%s</td></tr>", reason) :
                        "",
                share.getDocumentHeader() != null ? share.getDocumentHeader().getTitle() : "N/A",
                share.getDocumentHeader() != null ? share.getDocumentHeader().getFileNo() : "N/A"
        );
    }

    private NotificationDTO convertToDTO(Notification notification) {
        return NotificationDTO.builder()
                .id(notification.getId())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .detailedMessage(notification.getDetailedMessage())
                .type(notification.getType())
                .isRead(notification.isRead())
                .createdOn(notification.getCreatedOn())
                .referenceId(notification.getReferenceId())
                .referenceType(notification.getReferenceType())
                .build();
    }
}
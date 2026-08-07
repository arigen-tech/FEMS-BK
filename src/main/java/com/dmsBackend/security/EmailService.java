package com.dmsBackend.security;

import com.dmsBackend.entity.Employee;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    public void sendOtp(String email, String otp, Employee employee) {
        MimeMessage message = mailSender.createMimeMessage();


        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setTo(email);
            helper.setSubject("DMS - OTP Alert");

            String otpp = "<html>\n" +
                    "<body>\n" +
                    "  \n" +
                    "\n" +
                    "\t\t\t\t<table width=\"100%\" bgcolor=\"#ffffff\" style=\"font-family:Arial,Helvetica,sans-serif;box-sizing:border-box;font-size:14px;width:100%;background-color:#ffffff;margin:0\">\n" +
                    "                    <tbody>\n" +
                    "                    <tr style=\"font-family:Arial,Helvetica,sans-serif;box-sizing:border-box;font-size:14px;margin:0\">\n" +
                    "                        <td width=\"600px\" valign=\"top\"\n" +
                    "                            style=\"font-family:Arial,Helvetica,sans-serif;box-sizing:border-box;font-size:14px;vertical-align:top;display:block!important;max-width:600px!important;clear:both!important;margin:0 auto\">\n" +
                    "                            <div style=\"font-family:Arial,Helvetica,sans-serif;box-sizing:border-box;font-size:14px;max-width:600px;display:block;margin:0 auto;background:#fff;border:1px solid #d6d6d6\">\n" +
                    "                                <div style=\"padding:20px\">\n" +
                    "                                 \n" +
                    "                                    <table bgcolor=\"#fff\" cellpadding=\"0\" cellspacing=\"0\"\n" +
                    "                                           style=\"font-family:Arial,Helvetica,sans-serif;box-sizing:border-box;font-size:14px;background-color:#fff;margin:0\"\n" +
                    "                                           width=\"100%\">\n" +
                    "                                        <tbody>\n" +
                    "                                        <tr style=\"font-family:Arial,Helvetica,sans-serif;box-sizing:border-box;font-size:14px;margin:0\">\n" +
                    "                                            <td style=\"font-family:Arial,Helvetica,sans-serif;box-sizing:border-box;font-size:14px;vertical-align:top;margin:0\"\n" +
                    "                                                valign=\"top\">\n" +
                    "                                                <table cellpadding=\"0\" cellspacing=\"0\"\n" +
                    "                                                       style=\"font-family:Arial,Helvetica,sans-serif;box-sizing:border-box;font-size:14px;margin:0\"\n" +
                    "                                                       width=\"100%\">\n" +
                    "                                                    <tbody>\n" +
                    "                                                    <tr style=\"font-family:Arial,Helvetica,sans-serif;box-sizing:border-box;font-size:14px;margin:0\">\n" +
                    "                                                        <td width=\"68%\" valign=\"top\"\n" +
                    "                                                            style=\"font-family:Arial,Helvetica,sans-serif;box-sizing:border-box;vertical-align:top;margin:0;padding:0 0 5px\">\n" +
                    "                                                            <span style=\"font-size:16px;padding-bottom:5px;font-weight:bold;font-family:Arial,Helvetica,sans-serif;color:rgba(0,0,0,0.8);margin:20px 0 10px 0;display:block\">Hi " + employee.getName() + ",</span>\n" +
                    "                                                            <div style=\"width:100%;border-radius:8px;background:#253858;color:#ffffff;display:block;text-align:left;font-family:Arial,Helvetica,sans-serif;padding:20px;box-sizing:border-box;font-size:16px\">\n" +
                    "                                                                <em style=\"font-style:normal;color:#ffab00\">" + otp + "</em> is\n" +
                    "                                                                your <span class=\"il\">OTP</span> (<span\n" +
                    "                                                                    class=\"il\">One</span> Time Password) to login to\n" +
                    "                                                                DMS. <span class=\"il\">OTP</span> will be valid\n" +
                    "                                                                for 10 minutes. Please do not share it with anyone.\n" +
                    "                                                            </div>\n" +
                    "                                                            <div style=\"width:100%;color:#5e6c84;text-align:left;font-family:Arial,Helvetica,sans-serif;box-sizing:border-box;font-size:16px;margin:20px 0 0 0\">\n" +
                    "                                                                Didn’t request for the <span class=\"il\">OTP</span>? No\n" +
                    "                                                                issue. Someone might have entered your mobile number by\n" +
                    "                                                                mistake. If you don’t feel this way, kindly contact us\n" +
                    "                                                                at ****-***-**** or easily reset your password from the\n" +
                    "                                                                Settings option in <a\n" +
                    "                                                                    style=\"color:#0065ff;text-decoration:none\"\n" +
                    "                                                                Account’</a>.\n" +
                    "                                                            </div>\n" +
                    "                                                        </td>\n" +
                    "                                                        <td width=\"32%\" align=\"right\" valign=\"top\"\n" +
                    "                                                            style=\"font-family:Arial,Helvetica,sans-serif;box-sizing:border-box;font-size:13px;vertical-align:top;margin:0;padding:0 0 5px\">\n" +
                    "                                                            <img src=\"https://ci3.googleusercontent.com/meips/ADKq_Na6pp7Ow9Bg0urSb6SmDRbFOwNrWiXFWR15G2w3JdCCubQRz1EHwRslSKw92lBp2BdpQkE1-gsA9phvNXiwUIkyLR2NitQ5JK-DJMm7v1uXLIuXfSJ3VA=s0-d-e1-ft#https://api.policybazaar.com/cs/resources/images/right_banner.png\"\n" +
                    "                                                                 class=\"CToWUd\" data-bit=\"iit\"></td>\n" +
                    "                                                    </tr>\n" +
                    "                                                    </tbody>\n" +
                    "                                                </table>\n" +
                    "                                            </td>\n" +
                    "                                        </tr>\n" +
                    "                                       \n" +
                    "                                       \n" +
                    "                                        </tbody>\n" +
                    "                                    </table>\n" +
                    "                                </div>\n" +
                    "                                <div style=\"display:block;width:100%;background:#f1f4f5\">\n" +
                    "                                    <table style=\"background:#f1f4f5\" width=\"100%\">\n" +
                    "                                        <tbody>\n" +
                    "                                        <tr style=\"font-family:Arial,Helvetica,sans-serif;box-sizing:border-box;font-size:13px;margin:0\">\n" +
                    "                                            <td style=\"font-family:Arial,Helvetica,sans-serif;box-sizing:border-box;font-size:14px;vertical-align:top;margin:0;padding:11px 0 12px 19px;line-height:initial;color:rgba(0,0,0,0.5)\"\n" +
                    "                                                valign=\"top\"> Your Companion <span style=\"color:#0065ff;display:block\">Arigen Technology Private Limited </span> Helping\n" +
                    "                                                you make informed IT Solution & Web Security choices\n" +
                    "                                            </td>\n" +
                    "                                        </tr>\n" +
                    "                                        </tbody>\n" +
                    "                                    </table>\n" +
                    "                                </div>\n" +
                    "                                <div style=\"padding:10px;text-align:center;font-size:10px;font-family:Arial,Helvetica,sans-serif\">\n" +
                    "                                    <div><span style=\"display:block;line-height:normal;padding:0 0 3px 0;color:#797979\"> Arigen Technology Private Limited (formerly known as IT Solution & Web Security), </span>\n" +
                    "                                        <span style=\"display:block;line-height:normal;padding:0 0 3px 0;color:#797979;text-decoration:none\">Registered Office no. - 1031, Tower-A, 10th Floor, I-Thum Tower, Sector-62 Noida, UP-201309 </span>\n" +
                    "                                        <span style=\"display:block;line-height:normal;padding:0 0 3px 0;color:#797979\">00A9 Copyright 2008| Arigentechnology.com. All Rights Reserved</span>\n" +
                    "                                    </div>\n" +
                    "                                </div>\n" +
                    "                            </div>\n" +
                    "                        </td>\n" +
                    "                    </tr>\n" +
                    "                    </tbody>\n" +
                    "                </table>\n" +
                    "        </div>\n" +
                    "\n" +
                    "</body>\n" +
                    "</html>";

            helper.setText(otpp, true); // true indicates HTML content
            message.setContent(otpp, "text/html; charset=UTF-8");
            // Send the email
            mailSender.send(message);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }




    }



    public void sendUpdateNotification(Employee existingEmployee) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "utf-8");

            helper.setTo(existingEmployee.getEmail());
            helper.setSubject("Employee Update Notification");

            Map<String, Boolean> changedFields = getChangedFields(existingEmployee, existingEmployee);

            String htmlMsg = "<!DOCTYPE html>"
                    + "<html lang='en'>"
                    + "<head>"
                    + "<meta charset='UTF-8'>"
                    + "<meta name='viewport' content='width=device-width, initial-scale=1.0'>"
                    + "<style>"
                    + "body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }"
                    + ".container { max-width: 600px; margin: 0 auto; padding: 20px; }"
                    + ".header { background-color: #f8f9fa; padding: 20px; text-align: center; }"
                    + ".content { background-color: #ffffff; padding: 20px; }"
                    + ".footer { background-color: #f8f9fa; padding: 20px; text-align: center; font-size: 12px; }"
                    + "table { width: 100%; border-collapse: collapse; }"
                    + "th, td { padding: 10px; border-bottom: 1px solid #ddd; }"
                    + "th { background-color: #f2f2f2; }"
                    + ".highlight { background-color: #fffacd; font-weight: bold; }"
                    + "</style>"
                    + "</head>"
                    + "<body>"
                    + "<div class='container'>"
                    + "<div class='header'>"
                    + "<h1>Employee Update Notification</h1>"
                    + "</div>"
                    + "<div class='content'>"
                    + "<p>Dear " + existingEmployee.getName() + ",</p>"
                    + "<p>Your employee record has been successfully updated.</p>"
                    + "<h2>Updated Details:</h2>"
                    + "<table>"
                    + "<tr><th>Field</th><th>Value</th></tr>"
                    + "<tr" + (changedFields.get("name") ? " class='highlight'" : "") + "><td>Name</td><td>" + existingEmployee.getName() + "</td></tr>"
                    + "<tr" + (changedFields.get("email") ? " class='highlight'" : "") + "><td>Email</td><td>" + existingEmployee.getEmail() + "</td></tr>"
                    + "<tr" + (changedFields.get("mobile") ? " class='highlight'" : "") + "><td>Mobile</td><td>" + existingEmployee.getMobile() + "</td></tr>"
                    + "<tr" + (changedFields.get("branch") ? " class='highlight'" : "") + "><td>Branch</td><td>" + (existingEmployee.getBranch() != null ? existingEmployee.getBranch().getName() : "N/A") + "</td></tr>"
                    + "<tr" + (changedFields.get("department") ? " class='highlight'" : "") + "><td>Department</td><td>" + (existingEmployee.getDepartment() != null ? existingEmployee.getDepartment().getName() : "N/A") + "</td></tr>"
                    + "</table>"
                    + "<p><strong>If you did not request this change, please contact the administrator immediately.</strong></p>"
                    + "</div>"
                    + "<div class='footer'>"
                    + "<p>Best regards,<br>The Company Team</p>"
                    + "</div>"
                    + "</div>"
                    + "</body>"
                    + "</html>";

            helper.setText(htmlMsg, true); // Use this to send an HTML email

            mailSender.send(mimeMessage);
            System.out.println("Update notification sent successfully to " + existingEmployee.getEmail());
        } catch (MessagingException e) {
            System.err.println("Error: Failed to send update notification to " + existingEmployee.getEmail() + ". " + e.getMessage());
        }
    }

    private Map<String, Boolean> getChangedFields(Employee existingEmployee, Employee updatedEmployee) {
        Map<String, Boolean> changedFields = new HashMap<>();
        changedFields.put("name", !existingEmployee.getName().equals(updatedEmployee.getName()));
        changedFields.put("email", !existingEmployee.getEmail().equals(updatedEmployee.getEmail()));
        changedFields.put("mobile", !existingEmployee.getMobile().equals(updatedEmployee.getMobile()));
        changedFields.put("branch", (existingEmployee.getBranch() == null && updatedEmployee.getBranch() != null) ||
                (existingEmployee.getBranch() != null && updatedEmployee.getBranch() == null) ||
                (existingEmployee.getBranch() != null && updatedEmployee.getBranch() != null &&
                        !existingEmployee.getBranch().getName().equals(updatedEmployee.getBranch().getName())));
        changedFields.put("department", (existingEmployee.getDepartment() == null && updatedEmployee.getDepartment() != null) ||
                (existingEmployee.getDepartment() != null && updatedEmployee.getDepartment() == null) ||
                (existingEmployee.getDepartment() != null && updatedEmployee.getDepartment() != null &&
                        !existingEmployee.getDepartment().getName().equals(updatedEmployee.getDepartment().getName())));
        return changedFields;
    }




    public void sendStatusUpdateNotification(Employee existingEmployee, boolean isActive) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "utf-8");

            helper.setTo(existingEmployee.getEmail());
            helper.setSubject("Employee Status Update Notification");

            String status = isActive ? "Activated" : "Deactivated";
            String statusColor = isActive ? "#28a745" : "#dc3545";

            String htmlMsg = "<!DOCTYPE html>"
                    + "<html lang='en'>"
                    + "<head>"
                    + "<meta charset='UTF-8'>"
                    + "<meta name='viewport' content='width=device-width, initial-scale=1.0'>"
                    + "<style>"
                    + "body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }"
                    + ".container { max-width: 600px; margin: 0 auto; padding: 20px; }"
                    + ".header { background-color: #f8f9fa; padding: 20px; text-align: center; }"
                    + ".content { background-color: #ffffff; padding: 20px; }"
                    + ".footer { background-color: #f8f9fa; padding: 20px; text-align: center; font-size: 12px; }"
                    + "table { width: 100%; border-collapse: collapse; }"
                    + "th, td { padding: 10px; border-bottom: 1px solid #ddd; }"
                    + "th { background-color: #f2f2f2; }"
                    + "</style>"
                    + "</head>"
                    + "<body>"
                    + "<div class='container'>"
                    + "<div class='header'>"
                    + "<h1>Employee Status Update</h1>"
                    + "</div>"
                    + "<div class='content'>"
                    + "<p>Dear " + existingEmployee.getName() + ",</p>"
                    + "<p>Your employee status has been successfully updated to: "
                    + "<span style='font-weight: bold; color: " + statusColor + ";'>" + status + "</span></p>"
                    + "<h2>Updated Details:</h2>"
                    + "<table>"
                    + "<tr><th>Name</th><td>" + existingEmployee.getName() + "</td></tr>"
                    + "<tr><th>Email</th><td>" + existingEmployee.getEmail() + "</td></tr>"
                    + "<tr><th>Mobile</th><td>" + existingEmployee.getMobile() + "</td></tr>"
                    + "<tr><th>Branch</th><td>" + (existingEmployee.getBranch() != null ? existingEmployee.getBranch().getName() : "N/A") + "</td></tr>"
                    + "<tr><th>Department</th><td>" + (existingEmployee.getDepartment() != null ? existingEmployee.getDepartment().getName() : "N/A") + "</td></tr>"
                    + "</table>"
                    + "<p><strong>If you did not request this change, please contact the administrator immediately.</strong></p>"
                    + "</div>"
                    + "<div class='footer'>"
                    + "<p>Best regards,<br>The Company Team</p>"
                    + "</div>"
                    + "</div>"
                    + "</body>"
                    + "</html>";

            helper.setText(htmlMsg, true); // Use this to send an HTML email

            mailSender.send(mimeMessage);
            System.out.println("Status update notification sent successfully to " + existingEmployee.getEmail());
        } catch (MessagingException e) {
            System.err.println("Error: Failed to send status update notification to " + existingEmployee.getEmail() + ". " + e.getMessage());
        }
    }



}

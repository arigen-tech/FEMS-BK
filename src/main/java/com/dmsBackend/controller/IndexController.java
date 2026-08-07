package com.dmsBackend.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
public class IndexController {

    private static String applicationName = "<html>\n" +
            "<body>\n" +
            "  \n" +
            "\n" +
            "\t\t\t\t<table width=\"100%\" bgcolor=\"#ffffff\" style=\"font-family:Arial,Helvetica,sans-serif;box-sizing:border-box;font-size:17px;width:100%;background-color:#ffffff;margin:0\">\n" +
            "                    <tbody>\n" +
            "                    <tr style=\"font-family:Arial,Helvetica,sans-serif;box-sizing:border-box;font-size:17px;margin:0\">\n" +
            "                        <td valign=\"top\"\n" +
            "                            style=\"font-family:Arial,Helvetica,sans-serif;box-sizing:border-box;font-size:17px;vertical-align:top;display:block!important;max-width:1000px!important;clear:both!important;margin:0 auto\">\n" +
            "                            <div style=\"font-family:Arial,Helvetica,sans-serif;box-sizing:border-box;font-size:17px;max-width:1000px;display:block;margin:0 auto;background:#fff;border:1px solid #d6d6d6\">\n" +
            "                                <div style=\"padding:20px\">\n" +
            "                                 \n" +
            "                                    <table bgcolor=\"#fff\" cellpadding=\"0\" cellspacing=\"0\"\n" +
            "                                           style=\"font-family:Arial,Helvetica,sans-serif;box-sizing:border-box;font-size:17px;background-color:#fff;margin:0\"\n" +
            "                                           width=\"100%\">\n" +
            "                                        <tbody>\n" +
            "                                        <tr style=\"font-family:Arial,Helvetica,sans-serif;box-sizing:border-box;font-size:17px;margin:0\">\n" +
            "                                            <td style=\"font-family:Arial,Helvetica,sans-serif;box-sizing:border-box;font-size:17px;vertical-align:top;margin:0\"\n" +
            "                                                valign=\"top\">\n" +
            "                                                <table cellpadding=\"0\" cellspacing=\"0\"\n" +
            "                                                       style=\"font-family:Arial,Helvetica,sans-serif;box-sizing:border-box;font-size:17px;margin:0\"\n" +
            "                                                       width=\"100%\">\n" +
            "                                                    <tbody>\n" +
            "                                                    <tr style=\"font-family:Arial,Helvetica,sans-serif;box-sizing:border-box;font-size:17px;margin:0\">\n" +
            "                                                        <td width=\"65%\" valign=\"top\"\n" +
            "                                                            style=\"font-family:Arial,Helvetica,sans-serif;box-sizing:border-box;vertical-align:top;margin:0;padding:0 0 5px\">\n" +
            "                                                            <span style=\"font-size:19px;padding-bottom:5px;font-weight:bold;font-family:Arial,Helvetica,sans-serif;color:rgba(0,0,0,0.8);margin:20px 0 10px 0;display:block\">Document Management System Services is runing now.</span>\n" +
            "                                                            <span style=\"font-size:13px;color:rgba(0,0,0,0.6);margin:0 0 15px 0;display:block\">Now Release Version 3.0 Under Testing</span>\n" +
            "                                                            <div style=\"width:100%;border-radius:8px;background:#253858;color:#ffffff;display:block;text-align:left;font-family:Arial,Helvetica,sans-serif;padding:20px;box-sizing:border-box;font-size:17px\">\n" +

            "                                                                <span class=\"il\">An effective document management system is a centralized repository where documents can be easily searched, accessed and updated by authorized business users.</span>\n" +
            "\n" +
            "\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t<span class=\"il\">A document management system (DMS) is usually a computerized system used to store, share, track and manage files or documents. Some systems include\n" +
            "\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t history tracking where a log of the various versions created and modified by different users is recorded.</span>\n" +
            "                                                            </div>\n" +
            "                                                            <div style=\"width:100%;color:#5e6c84;text-align:left;font-family:Arial,Helvetica,sans-serif;box-sizing:border-box;font-size:18px;margin:20px 0 0 0\">\n" +
            "                                                                 If you don’t feel this way, kindly contact us\n" +
            "                                                                at 0000-000-0000 or easily reset your password from the\n" +
            "                                                                Settings option in <a\n" +
            "                                                                    style=\"color:#0065ff;text-decoration:none\"\n" +
            "                                                                Account’</a>.\n" +
            "                                                            </div>\n" +
            "                                                        </td>\n" +
            "                                                        <td width=\"35%\" align=\"right\" valign=\"top\"\n" +
            "                                                            style=\"font-family:Arial,Helvetica,sans-serif;box-sizing:border-box;font-size:16px;vertical-align:top;margin:0;padding:0 0 5px\">\n" +
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
            "                                        <tr style=\"font-family:Arial,Helvetica,sans-serif;box-sizing:border-box;font-size:16px;margin:0\">\n" +
            "                                            <td style=\"font-family:Arial,Helvetica,sans-serif;box-sizing:border-box;font-size:17px;vertical-align:top;margin:0;padding:11px 0 12px 19px;line-height:initial;color:rgba(0,0,0,0.5)\"\n" +
            "                                                valign=\"top\"> Your Companion <span style=\"color:#0065ff;display:block\">Arigen Technology Private Limited </span> Helping\n" +
            "                                                you make informed IT Solution & Web Security choices\n" +
            "                                            </td>\n" +
            "                                        </tr>\n" +
            "                                        </tbody>\n" +
            "                                    </table>\n" +
            "                                </div>\n" +
            "                                <div style=\"padding:10px;text-align:center;font-size:16px;font-family:Arial,Helvetica,sans-serif\">\n" +
            "                                    <div><span style=\"display:block;line-height:normal;padding:0 0 3px 0;color:#797979\"> Arigen Technology Private Limited , </span>\n" +
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

    @GetMapping("/")
    public String login() {
        return applicationName;
    }



}

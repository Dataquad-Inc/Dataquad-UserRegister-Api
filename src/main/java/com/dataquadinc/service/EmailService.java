package com.dataquadinc.service;

import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    @Autowired
    private JavaMailSender mailSender;

    public void sendPasswordEmailHtml(String to, String userName, String password) {
        try {
            logger.info("Preparing to send password email to {}", to);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            String subject = "Welcome to MyMulya! Here Are Your Login Details";
            String htmlBody = buildHtmlPasswordEmailBody(userName, password, to);

            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);  // true for HTML
            mailSender.send(message);

            logger.info("Password email sent successfully to {}", to);
        } catch (Exception e) {
            logger.error("Failed to send password email to {}", to, e);
            throw new RuntimeException("Failed to send email to " + to, e);
        }
    }


    private String buildHtmlPasswordEmailBody(String userName, String password, String email) {
        String loginUrl = "https://mymulya.com";
        return "<!DOCTYPE html><html><head><style>"
                + "body {font-family: 'Helvetica Neue', Arial, sans-serif; background-color: #f6f8fa; margin: 0; padding: 0;}"
                + ".email-container {max-width: 480px; background-color: #fff; margin: 40px auto; border-radius: 8px; "
                + "box-shadow: 0 2px 18px rgba(32, 82, 138, 0.13); border: 1px solid #eeeeee; padding: 20px;}"
                + "h2 {font-size: 1.3rem; color: #0056b3; text-align: center; margin-bottom: 20px; letter-spacing: 1px;}"
                + "p {color: #1b243a; font-size: 1rem; line-height: 1.5; margin: 14px 0;}"
                + "table {margin: 20px auto; font-size: 1rem; color: #2a3357; border-collapse: collapse;}"
                + "td {padding: 6px 12px;}"
                + ".credential {font-size: 1rem; color: #2a3357; margin: 12px 0; text-align: center; line-height: 1.4;}"
                + ".highlight {color: #38cf95; font-weight: bold;}"
                + ".action-btn {background: #ffffff; color: #007bff; font-size: 1.08rem; padding: 12px 38px; border-radius: 7px;"
                + " border: 2px solid #007bff; text-decoration: none; font-weight: 600; display: block; margin: 24px auto 12px auto; text-align: center;"
                + " transition: all .2s ease-in-out; cursor: pointer;}"
                + ".action-btn:hover {background: #007bff; color: #ffffff;}"
                + ".reminder {color: #00897b; font-size: 1.04rem; font-weight: 500; text-align: center; margin: 10px 0 12px 0;}"
                + ".footer {font-size: 0.9rem; color: #5e6b8b; text-align: center; margin-top: 30px; padding-top: 10px; border-top: 1px solid #eee;}"
                + "</style></head><body>"
                + "<div class='email-container'>"
                + "<h2>Welcome to MyMulya!</h2>"
                + "<p>Hi " + userName + ",</p>"
                + "<p>Congratulations! Your MyMulya account has been created successfully</p>"
                + "<p class='credential'>You can now log in to your account using the following credentials:</p>"

                // ✅ Table for Email & Password
                + "<table>"
                + "<tr><td><strong>Email:</strong></td><td>" + email + "</td></tr>"
                + "<tr><td><strong>Password:</strong></td><td><span class='highlight'>" + password + "</span></td></tr>"
                + "</table>"

                + "<a href='" + loginUrl + "' class='action-btn' target='_blank'>Login to MyMulya</a>"
                + "<div class='reminder'>For your security, please change your password immediately after logging in.</div>"
                + "<p>If you have any questions or require assistance, please contact our support team.</p>"
                + "<p>Regards,<br/>Mulya Team</p>"
                + "<div class='footer'>This is an automated message, please do not reply.</div>"
                + "</div></body></html>";
    }
}

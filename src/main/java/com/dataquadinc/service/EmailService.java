package com.dataquadinc.service;

import com.dataquadinc.model.UserDetails;
import com.dataquadinc.repository.UserDao;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private UserDao userDao;


    private static final String FROM_EMAIL = "notifications@adroitinnovative.com";
    private static final String ADMIN_EMAIL = "sasaank9110@gmail.com";



    public void sendPasswordEmailHtml(String to, String userName, String password) {
        try {
            logger.info("Preparing to send password email to {}", to);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            String subject = "Welcome to MyMulya! Here Are Your Login Details";
            String htmlBody = buildHtmlPasswordEmailBody(userName, password, to);
            helper.setFrom("notifications@adroitinnovative.com");

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

    public void sendProfileUpdateEmailToUser(UserDetails user, Map<String, String> updatedFields) {
        try {
            logger.info("Preparing to send profile update email to user: {}", user.getEmail());

            String subject = "Your Profile Has Been Updated Successfully!";
            String htmlBody = buildUserProfileUpdateEmailBody(user, updatedFields);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(FROM_EMAIL);
            helper.setTo(user.getEmail());
            helper.setSubject(subject);
            helper.setText(htmlBody, true);

            mailSender.send(message);
            logger.info("Profile update email sent successfully to user {}", user.getEmail());
        } catch (Exception e) {
            logger.error("Failed to send profile update email to user {}: {}", user.getEmail(), e.getMessage(), e);
            throw new RuntimeException("Failed to send email to user " + user.getEmail(), e);
        }
    }

    /**
     * Send profile update email to admin.
     */
    public void sendProfileUpdateEmailToAdmin(UserDetails user, Map<String, String> updatedFields) {
        try {
            logger.info("Preparing to send profile update notification to admin for user: {}", user.getEmail());

            String subject = "Employee Profile Updated - " + user.getUserName();
            String htmlBody = buildAdminProfileUpdateEmailBody(user, updatedFields);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(FROM_EMAIL);
            helper.setTo(ADMIN_EMAIL);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);

            mailSender.send(message);
            logger.info("Profile update notification sent successfully to admin for user {}", user.getEmail());
        } catch (Exception e) {
            logger.error("Failed to send admin profile update email for user {}: {}", user.getEmail(), e.getMessage(), e);
            throw new RuntimeException("Failed to send admin notification for user " + user.getEmail(), e);
        }
    }

    /**
     * Build HTML email for user profile update confirmation.
     */
    private String buildUserProfileUpdateEmailBody(UserDetails user, Map<String, String> updatedFields) {
        String changesList = updatedFields.entrySet().stream()
                .map(entry -> "<li><strong>" + entry.getKey() + ":</strong> " + entry.getValue() + "</li>")
                .collect(Collectors.joining());

        return String.format("""
        <!DOCTYPE html>
        <html>
        <head>
        <style>
        body {font-family: Arial, sans-serif; background-color: #f4f6f9; margin: 0; padding: 0;}
        .container {max-width: 600px; margin: 30px auto; background: #ffffff; padding: 25px; border-radius: 8px;
        box-shadow: 0 3px 10px rgba(0,0,0,0.1);}
        h2 {color: #007bff; text-align: center;}
        p {font-size: 15px; color: #333;}
        ul {font-size: 15px; color: #444;}
        .footer {text-align: center; margin-top: 20px; font-size: 13px; color: #888;}
        </style>
        </head>
        <body>
        <div class='container'>
            <h2>Profile Update Confirmation</h2>
            <p>Hi <strong>%s</strong>,</p>
            <p>Your profile has been successfully updated with the following changes:</p>
            <ul>%s</ul>
            <p>If you did not make these changes, please contact HR immediately.</p>
            <div class='footer'>© MyMulya | This is an automated message, please do not reply.</div>
        </div>
        </body>
        </html>
        """,
                user.getUserName(),
                changesList
        );
    }


    /**
     * Build HTML email for admin when employee updates profile.
     */
    private String buildAdminProfileUpdateEmailBody(UserDetails user, Map<String, String> updatedFields) {
        String changesList = updatedFields.entrySet().stream()
                .map(entry -> "<li><strong>" + entry.getKey() + ":</strong> " + entry.getValue() + "</li>")
                .collect(Collectors.joining());

        return """
            <!DOCTYPE html>
            <html>
            <head>
            <style>
            body {font-family: Arial, sans-serif; background-color: #f4f6f9; margin: 0; padding: 0;}
            .container {max-width: 600px; margin: 30px auto; background: #ffffff; padding: 25px; border-radius: 8px;
            box-shadow: 0 3px 10px rgba(0,0,0,0.1);}
            h2 {color: #d9534f; text-align: center;}
            p {font-size: 15px; color: #333;}
            ul {font-size: 15px; color: #444;}
            .footer {text-align: center; margin-top: 20px; font-size: 13px; color: #888;}
            </style>
            </head>
            <body>
            <div class='container'>
                <h2>Employee Profile Updated</h2>
                <p>The following employee has updated their profile:</p>
            """ +
                "<p><strong>Name:</strong> " + user.getUserName() + "<br/>" +
                "<strong>Email:</strong> " + user.getEmail() + "</p>" +
                "<p><strong>Updated Fields:</strong></p>" +
                "<ul>" + changesList + "</ul>" +
                """
                    <div class='footer'>© MyMulya Admin | This is an automated message.</div>
                </div>
                </body>
                </html>
                """;
    }

}

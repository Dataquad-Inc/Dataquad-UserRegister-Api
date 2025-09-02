package com.dataquadinc.service;

import com.dataquadinc.dto.ForgotResponseDto;
import com.dataquadinc.model.UserDetails;
import com.dataquadinc.repository.UserDao;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;

import javax.naming.Context;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;
import java.util.*;
import java.util.regex.Pattern;

@Service
public class ForgotPasswordService {

    @Autowired
    private UserDao userDao;

    @Autowired
    private JavaMailSender mailSender;
    private static final Logger logger = LoggerFactory.getLogger(ForgotPasswordService.class);



    private final Map<String, String> otpStorage = new HashMap<>();

    public ForgotResponseDto generateOtp(String email) {
        if (!isValidEmailFormat(email)) {
            return new ForgotResponseDto(false, "Invalid email format: " + email, "Email format is invalid.");
        }

        String domain = email.substring(email.indexOf("@") + 1);

        if (!hasValidMXRecord(domain)) {
            // Additional check for well-known email services (e.g., Gmail, Yahoo, etc.)
            if (domain.equalsIgnoreCase("gmail.com") || domain.equalsIgnoreCase("yahoo.com")) {
                return new ForgotResponseDto(false, "Invalid email domain: " + email, "Email domain is invalid.");
            }
            return new ForgotResponseDto(false, "Invalid email domain: " + email, "Domain doesn't have valid MX records.");
        }

//        if (!isEmailValidOnServer(email)) {
//            return new ForgotResponseDto(false, "The email address does not exist on the server: " + email, "Email not found.");
//        }

        // Step 2: Check if email exists on mail server
        if (!isEmailDeliverable(email)) {
            return new ForgotResponseDto(false, "Email address does not exist: " + email, "Invalid email address.");
        }

        UserDetails user = userDao.findByEmail(email);
        if (user == null) {
            return new ForgotResponseDto(false, "User not found: " + email, "User does not exist.");
        }

        String otp = String.format("%06d", new Random().nextInt(999999));
        otpStorage.put(email, otp);

        try {
            sendOtpToEmail(email, otp);
        } catch (MailSendException e) {
            return new ForgotResponseDto(false, "Failed to send OTP: " + email, "Email sending failed.");
        } catch (Exception e) {
            return new ForgotResponseDto(false, "Unexpected error while sending OTP: " + e.getMessage(), "Error sending email.");
        }

        return new ForgotResponseDto(true, "OTP sent successfully.", null);
    }


    private boolean isValidEmailFormat(String email) {
        String emailRegex = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$";
        return email != null && Pattern.matches(emailRegex, email);
    }

    private boolean isEmailDeliverable(String email) {
        String domain = email.substring(email.indexOf("@") + 1);

        try {
            // First check MX records
            Hashtable<String, String> env = new Hashtable<>();
            env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.dns.DnsContextFactory");
            DirContext ctx = new InitialDirContext(env);
            Attributes attrs = ctx.getAttributes(domain, new String[]{"MX"});

            if (attrs.get("MX") == null) {
                return false;
            }

            // Then try to send a test email
            SimpleMailMessage testMessage = new SimpleMailMessage();
            testMessage.setTo(email);
            testMessage.setSubject("Email Verification");
            testMessage.setText("This is a test email to verify your email address.");

            mailSender.send(testMessage);
            return true;

        } catch (MailSendException e) {
            // Check for specific error codes that indicate invalid email
            String errorMsg = e.getMessage().toLowerCase();
            return !(errorMsg.contains("550") ||
                    errorMsg.contains("553") ||
                    errorMsg.contains("501") ||
                    errorMsg.contains("user unknown") ||
                    errorMsg.contains("user not found") ||
                    errorMsg.contains("does not exist") ||
                    errorMsg.contains("recipient rejected"));
        } catch (Exception e) {
            // Log the error for debugging
            System.out.println("Error checking email " + email + ": " + e.getMessage());
            return false;
        }
    }
    private boolean hasValidMXRecord(String domain) {
        try {
            // Check MX records without the 'mail.' prefix
            Hashtable<String, String> env = new Hashtable<>();
            env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.dns.DnsContextFactory");
            DirContext ctx = new InitialDirContext(env);
            Attributes attrs = ctx.getAttributes(domain, new String[]{"MX"});

            return attrs.get("MX") != null;
        } catch (Exception e) {
            System.out.println("MX Record lookup failed for domain: " + domain);
            return false;  // MX record not found, meaning no mail servers for this domain
        }
    }



    public boolean isEmailValidOnServer(String email) {
        String domain = email.substring(email.indexOf("@") + 1);

        try {
            // Step 1: Get MX Records for the email domain
            Hashtable<String, String> env = new Hashtable<>();
            env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.dns.DnsContextFactory");
            DirContext ctx = new InitialDirContext(env);
            Attributes attrs = ctx.getAttributes(domain, new String[]{"MX"});

            if (attrs.get("MX") == null) {
                System.out.println("No MX records found for domain: " + domain);
                return false;
            }

            // Get all MX records
            String[] mxRecords = attrs.get("MX").get().toString().split("\n");
            String mxRecord = null;

            // Find the appropriate MX record
            for (String record : mxRecords) {
                String serverPart = record.split(" ")[1];
                if (serverPart != null && !serverPart.trim().isEmpty()) {
                    mxRecord = serverPart.trim();
                    break;
                }
            }

            if (mxRecord == null) {
                System.out.println("No valid MX record found for domain: " + domain);
                return false;
            }

            // Step 2: Try SMTP Connection with appropriate timeout
            Socket socket = new Socket();
            socket.setSoTimeout(10000); // 10 seconds timeout
            socket.connect(new InetSocketAddress(mxRecord, 25), 10000);

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                 BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()))) {

                String serverResponse = reader.readLine();
                System.out.println("SMTP Server Response: " + serverResponse);
                if (!serverResponse.startsWith("220")) return false;

                // Use the actual domain in HELO command
                writer.write("HELO " + domain + "\r\n");
                writer.flush();
                System.out.println("HELO Response: " + reader.readLine());

                writer.write("MAIL FROM:<verify@" + domain + ">\r\n");
                writer.flush();
                System.out.println("MAIL FROM Response: " + reader.readLine());

                writer.write("RCPT TO:<" + email + ">\r\n");
                writer.flush();
                String rcptResponse = reader.readLine();
                System.out.println("RCPT TO Response: " + rcptResponse);

                writer.write("QUIT\r\n");
                writer.flush();

                socket.close();
                return rcptResponse != null && rcptResponse.startsWith("250");
            }
        } catch (Exception e) {
            System.out.println("Error validating email for domain " + domain + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Check if a domain has a valid MX record (Mail Exchange record).
     */



    // ✅ Method to check if the email server accepts the email using SMTP
    private boolean smtpEmailCheck(String email, String domain) {
        String smtpServer = "smtp." + domain;

        // Check for common email providers like Gmail, Outlook, etc.
        if (domain.equalsIgnoreCase("gmail.com")) {
            smtpServer = "smtp.gmail.com";  // Gmail's SMTP server
        } else if (domain.equalsIgnoreCase("outlook.com") || domain.equalsIgnoreCase("hotmail.com")) {
            smtpServer = "smtp-mail.outlook.com";  // Outlook's SMTP server
        } else if (domain.equalsIgnoreCase("yahoo.com")) {
            smtpServer = "smtp.mail.yahoo.com";  // Yahoo's SMTP server
        }else if (domain.equalsIgnoreCase("dataqinc.com")) {
            smtpServer = "smtp.mail.dataqinc.com";  // Yahoo's SMTP server
        }

        try {
            // Try to connect to the SMTP server with the correct domain
            Socket socket = new Socket();
            socket.connect(new InetSocketAddress(smtpServer, 25), 5000); // Try SMTP on port 25 first
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);

            // Read server greeting
            String response = reader.readLine();
            if (!response.startsWith("220")) {
                socket.close();
                return false;
            }

            // Introduce ourselves
            writer.println("HELO mydomain.com");
            reader.readLine();

            // Mail from command
            writer.println("MAIL FROM:<test@mydomain.com>");
            reader.readLine();

            // Check if the email address can receive mail (RCPT TO command)
            writer.println("RCPT TO:<" + email + ">");
            response = reader.readLine();

            // Send quit command and close connection
            writer.println("QUIT");
            socket.close();

            // If the server responds with 250, the recipient email is valid
            return response.startsWith("250");
        } catch (IOException e) {
            System.out.println("SMTP check failed for email: " + email + " with error: " + e.getMessage());
            return false;  // SMTP validation failed
        }
    }

//    // Function to check email via an external API
//    private boolean checkEmailWithAPI(String email) {
//        try {
//            String apiKey = "d6f4da1e713d43d28cda2165a27db915";
//            String url = "https://apilayer.net/api/check?access_key=" + apiKey + "&email=" + email;
//
//            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
//            conn.setRequestMethod("GET");
//
//            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
//            String response = in.readLine();
//            in.close();
//
//            System.out.println("API Response: " + response);
//
//            return response.contains("\"format_valid\":true") && response.contains("\"smtp_check\":true");
//        } catch (Exception e) {
//            System.out.println("API check failed for email: " + email + " with error: " + e.getMessage());
//            return false;
//        }
//    }


    public void sendOtpToEmail(String email, String otp) throws MailSendException {
        try {
            // Create a MimeMessage for HTML content
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            // Set recipient, subject, and sender
            helper.setTo(email);
            helper.setSubject("Your OTP for password reset");
            helper.setFrom("notifications@adroitinnovative.com"); // 👈 force sender

            // Build the HTML content
            String emailContent = "<html>" +
                    "<body>" +
                    "<p>Your OTP code for password reset is: <strong>" + otp + "</strong>.</p>" +
                    "<p> It is valid for 5 minutes.</p>"+
                    "<p><strong>Note: This is an auto-generated email. Please do not reply to this email.</strong></p>" +
                    "</body>" +
                    "</html>";

            // Set email content as HTML
            helper.setText(emailContent, true);

            // Send the email
            mailSender.send(message);
            logger.info("OTP email sent successfully to {}", email);
        } catch (MessagingException e) {
            logger.error("Failed to create or send OTP email to {}: {}", email, e.getMessage());
            throw new MailSendException("Failed to send OTP: " + email, e);
        } catch (MailSendException e) {
            logger.error("Failed to send OTP email to {}: {}", email, e.getMessage());
            throw e;  // Re-throw the exception
        }
    }


    public ForgotResponseDto verifyOtp(String email, String otp) {
        // Check if the email is valid and the OTP exists
        String storedOtp = otpStorage.get(email);

        if (storedOtp == null) {
            // OTP not found or expired, so the email user doesn't exist or OTP is not generated
            return new ForgotResponseDto(false, "User does not exist or OTP not generated for the email: " + email, "User not found or OTP expired.");
        }

        // Compare the stored OTP with the entered OTP
        if (storedOtp.equals(otp)) {
            // OTP matches, verification successful
            otpStorage.remove(email); // Remove the OTP after successful verification
            return new ForgotResponseDto(true, "OTP verified successfully.", null);
        } else {
            // OTP does not match, return an error
            return new ForgotResponseDto(false, "Invalid OTP for email: " + email, "Incorrect OTP.");
        }
    }

    public ForgotResponseDto updatePassword(String email, String updatePassword) {
        UserDetails user = userDao.findByEmail(email);
        if (user == null) {
            return new ForgotResponseDto(false, "User not found: " + email, "User does not exist.");
        }

        if (BCrypt.checkpw(updatePassword, user.getPassword())) {
            return new ForgotResponseDto(false, "New password cannot be the same as the old one.", "Password reuse not allowed.");
        }

        String hashedPassword = BCrypt.hashpw(updatePassword, BCrypt.gensalt());
        user.setPassword(hashedPassword);
        userDao.save(user);
        otpStorage.remove(email);
        sendPasswordUpdateConfirmationEmail(email);

        return new ForgotResponseDto(true, "Password updated successfully.", null);
    }

    private void sendPasswordUpdateConfirmationEmail(String email) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("Password Updated Successfully");
        message.setText("Your password has been updated. If this wasn't you, contact support.");

        try {
            mailSender.send(message);
        } catch (Exception e) {
            throw new RuntimeException("Error sending confirmation email: " + e.getMessage());
        }
    }
}

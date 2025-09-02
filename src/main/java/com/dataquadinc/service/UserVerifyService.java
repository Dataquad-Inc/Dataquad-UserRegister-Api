package com.dataquadinc.service;

import com.dataquadinc.dto.ForgotResponseDto;
import com.dataquadinc.dto.UserVerifyDto;
import com.dataquadinc.model.UserDetails;
import com.dataquadinc.repository.UserDao;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Service
public class UserVerifyService {

    private static final Logger logger = LoggerFactory.getLogger(UserVerifyService.class);
    private final JavaMailSender javaMailSender;
    private final UserDao userDao; // Inject UserDao instead of UserDetailsRepository
    private final Map<String, String> otpStorage = new ConcurrentHashMap<>();
    private final Map<String, Long> otpTimestamps = new ConcurrentHashMap<>();
    private final Random random = new Random();

    private static final long OTP_EXPIRY_TIME_MS = 5 * 60 * 1000; // 5 minutes
    private static final long OTP_COOLDOWN_MS = 60 * 1000; // 1 minute

    public UserVerifyService(JavaMailSender javaMailSender, UserDao userDao) {
        this.javaMailSender = javaMailSender;
        this.userDao = userDao; // Initialize UserDao
        startOtpCleanupTask();
    }

    public ForgotResponseDto sendOtp(String email) {
        if (email == null || email.isEmpty()) {
            return new ForgotResponseDto(false, "Invalid email address " + email);
        }

        // Trim the email to remove any whitespace
        email = email.trim();

        // Check if email already exists in the database using UserDao
        Optional<UserDetails> user = Optional.ofNullable(userDao.findByEmail(email));
        if (userDao.findByEmail(email)
                != null) {
            return new ForgotResponseDto(false, "This email is already registered: " + email);
        }

        // Check cooldown period
        long currentTime = System.currentTimeMillis();
        if (otpTimestamps.containsKey(email) && (currentTime - otpTimestamps.get(email)) < OTP_COOLDOWN_MS) {
            return new ForgotResponseDto(false, "Please wait before requesting a new OTP.");
        }

        // Generate a 6-digit OTP
        String otp = String.format("%06d", random.nextInt(999999));

        // Store OTP with timestamp
        otpStorage.put(email, otp.trim()); // Ensure OTP is trimmed
        otpTimestamps.put(email, currentTime);
        logger.info("Stored OTP for {}: {}", email, otp);

        // Send OTP via email
        sendOtpEmail(email, otp);
        return new ForgotResponseDto(true, "OTP sent successfully.", null);
    }

    public ForgotResponseDto verifyOtp(UserVerifyDto userDTO) {
        String email = userDTO.getEmail().trim();
        String enteredOtp = userDTO.getOtp().trim();
        if (userDTO == null || userDTO.getEmail() == null || userDTO.getOtp() == null) {
            return new ForgotResponseDto(false, "Invalid email format: " + email);
        }

        // Retrieve stored OTP
        String storedOtp = otpStorage.get(email);
        Long timestamp = otpTimestamps.get(email);
        logger.info("Verifying OTP for {}. Entered: {}, Stored: {}", email, enteredOtp, storedOtp);
        logger.info("Current OTP storage before verification: {}", otpStorage);

        if (storedOtp == null) {
            logger.warn("No OTP found for email: {}", email);
            return new ForgotResponseDto(false, "Invalid or expired OTP for " + email);
        }

        long currentTime = System.currentTimeMillis();
        if ((currentTime - timestamp) > OTP_EXPIRY_TIME_MS) {
            otpStorage.remove(email); // Remove expired OTP
            otpTimestamps.remove(email); // Remove expired timestamp
            logger.warn("OTP expired for email: {}", email);
            return new ForgotResponseDto(false, "OTP has expired for " + email);
        }

        if (storedOtp.equals(enteredOtp)) {
            otpStorage.remove(email); // Remove OTP after successful verification
            otpTimestamps.remove(email); // Remove timestamp to allow new requests
            logger.info("OTP verification successful for {}", email);
            return new ForgotResponseDto(true, "Email verified successfully! " + email);
        }

        logger.warn("OTP verification failed for {}. Entered: {}, Expected: {}", email, enteredOtp, storedOtp);
        return new ForgotResponseDto(false, "Invalid or expired OTP for " + email);
    }

    private void sendOtpEmail(String email, String otp) {
        try {
            // Create a MimeMessage for HTML content
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            // Set the recipient, subject, and sender
            helper.setTo(email);
            helper.setSubject("Your OTP Code For Registration");
            helper.setFrom("notifications@adroitinnovative.com"); // 👈 force sender

            // Build the HTML content
            String emailContent = "<html>" +
                    "<body>" +
                    "<p>Your OTP for registration is: <strong>" + otp + "</strong>.</p>" +
                    "<p> It is valid for 5 minutes.</p>"+
                    "<p><strong>Note: This is an auto-generated email. Please do not reply to this email.</strong></p>" +
                    "</body>" +
                    "</html>";

            // Set the email content as HTML
            helper.setText(emailContent, true);

            // Send the email
            javaMailSender.send(message);
            logger.info("OTP email sent successfully to {}", email);
        } catch (Exception e) {
            logger.error("Failed to send OTP email to {}: {}", email, e.getMessage());
        }
    }


    private void startOtpCleanupTask() {
        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> {
            long currentTime = System.currentTimeMillis();
            logger.debug("Running OTP cleanup task at time: {}", currentTime);

            // Only remove OTPs that have expired (older than 5 minutes)
            otpStorage.entrySet().removeIf(entry -> {
                Long timestamp = otpTimestamps.get(entry.getKey());
                boolean expired = (currentTime - timestamp) > OTP_EXPIRY_TIME_MS;
                if (expired) {
                    logger.info("Removing expired OTP for email: {}", entry.getKey());
                }
                return expired;
            });

            otpTimestamps.entrySet().removeIf(entry -> {
                long timestamp = entry.getValue();
                boolean expired = (currentTime - timestamp) > OTP_EXPIRY_TIME_MS;
                if (expired) {
                    logger.info("Removing expired timestamp for email: {}", entry.getKey());
                }
                return expired;
            });

            logger.debug("Current OTP storage after cleanup: {}", otpStorage);
        }, 1, 5, TimeUnit.MINUTES); // Adjusted to 5 minutes interval

    }
}

package opgg.ghrami.controller;

import opgg.ghrami.model.User;
import opgg.ghrami.util.EmailService;
import opgg.ghrami.util.PasswordUtil;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Controller for handling password reset functionality
 * Uses in-memory storage for reset codes (no database changes required)
 */
public class PasswordResetController {
    
    private static PasswordResetController instance;
    
    // In-memory storage: email -> ResetToken
    private final Map<String, ResetToken> resetTokens = new ConcurrentHashMap<>();
    
    // Configuration
    private static final int CODE_LENGTH = 6;
    private static final int EXPIRY_MINUTES = 15;
    private static final int MAX_ATTEMPTS_PER_HOUR = 3;
    
    private final UserController userController;
    private final EmailService emailService;
    
    private PasswordResetController() {
        this.userController = new UserController();
        this.emailService = EmailService.getInstance();
        
        // Start cleanup thread to remove expired tokens
        startCleanupThread();
    }
    
    public static PasswordResetController getInstance() {
        if (instance == null) {
            instance = new PasswordResetController();
        }
        return instance;
    }
    
    /**
     * Initiate password reset process
     * Generates code, stores it, and sends email
     * @param email User's email address
     * @return true if email sent successfully
     */
    public boolean initiatePasswordReset(String email) {
        try {
            // Check if user exists
            var userOpt = userController.findByEmail(email);
            if (!userOpt.isPresent()) {
                System.err.println("Password reset requested for non-existent email: " + email);
                return false; // Return false to show error to user
            }
            
            User user = userOpt.get();
            
            // Check rate limiting
            if (!checkRateLimit(email)) {
                System.err.println("Rate limit exceeded for email: " + email);
                return false;
            }
            
            // Generate 6-digit code
            String code = generateResetCode();
            
            // Store token in memory
            ResetToken token = new ResetToken(
                code,
                LocalDateTime.now().plusMinutes(EXPIRY_MINUTES),
                user.getUserId()
            );
            resetTokens.put(email.toLowerCase(), token);
            
            // Send email
            boolean emailSent = emailService.sendPasswordResetEmail(email, user.getUsername(), code);
            
            if (emailSent) {
                System.out.println("Password reset initiated for: " + email);
                return true;
            } else {
                // Remove token if email failed
                resetTokens.remove(email.toLowerCase());
                return false;
            }
            
        } catch (Exception e) {
            System.err.println("Error initiating password reset: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Verify reset code and reset password
     * @param email User's email
     * @param code Reset code from email
     * @param newPassword New password
     * @return true if password reset successful
     */
    public boolean resetPassword(String email, String code, String newPassword) {
        try {
            String emailKey = email.toLowerCase();
            
            // Check if token exists
            ResetToken token = resetTokens.get(emailKey);
            if (token == null) {
                System.err.println("No reset token found for email: " + email);
                return false;
            }
            
            // Check if expired
            if (LocalDateTime.now().isAfter(token.getExpiryTime())) {
                System.err.println("Reset token expired for email: " + email);
                resetTokens.remove(emailKey);
                return false;
            }
            
            // Verify code
            if (!token.getCode().equals(code)) {
                System.err.println("Invalid reset code for email: " + email);
                token.incrementAttempts();
                
                // Remove token after 3 failed attempts
                if (token.getAttempts() >= 3) {
                    resetTokens.remove(emailKey);
                }
                return false;
            }
            
            // Check if already used
            if (token.isUsed()) {
                System.err.println("Reset token already used for email: " + email);
                return false;
            }
            
            // Validate new password
            if (newPassword == null || newPassword.length() < 8) {
                System.err.println("New password too short");
                return false;
            }
            
            // Get user and update password
            var userOpt = userController.findByEmail(email);
            if (!userOpt.isPresent()) {
                return false;
            }
            
            User user = userOpt.get();
            String hashedPassword = PasswordUtil.hashPassword(newPassword);
            user.setPassword(hashedPassword);
            
            User updatedUser = userController.update(user);
            
            if (updatedUser != null) {
                // Mark token as used and remove it
                token.markAsUsed();
                resetTokens.remove(emailKey);
                System.out.println("Password reset successful for: " + email);
                return true;
            }
            
            return false;
            
        } catch (Exception e) {
            System.err.println("Error resetting password: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Verify if a reset code is valid (without resetting password)
     * @param email User's email
     * @param code Reset code
     * @return true if code is valid and not expired
     */
    public boolean verifyResetCode(String email, String code) {
        String emailKey = email.toLowerCase();
        ResetToken token = resetTokens.get(emailKey);
        
        if (token == null) {
            return false;
        }
        
        if (LocalDateTime.now().isAfter(token.getExpiryTime())) {
            resetTokens.remove(emailKey);
            return false;
        }
        
        if (token.isUsed()) {
            return false;
        }
        
        return token.getCode().equals(code);
    }
    
    /**
     * Cancel a password reset request
     * @param email User's email
     */
    public void cancelPasswordReset(String email) {
        resetTokens.remove(email.toLowerCase());
    }
    
    /**
     * Generate random 6-digit code
     */
    private String generateResetCode() {
        Random random = new Random();
        int code = 100000 + random.nextInt(900000); // Ensures 6 digits
        return String.valueOf(code);
    }
    
    /**
     * Check rate limiting (max 3 attempts per hour)
     */
    private boolean checkRateLimit(String email) {
        // Simple rate limiting: check if there's already a recent token
        ResetToken existingToken = resetTokens.get(email.toLowerCase());
        if (existingToken != null) {
            LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
            if (existingToken.getCreatedAt().isAfter(oneHourAgo)) {
                // Token created within last hour
                return existingToken.getRequestCount() < MAX_ATTEMPTS_PER_HOUR;
            }
        }
        return true;
    }
    
    /**
     * Start background thread to clean up expired tokens
     */
    private void startCleanupThread() {
        Thread cleanupThread = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(60000); // Run every minute
                    cleanupExpiredTokens();
                } catch (InterruptedException e) {
                    break;
                }
            }
        });
        cleanupThread.setDaemon(true);
        cleanupThread.start();
    }
    
    /**
     * Remove expired tokens from memory
     */
    private void cleanupExpiredTokens() {
        LocalDateTime now = LocalDateTime.now();
        resetTokens.entrySet().removeIf(entry -> 
            now.isAfter(entry.getValue().getExpiryTime())
        );
    }
    
    /**
     * Get number of active reset tokens (for testing/monitoring)
     */
    public int getActiveTokenCount() {
        return resetTokens.size();
    }
    
    /**
     * Inner class to represent a reset token
     */
    private static class ResetToken {
        private final String code;
        private final LocalDateTime expiryTime;
        private final LocalDateTime createdAt;
        private final Long userId;
        private boolean used;
        private int attempts;
        private int requestCount;
        
        public ResetToken(String code, LocalDateTime expiryTime, Long userId) {
            this.code = code;
            this.expiryTime = expiryTime;
            this.userId = userId;
            this.createdAt = LocalDateTime.now();
            this.used = false;
            this.attempts = 0;
            this.requestCount = 1;
        }
        
        public String getCode() {
            return code;
        }
        
        public LocalDateTime getExpiryTime() {
            return expiryTime;
        }
        
        public LocalDateTime getCreatedAt() {
            return createdAt;
        }
        
        public Long getUserId() {
            return userId;
        }
        
        public boolean isUsed() {
            return used;
        }
        
        public void markAsUsed() {
            this.used = true;
        }
        
        public int getAttempts() {
            return attempts;
        }
        
        public void incrementAttempts() {
            this.attempts++;
        }
        
        public int getRequestCount() {
            return requestCount;
        }
        
        public void incrementRequestCount() {
            this.requestCount++;
        }
    }
}

package opgg.ghrami.util;

import javax.mail.*;
import javax.mail.internet.*;
import java.util.Properties;

/**
 * Email service for sending password reset codes via Brevo (formerly Sendinblue)
 * Configured for production email delivery
 */
public class EmailService {
    
    // Brevo SMTP Configuration
    private static final String SMTP_HOST = "smtp-relay.brevo.com";
    private static final String SMTP_PORT = "587";
    private static final String SMTP_USERNAME = "a226f9001@smtp-brevo.com"; // Your Brevo SMTP Login (from Settings → SMTP & API)
    private static final String SMTP_PASSWORD = "bskHN0dFTXBdnnb"; // Get from Brevo: Settings → SMTP & API
    private static final String FROM_EMAIL = "chahineaouledamor721@gmail.com"; // Use your verified sender email
    private static final String FROM_NAME = "Ghrami Platform";
    
    private static EmailService instance;
    
    private EmailService() {}
    
    public static EmailService getInstance() {
        if (instance == null) {
            instance = new EmailService();
        }
        return instance;
    }
    
    /**
     * Send password reset email with 6-digit code
     * @param toEmail Recipient email address
     * @param username Recipient username
     * @param resetCode 6-digit reset code
     * @return true if email sent successfully
     */
    public boolean sendPasswordResetEmail(String toEmail, String username, String resetCode) {
        try {
            // Configure mail server properties
            Properties props = new Properties();
            props.put("mail.smtp.host", SMTP_HOST);
            props.put("mail.smtp.port", SMTP_PORT);
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.debug", "false"); // Set to true for detailed SMTP logs
            
            // Create authenticator
            Authenticator auth = new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(SMTP_USERNAME, SMTP_PASSWORD);
                }
            };
            
            // Create session
            Session session = Session.getInstance(props, auth);
            
            // Create email message
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(FROM_EMAIL, FROM_NAME));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            message.setSubject("🔐 Réinitialisation de votre mot de passe Ghrami");
            
            // Email body with HTML formatting
            String emailBody = buildPasswordResetEmailHTML(username, resetCode);
            message.setContent(emailBody, "text/html; charset=utf-8");
            
            // Send email
            Transport.send(message);
            return true;
            
        } catch (MessagingException e) {
            System.err.println("Failed to send password reset email: " + e.getMessage());
            return false;
        } catch (Exception e) {
            System.err.println("Error sending email: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Build HTML email template for password reset
     */
    private String buildPasswordResetEmailHTML(String username, String resetCode) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: 'Segoe UI', Arial, sans-serif; background-color: #f5f7fa; margin: 0; padding: 20px; }
                    .container { max-width: 600px; margin: 0 auto; background-color: #ffffff; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 12px rgba(0,0,0,0.1); }
                    .header { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); padding: 40px 30px; text-align: center; color: white; }
                    .header h1 { margin: 0; font-size: 28px; font-weight: bold; }
                    .content { padding: 40px 30px; color: #333; }
                    .code-box { background: linear-gradient(135deg, rgba(102, 126, 234, 0.1) 0%, rgba(118, 75, 162, 0.1) 100%); border: 2px solid #667eea; border-radius: 8px; padding: 30px; text-align: center; margin: 25px 0; }
                    .code { font-size: 36px; font-weight: bold; letter-spacing: 8px; color: #667eea; font-family: 'Courier New', monospace; }
                    .warning { background-color: #fff3cd; border-left: 4px solid #ffc107; padding: 15px; margin: 20px 0; border-radius: 4px; }
                    .footer { background-color: #f8f9fa; padding: 25px 30px; text-align: center; color: #6c757d; font-size: 13px; border-top: 1px solid #e9ecef; }
                    strong { color: #667eea; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>🔐 Réinitialisation de mot de passe</h1>
                    </div>
                    <div class="content">
                        <p style="font-size: 16px;">Bonjour <strong>""" + username + """
            </strong>,</p>
                        <p>Vous avez demandé la réinitialisation de votre mot de passe Ghrami. Utilisez le code ci-dessous dans l'application :</p>
                        
                        <div class="code-box">
                            <p style="margin: 0 0 10px 0; font-size: 14px; color: #666;">Votre code de réinitialisation :</p>
                            <div class="code">""" + resetCode + """
            </div>
                            <p style="margin: 10px 0 0 0; font-size: 12px; color: #999;">⏱️ Valide pendant 15 minutes</p>
                        </div>
                        
                        <div class="warning">
                            <strong>⚠️ Important :</strong>
                            <ul style="margin: 10px 0 0 0; padding-left: 20px;">
                                <li>Ce code expire dans <strong>15 minutes</strong></li>
                                <li>Ne partagez jamais ce code avec personne</li>
                                <li>Si vous n'avez pas demandé cette réinitialisation, ignorez cet email</li>
                            </ul>
                        </div>
                        
                        <p style="margin-top: 30px;">Pour réinitialiser votre mot de passe :</p>
                        <ol style="line-height: 1.8;">
                            <li>Retournez à l'application Ghrami</li>
                            <li>Entrez ce code dans le champ prévu</li>
                            <li>Choisissez un nouveau mot de passe sécurisé</li>
                        </ol>
                    </div>
                    <div class="footer">
                        <p style="margin: 0 0 10px 0;"><strong>Ghrami Platform</strong> by OPGG</p>
                        <p style="margin: 0; font-size: 12px;">Plateforme sociale de gestion de hobbies et connectivité</p>
                        <p style="margin: 15px 0 0 0; font-size: 11px; color: #999;">Cet email a été envoyé automatiquement, merci de ne pas répondre.</p>
                    </div>
                </div>
            </body>
            </html>
            """;
    }
    
    /**
     * Send test email to verify configuration
     * @param toEmail Test recipient email
     * @return true if successful
     */
    public boolean sendTestEmail(String toEmail) {
        try {
            Properties props = new Properties();
            props.put("mail.smtp.host", SMTP_HOST);
            props.put("mail.smtp.port", SMTP_PORT);
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");
            
            Authenticator auth = new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(SMTP_USERNAME, SMTP_PASSWORD);
                }
            };
            
            Session session = Session.getInstance(props, auth);
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(FROM_EMAIL, FROM_NAME));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            message.setSubject("✅ Test Email - Ghrami Email Service");
            message.setText("This is a test email from Ghrami Platform. Email service is working correctly!");
            
            Transport.send(message);
            
            System.out.println("Test email sent successfully to: " + toEmail);
            return true;
            
        } catch (Exception e) {
            System.err.println("Failed to send test email: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}

package opgg.ghrami.util;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;

public class EmailService {

    private static final String API_KEY    = "0851a8baa5dc2a95b67086153b0eb04c";
private static final String API_SECRET = "97b1d9db7d49109fc208e309111d09f2";
    private static final String FROM_EMAIL = "chahineaouledamor721@gmail.com";
    private static final String FROM_NAME  = "Ghrami Platform";
    private static final String MAILJET_URL = "https://api.mailjet.com/v3.1/send";

    private static EmailService instance;
    private final HttpClient httpClient;

    private EmailService() {
        this.httpClient = HttpClient.newHttpClient();
    }

    public static EmailService getInstance() {
        if (instance == null) {
            instance = new EmailService();
        }
        return instance;
    }

    private String buildAuth() {
        String credentials = API_KEY + ":" + API_SECRET;
        return "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes());
    }

    private boolean sendEmail(String toEmail, String toName, String subject, String htmlContent) {
        try {
            String json = """
                {
                    "Messages": [
                        {
                            "From": {
                                "Email": "%s",
                                "Name": "%s"
                            },
                            "To": [
                                {
                                    "Email": "%s",
                                    "Name": "%s"
                                }
                            ],
                            "Subject": "%s",
                            "HTMLPart": "%s"
                        }
                    ]
                }
                """.formatted(
                    FROM_EMAIL, FROM_NAME,
                    toEmail, toName,
                    subject,
                    htmlContent.replace("\"", "\\\"").replace("\n", "").replace("\r", "")
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(MAILJET_URL))
                    .header("Content-Type", "application/json")
                    .header("Authorization", buildAuth())
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println("Mailjet response: " + response.statusCode() + " - " + response.body());
            return response.statusCode() == 200;

        } catch (Exception e) {
            System.err.println("Failed to send email: " + e.getMessage());
            return false;
        }
    }

    public boolean sendPasswordResetEmail(String toEmail, String username, String resetCode) {
        String subject = "🔐 Réinitialisation de votre mot de passe Ghrami";
        String html = buildPasswordResetEmailHTML(username, resetCode);
        return sendEmail(toEmail, username, subject, html);
    }

    public boolean sendTestEmail(String toEmail) {
        String subject = "✅ Test Email - Ghrami Email Service";
        String html = "<p>This is a test email from <strong>Ghrami Platform</strong>. Email service is working correctly!</p>";
        return sendEmail(toEmail, "Test User", subject, html);
    }

    private String buildPasswordResetEmailHTML(String username, String resetCode) {
        return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8">
            <style>
                body { font-family: 'Segoe UI', Arial, sans-serif; background-color: #f5f7fa; margin: 0; padding: 20px; }
                .container { max-width: 600px; margin: 0 auto; background-color: #ffffff; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 12px rgba(0,0,0,0.1); }
                .header { background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); padding: 40px 30px; text-align: center; color: white; }
                .header h1 { margin: 0; font-size: 28px; font-weight: bold; }
                .content { padding: 40px 30px; color: #333; }
                .code-box { background: linear-gradient(135deg, rgba(102,126,234,0.1) 0%%, rgba(118,75,162,0.1) 100%%); border: 2px solid #667eea; border-radius: 8px; padding: 30px; text-align: center; margin: 25px 0; }
                .code { font-size: 36px; font-weight: bold; letter-spacing: 8px; color: #667eea; font-family: 'Courier New', monospace; }
                .warning { background-color: #fff3cd; border-left: 4px solid #ffc107; padding: 15px; margin: 20px 0; border-radius: 4px; }
                .footer { background-color: #f8f9fa; padding: 25px 30px; text-align: center; color: #6c757d; font-size: 13px; border-top: 1px solid #e9ecef; }
                strong { color: #667eea; }
            </style>
        </head>
        <body>
            <div class="container">
                <div class="header"><h1>🔐 Réinitialisation de mot de passe</h1></div>
                <div class="content">
                    <p style="font-size:16px;">Bonjour <strong>%s</strong>,</p>
                    <p>Vous avez demandé la réinitialisation de votre mot de passe Ghrami.</p>
                    <div class="code-box">
                        <p style="margin:0 0 10px 0;font-size:14px;color:#666;">Votre code de réinitialisation :</p>
                        <div class="code">%s</div>
                        <p style="margin:10px 0 0 0;font-size:12px;color:#999;">⏱️ Valide pendant 15 minutes</p>
                    </div>
                    <div class="warning">
                        <strong>⚠️ Important :</strong>
                        <ul style="margin:10px 0 0 0;padding-left:20px;">
                            <li>Ce code expire dans <strong>15 minutes</strong></li>
                            <li>Ne partagez jamais ce code avec personne</li>
                            <li>Si vous n'avez pas demandé cette réinitialisation, ignorez cet email</li>
                        </ul>
                    </div>
                    <p style="margin-top:30px;">Pour réinitialiser votre mot de passe :</p>
                    <ol style="line-height:1.8;">
                        <li>Retournez à l'application Ghrami</li>
                        <li>Entrez ce code dans le champ prévu</li>
                        <li>Choisissez un nouveau mot de passe sécurisé</li>
                    </ol>
                </div>
                <div class="footer">
                    <p style="margin:0 0 10px 0;"><strong>Ghrami Platform</strong> by OPGG</p>
                    <p style="margin:0;font-size:12px;">Plateforme sociale de gestion de hobbies et connectivité</p>
                    <p style="margin:15px 0 0 0;font-size:11px;color:#999;">Cet email a été envoyé automatiquement, merci de ne pas répondre.</p>
                </div>
            </div>
        </body>
        </html>
        """.formatted(username, resetCode);
    }
}
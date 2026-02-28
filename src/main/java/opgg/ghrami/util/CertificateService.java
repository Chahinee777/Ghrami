package opgg.ghrami.util;

import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.layout.Canvas;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Text;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.VerticalAlignment;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Function;  // ✅ added

/**
 * Generates a professional A4-landscape PDF Certificate of Completion.
 * Uses iTextPDF 7 (the dependency already in the project).
 *
 * Output saved to:  ~/Desktop/GhramiCertificates/<SafeName>.pdf
 * Returns the absolute path on success, or null on failure.
 *
 * NOTE: Add these lines to your module-info.java if not already present:
 *   requires com.itextpdf.kernel;
 *   requires com.itextpdf.layout;
 *   requires com.itextpdf.io;
 */
public class CertificateService {

    // ── Brand palette ─────────────────────────────────────────────────────────
    private static final DeviceRgb PURPLE      = new DeviceRgb(102, 126, 234);
    private static final DeviceRgb DARK_PURPLE = new DeviceRgb( 80,  60, 160);
    private static final DeviceRgb GOLD        = new DeviceRgb(243, 156,  18);
    private static final DeviceRgb DARK        = new DeviceRgb( 28,  30,  33);
    private static final DeviceRgb GREY        = new DeviceRgb(101, 103, 107);
    private static final DeviceRgb WHITE       = new DeviceRgb(255, 255, 255);
    private static final DeviceRgb LIGHT_BG    = new DeviceRgb(248, 249, 250);

    /**
     * Generates the certificate PDF and returns the file path.
     *
     * @param studentName    Full name of the student
     * @param className      Title of the completed class
     * @param instructorName Instructor's display name
     * @param completedAt    Timestamp when the booking was marked completed
     * @return Absolute path of the generated PDF, or null on error
     */
    public static String generate(String studentName,
                                  String className,
                                  String instructorName,
                                  LocalDateTime completedAt) {
        try {
            // ── Output folder ─────────────────────────────────────────────────
            File certDir = new File(System.getProperty("user.home")
                    + File.separator + "Desktop"
                    + File.separator + "GhramiCertificates");
            certDir.mkdirs();

            // ✅ Fixed: Function<String,String> instead of String
            Function<String, String> safe = s -> s.replaceAll("[^a-zA-Z0-9 _\\-]", "").trim();
            String filename = "Certificate_" + safe.apply(studentName)
                    + "_" + safe.apply(className) + ".pdf";
            File out = new File(certDir, filename);

            // ── PDF document — A4 Landscape ───────────────────────────────────
            PdfDocument pdf = new PdfDocument(new PdfWriter(out.getAbsolutePath()));
            PageSize pageSize = PageSize.A4.rotate();           // 841.89 × 595.28 pt
            // Add the page FIRST so getFirstPage() is never called on an empty doc
            PdfPage page = pdf.addNewPage(pageSize);
            Document doc = new Document(pdf, pageSize);
            doc.setMargins(0, 0, 0, 0);
            PdfCanvas cv = new PdfCanvas(page);
            float W = pageSize.getWidth();
            float H = pageSize.getHeight();

            // ── Background ────────────────────────────────────────────────────
            cv.setFillColor(LIGHT_BG).rectangle(0, 0, W, H).fill();

            // Top banner
            cv.setFillColor(PURPLE).rectangle(0, H - 120, W, 120).fill();

            // Gold accent under banner
            cv.setFillColor(GOLD).rectangle(0, H - 125, W, 6).fill();

            // Bottom strip
            cv.setFillColor(DARK_PURPLE).rectangle(0, 0, W, 44).fill();

            // Corner ornaments (gold)
            float orn = 55f;
            cv.setStrokeColor(GOLD).setLineWidth(2f);
            // top-left
            cv.moveTo(22, H - 22).lineTo(22 + orn, H - 22).stroke();
            cv.moveTo(22, H - 22).lineTo(22, H - 22 - orn).stroke();
            // top-right
            cv.moveTo(W - 22, H - 22).lineTo(W - 22 - orn, H - 22).stroke();
            cv.moveTo(W - 22, H - 22).lineTo(W - 22, H - 22 - orn).stroke();
            // bottom-left
            cv.moveTo(22, 22).lineTo(22 + orn, 22).stroke();
            cv.moveTo(22, 22).lineTo(22, 22 + orn).stroke();
            // bottom-right
            cv.moveTo(W - 22, 22).lineTo(W - 22 - orn, 22).stroke();
            cv.moveTo(W - 22, 22).lineTo(W - 22, 22 + orn).stroke();

            // Decorative side lines
            cv.setStrokeColor(GOLD).setLineWidth(0.5f);
            cv.moveTo(50, H - 130).lineTo(50, 50).stroke();
            cv.moveTo(W - 50, H - 130).lineTo(W - 50, 50).stroke();

            // ── Fonts ─────────────────────────────────────────────────────────
            PdfFont bold    = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
            PdfFont regular = PdfFontFactory.createFont(StandardFonts.HELVETICA);
            PdfFont italic  = PdfFontFactory.createFont(StandardFonts.HELVETICA_OBLIQUE);

            // ── Banner text ───────────────────────────────────────────────────
            drawText(cv, bold,   "GHRAMI",                  0, H - 68,  W, 40,  28f, WHITE,  TextAlignment.CENTER);
            drawText(cv, italic, "Certificate of Completion",0, H - 108, W, 36,  13f, new DeviceRgb(200, 210, 255), TextAlignment.CENTER);

            // ── Body ──────────────────────────────────────────────────────────
            float bodyTop = H - 155;

            drawText(cv, regular, "This certifies that",
                    60, bodyTop, W - 120, 26, 12f, GREY, TextAlignment.CENTER);

            // Student name — big gold
            drawText(cv, bold, studentName.toUpperCase(),
                    60, bodyTop - 56, W - 120, 50, 36f, GOLD, TextAlignment.CENTER);

            // Thin rule under name
            cv.setStrokeColor(new DeviceRgb(220, 220, 220)).setLineWidth(0.8f)
                    .moveTo(100, bodyTop - 62).lineTo(W - 100, bodyTop - 62).stroke();

            drawText(cv, regular, "has successfully completed the class",
                    60, bodyTop - 92, W - 120, 26, 12f, GREY, TextAlignment.CENTER);

            // Class name — purple
            drawText(cv, bold, "\u201c" + className + "\u201d",
                    60, bodyTop - 142, W - 120, 44, 20f, PURPLE, TextAlignment.CENTER);

            // Date & instructor
            String dateStr = completedAt.format(DateTimeFormatter.ofPattern("MMMM dd, yyyy"));
            drawText(cv, regular,
                    "Instructed by  " + instructorName + "     \u00B7     Completed on  " + dateStr,
                    60, bodyTop - 178, W - 120, 26, 11f, GREY, TextAlignment.CENTER);

            // ── Signature area ────────────────────────────────────────────────
            float sigY  = 70;
            float sigCX = W / 2f;

            cv.setStrokeColor(DARK).setLineWidth(0.8f)
                    .moveTo(sigCX - 110, sigY).lineTo(sigCX + 110, sigY).stroke();

            drawText(cv, italic,  instructorName, sigCX - 120, sigY - 17, 240, 18, 11f, DARK, TextAlignment.CENTER);
            drawText(cv, regular, "Instructor",   sigCX - 120, sigY - 31, 240, 16,  9f, GREY, TextAlignment.CENTER);

            // Bottom strip text
            drawText(cv, regular,
                    "Ghrami Learning Platform  \u00B7  " + dateStr,
                    0, 12, W, 20, 9f, WHITE, TextAlignment.CENTER);

            doc.close();
            return out.getAbsolutePath();

        } catch (Exception e) {
            System.err.println("[CertificateService] PDF generation failed: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /** Draw a string inside a Rectangle, vertically centred. */
    private static void drawText(PdfCanvas cv, PdfFont font, String text,
                                 float x, float y, float w, float h,
                                 float size, DeviceRgb color, TextAlignment align) {
        try (Canvas c = new Canvas(cv, new Rectangle(x, y, w, h))) {
            c.add(new Paragraph()
                    .add(new Text(text).setFont(font).setFontSize(size).setFontColor(color))
                    .setTextAlignment(align)
                    .setVerticalAlignment(VerticalAlignment.MIDDLE)
                    .setMultipliedLeading(1.0f)
                    .setMargin(0));
        }
    }
}
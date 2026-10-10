package com.dataquadinc.service;

import com.dataquadinc.model.Payslip;
import com.dataquadinc.model.UserDetails;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
public class PayslipPdfService {

    private static final Color PEACH = new Color(0xF5, 0xC4, 0xA0);
    private static final Color LIGHT_GRAY = new Color(0xEE, 0xEE, 0xEE);
    private static final Color ROW_ALT = new Color(0xF7, 0xF7, 0xF7);
    private static final DateTimeFormatter SHORT_DATE = DateTimeFormatter.ofPattern("dd-MMM-yy", Locale.ENGLISH);
    private static final DateTimeFormatter PERIOD_LABEL = DateTimeFormatter.ofPattern("MMM-yy", Locale.ENGLISH);
    private static final NumberFormat MONEY = NumberFormat.getNumberInstance(Locale.US);

    static {
        MONEY.setMinimumFractionDigits(2);
        MONEY.setMaximumFractionDigits(2);
    }

    public byte[] generate(UserDetails user, Payslip payslip) throws IOException {
        try (PDDocument doc = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);

            float pageW = page.getMediaBox().getWidth();
            float margin = 40f;
            float contentW = pageW - 2 * margin;
            float y = page.getMediaBox().getHeight() - margin;

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                y = drawHeader(doc, cs, margin, y, contentW);
                y -= 18;
                y = drawConsultantDetails(cs, margin, y, contentW, user, payslip);
                y -= 22;
                y = drawEarnings(cs, margin, y, contentW, payslip);
                y -= 28;
                drawFooter(cs, margin, y, contentW);
            }

            doc.save(out);
            return out.toByteArray();
        }
    }

    private float drawHeader(PDDocument doc, PDPageContentStream cs, float x, float y, float contentW) throws IOException {
        float headerTop = y;

        // Logo top-right, aligned with company name (same placement as sample)
        float logoW = 130f;
        float logoH = 40f;
        float logoX = x + contentW - logoW;
        float logoBottom = headerTop - logoH + 6;
        boolean logoDrawn = false;
        try {
            ClassPathResource logoRes = new ClassPathResource("static/dataquad-logo.png");
            if (logoRes.exists()) {
                try (InputStream in = logoRes.getInputStream()) {
                    PDImageXObject img = PDImageXObject.createFromByteArray(doc, in.readAllBytes(), "logo");
                    logoH = logoW * img.getHeight() / (float) img.getWidth();
                    logoBottom = headerTop - logoH + 8;
                    cs.drawImage(img, logoX, logoBottom, logoW, logoH);
                    logoDrawn = true;
                }
            }
        } catch (Exception ignored) {
            // fall through to vector logo
        }
        if (!logoDrawn) {
            drawTextLogo(cs, logoX, headerTop - 8);
        }

        writeText(cs, PDType1Font.HELVETICA_BOLD, 12, x, y, "DATAQUAD IT SOLUTIONS PRIVATE LIMITED");
        y -= 14;
        writeText(cs, PDType1Font.HELVETICA, 9, x, y, "Level 2, Block D, CYBER GATEWAY, Wing 1/B,");
        y -= 12;
        writeText(cs, PDType1Font.HELVETICA, 9, x, y, "HITEC City, Hyderabad, Telangana 500081.");
        y -= 12;
        writeText(cs, PDType1Font.HELVETICA, 9, x, y, "www.adroitinnovative.com");
        y -= 12;
        writeText(cs, PDType1Font.HELVETICA, 9, x, y, "+91-8367643824");

        // Keep content below whichever is lower: address block or logo
        return Math.min(y, logoBottom) - 10;
    }

    private void drawTextLogo(PDPageContentStream cs, float x, float topY) throws IOException {
        // Fallback vector mark: large orange D + grey DATAQUAD / IT SOLUTIONS
        Color orange = new Color(0xED, 0x7D, 0x31);
        Color grey = new Color(0x5A, 0x5A, 0x5A);
        cs.setNonStrokingColor(orange);
        cs.beginText();
        cs.setFont(PDType1Font.HELVETICA_BOLD, 36);
        cs.newLineAtOffset(x, topY - 30);
        cs.showText("D");
        cs.endText();
        cs.setNonStrokingColor(grey);
        writeText(cs, PDType1Font.HELVETICA_BOLD, 11, x + 30, topY - 12, "DATAQUAD");
        writeText(cs, PDType1Font.HELVETICA, 9, x + 30, topY - 26, "IT SOLUTIONS");
        cs.setNonStrokingColor(Color.BLACK);
    }

    private float drawConsultantDetails(PDPageContentStream cs, float x, float y, float w,
                                        UserDetails user, Payslip payslip) throws IOException {
        float headerH = 22;
        fillRect(cs, x, y - headerH, w, headerH, PEACH);
        strokeRect(cs, x, y - headerH, w, headerH);
        writeCentered(cs, PDType1Font.HELVETICA_BOLD, 11, x, y - 15, w, "Consultant Details");
        y -= headerH;

        float colW = w / 4f;
        float subH = 18;
        // sub headers
        fillRect(cs, x, y - subH, w, subH, Color.WHITE);
        strokeRect(cs, x, y - subH, w, subH);
        drawVLine(cs, x + colW, y, y - subH);
        drawVLine(cs, x + 2 * colW, y, y - subH);
        drawVLine(cs, x + 3 * colW, y, y - subH);
        writeText(cs, PDType1Font.HELVETICA_BOLD, 9, x + 4, y - 12, "Particulars");
        writeText(cs, PDType1Font.HELVETICA_BOLD, 9, x + colW + 4, y - 12, "Details");
        writeText(cs, PDType1Font.HELVETICA_BOLD, 9, x + 2 * colW + 4, y - 12, "Particulars");
        writeText(cs, PDType1Font.HELVETICA_BOLD, 9, x + 3 * colW + 4, y - 12, "Details");
        y -= subH;

        String doj = formatDate(user.getJoiningDate());
        String payPeriodLabel = formatPayPeriod(payslip.getPayPeriod());
        String paymentDate = formatDate(payslip.getPaymentDate());

        String[][] rows = {
                {"Consultant ID", nz(user.getUserId()), "Bank Name", nz(user.getBankName())},
                {"Consultant Name", nz(user.getUserName()), "Account Number", nz(user.getAccountNumber())},
                {"Designation", nz(user.getDesignation()), "IFSC Code", nz(user.getIfscCode())},
                {"Department", nz(user.getDepartment()), "Payable Days", String.valueOf(payslip.getPayableDays())},
                {"Date of Joining", doj, "Pay Period", payPeriodLabel},
                {"Payment Date", paymentDate, "", ""},
        };

        float rowH = 18;
        for (int i = 0; i < rows.length; i++) {
            Color bg = (i % 2 == 0) ? Color.WHITE : ROW_ALT;
            fillRect(cs, x, y - rowH, w, rowH, bg);
            strokeRect(cs, x, y - rowH, w, rowH);
            drawVLine(cs, x + colW, y, y - rowH);
            drawVLine(cs, x + 2 * colW, y, y - rowH);
            drawVLine(cs, x + 3 * colW, y, y - rowH);
            writeText(cs, PDType1Font.HELVETICA, 9, x + 4, y - 12, rows[i][0]);
            writeText(cs, PDType1Font.HELVETICA, 9, x + colW + 4, y - 12, truncate(rows[i][1], 28));
            writeText(cs, PDType1Font.HELVETICA, 9, x + 2 * colW + 4, y - 12, rows[i][2]);
            writeText(cs, PDType1Font.HELVETICA, 9, x + 3 * colW + 4, y - 12, truncate(rows[i][3], 28));
            y -= rowH;
        }
        return y;
    }

    private float drawEarnings(PDPageContentStream cs, float x, float y, float w, Payslip payslip) throws IOException {
        float headerH = 22;
        fillRect(cs, x, y - headerH, w, headerH, PEACH);
        strokeRect(cs, x, y - headerH, w, headerH);
        writeCentered(cs, PDType1Font.HELVETICA_BOLD, 11, x, y - 15, w, "Earnings & Deductions");
        y -= headerH;

        float descW = w * 0.65f;
        float amtW = w - descW;
        float subH = 18;
        fillRect(cs, x, y - subH, w, subH, Color.WHITE);
        strokeRect(cs, x, y - subH, w, subH);
        drawVLine(cs, x + descW, y, y - subH);
        writeText(cs, PDType1Font.HELVETICA_BOLD, 9, x + 4, y - 12, "Description");
        writeText(cs, PDType1Font.HELVETICA_BOLD, 9, x + descW + 4, y - 12, "Amount (\u20B9)");
        y -= subH;

        BigDecimal gross = scale(payslip.getGrossConsultancyFee());
        BigDecimal tds = scale(payslip.getTdsAmount());
        BigDecimal net = scale(payslip.getNetPayable());

        y = earningsRow(cs, x, y, w, descW, "Gross Consultancy Fee", money(gross), false, false);
        y = earningsRow(cs, x, y, w, descW, "Total Earnings", money(gross), true, true);
        y = earningsRow(cs, x, y, w, descW, "TDS @ 2%", money(tds), false, false);
        y = earningsRow(cs, x, y, w, descW, "Total Deductions", money(tds), true, true);
        y = earningsRow(cs, x, y, w, descW, "Net Amount Payable", money(net), true, true);

        float wordsH = 22;
        fillRect(cs, x, y - wordsH, w, wordsH, LIGHT_GRAY);
        strokeRect(cs, x, y - wordsH, w, wordsH);
        String words = "In Words: " + nz(payslip.getAmountInWords());
        writeText(cs, PDType1Font.HELVETICA_BOLD, 9, x + 4, y - 14, truncate(words, 95));
        y -= wordsH;
        return y;
    }

    private float earningsRow(PDPageContentStream cs, float x, float y, float w, float descW,
                              String desc, String amount, boolean bold, boolean gray) throws IOException {
        float rowH = 18;
        fillRect(cs, x, y - rowH, w, rowH, gray ? LIGHT_GRAY : Color.WHITE);
        strokeRect(cs, x, y - rowH, w, rowH);
        drawVLine(cs, x + descW, y, y - rowH);
        PDType1Font font = bold ? PDType1Font.HELVETICA_BOLD : PDType1Font.HELVETICA;
        writeText(cs, font, 9, x + 4, y - 12, desc);
        writeRight(cs, font, 9, x + w - 6, y - 12, amount);
        return y - rowH;
    }

    private void drawFooter(PDPageContentStream cs, float x, float y, float w) throws IOException {
        writeText(cs, PDType1Font.HELVETICA_BOLD, 10, x, y, "Declaration");
        y -= 14;
        y = writeWrapped(cs, PDType1Font.HELVETICA, 9, x, y, w,
                "This is to certify that consultancy services were rendered during the above-mentioned period and the payment has been processed after applicable statutory deductions.");
        y -= 16;
        writeText(cs, PDType1Font.HELVETICA_BOLD, 9, x, y, "Note:");
        writeWrapped(cs, PDType1Font.HELVETICA, 9, x + 32, y, w - 32,
                "Please be informed that the TDS Certificate (Form 16A) corresponding to the deducted tax amount will be made available once the Q2 TDS return has been filed.");
    }

    private float writeWrapped(PDPageContentStream cs, PDType1Font font, float size,
                               float x, float y, float maxW, String text) throws IOException {
        String[] words = text.split("\\s+");
        StringBuilder line = new StringBuilder();
        float cursor = y;
        for (String word : words) {
            String trial = line.length() == 0 ? word : line + " " + word;
            float tw = font.getStringWidth(trial) / 1000 * size;
            if (tw > maxW && line.length() > 0) {
                writeText(cs, font, size, x, cursor, line.toString());
                cursor -= 12;
                line = new StringBuilder(word);
            } else {
                line = new StringBuilder(trial);
            }
        }
        if (line.length() > 0) {
            writeText(cs, font, size, x, cursor, line.toString());
            cursor -= 12;
        }
        return cursor;
    }

    public static String formatPayPeriod(String yyyyMm) {
        try {
            return YearMonth.parse(yyyyMm).format(PERIOD_LABEL);
        } catch (Exception e) {
            return yyyyMm == null ? "" : yyyyMm;
        }
    }

    private static String formatDate(LocalDate date) {
        return date == null ? "" : date.format(SHORT_DATE);
    }

    private static BigDecimal scale(BigDecimal v) {
        return v == null ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                : v.setScale(2, RoundingMode.HALF_UP);
    }

    private static String money(BigDecimal v) {
        return MONEY.format(v);
    }

    private static String nz(String v) {
        return v == null || v.isBlank() ? "-" : v.trim();
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }

    private static void writeText(PDPageContentStream cs, PDType1Font font, float size,
                                  float x, float y, String text) throws IOException {
        cs.beginText();
        cs.setFont(font, size);
        cs.newLineAtOffset(x, y);
        cs.showText(sanitize(text));
        cs.endText();
    }

    private static void writeRight(PDPageContentStream cs, PDType1Font font, float size,
                                   float rightX, float y, String text) throws IOException {
        String t = sanitize(text);
        float tw = font.getStringWidth(t) / 1000 * size;
        writeText(cs, font, size, rightX - tw, y, t);
    }

    private static void writeCentered(PDPageContentStream cs, PDType1Font font, float size,
                                      float x, float y, float w, String text) throws IOException {
        String t = sanitize(text);
        float tw = font.getStringWidth(t) / 1000 * size;
        writeText(cs, font, size, x + (w - tw) / 2, y, t);
    }

    private static String sanitize(String text) {
        if (text == null) return "";
        // WinAnsi does not support ₹; keep ASCII-friendly amount label already using (Rs) in header cell
        return text.replace("₹", "Rs.").replace("…", "...");
    }

    private static void fillRect(PDPageContentStream cs, float x, float y, float w, float h, Color c) throws IOException {
        cs.setNonStrokingColor(c);
        cs.addRect(x, y, w, h);
        cs.fill();
        cs.setNonStrokingColor(Color.BLACK);
    }

    private static void strokeRect(PDPageContentStream cs, float x, float y, float w, float h) throws IOException {
        cs.setStrokingColor(Color.BLACK);
        cs.setLineWidth(0.6f);
        cs.addRect(x, y, w, h);
        cs.stroke();
    }

    private static void drawVLine(PDPageContentStream cs, float x, float yTop, float yBottom) throws IOException {
        cs.setStrokingColor(Color.BLACK);
        cs.setLineWidth(0.6f);
        cs.moveTo(x, yTop);
        cs.lineTo(x, yBottom);
        cs.stroke();
    }
}

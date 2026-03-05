import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import java.io.File;
import java.util.regex.*;

public class ReceiptParser {

    // ★ MODIFY THESE PATTERNS to match your receipt format ★
    private static final Pattern PRICE_LINE = Pattern.compile("^(.+?)\\s+\\$?(\\d+\\.\\d{2})\\s*$");
    private static final Pattern TOTAL_LINE = Pattern.compile("(?i)total\\s*\\$?(\\d+\\.\\d{2})");
    private static final Pattern DATE_LINE = Pattern.compile("(\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4})");

    public static Receipt parse(File pdfFile) throws Exception {
        PDDocument doc = PDDocument.load(pdfFile);
        String text = new PDFTextStripper().getText(doc);
        doc.close();

        Receipt receipt = new Receipt();
        String[] lines = text.split("\\n");

        // First non-empty line is usually the store name
        for (String line : lines) {
            if (!line.trim().isEmpty()) {
                receipt.storeName = line.trim();
                break;
            }
        }

        for (String line : lines) {
            String trimmed = line.trim();

            // Check for date
            Matcher dateMatcher = DATE_LINE.matcher(trimmed);
            if (dateMatcher.find() && receipt.date.isEmpty()) {
                receipt.date = dateMatcher.group(1);
            }

            // Check for total
            Matcher totalMatcher = TOTAL_LINE.matcher(trimmed);
            if (totalMatcher.find()) {
                receipt.total = totalMatcher.group(1);
                continue;
            }

            // Check for line items (name + price)
            Matcher itemMatcher = PRICE_LINE.matcher(trimmed);
            if (itemMatcher.find()) {
                receipt.items.add(new Receipt.LineItem(
                        itemMatcher.group(1).trim(),
                        itemMatcher.group(2)
                ));
            }
        }

        return receipt;
    }
}

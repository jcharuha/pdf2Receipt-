import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ReceiptParser 
{

    private static final Pattern PRICE_LINE =
            Pattern.compile("(.+?)\\s+\\$?(\\d{1,3}(?:[.,]\\d{3})*(?:[.,]\\d{2}))(?=\\s|$)");

    private static final Pattern TOTAL_LINE =
            Pattern.compile("(?i).*\\btotal\\b.*\\$?(\\d{1,3}(?:[.,]\\d{3})*(?:[.,]\\d{2})).*");

    private static final Pattern DATE_LINE =
            Pattern.compile("(\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4})");

    public static Receipt parse(File pdfFile) throws Exception 
    {
        String text;

        try (PDDocument doc = Loader.loadPDF(pdfFile))
        {
            text = new PDFTextStripper().getText(doc);
        }

        Receipt receipt = new Receipt();
        String[] lines = text.split("\\R");

        for (String line : lines) 
            {
            String trimmed = line.trim();
            if (!trimmed.isEmpty()) 
            {
                receipt.storeName = trimmed;
                break;
            }
        }

        String pendingItem = null;

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                continue;
            }

            Matcher dateMatcher = DATE_LINE.matcher(trimmed);
            if (dateMatcher.find() && receipt.date.isEmpty()) {
                receipt.date = dateMatcher.group(1);
            }

            String lower = trimmed.toLowerCase();
            if (lower.contains("subtotal") ||
                lower.contains("tax") ||
                lower.contains("change") ||
                lower.contains("balance") ||
                lower.contains("code") ||
                lower.contains("points") ||
                lower.contains("units") ||
                lower.contains("size") ||
                lower.contains("markdown")) {
                pendingItem = null;
                continue;
            }

            Matcher totalMatcher = TOTAL_LINE.matcher(trimmed);
            if (totalMatcher.find()) {
                receipt.total = totalMatcher.group(1).replace(",", ".");
                pendingItem = null;
                continue;
            }

            Matcher itemMatcher = PRICE_LINE.matcher(trimmed);
            if (itemMatcher.find()) {
                String itemName = itemMatcher.group(1).trim();
                String itemPrice = itemMatcher.group(2).replace(",", ".");

                // If no descriptive name on same line, use pending name if present
                if (itemName.isEmpty() && pendingItem != null) {
                    itemName = pendingItem;
                }

                if (!itemName.equalsIgnoreCase("total") && !itemName.isEmpty()) {
                    receipt.items.add(new Receipt.LineItem(itemName, itemPrice));
                }

                pendingItem = null;
                continue;
            }

            // Keep possible multi-line description for next price-line
            if (trimmed.length() > 2 && !trimmed.matches("^\\d+$")) {
                pendingItem = trimmed;
            }
        }

        return receipt;
    }
}
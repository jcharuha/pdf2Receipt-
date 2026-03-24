import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ReceiptParser 
{

    private static final Pattern PRICE_LINE =
            Pattern.compile("^(.+?)\\s+\\$?(\\d+[.,]\\d{2})\\s*$");

    private static final Pattern TOTAL_LINE =
            Pattern.compile("(?i)^\\s*total\\s*[: ]?\\$?(\\d+[.,]\\d{2})\\s*$");

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

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                continue;
            }

            Matcher dateMatcher = DATE_LINE.matcher(trimmed);
            if (dateMatcher.find() && receipt.date.isEmpty()) {
                receipt.date = dateMatcher.group(1);
            }

            Matcher totalMatcher = TOTAL_LINE.matcher(trimmed);
            if (totalMatcher.find()) {
                receipt.total = totalMatcher.group(1).replace(",", ".");
                continue;
            }

            if (trimmed.toLowerCase().contains("subtotal") ||
                trimmed.toLowerCase().contains("tax") ||
                trimmed.toLowerCase().contains("change") ||
                trimmed.toLowerCase().contains("balance")) {
                continue;
            }

            Matcher itemMatcher = PRICE_LINE.matcher(trimmed);
            if (itemMatcher.find()) {
                String itemName = itemMatcher.group(1).trim();
                String itemPrice = itemMatcher.group(2).replace(",", ".");

                if (!itemName.equalsIgnoreCase("total")) {
                    receipt.items.add(new Receipt.LineItem(itemName, itemPrice));
                }
            }
        }

        return receipt;
    }
}
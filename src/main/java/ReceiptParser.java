import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ReceiptParser 
{
    private static final Pattern PRICE_LINE =
            Pattern.compile("(.+?)\\s+\\$?(\\d{1,3}(?:[.,]\\d{3})*(?:[.,]\\d{2}))(?=\\s|$)");

    private static final Pattern TOTAL_LINE =
            Pattern.compile("(?i).*\\btotal\\b.*?\\$?(\\d{1,3}(?:[.,]\\d{3})*(?:[.,]\\d{2})).*");

    private static final Pattern DATE_LINE =
            Pattern.compile("(\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4})");

    public static Receipt parse(File pdfFile) throws Exception 
    {
        String text = extractText(pdfFile);
        Receipt receipt = parseText(text);

        // OCR fallback for image-based PDFs
        if (receipt.items.isEmpty() && (receipt.total == null || receipt.total.isEmpty())) 
        {
            String ocrText = extractTextWithOcr(pdfFile);
            if (ocrText != null && !ocrText.isEmpty()) 
            {
                Receipt ocrReceipt = parseText(ocrText);
                if (!ocrReceipt.items.isEmpty() || (ocrReceipt.total != null && !ocrReceipt.total.isEmpty())) 
                {
                    receipt.items = ocrReceipt.items;

                    if (receipt.storeName == null || receipt.storeName.isEmpty()) 
                    {
                        receipt.storeName = ocrReceipt.storeName;
                    }

                    if (receipt.date == null || receipt.date.isEmpty()) 
                    {
                        receipt.date = ocrReceipt.date;
                    }

                    if (receipt.total == null || receipt.total.isEmpty()) 
                    {
                        receipt.total = ocrReceipt.total;
                    }
                }
            }
        }

        return receipt;
    }

    private static String extractText(File pdfFile) throws Exception 
    {
        try (PDDocument doc = Loader.loadPDF(pdfFile)) 
        {
            return new PDFTextStripper().getText(doc);
        }
    }

    private static String extractTextWithOcr(File pdfFile) 
    {
        try (PDDocument doc = Loader.loadPDF(pdfFile)) 
        {
            String tessDataPath = System.getenv("TESSDATA_PREFIX");
            if (tessDataPath == null || tessDataPath.isBlank()) 
            {
                tessDataPath = "C:\\Program Files\\Tesseract-OCR\\tessdata";
            }

            File eng = new File(tessDataPath, "eng.traineddata");
            if (!eng.exists()) 
            {
                eng = new File("tessdata", "eng.traineddata");
            }

            if (!eng.exists()) 
            {
                System.err.println("Tesseract data file not found at: " + tessDataPath + " or ./tessdata");
                return "";
            }

            File tessDataDir = eng.getParentFile();

            ITesseract tesseract = new Tesseract();
            tesseract.setDatapath(tessDataDir.getAbsolutePath());
            tesseract.setLanguage("eng");

            PDFRenderer renderer = new PDFRenderer(doc);
            StringBuilder allText = new StringBuilder();

            for (int page = 0; page < doc.getNumberOfPages(); page++) 
            {
                BufferedImage image = renderer.renderImageWithDPI(page, 300, ImageType.RGB);
                String pageText = tesseract.doOCR(image);
                allText.append(pageText).append("\n");
            }

            return allText.toString();
        } 
        catch (TesseractException | java.io.IOException | Error e) 
        {
            e.printStackTrace();
            return "";
        }
    }

    private static Receipt parseText(String text) 
    {
        Receipt receipt = new Receipt();
        String[] lines = text.split("\\R");
        Map<String, Receipt.LineItem> groupedItems = new LinkedHashMap<>();

        // First non-empty line as store name
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
        String foundTotal = "";

        for (String line : lines) 
        {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) 
            {
                continue;
            }

            // date
            Matcher dateMatcher = DATE_LINE.matcher(trimmed);
            if (dateMatcher.find() && (receipt.date == null || receipt.date.isEmpty())) 
            {
                receipt.date = dateMatcher.group(1);
            }

            String lower = trimmed.toLowerCase();

            // skip non-item summary lines
            if (lower.contains("subtotal") ||
                lower.contains("tax") ||
                lower.contains("change") ||
                lower.contains("balance") ||
                lower.contains("code") ||
                lower.contains("points") ||
                lower.contains("units") ||
                lower.contains("size") ||
                lower.contains("markdown")) 
            {
                pendingItem = null;
                continue;
            }

            // total (keep last match)
            Matcher totalMatcher = TOTAL_LINE.matcher(trimmed);
            if (totalMatcher.find()) 
            {
                foundTotal = totalMatcher.group(1).replace(",", ".");
                pendingItem = null;
                continue;
            }

            // item line
            Matcher itemMatcher = PRICE_LINE.matcher(trimmed);
            if (itemMatcher.find()) 
            {
                String itemName = itemMatcher.group(1).trim();
                String itemPrice = itemMatcher.group(2).replace(",", ".");

                if (itemName.isEmpty() && pendingItem != null) 
                {
                    itemName = pendingItem;
                }

                if (!itemName.equalsIgnoreCase("total") && !itemName.isEmpty()) 
                {
                    String key = itemName.toLowerCase().trim();

                    if (groupedItems.containsKey(key)) 
                    {
                        groupedItems.get(key).addOne(itemPrice);
                    } 
                    else 
                    {
                        groupedItems.put(key, new Receipt.LineItem(itemName, itemPrice));
                    }
                }

                pendingItem = null;
                continue;
            }

            // possible item description line before price line
            if (trimmed.length() > 2 && !trimmed.matches("^\\d+$")) 
            {
                pendingItem = trimmed;
            }
        }

        receipt.total = foundTotal;
        receipt.items.clear();
        receipt.items.addAll(groupedItems.values());

        return receipt;
    }
}
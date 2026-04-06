import javax.swing.SwingUtilities;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) 
    {
        // Initialize database
        ReceiptDatabase.initialize();

        // Start UI (Uploader)
        SwingUtilities.invokeLater(() -> new Uploader().createUI());

        // --- OPTIONAL: console-based price history search ---
        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.print("\nEnter item name to check price history (or 'exit'): ");
            String itemName = scanner.nextLine();

            if (itemName.equalsIgnoreCase("exit")) {
                break;
            }

            ReceiptDatabase.printPriceHistory(itemName);
        }

        scanner.close();
    }
}
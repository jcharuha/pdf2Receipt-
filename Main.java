public class Main 
{

    public static void main(String[] args) 
    { 

        // Create a receipt object
        Receipt receipt = new Receipt();

        // Fill receipt information
        receipt.storeName = "Target";
        receipt.date = "03/04/2026";
        receipt.total = "12.99";

        // Add items
        receipt.items.add(new Receipt.LineItem("Milk", "4.50"));
        receipt.items.add(new Receipt.LineItem("Bread", "3.00"));
        receipt.items.add(new Receipt.LineItem("Eggs", "5.49"));   ///

        // Print receipt
        System.out.println(receipt);
    }
}

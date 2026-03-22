public class Main {

    public static void main(String[] args) {

        ReceiptDatabase.initialize();

        Receipt receipt = new Receipt();
        receipt.storeName = "Target";
        receipt.date = "03/04/2026";
        receipt.total = "12.99";

        receipt.items.add(new Receipt.LineItem("Milk", "4.50"));
        receipt.items.add(new Receipt.LineItem("Bread", "3.00"));
        receipt.items.add(new Receipt.LineItem("Eggs", "5.49"));

        int receiptId = ReceiptDatabase.saveReceipt(receipt);

        System.out.println("Saved receipt with ID: " + receiptId);
        System.out.println(receipt);
    }
}

import java.util.*;

public class Receipt {
    public String storeName = "";
    public String date = "";
    public String total = "";
    public List<LineItem> items = new ArrayList<>();

    public static class LineItem {
        public String name;
        public String price;
        public LineItem(String name, String price) {
            this.name = name;
            this.price = price;
        }
        @Override
        public String toString() {
            return name + " — $" + price;
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Store: ").append(storeName).append("\n");
        sb.append("Date: ").append(date).append("\n");
        sb.append("Items:\n");
        for (LineItem item : items) sb.append("  ").append(item).append("\n");
        sb.append("Total: $").append(total).append("\n");
        return sb.toString();
    }
}

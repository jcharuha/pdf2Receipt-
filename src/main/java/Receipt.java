import java.util.ArrayList;
import java.util.List;

public class Receipt 
{
    public String storeName = "";
    public String date = "";
    public String total = "";
    public List<LineItem> items = new ArrayList<>();

    public static class LineItem 
    {
        public String name;
        public String price;       // unit price as string
        public int quantity;
        public double totalPrice;

        public LineItem(String name, String price) 
        {
            this.name = name;
            this.price = price;
            this.quantity = 1;
            this.totalPrice = parseDoubleSafe(price);
        }

        public void addOne(String price) 
        {
            this.quantity++;
            this.totalPrice += parseDoubleSafe(price);
        }

        private static double parseDoubleSafe(String value)
        {
            if (value == null || value.trim().isEmpty())
            {
                return 0.0;
            }

            try
            {
                String cleaned = value.replace("$", "").replace(",", "").trim();
                return Double.parseDouble(cleaned);
            }
            catch (NumberFormatException e)
            {
                return 0.0;
            }
        }

        @Override
        public String toString() 
        {
            return name + " x" + quantity + " - $" + String.format("%.2f", totalPrice);
        }
    }

    @Override
    public String toString() 
    {
        StringBuilder sb = new StringBuilder();
        sb.append("Store: ").append(storeName).append("\n");
        sb.append("Date: ").append(date).append("\n");
        sb.append("Items:\n");
        for (LineItem item : items) 
        {
            sb.append("  ").append(item).append("\n");
        }
        sb.append("Total: $").append(total).append("\n");
        return sb.toString();
    }
}
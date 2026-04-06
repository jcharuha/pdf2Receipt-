import java.sql.*;

public class ReceiptDatabase {

    private static final String DB_URL = "jdbc:sqlite:receipts.db";

    public static void initialize() {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement()) {

            stmt.execute("PRAGMA foreign_keys = ON");

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS receipts (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    store_name TEXT,
                    date TEXT,
                    total REAL,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS line_items (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    receipt_id INTEGER NOT NULL,
                    item_name TEXT,
                    quantity INTEGER,
                    price_per_item REAL,
                    total_price REAL,
                    FOREIGN KEY (receipt_id) REFERENCES receipts(id) ON DELETE CASCADE
                )
            """);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static int saveReceipt(Receipt receipt) {
        String sql = "INSERT INTO receipts (store_name, date, total) VALUES (?, ?, ?)";

        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            conn.setAutoCommit(false);

            try {
                pstmt.setString(1, receipt.storeName);
                pstmt.setString(2, receipt.date);
                pstmt.setDouble(3, parseDoubleSafe(receipt.total));
                pstmt.executeUpdate();

                try (ResultSet keys = pstmt.getGeneratedKeys()) {
                    if (!keys.next()) {
                        conn.rollback();
                        return -1;
                    }

                    int receiptId = keys.getInt(1);
                    saveLineItems(conn, receiptId, receipt);
                    conn.commit();
                    return receiptId;
                }

            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }

        } catch (SQLException e) {
            e.printStackTrace();
            return -1;
        }
    }

    private static void saveLineItems(Connection conn, int receiptId, Receipt receipt) throws SQLException {
        String sql = "INSERT INTO line_items (receipt_id, item_name, quantity, price_per_item, total_price) VALUES (?, ?, ?, ?, ?)";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            for (Receipt.LineItem item : receipt.items) {
                pstmt.setInt(1, receiptId);
                pstmt.setString(2, item.name);
                pstmt.setInt(3, item.quantity);
                pstmt.setDouble(4, parseDoubleSafe(item.price));
                pstmt.setDouble(5, item.totalPrice);
                pstmt.addBatch();
            }
            pstmt.executeBatch();
        }
    }

    public static void printPriceHistory(String itemName) {
        String sql = """
            SELECT r.date, l.item_name, l.quantity, l.price_per_item, l.total_price
            FROM line_items l
            JOIN receipts r ON l.receipt_id = r.id
            WHERE LOWER(l.item_name) = LOWER(?)
            ORDER BY r.date
        """;

        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, itemName);

            try (ResultSet rs = pstmt.executeQuery()) {
                boolean found = false;

                System.out.println("Price history for: " + itemName);
                System.out.println("----------------------------------------");

                while (rs.next()) {
                    found = true;

                    String date = rs.getString("date");
                    String name = rs.getString("item_name");
                    int quantity = rs.getInt("quantity");
                    double pricePerItem = rs.getDouble("price_per_item");
                    double totalPrice = rs.getDouble("total_price");

                    System.out.println(
                        "Date: " + date +
                        " | Item: " + name +
                        " | Qty: " + quantity +
                        " | Price Each: $" + String.format("%.2f", pricePerItem) +
                        " | Total: $" + String.format("%.2f", totalPrice)
                    );
                }

                if (!found) {
                    System.out.println("No history found for item: " + itemName);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static double parseDoubleSafe(String value) {
        if (value == null || value.trim().isEmpty()) {
            return 0.0;
        }

        try {
            String cleaned = value.replace("$", "").replace(",", "").trim();
            return Double.parseDouble(cleaned);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }
}
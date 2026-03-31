import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;

public class Uploader {

    public void createUI() {
        JFrame frame = new JFrame("Receipt Upload");
        frame.setSize(450, 220);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);

        JButton uploadButton = new JButton("Upload Receipt (PDF)");
        uploadButton.addActionListener((ActionEvent e) -> openFileChooser(frame));

        frame.getContentPane().add(uploadButton);
        frame.setVisible(true);
    }

    private void openFileChooser(JFrame parent) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Select Receipt PDF");

        // Start in receipts folder, falling back to user home.
        File receiptsDir = new File("receipts");
        if (!receiptsDir.exists()) {
            receiptsDir.mkdirs();
        }

        if (receiptsDir.exists() && receiptsDir.isDirectory()) {
            fileChooser.setCurrentDirectory(receiptsDir);
        } else {
            fileChooser.setCurrentDirectory(new File(System.getProperty("user.home")));
        }

        FileNameExtensionFilter pdfFilter = new FileNameExtensionFilter("PDF Files", "pdf", "PDF");
        fileChooser.setFileFilter(pdfFilter);

        // Allow users to switch filter if needed in case the system-derived extension does not match.
        fileChooser.setAcceptAllFileFilterUsed(true);

        int result = fileChooser.showOpenDialog(parent);

        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();

            if (!selectedFile.getName().toLowerCase().endsWith(".pdf")) {
                JOptionPane.showMessageDialog(parent,
                        "Please select a PDF file.",
                        "Invalid File",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            processReceipt(selectedFile, parent);
        }
    }

    private void processReceipt(File originalFile, JFrame parent) {
        try {
            File savedFile = saveLocally(originalFile);

            Receipt receipt = ReceiptParser.parse(savedFile);

            if (receipt == null) {
                JOptionPane.showMessageDialog(parent,
                        "Failed to parse receipt.",
                        "Parse Error",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            int receiptId = ReceiptDatabase.saveReceipt(receipt);

            if (receiptId == -1) {
                JOptionPane.showMessageDialog(parent,
                        "Receipt parsed, but failed to save to database.",
                        "Database Error",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            JOptionPane.showMessageDialog(parent,
                    "Receipt saved successfully!\n\nID: " + receiptId + "\n\n" + receipt,
                    "Success",
                    JOptionPane.INFORMATION_MESSAGE);

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(parent,
                    "Error processing receipt: " + ex.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }

    private File saveLocally(File file) throws IOException {
        Path receiptsFolder = Paths.get("receipts");
        Files.createDirectories(receiptsFolder);

        Path destination = receiptsFolder.resolve(file.getName());
        Files.copy(file.toPath(), destination, StandardCopyOption.REPLACE_EXISTING);

        return destination.toFile();
    }
}
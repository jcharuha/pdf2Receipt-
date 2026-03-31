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

        // Fix: only show PDF files
        fileChooser.setAcceptAllFileFilterUsed(false);
        fileChooser.setFileFilter(new FileNameExtensionFilter("PDF Files", "pdf"));

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
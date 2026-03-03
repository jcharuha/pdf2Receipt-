import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;

public class Uploader 
{

    public static void main(String[] args) 
    {
        SwingUtilities.invokeLater(() -> new Uploader().createUI());
    }


    //creates a jframe box ui for the reciept upload
    private void createUI() 
    {
        JFrame frame = new JFrame("Receipt Upload");
        frame.setSize(400, 200);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);

        JButton uploadButton = new JButton("Upload Receipt (PDF)");

        uploadButton.addActionListener((ActionEvent e) -> openFileChooser(frame));

        frame.getContentPane().add(uploadButton);
        frame.setVisible(true);
    }

    private void openFileChooser(JFrame parent) 
    {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Select Receipt PDF");

        int result = fileChooser.showOpenDialog(parent);

        if (result == JFileChooser.APPROVE_OPTION) 
        {
            File selectedFile = fileChooser.getSelectedFile();

            if (!selectedFile.getName().toLowerCase().endsWith(".pdf")) 
            {
                JOptionPane.showMessageDialog(parent,
                        "Please select a PDF file.",
                        "Invalid File",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            saveLocally(selectedFile, parent);
        }
    }

    private void saveLocally(File file, JFrame parent) 
    {
        try 
        {
            Path receiptsFolder = Paths.get("receipts");
            Files.createDirectories(receiptsFolder);

            Path destination = receiptsFolder.resolve(file.getName());

            Files.copy(file.toPath(), destination, StandardCopyOption.REPLACE_EXISTING);

            JOptionPane.showMessageDialog(parent,
                    "Receipt saved successfully!",
                    "Success",
                    JOptionPane.INFORMATION_MESSAGE);

        } catch (IOException ex) 
        {
            JOptionPane.showMessageDialog(parent,
                    "Error saving file: " + ex.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }
}
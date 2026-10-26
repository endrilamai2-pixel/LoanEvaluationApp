package LoanApp.LoanApp;

import javax.swing.*;

/** Ανοίγει το κύριο παράθυρο της εφαρμογής, με καρτέλες για ιδιώτες και επιχειρήσεις. */
public class LoanGUI {

    public static void launch() {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
                // αν αποτύχει, χρησιμοποιείται το προεπιλεγμένο look
            }

            JTabbedPane tabs = new JTabbedPane();
            tabs.addTab("Ιδιώτης", new LoanPanel());
            tabs.addTab("Επιχείρηση", new BusinessLoanPanel());
            tabs.setFont(tabs.getFont().deriveFont(java.awt.Font.BOLD, 14f));

            JFrame frame = new JFrame("Αξιολόγηση Αίτησης Δανείου");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setContentPane(tabs);
            frame.pack();
            // Το παράθυρο δεν πρέπει να ξεπερνά την οθόνη (χωρίς taskbar)
            java.awt.Rectangle screen = java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
            frame.setSize(Math.min(frame.getWidth(), screen.width), Math.min(frame.getHeight(), screen.height));
            frame.setLocationRelativeTo(null);   // στο κέντρο της οθόνης
            frame.setVisible(true);
        });
    }

    public static void main(String[] args) {
        launch();
    }
}

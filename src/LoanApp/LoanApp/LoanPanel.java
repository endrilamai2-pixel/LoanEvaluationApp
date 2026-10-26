package LoanApp.LoanApp;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.text.ParseException;
import java.util.Locale;

/**
 * Η κύρια οθόνη της εφαρμογής: φόρμα αριστερά, αποτέλεσμα δεξιά.
 * Το εκτιμώμενο credit score ενημερώνεται ζωντανά καθώς συμπληρώνονται τα στοιχεία.
 */
public class LoanPanel extends JPanel {

    static final Locale GR = Locale.forLanguageTag("el-GR");
    static final Color GREEN = new Color(0x1E8E3E);
    static final Color RED = new Color(0xC62828);
    static final Color ORANGE = new Color(0xE65100);

    // ---------- Πεδία φόρμας ----------
    final JTextField nameField = new JTextField(16);

    final JSpinner late30 = intSpinner(0, 100, 0);
    final JSpinner late60 = intSpinner(0, 100, 0);
    final JSpinner late90 = intSpinner(0, 100, 0);
    final JCheckBox bankrupt = new JCheckBox("Ναι");
    final JTextField creditLimit = new MoneyField("0", false);
    final JTextField creditUsed = new MoneyField("0", false);
    final JTextField historyYears = new JTextField("0");
    final JSpinner inquiries = intSpinner(0, 100, 0);
    final JSpinner accountTypes = intSpinner(0, 10, 0);

    final JTextField assets = new MoneyField("", false);
    final JTextField amount = new MoneyField("", false);
    final JSpinner term = intSpinner(1, LoanAppParameters.MAX_TERM_YEARS, 10);
    final JComboBox<Purpose> purpose = new JComboBox<>(Purpose.values());

    // ---------- Πεδία αποτελέσματος ----------
    final JProgressBar scoreBar = new JProgressBar(CreditScoreCalculator.MIN_SCORE, CreditScoreCalculator.MAX_SCORE);
    final JLabel scoreBreakdown = new JLabel(" ");
    final JLabel decisionLabel = new JLabel(" ", SwingConstants.CENTER);
    final JLabel detailsLabel = new JLabel();

    public LoanPanel() {
        setLayout(new BorderLayout(12, 12));
        setBorder(new EmptyBorder(12, 12, 12, 12));

        purpose.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean selected, boolean focus) {
                super.getListCellRendererComponent(list, value, index, selected, focus);
                if (value instanceof Purpose p) setText(p.label);
                return this;
            }
        });

        JScrollPane formScroll = new JScrollPane(buildForm());
        formScroll.setBorder(null);
        formScroll.getVerticalScrollBar().setUnitIncrement(16);
        add(formScroll, BorderLayout.WEST);
        add(buildResultPanel(), BorderLayout.CENTER);

        // Ζωντανή ενημέρωση του credit score
        for (JSpinner s : new JSpinner[]{late30, late60, late90, inquiries, accountTypes}) {
            s.addChangeListener(e -> updateScorePreview());
        }
        bankrupt.addActionListener(e -> updateScorePreview());
        for (JTextField f : new JTextField[]{creditLimit, creditUsed, historyYears}) {
            onTextChange(f, this::updateScorePreview);
        }
        updateScorePreview();
        showPlaceholder();
    }

    // ===================== ΚΑΤΑΣΚΕΥΗ ΕΜΦΑΝΙΣΗΣ =====================

    private JPanel buildForm() {
        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));

        JPanel personal = section("Στοιχεία αιτούντος");
        addRow(personal, 0, "Ονοματεπώνυμο", nameField);

        JPanel history = section("Πιστοληπτικό ιστορικό");
        addRow(history, 0, "Καθυστερήσεις 30+ ημερών", late30);
        addRow(history, 1, "Καθυστερήσεις 60+ ημερών", late60);
        addRow(history, 2, "Καθυστερήσεις 90+ ημερών", late90);
        addRow(history, 3, "Πτώχευση / αθέτηση", bankrupt);
        addRow(history, 4, "Συνολικό όριο καρτών (€)", creditLimit);
        addRow(history, 5, "Χρησιμοποιημένο υπόλοιπο (€)", creditUsed);
        addRow(history, 6, "Έτη πιστωτικού ιστορικού", historyYears);
        addRow(history, 7, "Αιτήσεις πίστωσης (12 μήνες)", inquiries);
        addRow(history, 8, "Είδη πίστωσης (κάρτα, στεγαστικό...)", accountTypes);

        JPanel loan = section("Στοιχεία δανείου");
        addRow(loan, 0, "Περιουσιακά στοιχεία (€)", assets);
        addRow(loan, 1, "Ποσό δανείου (€)", amount);
        addRow(loan, 2, "Διάρκεια (έτη)", term);
        addRow(loan, 3, "Σκοπός δανείου", purpose);

        JButton evaluateBtn = new JButton("Αξιολόγηση αίτησης");
        evaluateBtn.setFont(evaluateBtn.getFont().deriveFont(Font.BOLD, 14f));
        evaluateBtn.addActionListener(e -> evaluate());
        JButton clearBtn = new JButton("Καθαρισμός");
        clearBtn.addActionListener(e -> clearForm());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        buttons.add(clearBtn);
        buttons.add(evaluateBtn);
        buttons.setAlignmentX(Component.LEFT_ALIGNMENT);

        for (JPanel p : new JPanel[]{personal, history, loan}) {
            p.setAlignmentX(Component.LEFT_ALIGNMENT);
            form.add(p);
            form.add(Box.createVerticalStrut(8));
        }
        form.add(buttons);
        getRootPaneSafe(evaluateBtn);
        return form;
    }

    private void getRootPaneSafe(JButton defaultButton) {
        // Το Enter ενεργοποιεί το κουμπί αξιολόγησης μόλις το panel μπει σε παράθυρο
        addHierarchyListener(e -> {
            JRootPane root = SwingUtilities.getRootPane(this);
            if (root != null && isShowing()) root.setDefaultButton(defaultButton);
        });
    }

    private JPanel buildResultPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(new TitledBorder("Αποτέλεσμα"));

        JLabel scoreTitle = new JLabel("Εκτιμώμενο credit score (ζωντανά)");
        scoreTitle.setFont(scoreTitle.getFont().deriveFont(Font.BOLD));
        scoreTitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        scoreBar.setStringPainted(true);
        scoreBar.setAlignmentX(Component.LEFT_ALIGNMENT);
        scoreBar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        scoreBreakdown.setAlignmentX(Component.LEFT_ALIGNMENT);

        decisionLabel.setFont(decisionLabel.getFont().deriveFont(Font.BOLD, 26f));
        decisionLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        decisionLabel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
        decisionLabel.setHorizontalAlignment(SwingConstants.LEFT);

        detailsLabel.setVerticalAlignment(SwingConstants.TOP);
        detailsLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        panel.add(Box.createVerticalStrut(6));
        panel.add(scoreTitle);
        panel.add(Box.createVerticalStrut(6));
        panel.add(scoreBar);
        panel.add(Box.createVerticalStrut(4));
        panel.add(scoreBreakdown);
        panel.add(Box.createVerticalStrut(16));
        panel.add(new JSeparator());
        panel.add(Box.createVerticalStrut(16));
        panel.add(decisionLabel);
        panel.add(Box.createVerticalStrut(10));
        panel.add(detailsLabel);
        panel.add(Box.createVerticalGlue());
        panel.setPreferredSize(new Dimension(460, 520));
        return panel;
    }

    static JPanel section(String title) {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(BorderFactory.createCompoundBorder(
                new TitledBorder(title), new EmptyBorder(2, 6, 6, 6)));
        return p;
    }

    static void addRow(JPanel p, int row, String label, Component field) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = row;
        c.insets = new Insets(3, 4, 3, 4);
        c.anchor = GridBagConstraints.WEST;

        c.gridx = 0;
        c.weightx = 0;
        p.add(new JLabel(label), c);

        c.gridx = 1;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        p.add(field, c);
    }

    static JSpinner intSpinner(int min, int max, int value) {
        return new JSpinner(new SpinnerNumberModel(value, min, max, 1));
    }

    // ===================== ΕΝΕΡΓΕΙΕΣ =====================

    /** Πατήθηκε το κουμπί «Αξιολόγηση αίτησης». */
    void evaluate() {
        try {
            String name = nameField.getText().trim();
            if (name.isEmpty()) throw new IllegalArgumentException("Συμπληρώστε το ονοματεπώνυμο.");

            CreditScoreCalculator.ScoreResult sr = CreditScoreCalculator.calculate(readProfile());
            double assetsValue = parseNumber(assets, "Περιουσιακά στοιχεία", 0);
            double amountValue = parseNumber(amount, "Ποσό δανείου", 1);
            int termValue = intValue(term);
            Purpose p = (Purpose) purpose.getSelectedItem();

            Main.Application app = new Main.Application(
                    name, sr.score(), assetsValue, amountValue, termValue, p);
            Main.Decision d = Calculations.evaluate(app);
            showResult(app, sr, d);
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(),
                    "Μη έγκυρα στοιχεία", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void showResult(Main.Application app, CreditScoreCalculator.ScoreResult sr, Main.Decision d) {
        StringBuilder html = new StringBuilder("<html><table cellpadding='3'>");
        row(html, "Αιτών", esc(app.name()));
        row(html, "Σκοπός", app.purpose().label);
        row(html, "Ιστορικό", d.tier().label + " (" + sr.score() + ")");
        row(html, "Ποσό", money(app.amount()));
        row(html, "Διάρκεια", app.termYears() + " έτη");

        if (d.approved()) {
            decisionLabel.setText("✔ ΕΓΚΡΙΣΗ");
            decisionLabel.setForeground(GREEN);
            row(html, "Επιτόκιο", String.format(GR, "%.2f%%", d.rate()));
            row(html, "Μηνιαία δόση", "<b>" + money(d.monthlyPayment()) + "</b>");
            row(html, "Συνολικό κόστος", money(d.totalCost()));
            row(html, "Συνολικοί τόκοι", money(d.totalCost() - app.amount()));
        } else {
            decisionLabel.setText("✘ ΑΠΟΡΡΙΨΗ");
            decisionLabel.setForeground(RED);
            row(html, "Λόγος", "<div style='width:260px'>" + esc(d.reason()) + "</div>");
        }
        html.append("</table></html>");
        detailsLabel.setText(html.toString());
    }

    private void showPlaceholder() {
        decisionLabel.setText(" ");
        detailsLabel.setText("<html><i>Συμπληρώστε τη φόρμα και πατήστε «Αξιολόγηση αίτησης».</i></html>");
    }

    private void clearForm() {
        nameField.setText("");
        for (JSpinner s : new JSpinner[]{late30, late60, late90, inquiries, accountTypes}) s.setValue(0);
        bankrupt.setSelected(false);
        creditLimit.setText("0");
        creditUsed.setText("0");
        historyYears.setText("0");
        assets.setText("");
        amount.setText("");
        term.setValue(10);
        purpose.setSelectedIndex(0);
        updateScorePreview();
        showPlaceholder();
    }

    /** Ενημερώνει τη μπάρα credit score όσο ο χρήστης πληκτρολογεί. */
    private void updateScorePreview() {
        try {
            CreditScoreCalculator.ScoreResult sr = CreditScoreCalculator.calculate(readProfile());
            CreditTier tier = CreditTier.fromScore(sr.score());
            scoreBar.setValue(sr.score());
            scoreBar.setString(sr.score() + "  –  " + tier.label);
            scoreBar.setForeground(switch (tier) {
                case EXCELLENT, GOOD -> GREEN;
                case FAIR -> ORANGE;
                case POOR, REJECTED -> RED;
            });
            scoreBreakdown.setText(String.format(GR,
                    "Πληρωμές %.0f  |  Χρήση πίστωσης %.0f  |  Διάρκεια %.0f  |  Αιτήματα %.0f  |  Ποικιλία %.0f",
                    sr.paymentHistory(), sr.utilization(), sr.historyLength(), sr.inquiries(), sr.creditMix()));
        } catch (IllegalArgumentException ex) {
            scoreBar.setValue(CreditScoreCalculator.MIN_SCORE);
            scoreBar.setString("Ελέγξτε τα πεδία του ιστορικού");
            scoreBreakdown.setText(" ");
        }
    }

    // ===================== ΒΟΗΘΗΤΙΚΑ =====================

    private CreditScoreCalculator.CreditProfile readProfile() {
        return new CreditScoreCalculator.CreditProfile(
                intValue(late30), intValue(late60), intValue(late90), bankrupt.isSelected(),
                parseNumber(creditLimit, "Συνολικό όριο καρτών", 0),
                parseNumber(creditUsed, "Χρησιμοποιημένο υπόλοιπο", 0),
                parseNumber(historyYears, "Έτη πιστωτικού ιστορικού", 0),
                intValue(inquiries), intValue(accountTypes));
    }

    static int intValue(JSpinner s) {
        try {
            s.commitEdit();               // πάρε και ό,τι έχει πληκτρολογηθεί αλλά δεν επιβεβαιώθηκε
        } catch (ParseException ignored) {
        }
        return ((Number) s.getValue()).intValue();
    }

    static double parseNumber(JTextField f, String fieldName, double min) {
        // Στα πεδία ποσού οι τελείες είναι διαχωριστικά χιλιάδων και το κόμμα είναι υποδιαστολή
        String text = (f instanceof MoneyField)
                ? MoneyField.toPlain(f.getText())
                : f.getText().trim().replace(',', '.');
        double v;
        try {
            v = Double.parseDouble(text);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Μη έγκυρος αριθμός στο πεδίο «" + fieldName + "».");
        }
        if (v < min) {
            throw new IllegalArgumentException("Το πεδίο «" + fieldName + "» πρέπει να είναι τουλάχιστον " + min + ".");
        }
        return v;
    }

    static void onTextChange(JTextField f, Runnable action) {
        f.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { action.run(); }
            public void removeUpdate(DocumentEvent e) { action.run(); }
            public void changedUpdate(DocumentEvent e) { action.run(); }
        });
    }

    static void row(StringBuilder sb, String label, String value) {
        sb.append("<tr><td><font color='gray'>").append(label).append("</font></td><td>")
                .append(value).append("</td></tr>");
    }

    static String money(double v) {
        return String.format(GR, "%,.2f €", v);
    }

    static String esc(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}


import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class PersonalFinanceTracker {
    private JFrame frame;
    private JTextField amountField, categoryField;
    private JComboBox<String> typeBox;
    private DefaultTableModel tableModel;
    private JLabel summaryLabel;
    private List<Transaction> transactions;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(PersonalFinanceTracker::new);
    }

    public PersonalFinanceTracker() {
        transactions = new ArrayList<>();
        initializeUI();
    }

    private void initializeUI() {
        frame = new JFrame("Personal Finance Tracker");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(650, 420);
        frame.setLayout(new BorderLayout());
        frame.setLocationRelativeTo(null); // Center the frame

        // Input Panel
        JPanel inputPanel = new JPanel(new FlowLayout());

        typeBox = new JComboBox<>(new String[]{"Income", "Expense"});
        amountField = new JTextField(10);
        categoryField = new JTextField(10);
        JButton addButton = new JButton("Add");

        addButton.addActionListener(e -> addTransaction());

        inputPanel.add(new JLabel("Type:"));
        inputPanel.add(typeBox);
        inputPanel.add(new JLabel("Amount:"));
        inputPanel.add(amountField);
        inputPanel.add(new JLabel("Category:"));
        inputPanel.add(categoryField);
        inputPanel.add(addButton);

        // Table Panel
        String[] columnNames = {"Date", "Type", "Amount", "Category"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Make table non-editable
            }
        };
        JTable table = new JTable(tableModel);
        table.setFillsViewportHeight(true);
        JScrollPane tableScroll = new JScrollPane(table);

        // Summary Panel
        JPanel summaryPanel = new JPanel(new FlowLayout());
        summaryLabel = new JLabel("Monthly Summary: Income: 0.00, Expenses: 0.00");
        summaryPanel.add(summaryLabel);

        frame.add(inputPanel, BorderLayout.NORTH);
        frame.add(tableScroll, BorderLayout.CENTER);
        frame.add(summaryPanel, BorderLayout.SOUTH);

        frame.setVisible(true);
    }

    private void addTransaction() {
        String type = (String) typeBox.getSelectedItem();
        String amountText = amountField.getText().trim();
        String category = categoryField.getText().trim();

        if (amountText.isEmpty() || category.isEmpty()) {
            JOptionPane.showMessageDialog(frame, "Please fill in all fields.");
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountText);
            if (amount <= 0) {
                JOptionPane.showMessageDialog(frame, "Amount must be positive.");
                return;
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(frame, "Please enter a valid amount.");
            return;
        }

        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        Transaction transaction = new Transaction(date, type, amount, category);
        transactions.add(transaction);

        tableModel.addRow(new Object[]{date, type, String.format("%.2f", amount), category});
        updateSummary();

        // Clear input fields
        amountField.setText("");
        categoryField.setText("");
        typeBox.setSelectedIndex(0);
    }

    private void updateSummary() {
        double income = 0;
        double expense = 0;

        for (Transaction t : transactions) {
            if (t.getType().equals("Income")) {
                income += t.getAmount();
            } else {
                expense += t.getAmount();
            }
        }

        summaryLabel.setText(String.format("Monthly Summary: Income: %.2f, Expenses: %.2f", income, expense));
    }

    static class Transaction {
        private final String date;
        private final String type;
        private final double amount;
        private final String category;

        public Transaction(String date, String type, double amount, String category) {
            this.date = date;
            this.type = type;
            this.amount = amount;
            this.category = category;
        }

        public String getDate() { return date; }
        public String getType() { return type; }
        public double getAmount() { return amount; }
        public String getCategory() { return category; }
    }
}
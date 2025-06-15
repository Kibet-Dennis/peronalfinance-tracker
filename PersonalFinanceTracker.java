import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.io.*;
import java.net.*;
import org.json.JSONObject;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.data.general.DefaultPieDataset;
import org.jfree.data.category.DefaultCategoryDataset;

public class PersonalFinanceTracker {
    private JFrame frame;
    private JTextField amountField, categoryField, mpesaField;
    private JComboBox<String> typeBox, currencyBox;
    private DefaultTableModel tableModel;
    private JLabel summaryLabel, rateLabel;
    private List<Transaction> transactions;
    private Map<String, Double> exchangeRates = new HashMap<>();
    private final String BASE_CURRENCY = "KES";
    private JTable table;

    public static void main(String[] args) {
        // 🔐 Login Support: Basic username/password authentication (admin/password)
        if (!showLoginDialog()) {
            JOptionPane.showMessageDialog(null, "Login failed. Exiting.");
            System.exit(0);
        }
        SwingUtilities.invokeLater(PersonalFinanceTracker::new);
    }

    // 🔐 Login dialog method
    private static boolean showLoginDialog() {
        JPanel panel = new JPanel(new GridLayout(2, 2));
        JTextField userField = new JTextField();
        JPasswordField passField = new JPasswordField();
        panel.add(new JLabel("Username:"));
        panel.add(userField);
        panel.add(new JLabel("Password:"));
        panel.add(passField);
        int result = JOptionPane.showConfirmDialog(null, panel, "Login", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result == JOptionPane.OK_OPTION) {
            String username = userField.getText();
            String password = new String(passField.getPassword());
            return "admin".equals(username) && "password".equals(password);
        }
        return false;
    }

    public PersonalFinanceTracker() {
        transactions = new ArrayList<>();
        initializeUI();
        fetchExchangeRates();
    }

    private void initializeUI() {
        frame = new JFrame("Personal Finance Tracker");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1000, 520);
        frame.setLayout(new BorderLayout());
        frame.setLocationRelativeTo(null);

        // Input Panel
        JPanel inputPanel = new JPanel(new FlowLayout());

        typeBox = new JComboBox<>(new String[]{"Income", "Expense"});
        amountField = new JTextField(8);
        categoryField = new JTextField(8);
        mpesaField = new JTextField(10);
        mpesaField.setText("0700545425");
        currencyBox = new JComboBox<>(new String[]{"KES", "USD", "EUR", "GBP", "UGX", "TZS"});
        JButton addButton = new JButton("Add");
        JButton deleteButton = new JButton("Delete");
        JButton refreshButton = new JButton("Refresh Rates");
        JButton exportPdfButton = new JButton("Export to PDF"); // 📄 Export to PDF placeholder
        JButton showChartButton = new JButton("Show Chart");    // 📊 Data Visualization placeholder

        addButton.addActionListener(e -> addTransaction());
        deleteButton.addActionListener(e -> deleteTransaction());
        refreshButton.addActionListener(e -> fetchExchangeRates());
        exportPdfButton.addActionListener(e -> exportToPDF()); // Placeholder
        showChartButton.addActionListener(e -> showChart());   // Placeholder

        inputPanel.add(new JLabel("Type:"));
        inputPanel.add(typeBox);
        inputPanel.add(new JLabel("Amount:"));
        inputPanel.add(amountField);
        inputPanel.add(new JLabel("Currency:"));
        inputPanel.add(currencyBox);
        inputPanel.add(new JLabel("Category:"));
        inputPanel.add(categoryField);
        inputPanel.add(new JLabel("M-Pesa Number:"));
        inputPanel.add(mpesaField);
        inputPanel.add(addButton);
        inputPanel.add(deleteButton);
        inputPanel.add(refreshButton);
        inputPanel.add(exportPdfButton); // 📄
        inputPanel.add(showChartButton); // 📊

        // Table Panel
        String[] columnNames = {"Date", "Type", "Amount", "Currency", "Category", "M-Pesa Number"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table = new JTable(tableModel);
        table.setFillsViewportHeight(true);
        JScrollPane tableScroll = new JScrollPane(table);

        // Summary Panel
        JPanel summaryPanel = new JPanel(new GridLayout(2, 1));
        summaryLabel = new JLabel("Monthly Summary: Income: 0.00 KES, Expenses: 0.00 KES");
        rateLabel = new JLabel("Exchange rates updating...");
        summaryPanel.add(summaryLabel);
        summaryPanel.add(rateLabel);

        frame.add(inputPanel, BorderLayout.NORTH);
        frame.add(tableScroll, BorderLayout.CENTER);
        frame.add(summaryPanel, BorderLayout.SOUTH);

        frame.setVisible(true);
    }

    private void addTransaction() {
        String type = (String) typeBox.getSelectedItem();
        String amountText = amountField.getText().trim();
        String category = categoryField.getText().trim();
        String mpesaNumber = mpesaField.getText().trim();
        String currency = (String) currencyBox.getSelectedItem();

        if (amountText.isEmpty() || category.isEmpty() || mpesaNumber.isEmpty()) {
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
        Transaction transaction = new Transaction(date, type, amount, currency, category, mpesaNumber);
        transactions.add(transaction);

        tableModel.addRow(new Object[]{date, type, String.format("%.2f", amount), currency, category, mpesaNumber});
        updateSummary();

        amountField.setText("");
        categoryField.setText("");
        typeBox.setSelectedIndex(0);
    }

    private void deleteTransaction() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow != -1) {
            transactions.remove(selectedRow);
            tableModel.removeRow(selectedRow);
            updateSummary();
        } else {
            JOptionPane.showMessageDialog(frame, "Please select a transaction to delete.");
        }
    }

    private void updateSummary() {
        double income = 0;
        double expense = 0;

        for (Transaction t : transactions) {
            double rate = exchangeRates.getOrDefault(t.getCurrency(), 1.0);
            double amountInBase = t.getAmount() * rate;
            if (t.getType().equals("Income")) {
                income += amountInBase;
            } else {
                expense += amountInBase;
            }
        }

        summaryLabel.setText(String.format("Monthly Summary: Income: %.2f %s, Expenses: %.2f %s",
                income, BASE_CURRENCY, expense, BASE_CURRENCY));
    }

    private void fetchExchangeRates() {
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            Map<String, Double> rates = new HashMap<>();
            StringBuilder rateInfo = new StringBuilder();

            @Override
            protected Void doInBackground() {
                try {
                    // Fetch rates with KES as base
                    URL url = new URL("https://api.exchangerate.host/latest?base=KES&symbols=USD,EUR,GBP,UGX,TZS,KES");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    String inputLine;
                    StringBuilder content = new StringBuilder();
                    while ((inputLine = in.readLine()) != null) {
                        content.append(inputLine);
                    }
                    in.close();
                    conn.disconnect();

                    JSONObject json = new JSONObject(content.toString());
                    JSONObject ratesJson = json.getJSONObject("rates");
                    for (String currency : Arrays.asList("KES", "USD", "EUR", "GBP", "UGX", "TZS")) {
                        rates.put(currency, ratesJson.getDouble(currency));
                        rateInfo.append(currency).append(": ").append(ratesJson.getDouble(currency)).append("  ");
                    }
                } catch (Exception e) {
                    rateInfo.append("Failed to update rates.");
                }
                return null;
            }

            @Override
            protected void done() {
                if (!rates.isEmpty()) {
                    exchangeRates = rates;
                    rateLabel.setText("Exchange rates (KES): " + rateInfo);
                } else {
                    rateLabel.setText("Exchange rates unavailable.");
                }
                updateSummary();
            }
        };
        worker.execute();
    }

    // 📄 Export to PDF: Placeholder set — PDF generation via iText coming next
    private void exportToPDF() {
        JOptionPane.showMessageDialog(frame, "PDF export feature coming soon! (iText integration placeholder)");
    }

    // 📊 Data Visualization: Real Pie and Bar Charts with JFreeChart
    private void showChart() {
        // Pie Chart: Income vs Expenses
        double income = 0;
        double expense = 0;
        Map<String, Double> expenseByCategory = new HashMap<>();

        for (Transaction t : transactions) {
            double rate = exchangeRates.getOrDefault(t.getCurrency(), 1.0);
            double amountInBase = t.getAmount() * rate;
            if (t.getType().equals("Income")) {
                income += amountInBase;
            } else {
                expense += amountInBase;
                // Bar chart: sum by category
                expenseByCategory.put(
                    t.getCategory(),
                    expenseByCategory.getOrDefault(t.getCategory(), 0.0) + amountInBase
                );
            }
        }

        // Pie dataset
        DefaultPieDataset pieDataset = new DefaultPieDataset();
        pieDataset.setValue("Income", income);
        pieDataset.setValue("Expenses", expense);

        JFreeChart pieChart = ChartFactory.createPieChart(
            "Income vs. Expenses", pieDataset, true, true, false
        );

        // Bar dataset
        DefaultCategoryDataset barDataset = new DefaultCategoryDataset();
        for (Map.Entry<String, Double> entry : expenseByCategory.entrySet()) {
            barDataset.addValue(entry.getValue(), "Expenses", entry.getKey());
        }

        JFreeChart barChart = ChartFactory.createBarChart(
            "Expenses by Category", "Category", "Amount (" + BASE_CURRENCY + ")", barDataset
        );

        // Show both charts in dialogs
        JFrame pieFrame = new JFrame("Pie Chart");
        pieFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        pieFrame.add(new ChartPanel(pieChart));
        pieFrame.setSize(500, 400);
        pieFrame.setLocationRelativeTo(frame);
        pieFrame.setVisible(true);

        JFrame barFrame = new JFrame("Bar Chart");
        barFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        barFrame.add(new ChartPanel(barChart));
        barFrame.setSize(600, 400);
        barFrame.setLocationRelativeTo(frame);
        barFrame.setVisible(true);
    }

    // 🌍 Convert app to a Spring Boot backend + simple React/Thymeleaf frontend
    // (For web: Move logic to REST controllers, use a database, and build a web UI.)

    static class Transaction {
        private final String date;
        private final String type;
        private final double amount;
        private final String currency;
        private final String category;
        private final String mpesaNumber;

        public Transaction(String date, String type, double amount, String currency, String category, String mpesaNumber) {
            this.date = date;
            this.type = type;
            this.amount = amount;
            this.currency = currency;
            this.category = category;
            this.mpesaNumber = mpesaNumber;
        }

        public String getDate() { return date; }
        public String getType() { return type; }
        public double getAmount() { return amount; }
        public String getCurrency() { return currency; }
        public String getCategory() { return category; }
        public String getMpesaNumber() { return mpesaNumber; }
    }
}
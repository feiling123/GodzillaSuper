package core.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.LinkedHashMap;
import java.util.Map;
import org.yaml.snakeyaml.Yaml;

public class StartupModeDialog extends JDialog {

    public static class DbConfig {
        public final String dbPath;
        public final String username;
        public final String password;
        public final String operatorName;
        public final boolean isPg;   // true=PostgreSQL, false=SQLite

        public DbConfig(String dbPath, String op, boolean isPg) {
            this(dbPath, "", "", op, isPg);
        }
        public DbConfig(String dbPath, String user, String pass, String op, boolean isPg) {
            this.dbPath = dbPath;
            this.username = user;
            this.password = pass;
            this.operatorName = op;
            this.isPg = isPg;
        }
        public boolean isRemote() { return dbPath != null && !dbPath.isEmpty(); }
    }

    private DbConfig result;
    private JRadioButton localRb, remoteSqliteRb, pgRb;
    private JTextField pathField, hostField, portField, dbNameField, userField, opField;
    private JPasswordField passField;
    private JButton testBtn, launchBtn, saveBtn;

    public static DbConfig showDialog() {
        StartupModeDialog d = new StartupModeDialog(null);
        d.setVisible(true);
        return d.result;
    }

    public StartupModeDialog(JFrame owner) {
        super(owner, "GSL5 - Start", true);
        setSize(500, 550);
        setLocationRelativeTo(owner);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) { System.exit(0); }
        });

        JPanel main = new JPanel(new BorderLayout(8, 8));
        main.setBorder(BorderFactory.createEmptyBorder(12, 16, 8, 16));
        JLabel title = new JLabel("Select data source");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 14f));
        main.add(title, BorderLayout.NORTH);

        JPanel center = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(4, 0, 4, 0);

        localRb = new JRadioButton("Standalone (local data.db)", true);
        remoteSqliteRb = new JRadioButton("Team - Remote SQLite");
        pgRb = new JRadioButton("Team - PostgreSQL");
        ButtonGroup bg = new ButtonGroup();
        bg.add(localRb); bg.add(remoteSqliteRb); bg.add(pgRb);

        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        center.add(localRb, gbc);
        gbc.gridy = 1; center.add(remoteSqliteRb, gbc);
        gbc.gridy = 2; center.add(pgRb, gbc);

        gbc.gridy = 3; gbc.gridwidth = 1; gbc.gridx = 0;
        center.add(new JLabel("  DB path:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        pathField = new JTextField("\\\\127.0.0.1\\gsl5\\data.db", 28);
        pathField.setEnabled(false); center.add(pathField, gbc);

        gbc.gridy = 4; gbc.gridx = 0; gbc.weightx = 0;
        center.add(new JLabel("  Host:"), gbc);
        gbc.gridx = 1; hostField = new JTextField("127.0.0.1", 28);
        hostField.setEnabled(false); center.add(hostField, gbc);

        gbc.gridy = 5; gbc.gridx = 0;
        center.add(new JLabel("  Port:"), gbc);
        gbc.gridx = 1; portField = new JTextField("5432", 6);
        portField.setEnabled(false); center.add(portField, gbc);

        gbc.gridy = 6; gbc.gridx = 0;
        center.add(new JLabel("  DB name:"), gbc);
        gbc.gridx = 1; dbNameField = new JTextField("gsl5", 28);
        dbNameField.setEnabled(false); center.add(dbNameField, gbc);

        gbc.gridy = 7; gbc.gridx = 0;
        center.add(new JLabel("  User:"), gbc);
        gbc.gridx = 1; userField = new JTextField("gsl5", 28);
        userField.setEnabled(false); center.add(userField, gbc);

        gbc.gridy = 8; gbc.gridx = 0;
        center.add(new JLabel("  Password:"), gbc);
        gbc.gridx = 1; passField = new JPasswordField(20);
        passField.setText(""); passField.setEnabled(false);
        center.add(passField, gbc);

        gbc.gridy = 9; gbc.gridx = 0;
        center.add(new JLabel("  Operator:"), gbc);
        gbc.gridx = 1; opField = new JTextField(System.getProperty("user.name", "op"), 28);
        opField.setEnabled(false); center.add(opField, gbc);

        main.add(center, BorderLayout.CENTER);

        localRb.addActionListener(e -> toggle(false, false));
        remoteSqliteRb.addActionListener(e -> toggle(true, false));
        pgRb.addActionListener(e -> toggle(false, true));

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        testBtn = new JButton("Test Connection");
        testBtn.setEnabled(false);
        testBtn.addActionListener(e -> {
            testBtn.setEnabled(false); testBtn.setText("Testing...");
            new Thread(() -> {
                try {
                    Class.forName("org.postgresql.Driver");
                    String url = "jdbc:postgresql://" + hostField.getText().trim() + ":" + portField.getText().trim()
                            + "/" + dbNameField.getText().trim();
                    Connection c = DriverManager.getConnection(url, userField.getText().trim(),
                            new String(passField.getPassword()));
                    c.close();
                    SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "OK", "Test", JOptionPane.INFORMATION_MESSAGE));
                } catch (Exception ex) {
                    SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "Failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE));
                } finally {
                    SwingUtilities.invokeLater(() -> { testBtn.setEnabled(true); testBtn.setText("Test Connection"); });
                }
            }).start();
        });

        saveBtn = new JButton("Save Config");
        saveBtn.addActionListener(e -> {
            try {
                Map<String, Object> root;
                java.io.File f = new java.io.File("config.yaml");
                if (f.exists()) {
                    String content = new String(Files.readAllBytes(Paths.get("config.yaml")), StandardCharsets.UTF_8);
                    root = new Yaml().load(content);
                    if (root == null) root = new LinkedHashMap<>();
                } else {
                    root = new LinkedHashMap<>();
                }
                Map<String, Object> db = new LinkedHashMap<>();
                db.put("mode", localRb.isSelected() ? "local" : (remoteSqliteRb.isSelected() ? "sqlite" : "pg"));
                db.put("host", hostField.getText().trim());
                db.put("port", portField.getText().trim());
                db.put("dbName", dbNameField.getText().trim());
                db.put("user", userField.getText().trim());
                db.put("password", new String(passField.getPassword()));
                db.put("operator", opField.getText().trim());
                root.put("database", db);
                String yamlStr = new Yaml().dump(root);
                Files.write(Paths.get("config.yaml"), yamlStr.getBytes(StandardCharsets.UTF_8));
                JOptionPane.showMessageDialog(this, "Config saved.", "OK", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Save failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        launchBtn = new JButton("Launch");
        launchBtn.addActionListener(e -> {
            String op = opField.getText().trim();
            if (op.isEmpty()) op = System.getProperty("user.name", "operator");
            if (localRb.isSelected()) result = new DbConfig("", op, false);
            else if (remoteSqliteRb.isSelected()) result = new DbConfig(pathField.getText().trim(), op, false);
            else {
                String url = "jdbc:postgresql://" + hostField.getText().trim() + ":" + portField.getText().trim()
                        + "/" + dbNameField.getText().trim();
                result = new DbConfig(url, userField.getText().trim(), new String(passField.getPassword()), op, true);
            }
            dispose();
        });
        btnPanel.add(testBtn); btnPanel.add(saveBtn); btnPanel.add(launchBtn);
        main.add(btnPanel, BorderLayout.SOUTH);
        setContentPane(main);
        loadSavedConfig();
    }

    private void toggle(boolean isSqlite, boolean isPg) {
        pathField.setEnabled(isSqlite);
        hostField.setEnabled(isPg); portField.setEnabled(isPg);
        dbNameField.setEnabled(isPg); userField.setEnabled(isPg); passField.setEnabled(isPg);
        opField.setEnabled(isSqlite || isPg);
        testBtn.setEnabled(isPg);
        saveBtn.setEnabled(true);
    }

    private void loadSavedConfig() {
        try {
            java.io.File f = new java.io.File("config.yaml");
            if (!f.exists()) return;
            String content = new String(Files.readAllBytes(Paths.get("config.yaml")), StandardCharsets.UTF_8);
            Map<String, Object> root = new Yaml().load(content);
            if (root == null) return;
            Object dbObj = root.get("database");
            if (!(dbObj instanceof Map)) return;
            Map<String, Object> db = (Map<String, Object>) dbObj;
            String mode = (String) db.getOrDefault("mode", "local");
            if ("pg".equals(mode)) {
                pgRb.setSelected(true);
                toggle(false, true);
                if (db.get("host") != null) hostField.setText(db.get("host").toString());
                if (db.get("port") != null) portField.setText(db.get("port").toString());
                if (db.get("dbName") != null) dbNameField.setText(db.get("dbName").toString());
                if (db.get("user") != null) userField.setText(db.get("user").toString());
                if (db.get("password") != null) passField.setText(db.get("password").toString());
                if (db.get("operator") != null) opField.setText(db.get("operator").toString());
            } else if ("sqlite".equals(mode)) {
                remoteSqliteRb.setSelected(true);
                toggle(true, false);
                if (db.get("host") != null) pathField.setText(db.get("host").toString());
                if (db.get("operator") != null) opField.setText(db.get("operator").toString());
            }
        } catch (Exception ignored) {}
    }
}

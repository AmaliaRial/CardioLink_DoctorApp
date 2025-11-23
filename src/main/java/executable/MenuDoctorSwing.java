package executable;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.plaf.basic.BasicButtonUI;
import java.awt.*;
import java.awt.event.*;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class MenuDoctorSwing extends JFrame {

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel cards = new JPanel(cardLayout);

    private Socket socket;
    private DataOutputStream out;
    private DataInputStream in;

    private JButton btnLogin;
    private JButton btnRegister;

    public MenuDoctorSwing() {
        super("App for Doctors");
        initUI();
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                cleanupResources();
            }
        });
        SwingUtilities.invokeLater(this::showConnectDialog);
    }

    private void initUI() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(720, 560);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(new Color(90, 136, 111));
        topBar.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));

        JButton btnExit = new JButton("✖");
        btnExit.setToolTipText("Exit");
        btnExit.setForeground(Color.WHITE);
        btnExit.setBackground(new Color(200, 0, 0));
        btnExit.setOpaque(true);
        btnExit.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
        btnExit.setFocusPainted(false);
        btnExit.setUI(new BasicButtonUI());
        btnExit.addActionListener(e -> {
            cleanupResources();
            System.exit(0);
        });

        JPanel leftWrap = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        leftWrap.setOpaque(false);
        leftWrap.add(btnExit);
        topBar.add(leftWrap, BorderLayout.WEST);
        add(topBar, BorderLayout.NORTH);

        // HOME
        JPanel home = new JPanel();
        home.setLayout(new BoxLayout(home, BoxLayout.Y_AXIS));
        home.setBackground(new Color(90, 136, 111));
        home.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));

        JLabel title = new JLabel("App for Doctors", SwingConstants.CENTER);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 28f));
        title.setForeground(new Color(255, 255, 255));

        JLabel subtitle = new JLabel("CadioLink", SwingConstants.CENTER);
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        subtitle.setFont(subtitle.getFont().deriveFont(Font.PLAIN, 20f));
        subtitle.setForeground(new Color(255, 255, 255));

        JButton btnContinue = new JButton("Continue >>");
        btnContinue.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnContinue.setFont(btnContinue.getFont().deriveFont(Font.BOLD, 16f));
        btnContinue.setBackground(new Color(94, 93, 14));
        btnContinue.setForeground(Color.WHITE);
        btnContinue.setOpaque(true);
        btnContinue.setBorderPainted(false);
        btnContinue.setFocusPainted(false);
        btnContinue.setUI(new BasicButtonUI());
        btnContinue.addActionListener(e -> cardLayout.show(cards, "auth"));

        home.add(Box.createVerticalGlue());
        home.add(title);
        home.add(Box.createRigidArea(new Dimension(0, 10)));
        home.add(subtitle);
        home.add(Box.createRigidArea(new Dimension(0, 24)));
        home.add(btnContinue);
        home.add(Box.createVerticalGlue());

        // AUTH
        JPanel auth = new JPanel();
        auth.setLayout(new BoxLayout(auth, BoxLayout.Y_AXIS));
        auth.setBackground(new Color(90, 136, 111));
        auth.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));

        JLabel choose = new JLabel("Choose an option", SwingConstants.CENTER);
        choose.setAlignmentX(Component.CENTER_ALIGNMENT);
        choose.setFont(choose.getFont().deriveFont(Font.BOLD, 22f));
        choose.setForeground(Color.WHITE);

        btnLogin = new JButton("Login");
        btnLogin.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnLogin.setFont(btnLogin.getFont().deriveFont(Font.BOLD, 15f));
        btnLogin.setBackground(new Color(220, 186, 76));
        btnLogin.setForeground(Color.WHITE);
        btnLogin.setOpaque(true);
        btnLogin.setBorderPainted(false);
        btnLogin.setFocusPainted(false);
        btnLogin.setUI(new BasicButtonUI());
        btnLogin.addActionListener(e -> showLoginDialog());

        btnRegister = new JButton("Register");
        btnRegister.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnRegister.setFont(btnRegister.getFont().deriveFont(Font.BOLD, 15f));
        btnRegister.setBackground(new Color(220, 152, 76));
        btnRegister.setForeground(Color.WHITE);
        btnRegister.setOpaque(true);
        btnRegister.setBorderPainted(false);
        btnRegister.setFocusPainted(false);
        btnRegister.setUI(new BasicButtonUI());
        btnRegister.addActionListener(e -> showSignUpDialog());

        JButton btnBackHome = new JButton("Return");
        btnBackHome.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnBackHome.addActionListener(e -> cardLayout.show(cards, "home"));

        // Initially disabled until connected
        btnLogin.setEnabled(false);
        btnRegister.setEnabled(false);

        auth.add(Box.createVerticalGlue());
        auth.add(choose);
        auth.add(Box.createRigidArea(new Dimension(0, 18)));
        auth.add(btnLogin);
        auth.add(Box.createRigidArea(new Dimension(0, 10)));
        auth.add(btnRegister);
        auth.add(Box.createRigidArea(new Dimension(0, 18)));
        auth.add(btnBackHome);
        auth.add(Box.createVerticalGlue());

        cards.add(home, "home");
        cards.add(auth, "auth");
        add(cards, BorderLayout.CENTER);

        cardLayout.show(cards, "home");
    }

    private void showConnectDialog() {
        JDialog dlg = new JDialog(this, "Connect to Server", true);
        dlg.setSize(420, 220);
        dlg.setLocationRelativeTo(this);
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JTextField txtHost = new JTextField("localhost");
        JTextField txtPort = new JTextField("9000");
        JTextField txtMac = new JTextField();

        p.add(new JLabel("Server IP or hostname:"));
        p.add(txtHost);
        p.add(Box.createRigidArea(new Dimension(0, 6)));
        p.add(new JLabel("Port:"));
        p.add(txtPort);
        p.add(Box.createRigidArea(new Dimension(0, 6)));
        p.add(new JLabel("MAC address (optional for Bitalino):"));
        p.add(txtMac);

        JButton btnConnect = new JButton("Connect");
        btnConnect.addActionListener(e -> {
            String host = txtHost.getText().trim();
            String portStr = txtPort.getText().trim();
            if (!isValidIPAddress(host)) {
                JOptionPane.showMessageDialog(dlg, "Invalid IP/host", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            int port;
            try {
                port = Integer.parseInt(portStr);
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(dlg, "Invalid port", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            btnConnect.setEnabled(false);
            new SwingWorker<Void, Void>() {
                private Exception error;
                @Override
                protected Void doInBackground() {
                    try {
                        Socket s = new Socket(host, port);
                        DataOutputStream o = new DataOutputStream(s.getOutputStream());
                        DataInputStream i = new DataInputStream(s.getInputStream());

                        // Identificarse como Doctor para que el servidor cree el hilo correcto
                        o.writeUTF("Doctor");
                        o.flush();

                        synchronized (MenuDoctorSwing.this) {
                            socket = s;
                            out = o;
                            in = i;
                        }
                    } catch (Exception ex) {
                        error = ex;
                    }
                    return null;
                }

                @Override
                protected void done() {
                    btnConnect.setEnabled(true);
                    if (error != null) {
                        JOptionPane.showMessageDialog(dlg, "Connection failed: " + error.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    } else {
                        btnLogin.setEnabled(true);
                        btnRegister.setEnabled(true);
                        JOptionPane.showMessageDialog(dlg, "Connected");
                        dlg.dispose();
                    }
                }
            }.execute();
        });

        p.add(Box.createRigidArea(new Dimension(0, 8)));
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnPanel.add(btnConnect);
        p.add(btnPanel);

        dlg.setContentPane(p);
        dlg.setVisible(true);
    }

    private void showLoginDialog() {
        JDialog dlg = new JDialog(this, "Login", true);
        dlg.setSize(360, 200);
        dlg.setLocationRelativeTo(this);
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JTextField txtUser = new JTextField();
        JPasswordField txtPass = new JPasswordField();

        p.add(new JLabel("Username:"));
        p.add(txtUser);
        p.add(Box.createRigidArea(new Dimension(0, 8)));
        p.add(new JLabel("Password:"));
        p.add(txtPass);

        JButton btnDoLogin = new JButton("Login");
        btnDoLogin.addActionListener(e -> {
            final String user = txtUser.getText().trim();
            String pass = new String(txtPass.getPassword());
            if (user.isEmpty() || pass.isEmpty()) {
                JOptionPane.showMessageDialog(dlg, "Complete all fields", "Warning", JOptionPane.WARNING_MESSAGE);
                return;
            }
            btnDoLogin.setEnabled(false);
            new SwingWorker<Boolean, Void>() {
                private String serverMsg = null;
                @Override
                protected Boolean doInBackground() {
                    if (out == null || in == null) {
                        serverMsg = "No server connection";
                        return false;
                    }
                    try {
                        synchronized (out) {
                            out.writeUTF("LOGIN");
                            out.writeUTF(user);
                            out.writeUTF(pass);
                            out.flush();

                            String response = in.readUTF();
                            if ("LOGIN_RESULT".equals(response)) {
                                boolean ok = in.readBoolean();
                                serverMsg = in.readUTF();   // mensaje del servidor
                                return ok;
                            } else {
                                serverMsg = "Unexpected response: " + response;
                                return false;
                            }
                        }
                    } catch (IOException ex) {
                        serverMsg = "I/O error: " + ex.getMessage();
                        return false;
                    }
                }

                @Override
                protected void done() {
                    btnDoLogin.setEnabled(true);
                    boolean ok = false;
                    try { ok = get(); } catch (Exception ignored) {}
                    JOptionPane.showMessageDialog(dlg,
                            serverMsg == null ? (ok ? "Logged in" : "Login failed") : serverMsg);
                    if (ok) {
                        dlg.dispose();
                        SwingUtilities.invokeLater(() -> {
                            MenuDoctorSwing.this.setVisible(false);
                            Pantalla2 p2 = new Pantalla2(user);
                            p2.setVisible(true);
                        });
                    }
                }
            }.execute();
        });

        p.add(Box.createRigidArea(new Dimension(0, 8)));
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnPanel.add(btnDoLogin);
        p.add(btnPanel);

        dlg.setContentPane(p);
        dlg.setVisible(true);
    }

    private void showSignUpDialog() {
        JDialog dlg = new JDialog(this, "Sign Up (Doctor)", true);
        dlg.setSize(460, 520);
        dlg.setLocationRelativeTo(this);
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JTextField txtUser = new JTextField();
        JPasswordField txtPass = new JPasswordField();
        JTextField txtName = new JTextField();
        JTextField txtSurname = new JTextField();
        JTextField txtBirthday = new JTextField("dd-MM-yyyy");
        JComboBox<String> cmbSex = new JComboBox<>(new String[]{"MALE", "FEMALE"});
        JTextField txtEmail = new JTextField();
        JTextField txtSpecialty = new JTextField();
        JTextField txtLicense = new JTextField();
        JTextField txtDni = new JTextField();

        p.add(new JLabel("Username:")); p.add(txtUser);
        p.add(Box.createRigidArea(new Dimension(0,6)));
        p.add(new JLabel("Password (min 6 chars):")); p.add(txtPass);
        p.add(Box.createRigidArea(new Dimension(0,6)));
        p.add(new JLabel("Name:")); p.add(txtName);
        p.add(Box.createRigidArea(new Dimension(0,6)));
        p.add(new JLabel("Surname:")); p.add(txtSurname);
        p.add(Box.createRigidArea(new Dimension(0,6)));
        p.add(new JLabel("Birthday (dd-MM-yyyy):")); p.add(txtBirthday);
        p.add(Box.createRigidArea(new Dimension(0,6)));
        p.add(new JLabel("Sex:")); p.add(cmbSex);
        p.add(Box.createRigidArea(new Dimension(0,6)));
        p.add(new JLabel("Email:")); p.add(txtEmail);
        p.add(Box.createRigidArea(new Dimension(0,6)));
        p.add(new JLabel("Specialty:")); p.add(txtSpecialty);
        p.add(Box.createRigidArea(new Dimension(0,6)));
        p.add(new JLabel("License number:")); p.add(txtLicense);
        p.add(Box.createRigidArea(new Dimension(0,6)));
        p.add(new JLabel("DNI/NIE (8 digits + letter or NIE like X1234567L):")); p.add(txtDni);
        p.add(Box.createRigidArea(new Dimension(0,8)));

        JButton btnDoSign = new JButton("Register");
        btnDoSign.addActionListener(e -> {
            String user = txtUser.getText().trim();
            String pass = new String(txtPass.getPassword());
            String name = txtName.getText().trim();
            String surname = txtSurname.getText().trim();
            String birthday = txtBirthday.getText().trim();
            String sex = (String) cmbSex.getSelectedItem();
            String email = txtEmail.getText().trim();
            String specialty = txtSpecialty.getText().trim();
            String license = txtLicense.getText().trim();
            String dniRaw = txtDni.getText().trim();
            String dni = normalizeDNI(dniRaw);

            if (user.isEmpty()) { showWarn("Username cannot be empty"); return; }
            if (pass.length() < 6) { showWarn("Password must be at least 6 characters"); return; }
            if (name.isEmpty() || !name.matches("[a-zA-Z ]+")) { showWarn("Invalid name"); return; }
            if (surname.isEmpty() || !surname.matches("[a-zA-Z ]+")) { showWarn("Invalid surname"); return; }
            if (!birthday.matches("\\d{2}-\\d{2}-\\d{4}")) {
                showWarn("Invalid birthday format, use dd-MM-yyyy");
                return;
            }
            if (!isValidEmail(email)) { showWarn("Invalid email"); return; }
            if (specialty.isEmpty()) { showWarn("Specialty required"); return; }
            if (license.isEmpty()) { showWarn("License required"); return; }


            if (!validateDNI(dni)) {
                JOptionPane.showMessageDialog(MenuDoctorSwing.this,
                        "Invalid DNI format. Expected 8 digits + letter (ej: 12345678A)",
                        "Warning", JOptionPane.WARNING_MESSAGE);
                return;
            }

            btnDoSign.setEnabled(false);

            new SwingWorker<Boolean, Void>() {
                private String serverMsg = null;
                @Override
                protected Boolean doInBackground() {
                    if (out == null || in == null) {
                        serverMsg = "No server connection";
                        return false;
                    }
                    try {
                        synchronized (out) {
                            out.writeUTF("SIGNUP");
                            out.writeUTF(user);
                            out.writeUTF(pass);
                            out.writeUTF(name);
                            out.writeUTF(surname);
                            out.writeUTF(birthday);
                            out.writeUTF(sex);
                            out.writeUTF(email);
                            out.writeUTF(specialty);
                            out.writeUTF(license);
                            out.writeUTF(dni);
                            out.flush();

                            String resp = in.readUTF();
                            if ("ACK".equals(resp)) {
                                serverMsg = in.readUTF();
                                return true;
                            } else if ("ERROR".equals(resp)) {
                                serverMsg = in.readUTF();
                                return false;
                            } else {
                                serverMsg = "Unexpected response: " + resp;
                                return false;
                            }
                        }
                    } catch (IOException ex) {
                        serverMsg = "I/O error: " + ex.getMessage();
                        return false;
                    }
                }

                @Override
                protected void done() {
                    btnDoSign.setEnabled(true);
                    boolean success = false;
                    try { success = get(); } catch (Exception ignored) {}
                    JOptionPane.showMessageDialog(dlg,
                            success ? (serverMsg == null ? "Registered" : serverMsg)
                                    : (serverMsg == null ? "Failed" : serverMsg));
                    if (success) dlg.dispose();
                }
            }.execute();
        });

        p.add(btnDoSign);
        dlg.setContentPane(new JScrollPane(p));
        dlg.setVisible(true);
    }

    private void showWarn(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Warning", JOptionPane.WARNING_MESSAGE);
    }

    private static boolean isValidIPAddress(String ip) {
        if (ip == null) return false;
        if (ip.equalsIgnoreCase("localhost")) return true;
        String ipv4 = "^((25[0-5]|2[0-4]\\d|1\\d{2}|[1-9]?\\d)(\\.|$)){4}$";
        String hostname = "^[a-zA-Z0-9.-]+$";
        return Pattern.matches(ipv4, ip) || Pattern.matches(hostname, ip);
    }

    private static String normalizeDNI(String dni) {
        if (dni == null) return null;
        return dni.replaceAll("[\\s-]", "").toUpperCase();
    }

    private static boolean validateDNI(String dni) {
        if (dni == null) return false;
        return dni.matches("\\d{8}[A-Z]");
    }

    private static boolean isValidEmail(String email) {
        if (email == null) return false;
        String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$";
        return Pattern.matches(emailRegex, email);
    }

    private void cleanupResources() {
        try { if (out != null) out.close(); } catch (IOException ignored) {}
        try { if (in != null) in.close(); } catch (IOException ignored) {}
        try { if (socket != null) socket.close(); } catch (IOException ignored) {}
    }

    // Clase interna Pantalla2 con desplegable buscable de pacientes
    private class Pantalla2 extends JFrame {
        private class Patient {
            final int id;
            final String name;
            final String surname;
            Patient(int id, String name, String surname) { this.id = id; this.name = name; this.surname = surname; }
            String display() { return name + " " + surname + " (id:" + id + ")"; }
        }

        public Pantalla2(String doctorName) {
            setTitle("Doctor Menu");
            setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            setSize(800, 400);
            setLocationRelativeTo(null);
            setLayout(new BorderLayout());

            JPanel topPanel = new JPanel();
            topPanel.setBackground(new Color(90, 136, 111));
            JLabel doctorLabel = new JLabel(doctorName.toUpperCase());
            doctorLabel.setFont(new Font("SansSerif", Font.BOLD, 22));
            topPanel.add(doctorLabel);
            add(topPanel, BorderLayout.NORTH);

            JPanel centerPanel = new JPanel();
            centerPanel.setBackground(Color.WHITE);
            centerPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 60, 80));

            JButton btnRecently = new JButton("Diagnosis files to do");
            btnRecently.setPreferredSize(new Dimension(230, 110));
            btnRecently.setBackground(new Color(90, 136, 111));
            btnRecently.setFocusPainted(false);

            JButton btnSearchPatient = new JButton("Search Patient");
            btnSearchPatient.setPreferredSize(new Dimension(230, 110));
            btnSearchPatient.setBackground(new Color(90, 136, 111));
            btnSearchPatient.setFocusPainted(false);

            centerPanel.add(btnRecently);
            centerPanel.add(btnSearchPatient);
            add(centerPanel, BorderLayout.CENTER);

            JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            bottomPanel.setBackground(new Color(255, 255, 255));
            JButton btnReturn = new JButton("Return");
            btnReturn.setFocusPainted(false);
            btnReturn.addActionListener(e -> {
                dispose();
                MenuDoctorSwing.this.setVisible(true);
            });
            bottomPanel.add(btnReturn);
            add(bottomPanel, BorderLayout.SOUTH);

            addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosed(WindowEvent e) {
                    MenuDoctorSwing.this.setVisible(true);
                }
            });

            btnSearchPatient.addActionListener(e -> openSearchPatientsDialog());
        }

        private void openSearchPatientsDialog() {
            if (out == null || in == null || socket == null || socket.isClosed()) {
                JOptionPane.showMessageDialog(this, "Not connected to server", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            JDialog dlg = new JDialog(MenuDoctorSwing.this, "Search Patient", true);
            dlg.setSize(520, 360);
            dlg.setLocationRelativeTo(this);

            JPanel p = new JPanel(new BorderLayout(8,8));
            p.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));

            JPanel searchBar = new JPanel(new BorderLayout(6,0));
            JTextField txtSearch = new JTextField();
            JButton btnClear = new JButton("X");
            btnClear.setFocusable(false);
            btnClear.addActionListener(ev -> { txtSearch.setText(""); txtSearch.requestFocus(); });
            searchBar.add(new JLabel("Buscar:"), BorderLayout.WEST);
            searchBar.add(txtSearch, BorderLayout.CENTER);
            searchBar.add(btnClear, BorderLayout.EAST);

            p.add(searchBar, BorderLayout.NORTH);

            DefaultListModel<String> listModel = new DefaultListModel<>();
            JList<String> resultsList = new JList<>(listModel);
            resultsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            JScrollPane scroll = new JScrollPane(resultsList);
            p.add(scroll, BorderLayout.CENTER);

            JPanel row = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            JButton btnSelect = new JButton("Select");
            JButton btnClose = new JButton("Close");
            row.add(btnSelect);
            row.add(btnClose);
            p.add(row, BorderLayout.SOUTH);

            dlg.setContentPane(p);

            final List<Patient> allPatients = new ArrayList<>();
            final List<Patient> displayedPatients = new ArrayList<>();

            Runnable filterList = () -> {
                String q = txtSearch.getText().trim().toLowerCase();
                SwingUtilities.invokeLater(() -> {
                    listModel.clear();
                    displayedPatients.clear();
                    for (Patient pt : allPatients) {
                        String combined = (pt.name + " " + pt.surname).toLowerCase();
                        if (q.isEmpty() || combined.contains(q)) {
                            listModel.addElement(pt.display());
                            displayedPatients.add(pt);
                        }
                    }
                });
            };

            txtSearch.getDocument().addDocumentListener(new DocumentListener() {
                public void insertUpdate(DocumentEvent e) { filterList.run(); }
                public void removeUpdate(DocumentEvent e) { filterList.run(); }
                public void changedUpdate(DocumentEvent e) { filterList.run(); }
            });

            resultsList.addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 2) {
                        int idx = resultsList.getSelectedIndex();
                        if (idx >= 0 && idx < displayedPatients.size()) {
                            Patient psel = displayedPatients.get(idx);
                            JOptionPane.showMessageDialog(dlg, "Selected: " + psel.display(), "Info", JOptionPane.INFORMATION_MESSAGE);
                        }
                    }
                }
            });

            btnSelect.addActionListener(ev -> {
                int idx = resultsList.getSelectedIndex();
                if (idx < 0 || idx >= displayedPatients.size()) {
                    JOptionPane.showMessageDialog(dlg, "Seleccione un paciente", "Warning", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                Patient psel = displayedPatients.get(idx);
                JOptionPane.showMessageDialog(dlg, "Selected: " + psel.display(), "Info", JOptionPane.INFORMATION_MESSAGE);
                // Aquí se puede ampliar: pedir más datos al servidor con psel.id
            });

            btnClose.addActionListener(ev -> dlg.dispose());

            new SwingWorker<Void, Void>() {
                private String error = null;
                @Override
                protected Void doInBackground() {
                    try {
                        synchronized (out) {
                            out.writeUTF("SEARCH_PATIENTS");
                            out.flush();

                            String resp = in.readUTF();
                            if (!"PATIENT_LIST".equals(resp)) {
                                error = "Unexpected response: " + resp;
                                return null;
                            }
                            int n = in.readInt();
                            allPatients.clear();
                            for (int i = 0; i < n; i++) {
                                int id = in.readInt();
                                String name = in.readUTF();
                                String surname = in.readUTF();
                                String dni = in.readUTF(); // se recibe pero no se usa
                                allPatients.add(new Patient(id, name, surname));
                            }
                        }
                    } catch (IOException ex) {
                        error = "I/O error: " + ex.getMessage();
                    }
                    return null;
                }

                @Override
                protected void done() {
                    if (error != null) {
                        JOptionPane.showMessageDialog(dlg, error, "Error", JOptionPane.ERROR_MESSAGE);
                    } else {
                        filterList.run();
                        txtSearch.requestFocus();
                    }
                }
            }.execute();

            dlg.setVisible(true);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MenuDoctorSwing().setVisible(true));
    }
}

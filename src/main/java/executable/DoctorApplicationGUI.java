package executable;

import javax.swing.*;
import javax.swing.plaf.basic.BasicButtonUI;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.net.Socket;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class DoctorApplicationGUI extends JFrame {

    private CardLayout cardLayout;
    private JPanel cardsPanel;

    // Estados/paneles
    private static final String AUTH_PANEL = "Auth Panel";
    private static final String LOGIN_PANEL = "Login Panel";
    private static final String REGISTER_PANEL = "Register Panel";
    private static final String DOCTOR_MENU_PANEL = "Doctor Menu Panel";
    private static final String SEARCH_PATIENT_PANEL = "Search Patient Panel";
    private static final String VIEW_PATIENT_PANEL = "View Patient Panel";
    private static final String VIEW_DIAGNOSISFILE_PANEL = "View Diagnosis File Panel";
    private static final String VIEW_RECORDING_PANEL = "View Recording Panel";
    private static final String RECENTLY_FINISH_PANEL = "Recently Finish Panel";
    private static final String COMPLETE_DIAGNOSISFILE_PANEL = "Complete Diagnosis File Panel";

    private String currentState = "AUTH";

    // Paneles como atributos de clase
    private AuthPanel authPanel;
    private LoginPanel loginPanel;
    private RegisterPanel registerPanel;
    private DoctorMenuPanel doctorMenuPanel;
    private SearchPatientPanel searchPatientPanel;
    private ViewPatientPanel viewPatientPanel;
    private ViewDiagnosisFilePanel viewDiagnosisFilePanel;
    private ViewRecordingPanel viewRecordingPanel;
    private RecentlyFinishPanel recentlyFinishPanel;
    private CompleteDiagnosisFilePanel completeDiagnosisFilePanel;

    // Conexión y datos
    private Socket socket;
    private DataOutputStream out;
    private DataInputStream in;

    private String lastHost = null;
    private int lastPort = -1;
    private boolean connectedFlag = false;
    private String currentUsername = null;

    // Datos de la sesión
    private String currentPatientInfo = null;
    private String currentDiagnosisFiles = null;
    private String currentRecordingData = null;
    private String currentFragmentStates = null;
    private int currentDiagnosisFileId = -1;

    public DoctorApplicationGUI() {
        super("Doctor Application");
        initializeUI();
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 640);
        setLocationRelativeTo(null);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                cleanupResources();
            }
        });

        SwingUtilities.invokeLater(this::showConnectDialog);
    }

    private void initializeUI() {
        // Configuración del CardLayout
        cardLayout = new CardLayout();
        cardsPanel = new JPanel(cardLayout);

        // Inicializamos los paneles
        authPanel = new AuthPanel();
        loginPanel = new LoginPanel();
        registerPanel = new RegisterPanel();
        doctorMenuPanel = new DoctorMenuPanel();
        searchPatientPanel = new SearchPatientPanel();
        viewPatientPanel = new ViewPatientPanel();
        viewDiagnosisFilePanel = new ViewDiagnosisFilePanel();
        viewRecordingPanel = new ViewRecordingPanel();
        recentlyFinishPanel = new RecentlyFinishPanel();
        completeDiagnosisFilePanel = new CompleteDiagnosisFilePanel();

        // Añadimos los paneles al panel principal
        cardsPanel.add(authPanel, AUTH_PANEL);
        cardsPanel.add(loginPanel, LOGIN_PANEL);
        cardsPanel.add(new JScrollPane(registerPanel), REGISTER_PANEL);
        cardsPanel.add(doctorMenuPanel, DOCTOR_MENU_PANEL);
        cardsPanel.add(searchPatientPanel, SEARCH_PATIENT_PANEL);
        cardsPanel.add(viewPatientPanel, VIEW_PATIENT_PANEL);
        cardsPanel.add(viewDiagnosisFilePanel, VIEW_DIAGNOSISFILE_PANEL);
        cardsPanel.add(viewRecordingPanel, VIEW_RECORDING_PANEL);
        cardsPanel.add(recentlyFinishPanel, RECENTLY_FINISH_PANEL);
        cardsPanel.add(completeDiagnosisFilePanel, COMPLETE_DIAGNOSISFILE_PANEL);

        setLayout(new BorderLayout());
        add(buildTopBar(), BorderLayout.NORTH);
        add(cardsPanel, BorderLayout.CENTER);

        // Estado inicial
        changeState("AUTH");
    }

    // Método para cambiar entre paneles según el estado
    public void showPanel(String panelName) {
        cardLayout.show(cardsPanel, panelName);
    }

    // Cambiar de estado
    public void changeState(String newState) {
        this.currentState = newState;

        switch (currentState) {
            case "AUTH":
                showPanel(AUTH_PANEL);
                break;
            case "LOGIN":
                showPanel(LOGIN_PANEL);
                break;
            case "REGISTER":
                showPanel(REGISTER_PANEL);
                break;
            case "DOCTOR_MENU":
                showPanel(DOCTOR_MENU_PANEL);
                break;
            case "SEARCH_PATIENT":
                showPanel(SEARCH_PATIENT_PANEL);
                break;
            case "VIEW_PATIENT":
                showPanel(VIEW_PATIENT_PANEL);
                break;
            case "VIEW_DIAGNOSISFILE":
                showPanel(VIEW_DIAGNOSISFILE_PANEL);
                break;
            case "VIEW_RECORDING":
                showPanel(VIEW_RECORDING_PANEL);
                break;
            case "RECENTLY_FINISH":
                showPanel(RECENTLY_FINISH_PANEL);
                break;
            case "COMPLETE_DIAGNOSISFILE":
                showPanel(COMPLETE_DIAGNOSISFILE_PANEL);
                break;
            default:
                System.out.println("Unknown state: " + currentState);
        }
    }

    private JPanel buildTopBar() {
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(new Color(171, 191, 234));
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
        topBar.add(btnExit, BorderLayout.EAST);
        return topBar;
    }

    // -----------------------
    // Clases internas para cada panel
    // -----------------------

    // Panel AUTH
    class AuthPanel extends JPanel {
        private JButton btnLogin;
        private JButton btnRegister;

        public AuthPanel() {
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setBackground(new Color(171, 191, 234));
            setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));

            JLabel title = new JLabel("Doctor Application", SwingConstants.CENTER);
            title.setAlignmentX(Component.CENTER_ALIGNMENT);
            title.setFont(title.getFont().deriveFont(Font.BOLD, 28f));

            JLabel subtitle = new JLabel("CardioLink - Medical Professional", SwingConstants.CENTER);
            subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);
            subtitle.setFont(subtitle.getFont().deriveFont(Font.PLAIN, 20f));
            subtitle.setForeground(new Color(80, 80, 80));

            btnLogin = new JButton("Doctor Login");
            btnLogin.setAlignmentX(Component.CENTER_ALIGNMENT);
            btnLogin.setFont(btnLogin.getFont().deriveFont(Font.BOLD, 16f));
            btnLogin.setBackground(new Color(11, 87, 147));
            btnLogin.setForeground(Color.WHITE);
            btnLogin.setOpaque(true);
            btnLogin.setBorderPainted(false);
            btnLogin.setFocusPainted(false);
            btnLogin.setUI(new BasicButtonUI());
            btnLogin.addActionListener(e -> changeState("LOGIN"));

            btnRegister = new JButton("Doctor Sign Up");
            btnRegister.setAlignmentX(Component.CENTER_ALIGNMENT);
            btnRegister.setFont(btnRegister.getFont().deriveFont(Font.BOLD, 16f));
            btnRegister.setBackground(new Color(221, 14, 96));
            btnRegister.setForeground(Color.WHITE);
            btnRegister.setOpaque(true);
            btnRegister.setBorderPainted(false);
            btnRegister.setFocusPainted(false);
            btnRegister.setUI(new BasicButtonUI());
            btnRegister.addActionListener(e -> changeState("REGISTER"));

            btnLogin.setEnabled(false);
            btnRegister.setEnabled(false);

            add(Box.createVerticalGlue());
            add(title);
            add(Box.createRigidArea(new Dimension(0, 10)));
            add(subtitle);
            add(Box.createRigidArea(new Dimension(0, 24)));
            add(btnLogin);
            add(Box.createRigidArea(new Dimension(0, 10)));
            add(btnRegister);
            add(Box.createVerticalGlue());
        }

        public void setLoginEnabled(boolean enabled) {
            btnLogin.setEnabled(enabled);
        }

        public void setRegisterEnabled(boolean enabled) {
            btnRegister.setEnabled(enabled);
        }
    }

    // Panel LOGIN
    class LoginPanel extends JPanel {
        private JTextField loginUsername;
        private JPasswordField loginPass;

        public LoginPanel() {
            setLayout(new GridBagLayout());
            setBackground(new Color(171, 191, 234));
            setBorder(BorderFactory.createEmptyBorder(24, 36, 24, 36));
            GridBagConstraints g = new GridBagConstraints();
            g.insets = new Insets(8, 8, 8, 8);
            g.fill = GridBagConstraints.HORIZONTAL;

            JLabel loginTitle = new JLabel("DOCTOR LOGIN", SwingConstants.CENTER);
            loginTitle.setFont(loginTitle.getFont().deriveFont(Font.BOLD, 24f));
            g.gridx = 0;
            g.gridy = 0;
            g.gridwidth = 3;
            g.anchor = GridBagConstraints.CENTER;
            add(loginTitle, g);

            loginUsername = underlineField(18);
            loginPass = (JPasswordField) underlineField(new JPasswordField(18));

            g.gridwidth = 1;
            g.anchor = GridBagConstraints.WEST;
            g.weightx = 0;
            g.gridx = 0;
            g.gridy = 1;
            add(new JLabel("Username:"), g);
            g.gridx = 1;
            g.gridy = 1;
            g.weightx = 1.0;
            add(loginUsername, g);

            g.gridx = 0;
            g.gridy = 2;
            g.weightx = 0;
            add(new JLabel("Password:"), g);
            g.gridx = 1;
            g.gridy = 2;
            g.weightx = 1.0;
            add(loginPass, g);

            JButton btnLoginContinue = new JButton("Login");
            btnLoginContinue.setBackground(new Color(11, 87, 147));
            btnLoginContinue.setForeground(Color.WHITE);
            btnLoginContinue.setOpaque(true);
            btnLoginContinue.setBorderPainted(false);
            btnLoginContinue.setFocusPainted(false);
            btnLoginContinue.setUI(new BasicButtonUI());
            btnLoginContinue.addActionListener(e -> handleLoginContinue());

            g.gridx = 2;
            g.gridy = 2;
            g.weightx = 0;
            g.fill = GridBagConstraints.NONE;
            add(btnLoginContinue, g);

            JButton loginReturn = new JButton("Return");
            loginReturn.addActionListener(e -> changeState("AUTH"));
            g.gridx = 0;
            g.gridy = 3;
            g.gridwidth = 3;
            g.fill = GridBagConstraints.HORIZONTAL;
            add(loginReturn, g);
        }

        public String getUsername() {
            return loginUsername.getText().trim();
        }

        public String getPassword() {
            return String.valueOf(loginPass.getPassword()).trim();
        }

        public void clearFields() {
            loginUsername.setText("");
            loginPass.setText("");
        }
    }

    // Panel REGISTER
    class RegisterPanel extends JPanel {
        private JTextField fUsername;
        private JPasswordField fPassword;
        private JTextField fName;
        private JTextField fSurname;
        private JTextField fBirthday;
        private JTextField fSex;
        private JTextField fEmail;
        private JTextField fSpecialty;
        private JTextField fLicense;
        private JTextField fDni;

        public RegisterPanel() {
            setLayout(new GridBagLayout());
            setBackground(new Color(171, 191, 234));
            setBorder(BorderFactory.createEmptyBorder(24, 36, 24, 36));
            GridBagConstraints r = new GridBagConstraints();
            r.insets = new Insets(6, 8, 6, 8);
            r.fill = GridBagConstraints.HORIZONTAL;

            JLabel regTitle = new JLabel("SIGN UP AS DOCTOR", SwingConstants.CENTER);
            regTitle.setFont(regTitle.getFont().deriveFont(Font.BOLD, 22f));
            r.gridx = 0;
            r.gridy = 0;
            r.gridwidth = 6;
            r.anchor = GridBagConstraints.CENTER;
            add(regTitle, r);

            fUsername = underlineField(18);
            fPassword = (JPasswordField) underlineField(new JPasswordField(18));
            fName = underlineField(18);
            fSurname = underlineField(18);
            fBirthday = underlineField(10);
            fBirthday.setToolTipText("dd-MM-yyyy (ej: 31-12-1990)");
            fBirthday.setText("dd-MM-yyyy");
            fSex = underlineField(6);
            fSex.setToolTipText("MALE o FEMALE");
            fEmail = underlineField(22);
            fSpecialty = underlineField(20);
            fLicense = underlineField(15);
            fDni = underlineField(14);

            int row = 1;
            r.gridwidth = 1;
            r.anchor = GridBagConstraints.WEST;
            r.weightx = 0;

            addField("Username:", fUsername, r, row++);
            addField("Name:", fName, r, row++);
            addField("Surname:", fSurname, r, row++);
            addField("Password:", fPassword, r, row++);
            addField("DNI:", fDni, r, row++);
            addField("Birthday (dd-MM-yyyy):", fBirthday, r, row++);
            addField("Email:", fEmail, r, row++);
            addField("Sex (MALE/FEMALE):", fSex, r, row++);
            addField("Specialty:", fSpecialty, r, row++);
            addField("License Number:", fLicense, r, row++);

            JButton regCancel = new JButton("Cancel");
            JButton regCreate = new JButton("Create Account");
            regCreate.setBackground(new Color(17, 49, 85));
            regCreate.setForeground(Color.WHITE);
            regCreate.setOpaque(true);
            regCreate.setBorderPainted(false);
            regCreate.setFocusPainted(false);
            regCreate.addActionListener(e -> handleRegisterCreate());

            JPanel btnRow = new JPanel(new BorderLayout());
            btnRow.setOpaque(false);
            btnRow.add(regCancel, BorderLayout.WEST);
            btnRow.add(regCreate, BorderLayout.EAST);

            r.gridx = 0;
            r.gridy = row;
            r.gridwidth = 6;
            r.weightx = 1;
            r.fill = GridBagConstraints.HORIZONTAL;
            add(btnRow, r);

            JButton regReturn = new JButton("Return");
            regReturn.addActionListener(e -> changeState("AUTH"));
            r.gridy = ++row;
            add(regReturn, r);

            regCancel.addActionListener(e -> clearFields());
        }

        private void addField(String label, JComponent field, GridBagConstraints r, int row) {
            r.gridwidth = 1;
            r.weightx = 0;
            r.gridx = 0;
            r.gridy = row;
            add(new JLabel(label), r);
            r.gridx = 1;
            r.gridy = row;
            r.weightx = 1;
            r.gridwidth = 5;
            add(field, r);
        }

        public void clearFields() {
            fUsername.setText("");
            fName.setText("");
            fSurname.setText("");
            fPassword.setText("");
            fDni.setText("");
            fBirthday.setText("dd-MM-yyyy");
            fEmail.setText("");
            fSex.setText("");
            fSpecialty.setText("");
            fLicense.setText("");
        }

        public Map<String, String> getFormData() {
            Map<String, String> data = new HashMap<>();
            data.put("username", fUsername.getText().trim());
            data.put("name", fName.getText().trim());
            data.put("surname", fSurname.getText().trim());
            data.put("dni", fDni.getText().trim().replaceAll("[\\s-]", "").toUpperCase());
            data.put("password", String.valueOf(fPassword.getPassword()).trim());
            data.put("birthday", fBirthday.getText().trim());
            data.put("email", fEmail.getText().trim());
            data.put("sex", fSex.getText().trim().toUpperCase());
            data.put("specialty", fSpecialty.getText().trim());
            data.put("license", fLicense.getText().trim());
            return data;
        }
    }

    // Panel del menú principal del doctor
    class DoctorMenuPanel extends JPanel {
        public DoctorMenuPanel() {
            setLayout(new GridBagLayout());
            setBackground(new Color(171, 191, 234));
            GridBagConstraints g = new GridBagConstraints();
            g.insets = new Insets(20, 20, 20, 20);
            g.fill = GridBagConstraints.NONE;

            JButton searchPatientButton = new JButton("Search Patient");
            searchPatientButton.setFont(searchPatientButton.getFont().deriveFont(Font.BOLD, 20f));
            searchPatientButton.setBackground(new Color(182, 118, 45));
            searchPatientButton.setForeground(Color.WHITE);
            searchPatientButton.setOpaque(true);
            searchPatientButton.setBorderPainted(false);
            searchPatientButton.setFocusPainted(false);
            searchPatientButton.setPreferredSize(new Dimension(300, 60));
            searchPatientButton.addActionListener(e -> handleSearchPatient());

            JButton recentlyFinishButton = new JButton("Recently Finished");
            recentlyFinishButton.setFont(recentlyFinishButton.getFont().deriveFont(Font.BOLD, 20f));
            recentlyFinishButton.setBackground(new Color(182, 118, 45));
            recentlyFinishButton.setForeground(Color.WHITE);
            recentlyFinishButton.setOpaque(true);
            recentlyFinishButton.setBorderPainted(false);
            recentlyFinishButton.setFocusPainted(false);
            recentlyFinishButton.setPreferredSize(new Dimension(300, 60));
            recentlyFinishButton.addActionListener(e -> handleRecentlyFinish());

            JButton logoutButton = new JButton("Logout");
            logoutButton.setFont(logoutButton.getFont().deriveFont(Font.BOLD, 16f));
            logoutButton.setBackground(new Color(200, 0, 0));
            logoutButton.setForeground(Color.WHITE);
            logoutButton.setOpaque(true);
            logoutButton.setBorderPainted(false);
            logoutButton.setFocusPainted(false);
            logoutButton.setPreferredSize(new Dimension(150, 40));
            logoutButton.addActionListener(e -> handleLogout());

            g.gridx = 0;
            g.gridy = 0;
            g.weightx = 1.0;
            g.weighty = 1.0;
            g.anchor = GridBagConstraints.CENTER;
            add(searchPatientButton, g);

            g.gridy = 1;
            add(recentlyFinishButton, g);

            g.gridy = 2;
            g.weighty = 0;
            add(logoutButton, g);
        }
    }

    // Panel de búsqueda de pacientes
    class SearchPatientPanel extends JPanel {
        private JComboBox<String> patientComboBox;
        private DefaultComboBoxModel<String> patientModel;

        public SearchPatientPanel() {
            setLayout(new GridBagLayout());
            setBackground(new Color(171, 191, 234));
            setBorder(BorderFactory.createEmptyBorder(24, 36, 24, 36));
            GridBagConstraints g = new GridBagConstraints();
            g.insets = new Insets(8, 8, 8, 8);
            g.fill = GridBagConstraints.HORIZONTAL;

            JLabel label = new JLabel("Search for a Patient", JLabel.CENTER);
            label.setFont(label.getFont().deriveFont(Font.BOLD, 22f));
            g.gridx = 0;
            g.gridy = 0;
            g.gridwidth = 2;
            g.anchor = GridBagConstraints.CENTER;
            add(label, g);

            patientModel = new DefaultComboBoxModel<>();
            patientComboBox = new JComboBox<>(patientModel);
            patientComboBox.setPreferredSize(new Dimension(200, 30));

            g.gridwidth = 2;
            g.gridy = 1;
            g.weightx = 1.0;
            add(patientComboBox, g);

            JButton searchButton = new JButton("Select Patient");
            searchButton.setBackground(new Color(11, 87, 147));
            searchButton.setForeground(Color.WHITE);
            searchButton.setOpaque(true);
            searchButton.setBorderPainted(false);
            searchButton.setFocusPainted(false);
            searchButton.addActionListener(e -> handleSelectPatient());

            JButton refreshButton = new JButton("Refresh List");
            refreshButton.addActionListener(e -> loadPatientList());

            JButton backButton = new JButton("Back to Menu");
            backButton.addActionListener(e -> changeState("DOCTOR_MENU"));

            JPanel buttonPanel = new JPanel(new FlowLayout());
            buttonPanel.setOpaque(false);
            buttonPanel.add(searchButton);
            buttonPanel.add(refreshButton);
            buttonPanel.add(backButton);

            g.gridx = 0;
            g.gridy = 2;
            g.gridwidth = 2;
            g.weightx = 1.0;
            add(buttonPanel, g);

            // Cargar lista de pacientes al inicializar
            loadPatientList();
        }

        public String getSelectedPatient() {
            return (String) patientComboBox.getSelectedItem();
        }

        private void loadPatientList() {
            new SwingWorker<Void, Void>() {
                private String patientList = null;

                @Override
                protected Void doInBackground() {
                    try {
                        out.writeUTF("SEARCH_PATIENT");
                        out.flush();
                        patientList = in.readUTF();
                    } catch (IOException ex) {
                        patientList = "Error loading patient list";
                    }
                    return null;
                }

                @Override
                protected void done() {
                    patientModel.removeAllElements();
                    if (patientList != null && !patientList.startsWith("Error")) {
                        String[] patients = patientList.split(", ");
                        for (String patient : patients) {
                            patientModel.addElement(patient);
                        }
                    } else {
                        patientModel.addElement("No patients available");
                    }
                }
            }.execute();
        }
    }

    // Panel de visualización de paciente
    class ViewPatientPanel extends JPanel {
        private JTextArea patientInfoArea;

        public ViewPatientPanel() {
            setLayout(new BorderLayout());
            setBackground(new Color(171, 191, 234));
            setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

            JLabel label = new JLabel("Patient Information", JLabel.CENTER);
            label.setFont(label.getFont().deriveFont(Font.BOLD, 22f));
            add(label, BorderLayout.NORTH);

            patientInfoArea = new JTextArea();
            patientInfoArea.setEditable(false);
            patientInfoArea.setBackground(new Color(200, 220, 240));
            patientInfoArea.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            add(new JScrollPane(patientInfoArea), BorderLayout.CENTER);

            JPanel buttonPanel = new JPanel(new FlowLayout());
            JButton viewDiagnosisButton = new JButton("View Diagnosis Files");
            viewDiagnosisButton.setBackground(new Color(11, 87, 147));
            viewDiagnosisButton.setForeground(Color.WHITE);
            viewDiagnosisButton.setOpaque(true);
            viewDiagnosisButton.setBorderPainted(false);
            viewDiagnosisButton.setFocusPainted(false);
            viewDiagnosisButton.addActionListener(e -> handleViewDiagnosisFiles());

            JButton backButton = new JButton("Back to Search");
            backButton.addActionListener(e -> changeState("SEARCH_PATIENT"));

            buttonPanel.add(viewDiagnosisButton);
            buttonPanel.add(backButton);
            add(buttonPanel, BorderLayout.SOUTH);
        }

        public void setPatientInfo(String info) {
            patientInfoArea.setText(info);
        }
    }

    // Panel de visualización de diagnóstico
    class ViewDiagnosisFilePanel extends JPanel {
        private JList<String> diagnosisList;
        private DefaultListModel<String> diagnosisModel;

        public ViewDiagnosisFilePanel() {
            setLayout(new BorderLayout());
            setBackground(new Color(171, 191, 234));
            setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

            JLabel label = new JLabel("Diagnosis Files", JLabel.CENTER);
            label.setFont(label.getFont().deriveFont(Font.BOLD, 22f));
            add(label, BorderLayout.NORTH);

            diagnosisModel = new DefaultListModel<>();
            diagnosisList = new JList<>(diagnosisModel);
            diagnosisList.setBackground(new Color(200, 220, 240));
            add(new JScrollPane(diagnosisList), BorderLayout.CENTER);

            JPanel buttonPanel = new JPanel(new FlowLayout());
            JButton viewRecordingButton = new JButton("View Recording");
            viewRecordingButton.setBackground(new Color(11, 87, 147));
            viewRecordingButton.setForeground(Color.WHITE);
            viewRecordingButton.setOpaque(true);
            viewRecordingButton.setBorderPainted(false);
            viewRecordingButton.setFocusPainted(false);
            viewRecordingButton.addActionListener(e -> handleViewRecording());

            JButton downloadButton = new JButton("Download Diagnosis");
            downloadButton.setBackground(new Color(46, 204, 113));
            downloadButton.setForeground(Color.WHITE);
            downloadButton.setOpaque(true);
            downloadButton.setBorderPainted(false);
            downloadButton.setFocusPainted(false);
            downloadButton.addActionListener(e -> handleDownloadDiagnosis());

            JButton backButton = new JButton("Back to Patient");
            backButton.addActionListener(e -> changeState("VIEW_PATIENT"));

            buttonPanel.add(viewRecordingButton);
            buttonPanel.add(downloadButton);
            buttonPanel.add(backButton);
            add(buttonPanel, BorderLayout.SOUTH);
        }

        public void setDiagnosisFiles(String files) {
            diagnosisModel.clear();
            if (files != null && !files.isEmpty()) {
                // Parsear la lista de DiagnosisFile del formato toString()
                String[] fileArray = files.split("MedicalRecord\\{");
                for (String file : fileArray) {
                    if (!file.trim().isEmpty()) {
                        diagnosisModel.addElement("MedicalRecord{" + file.trim());
                    }
                }
            }
        }

        public String getSelectedDiagnosis() {
            return diagnosisList.getSelectedValue();
        }

        public int getSelectedDiagnosisId() {
            String selected = getSelectedDiagnosis();
            if (selected != null) {
                // Extraer ID del formato: MedicalRecord{id='123', ...}
                try {
                    String idStr = selected.split("id='")[1].split("'")[0];
                    return Integer.parseInt(idStr);
                } catch (Exception e) {
                    return -1;
                }
            }
            return -1;
        }
    }

    // Panel de visualización de grabaciones
    class ViewRecordingPanel extends JPanel {
        private JTextArea recordingView;
        private JLabel stateLabel;
        private int currentFragmentIndex = 0;
        private int currentDiagnosisFileId = -1;

        public ViewRecordingPanel() {
            setLayout(new BorderLayout());
            setBackground(new Color(171, 191, 234));
            setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

            JLabel label = new JLabel("Recording Viewer", JLabel.CENTER);
            label.setFont(label.getFont().deriveFont(Font.BOLD, 22f));
            add(label, BorderLayout.NORTH);

            recordingView = new JTextArea();
            recordingView.setEditable(false);
            recordingView.setBackground(new Color(200, 220, 240));
            recordingView.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

            stateLabel = new JLabel("Fragment states: ");
            stateLabel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

            JPanel centerPanel = new JPanel(new BorderLayout());
            centerPanel.add(new JScrollPane(recordingView), BorderLayout.CENTER);
            centerPanel.add(stateLabel, BorderLayout.SOUTH);

            add(centerPanel, BorderLayout.CENTER);

            JPanel buttonPanel = new JPanel(new FlowLayout());
            JButton prevFragmentButton = new JButton("Previous Fragment");
            prevFragmentButton.addActionListener(e -> handleChangeFragment(-1));

            JButton nextFragmentButton = new JButton("Next Fragment");
            nextFragmentButton.addActionListener(e -> handleChangeFragment(1));

            JButton downloadButton = new JButton("Download Recording");
            downloadButton.addActionListener(e -> handleDownloadRecording());

            JButton backButton = new JButton("Back to Diagnosis");
            backButton.addActionListener(e -> changeState("VIEW_DIAGNOSISFILE"));

            buttonPanel.add(prevFragmentButton);
            buttonPanel.add(nextFragmentButton);
            buttonPanel.add(downloadButton);
            buttonPanel.add(backButton);
            add(buttonPanel, BorderLayout.SOUTH);
        }

        public void setRecordingData(String data, String states, int diagnosisFileId) {
            recordingView.setText(data);
            stateLabel.setText("Fragment states: " + states + " | Current fragment: " + currentFragmentIndex);
            currentDiagnosisFileId = diagnosisFileId;
            currentFragmentStates = states;
        }

        public void setFragmentStates(String states) {
            stateLabel.setText("Fragment states: " + states + " | Current fragment: " + currentFragmentIndex);
            currentFragmentStates = states;
        }
    }

    // Panel de recientemente terminados
    class RecentlyFinishPanel extends JPanel {
        private JList<String> recentList;
        private DefaultListModel<String> recentModel;

        public RecentlyFinishPanel() {
            setLayout(new BorderLayout());
            setBackground(new Color(171, 191, 234));
            setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

            JLabel label = new JLabel("Recently Finished Diagnoses", JLabel.CENTER);
            label.setFont(label.getFont().deriveFont(Font.BOLD, 22f));
            add(label, BorderLayout.NORTH);

            recentModel = new DefaultListModel<>();
            recentList = new JList<>(recentModel);
            recentList.setBackground(new Color(200, 220, 240));

            add(new JScrollPane(recentList), BorderLayout.CENTER);

            JPanel buttonPanel = new JPanel(new FlowLayout());
            JButton completeButton = new JButton("Complete Selected");
            completeButton.setBackground(new Color(46, 204, 113));
            completeButton.setForeground(Color.WHITE);
            completeButton.setOpaque(true);
            completeButton.setBorderPainted(false);
            completeButton.setFocusPainted(false);
            completeButton.addActionListener(e -> handleCompleteDiagnosis());

            JButton refreshButton = new JButton("Refresh");
            refreshButton.addActionListener(e -> loadRecentlyFinished());

            JButton backButton = new JButton("Back to Menu");
            backButton.addActionListener(e -> changeState("DOCTOR_MENU"));

            buttonPanel.add(completeButton);
            buttonPanel.add(refreshButton);
            buttonPanel.add(backButton);
            add(buttonPanel, BorderLayout.SOUTH);

            // Cargar lista al inicializar
            loadRecentlyFinished();
        }

        public String getSelectedDiagnosis() {
            return recentList.getSelectedValue();
        }

        public int getSelectedDiagnosisId() {
            String selected = getSelectedDiagnosis();
            if (selected != null) {
                // Extraer ID del formato: MedicalRecord{id='123', ...}
                try {
                    String idStr = selected.split("id='")[1].split("'")[0];
                    return Integer.parseInt(idStr);
                } catch (Exception e) {
                    return -1;
                }
            }
            return -1;
        }

        private void loadRecentlyFinished() {
            new SwingWorker<Void, Void>() {
                private String recentData = null;

                @Override
                protected Void doInBackground() {
                    try {
                        out.writeUTF("RECENTLY_FINISH");
                        out.flush();
                        String response = in.readUTF();
                        if ("RECENTLY_FINISH_LIST".equals(response)) {
                            recentData = in.readUTF();
                        }
                    } catch (IOException ex) {
                        recentData = "Error loading recent diagnoses";
                    }
                    return null;
                }

                @Override
                protected void done() {
                    recentModel.clear();
                    if (recentData != null && !recentData.startsWith("Error")) {
                        // Parsear la lista de DiagnosisFile del formato toString()
                        String[] diagnoses = recentData.split("MedicalRecord\\{");
                        for (String diagnosis : diagnoses) {
                            if (!diagnosis.trim().isEmpty()) {
                                recentModel.addElement("MedicalRecord{" + diagnosis.trim());
                            }
                        }
                    } else {
                        recentModel.addElement("No recent diagnoses available");
                    }
                }
            }.execute();
        }
    }

    // Panel de completar diagnóstico
    class CompleteDiagnosisFilePanel extends JPanel {
        private JTextArea diagnosisText;

        public CompleteDiagnosisFilePanel() {
            setLayout(new BorderLayout());
            setBackground(new Color(171, 191, 234));
            setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

            JLabel label = new JLabel("Complete Diagnosis Report", JLabel.CENTER);
            label.setFont(label.getFont().deriveFont(Font.BOLD, 22f));
            add(label, BorderLayout.NORTH);

            diagnosisText = new JTextArea(15, 50);
            diagnosisText.setText("Enter diagnosis details here...\n\nObservations:\n\nTreatment Plan:\n\nMedications:\n\nFollow-up:");
            diagnosisText.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            diagnosisText.setLineWrap(true);
            diagnosisText.setWrapStyleWord(true);

            add(new JScrollPane(diagnosisText), BorderLayout.CENTER);

            JPanel buttonPanel = new JPanel(new FlowLayout());
            JButton saveButton = new JButton("Save Diagnosis");
            saveButton.setBackground(new Color(46, 204, 113));
            saveButton.setForeground(Color.WHITE);
            saveButton.setOpaque(true);
            saveButton.setBorderPainted(false);
            saveButton.setFocusPainted(false);
            saveButton.addActionListener(e -> handleSaveDiagnosis());

            JButton backButton = new JButton("Back to Recently Finished");
            backButton.addActionListener(e -> changeState("RECENTLY_FINISH"));

            buttonPanel.add(saveButton);
            buttonPanel.add(backButton);
            add(buttonPanel, BorderLayout.SOUTH);
        }

        public String getDiagnosisText() {
            return diagnosisText.getText().trim();
        }

        public void setDiagnosisText(String text) {
            diagnosisText.setText(text);
        }
    }

    // -----------------------
    // Handlers (Action logic)
    // -----------------------

    private void handleLoginContinue() {
        String username = loginPanel.getUsername();
        String pass = loginPanel.getPassword();

        if (username.isBlank() || pass.isBlank()) {
            JOptionPane.showMessageDialog(this, "Complete all fields", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }

        new SwingWorker<Void, Void>() {
            private String serverMsg = null;
            private boolean success = false;

            @Override
            protected Void doInBackground() {
                try {
                    out.writeUTF("LOGIN");
                    out.writeUTF(username);
                    out.writeUTF(pass);
                    out.flush();

                    String response = in.readUTF();
                    if ("LOGIN_RESULT".equals(response)) {
                        success = in.readBoolean();
                        serverMsg = serverMsg+ "->"+ in.readUTF();
                        serverMsg = in.readUTF();
                        if (success) currentUsername = username;
                    } else {
                        serverMsg = "Unexpected server response: " + response;
                    }
                } catch (IOException ex) {
                    serverMsg = "Connection error: " + ex.getMessage();
                }
                return null;
            }

            @Override
            protected void done() {
                if (success) {
                    JOptionPane.showMessageDialog(DoctorApplicationGUI.this,
                            serverMsg == null ? "Login successful" : serverMsg,
                            "Success", JOptionPane.INFORMATION_MESSAGE);
                    changeState("DOCTOR_MENU");
                    loginPanel.clearFields();
                } else {
                    JOptionPane.showMessageDialog(DoctorApplicationGUI.this,
                            "Login failed: " + (serverMsg == null ? "unknown error" : serverMsg),
                            "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void handleRegisterCreate() {
        Map<String, String> formData = registerPanel.getFormData();

        // Validaciones básicas
        if (formData.values().stream().anyMatch(String::isBlank)) {
            JOptionPane.showMessageDialog(this, "All fields are required", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Validar formato de fecha
        if (!formData.get("birthday").matches("\\d{2}-\\d{2}-\\d{4}")) {
            JOptionPane.showMessageDialog(this, "Birthday must be in dd-MM-yyyy format", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Validar sexo
        if (!formData.get("sex").equals("MALE") && !formData.get("sex").equals("FEMALE")) {
            JOptionPane.showMessageDialog(this, "Sex must be MALE or FEMALE", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }

        new SwingWorker<Void, Void>() {
            private String msg = null;
            private boolean ok = false;

            @Override
            protected Void doInBackground() {
                try {
                    out.writeUTF("SIGNUP");
                    out.writeUTF(formData.get("username"));
                    out.writeUTF(formData.get("password"));
                    out.writeUTF(formData.get("name"));
                    out.writeUTF(formData.get("surname"));
                    out.writeUTF(formData.get("birthday"));
                    out.writeUTF(formData.get("sex"));
                    out.writeUTF(formData.get("email"));
                    out.writeUTF(formData.get("specialty"));
                    out.writeUTF(formData.get("license"));
                    out.writeUTF(formData.get("dni"));
                    out.flush();

                    String resp = in.readUTF();
                    if ("ACK".equals(resp)) {
                        ok = true;
                        msg = in.readUTF();
                        msg=msg+ "->" + in.readUTF();
                    } else if ("ERROR".equals(resp)) {
                        msg = in.readUTF();
                    } else {
                        msg = "Unexpected server response: " + resp;
                    }
                } catch (IOException ex) {
                    msg = "Connection error: " + ex.getMessage();
                }
                return null;
            }

            @Override
            protected void done() {
                if (ok) {
                    JOptionPane.showMessageDialog(DoctorApplicationGUI.this,
                            msg != null ? msg : "Account created successfully",
                            "Success", JOptionPane.INFORMATION_MESSAGE);
                    changeState("DOCTOR_MENU");
                    registerPanel.clearFields();
                } else {
                    JOptionPane.showMessageDialog(DoctorApplicationGUI.this,
                            "Registration failed: " + (msg != null ? msg : "Unknown error"),
                            "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void handleSearchPatient() {
        try {
            out.writeUTF("SEARCH_PATIENT");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        changeState("SEARCH_PATIENT");
    }

    private void handleSelectPatient() {
        String selectedPatient = searchPatientPanel.getSelectedPatient();
        if (selectedPatient == null || selectedPatient.equals("No patients available")) {
            JOptionPane.showMessageDialog(this, "Please select a patient", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }

        new SwingWorker<Void, Void>() {
            private String patientInfo = null;
            private boolean success = false;

            @Override
            protected Void doInBackground() {
                try {
                    out.writeUTF("VIEW_PATIENT");
                    // Extraer HIN del string del paciente
                    String hin = selectedPatient.replaceAll(".*?([0-9]+).*", "$1");
                    out.writeInt(Integer.parseInt(hin));
                    out.flush();

                    String response = in.readUTF();
                    if ("PATIENT_OVERVIEW_SENT".equals(response)) {
                        patientInfo = in.readUTF();
                        success = true;
                    } else {
                        patientInfo = "Server error: " + response;
                    }
                } catch (IOException ex) {
                    patientInfo = "Error retrieving patient information: " + ex.getMessage();
                } catch (NumberFormatException ex) {
                    patientInfo = "Invalid patient format";
                }
                return null;
            }

            @Override
            protected void done() {
                if (success) {
                    viewPatientPanel.setPatientInfo(patientInfo);
                    currentPatientInfo = patientInfo;
                    changeState("VIEW_PATIENT");
                } else {
                    JOptionPane.showMessageDialog(DoctorApplicationGUI.this,
                            patientInfo, "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void handleViewDiagnosisFiles() {
        // Asumimos que la información del paciente ya incluye los diagnósticos
        // según la implementación del servidor en doSelectPatientAndShowInfo()
        if (currentPatientInfo != null) {
            viewDiagnosisFilePanel.setDiagnosisFiles(currentPatientInfo);
            changeState("VIEW_DIAGNOSISFILE");
        } else {
            JOptionPane.showMessageDialog(this, "No patient information available", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleViewRecording() {
        int diagnosisId = viewDiagnosisFilePanel.getSelectedDiagnosisId();
        if (diagnosisId == -1) {
            JOptionPane.showMessageDialog(this, "Please select a diagnosis file", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }

        new SwingWorker<Void, Void>() {
            private String recordingData = null;
            private String fragmentStates = null;
            private boolean success = false;

            @Override
            protected Void doInBackground() {
                try {
                    out.writeUTF("VIEW_RECORDING");
                    out.writeUTF(diagnosisId + ",0"); // Empezar con el fragmento 0
                    out.flush();

                    recordingData = in.readUTF();
                    fragmentStates = in.readUTF();
                    success = true;
                } catch (IOException ex) {
                    recordingData = "Error loading recording: " + ex.getMessage();
                }
                return null;
            }

            @Override
            protected void done() {
                if (success) {
                    viewRecordingPanel.setRecordingData(recordingData, fragmentStates, diagnosisId);
                    currentRecordingData = recordingData;
                    currentFragmentStates = fragmentStates;
                    changeState("VIEW_RECORDING");
                } else {
                    JOptionPane.showMessageDialog(DoctorApplicationGUI.this,
                            recordingData, "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void handleChangeFragment(int direction) {
        if (currentDiagnosisFileId == -1) {
            JOptionPane.showMessageDialog(this, "No recording loaded", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        new SwingWorker<Void, Void>() {
            private String fragmentData = null;
            private boolean success = false;

            @Override
            protected Void doInBackground() {
                try {
                    // Calcular nuevo índice
                    viewRecordingPanel.currentFragmentIndex += direction;
                    if (viewRecordingPanel.currentFragmentIndex < 0) {
                        viewRecordingPanel.currentFragmentIndex = 0;
                    }

                    out.writeUTF("CHANGE_FRAGMENT");
                    out.writeUTF(currentDiagnosisFileId + "," + viewRecordingPanel.currentFragmentIndex);
                    out.flush();

                    fragmentData = in.readUTF();
                    success = true;
                } catch (IOException ex) {
                    fragmentData = "Error changing fragment: " + ex.getMessage();
                    viewRecordingPanel.currentFragmentIndex -= direction; // Revertir cambio
                }
                return null;
            }

            @Override
            protected void done() {
                if (success) {
                    viewRecordingPanel.setRecordingData(fragmentData, currentFragmentStates, currentDiagnosisFileId);
                } else {
                    JOptionPane.showMessageDialog(DoctorApplicationGUI.this,
                            fragmentData, "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void handleDownloadDiagnosis() {
        int diagnosisId = viewDiagnosisFilePanel.getSelectedDiagnosisId();
        if (diagnosisId == -1) {
            JOptionPane.showMessageDialog(this, "Please select a diagnosis file", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }

        new SwingWorker<Void, Void>() {
            private boolean success = false;

            @Override
            protected Void doInBackground() {
                try {
                    out.writeUTF("DOWNLOAD_DIAGNOSISFILE");
                    out.writeUTF(String.valueOf(diagnosisId));
                    out.flush();

                    String response = in.readUTF();
                    if ("DOWNLOAD_DIAGNOSISFILE_STARTED".equals(response)) {
                        String diagnosisContent = in.readUTF();
                        saveToFile("diagnosis_" + diagnosisId + "_" + LocalDate.now() + ".txt", diagnosisContent);
                        success = true;
                    }
                } catch (IOException ex) {
                    // Error ya manejado en saveToFile
                }
                return null;
            }

            @Override
            protected void done() {
                if (success) {
                    JOptionPane.showMessageDialog(DoctorApplicationGUI.this,
                            "Diagnosis downloaded successfully", "Success", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(DoctorApplicationGUI.this,
                            "Error downloading diagnosis", "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void handleDownloadRecording() {
        int diagnosisId = viewDiagnosisFilePanel.getSelectedDiagnosisId();
        if (diagnosisId == -1) {
            JOptionPane.showMessageDialog(this, "Please select a diagnosis file", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }

        new SwingWorker<Void, Void>() {
            private boolean success = false;

            @Override
            protected Void doInBackground() {
                try {
                    out.writeUTF("DOWNLOAD_RECORDING");
                    out.writeUTF(String.valueOf(diagnosisId));
                    out.flush();

                    String response = in.readUTF();
                    if ("DOWNLOAD_RECORDING_STARTED".equals(response)) {
                        String ecgData = in.readUTF();
                        String edaData = in.readUTF();
                        String finishResponse = in.readUTF();

                        if ("DOWNLOAD_FINISHED".equals(finishResponse)) {
                            String content = "ECG Data:\n" + ecgData + "\n\nEDA Data:\n" + edaData;
                            saveToFile("recording_" + diagnosisId + "_" + LocalDate.now() + ".txt", content);
                            success = true;
                        }
                    }
                } catch (IOException ex) {
                    // Error ya manejado en saveToFile
                }
                return null;
            }

            @Override
            protected void done() {
                if (success) {
                    JOptionPane.showMessageDialog(DoctorApplicationGUI.this,
                            "Recording downloaded successfully", "Success", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(DoctorApplicationGUI.this,
                            "Error downloading recording", "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void handleRecentlyFinish() {
        changeState("RECENTLY_FINISH");
    }

    private void handleCompleteDiagnosis() {
        int diagnosisId = recentlyFinishPanel.getSelectedDiagnosisId();
        if (diagnosisId == -1) {
            JOptionPane.showMessageDialog(this, "Please select a diagnosis to complete", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }

        currentDiagnosisFileId = diagnosisId;
        completeDiagnosisFilePanel.setDiagnosisText("Enter diagnosis details for file ID: " + diagnosisId + "\n\nObservations:\n\nTreatment Plan:\n\nMedications:\n\nFollow-up:");
        changeState("COMPLETE_DIAGNOSISFILE");
    }

    private void handleSaveDiagnosis() {
        String diagnosis = completeDiagnosisFilePanel.getDiagnosisText();
        if (diagnosis.isBlank() || diagnosis.contains("Enter diagnosis details")) {
            JOptionPane.showMessageDialog(this, "Please enter diagnosis details", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }

        new SwingWorker<Void, Void>() {
            private boolean success = false;
            private String message = null;

            @Override
            protected Void doInBackground() {
                try {
                    out.writeUTF("COMPLETE_DIAGNOSISFILE");
                    out.writeUTF(String.valueOf(currentDiagnosisFileId));
                    out.writeUTF(diagnosis);
                    out.flush();

                    String response = in.readUTF();
                    if ("COMPLETE_DIAGNOSISFILE_SAVED".equals(response)) {
                        success = true;
                        message = "Diagnosis saved successfully";
                    } else {
                        message = "Server error: " + response;
                    }
                } catch (IOException ex) {
                    message = "Error saving diagnosis: " + ex.getMessage();
                }
                return null;
            }

            @Override
            protected void done() {
                if (success) {
                    JOptionPane.showMessageDialog(DoctorApplicationGUI.this,
                            message, "Success", JOptionPane.INFORMATION_MESSAGE);
                    changeState("RECENTLY_FINISH");
                } else {
                    JOptionPane.showMessageDialog(DoctorApplicationGUI.this,
                            message, "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void handleLogout() {
        try {
            out.writeUTF("LOG_OUT");
            out.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        currentUsername = null;
        //cleanupResources();
        changeState("AUTH");
    }

    // -----------------------
    // Métodos de utilidad
    // -----------------------

    private void saveToFile(String filename, String content) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
            writer.write(content);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Error saving file: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // -----------------------
    // Métodos de conexión
    // -----------------------

    private void showConnectDialog() {
        JDialog dlg = new JDialog(this, "Connect to Server", true);
        dlg.setSize(420, 200);
        dlg.setLocationRelativeTo(this);
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JTextField txtHost = new JTextField(lastHost == null ? "localhost" : lastHost);
        JTextField txtPort = new JTextField(lastPort <= 0 ? "9000" : String.valueOf(lastPort));

        p.add(new JLabel("Server IP or hostname:"));
        p.add(txtHost);
        p.add(Box.createRigidArea(new Dimension(0, 6)));
        p.add(new JLabel("Port:"));
        p.add(txtPort);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnConnect = new JButton("Connect");
        JButton btnCancel = new JButton("Cancel");
        btnPanel.add(btnCancel);
        btnPanel.add(btnConnect);
        p.add(Box.createRigidArea(new Dimension(0, 8)));
        p.add(btnPanel);

        final JLabel status = new JLabel(" ");
        status.setForeground(Color.DARK_GRAY);
        p.add(status);

        btnCancel.addActionListener(e -> dlg.dispose());

        btnConnect.addActionListener(e -> {
            String host = txtHost.getText().trim();
            String portStr = txtPort.getText().trim();
            if (!isValidIPAddress(host)) {
                JOptionPane.showMessageDialog(dlg, "Invalid host", "Error", JOptionPane.ERROR_MESSAGE);
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
            status.setText("Connecting...");
            new SwingWorker<Void, Void>() {
                private String msg = null;
                private boolean ok = false;

                @Override
                protected Void doInBackground() {
                    try {
                        connectToServer(host, port);
                        ok = true;
                        msg = "Connected to server";
                    } catch (IOException ex) {
                        msg = "Connect failed: " + ex.getMessage();
                        cleanupResources();
                    }
                    return null;
                }

                @Override
                protected void done() {
                    btnConnect.setEnabled(true);
                    status.setText(msg);
                    if (ok) {
                        lastHost = host;
                        lastPort = port;
                        connectedFlag = true;
                        authPanel.setLoginEnabled(true);
                        authPanel.setRegisterEnabled(true);
                        JOptionPane.showMessageDialog(dlg, "Connected!", "Info", JOptionPane.INFORMATION_MESSAGE);
                        dlg.dispose();
                    } else {
                        JOptionPane.showMessageDialog(dlg, msg, "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }.execute();
        });

        dlg.setContentPane(p);
        dlg.setResizable(false);
        dlg.setVisible(true);
    }

    private synchronized void connectToServer(String host, int port) throws IOException {
        cleanupResources();
        socket = new Socket(host, port);
        out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
        in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
        out.writeUTF("Doctor");
        out.flush();
        lastHost = host;
        lastPort = port;
        connectedFlag = true;
    }

    private void cleanupResources() {
        try { if (out != null) out.close(); } catch (IOException ignored) {}
        try { if (in != null) in.close(); } catch (IOException ignored) {}
        try { if (socket != null) socket.close(); } catch (IOException ignored) {}
        out = null;
        in = null;
        socket = null;
        connectedFlag = false;
    }

    // Métodos de utilidad estáticos
    private static boolean isValidIPAddress(String ip) {
        if (ip == null) return false;
        if (ip.equalsIgnoreCase("localhost")) return true;
        String ipv4 = "^((25[0-5]|2[0-4]\\d|1\\d{2}|[1-9]?\\d)(\\.|$)){4}$";
        String hostname = "^[a-zA-Z0-9.-]+$";
        return Pattern.matches(ipv4, ip) || Pattern.matches(hostname, ip);
    }

    private static JTextField underlineField(int columns) {
        JTextField f = new JTextField(columns);
        f.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, new Color(0,0,0,90)));
        f.setOpaque(false);
        return f;
    }

    private static JComponent underlineField(JComponent f) {
        f.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, new Color(0,0,0,90)));
        f.setOpaque(false);
        return f;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new DoctorApplicationGUI().setVisible(true));
    }
}
package executable;

import pojos.DiagnosisFile;
import pojos.Patient;

import javax.swing.*;
import javax.swing.plaf.basic.BasicButtonUI;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.lang.reflect.Method;
import java.net.Socket;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
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
    private Patient currentPatient =  null;
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
                try {
                    System.out.println(in.available());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                showPanel(SEARCH_PATIENT_PANEL);
                try {
                    System.out.println(in.available());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                try {
                   searchPatientPanel.loadPatientList();
               } catch (Exception ignored){
                   System.out.println("error");
               }
                try {
                    System.out.println(in.available());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
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

            JButton backButton = new JButton("Back to Menu");
            backButton.addActionListener(e -> handleBackToMenuFromSearchPatient());

            JPanel buttonPanel = new JPanel(new FlowLayout());
            buttonPanel.setOpaque(false);
            buttonPanel.add(searchButton);
            buttonPanel.add(backButton);

            g.gridx = 0;
            g.gridy = 2;
            g.gridwidth = 2;
            g.weightx = 1.0;
            add(buttonPanel, g);

            // Cargar lista de pacientes al inicializar
            //loadPatientList();
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
                        //out.writeUTF("SEARCH_PATIENT");
                        //out.flush();
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

        private static String[] getAllHIN(DataOutputStream out, DataInputStream in) throws IOException {
            out.writeUTF("SEARCH_PATIENT");
            out.flush();
            String listHIN = null;
            listHIN = in.readUTF();
            if (listHIN == null || listHIN.isBlank()) {
                return new String[0];
            }
            listHIN = listHIN.trim();
            if (listHIN.isEmpty()) {
                return new String[0];
            }
            String[] hinArray = listHIN.split("\\s*,\\s*");
            return Arrays.stream(hinArray).map(String::trim).toArray(String[]::new);
        }
    }

    // Panel de visualización de paciente
    class ViewPatientPanel extends JPanel {
        private JLabel nameLabel;
        private JLabel dobLabel;
        private JLabel hinLabel;
        private JLabel sexLabel;
        private JList<String> diagnosisList;
        private DefaultListModel<String> diagnosisModel;

        public ViewPatientPanel() {
            setLayout(new BorderLayout(20, 20));
            setBackground(new Color(171, 191, 234));
            setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

            // Panel superior con información del paciente
            JPanel patientInfoPanel = createPatientInfoPanel();
            add(patientInfoPanel, BorderLayout.NORTH);

            // Panel central con lista de diagnósticos
            JPanel diagnosisPanel = createDiagnosisPanel();
            add(diagnosisPanel, BorderLayout.CENTER);

            // Panel inferior con botones
            JPanel buttonPanel = createButtonPanel();
            add(buttonPanel, BorderLayout.SOUTH);
        }

        private JPanel createPatientInfoPanel() {
            JPanel panel = new JPanel(new GridBagLayout());
            panel.setBackground(new Color(171, 191, 234));
            panel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createTitledBorder("Patient Information"),
                    BorderFactory.createEmptyBorder(10, 10, 10, 10)
            ));

            GridBagConstraints g = new GridBagConstraints();
            g.insets = new Insets(5, 5, 5, 5);
            g.anchor = GridBagConstraints.WEST;

            // Name
            g.gridx = 0; g.gridy = 0;
            panel.add(new JLabel("Name:"), g);
            g.gridx = 1;
            nameLabel = new JLabel();
            nameLabel.setFont(nameLabel.getFont().deriveFont(Font.BOLD));
            panel.add(nameLabel, g);

            // Date of Birth
            g.gridx = 0; g.gridy = 1;
            panel.add(new JLabel("Date of Birth:"), g);
            g.gridx = 1;
            dobLabel = new JLabel();
            panel.add(dobLabel, g);

            // Health Insurance Number
            g.gridx = 0; g.gridy = 2;
            panel.add(new JLabel("Insurance Number:"), g);
            g.gridx = 1;
            hinLabel = new JLabel();
            hinLabel.setFont(hinLabel.getFont().deriveFont(Font.BOLD));
            panel.add(hinLabel, g);

            // Sex
            g.gridx = 0; g.gridy = 3;
            panel.add(new JLabel("Sex:"), g);
            g.gridx = 1;
            sexLabel = new JLabel();
            panel.add(sexLabel, g);

            return panel;
        }

        private JPanel createDiagnosisPanel() {
            JPanel panel = new JPanel(new BorderLayout());
            panel.setBackground(new Color(171, 191, 234));
            panel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createTitledBorder("Diagnosis History"),
                    BorderFactory.createEmptyBorder(10, 10, 10, 10)
            ));

            diagnosisModel = new DefaultListModel<>();
            diagnosisList = new JList<>(diagnosisModel);
            diagnosisList.setBackground(Color.WHITE);
            diagnosisList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            diagnosisList.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));

            JScrollPane scrollPane = new JScrollPane(diagnosisList);
            scrollPane.setPreferredSize(new Dimension(400, 200));
            panel.add(scrollPane, BorderLayout.CENTER);

            return panel;
        }

        private JPanel createButtonPanel() {
            JPanel panel = new JPanel(new FlowLayout());
            panel.setBackground(new Color(171, 191, 234));

            JButton viewDiagnosisButton = new JButton("View Diagnosis File");
            viewDiagnosisButton.setBackground(new Color(11, 87, 147));
            viewDiagnosisButton.setForeground(Color.WHITE);
            viewDiagnosisButton.setOpaque(true);
            viewDiagnosisButton.setBorderPainted(false);
            viewDiagnosisButton.setFocusPainted(false);
            viewDiagnosisButton.addActionListener(e -> handleViewDiagnosisFile());

            JButton backButton = new JButton("Back to Search");
            backButton.addActionListener(e -> handleBackToSearchPatientFromViewPatient());

            panel.add(viewDiagnosisButton);
            panel.add(backButton);

            return panel;
        }

        public void setPatientInfo(Patient patient) {
            // Actualizar información del paciente
            nameLabel.setText(patient.getNamePatient() + " " + patient.getSurnamePatient());
            dobLabel.setText(String.format("%02d-%02d-%d",
                    patient.getDobPatient().getDay(),
                    patient.getDobPatient().getMonth(),
                    patient.getDobPatient().getYear()));
            hinLabel.setText(String.valueOf(patient.getHealthInsuranceNumberPatient()));
            sexLabel.setText(patient.getSexPatient().toString());

            // Limpiar y actualizar lista de diagnósticos
            diagnosisModel.clear();
            if (patient.getDiagnosisList() != null && !patient.getDiagnosisList().isEmpty()) {
                for (DiagnosisFile df : patient.getDiagnosisList()) {
                    String diagnosisEntry ="Date:"+
                            df.getDate().getMonth()+
                            df.getDate().getDayOfMonth()+
                            df.getDate().getYear()+
                            " | Diagnosis:"+
                            df.getDiagnosis();
                    diagnosisModel.addElement(diagnosisEntry);
                }
            } else {
                diagnosisModel.addElement("No diagnosis records found");
            }
        }

        public int getSelectedDiagnosisIndex() {
            return diagnosisList.getSelectedIndex();
        }

    }


    // Panel de visualización de diagnóstico
    class ViewDiagnosisFilePanel extends JPanel {
        // patient labels
        private JLabel pNameLabel;
        private JLabel pDobLabel;
        private JLabel pHinLabel;
        private JLabel pSexLabel;

        // diagnosis labels
        private JLabel symptomsLabel;
        private JLabel diagnosisLabel;
        private JLabel medicationLabel;
        private JLabel dateLabel;
        private int currentDiagnosisFileId = -1;

        public ViewDiagnosisFilePanel() {
            setLayout(new BorderLayout());
            setBackground(new Color(171, 191, 234));
            setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

            JLabel title = new JLabel("Diagnosis File", JLabel.CENTER);
            title.setFont(title.getFont().deriveFont(Font.BOLD, 22f));
            add(title, BorderLayout.NORTH);

            // Patient info panel (left)
            JPanel patientPanel = new JPanel(new GridBagLayout());
            patientPanel.setBackground(new Color(171, 191, 234));
            patientPanel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createTitledBorder("Patient Information"),
                    BorderFactory.createEmptyBorder(8, 8, 8, 8)
            ));
            GridBagConstraints pg = new GridBagConstraints();
            pg.insets = new Insets(4, 4, 4, 4);
            pg.anchor = GridBagConstraints.WEST;
            pg.gridx = 0;
            pg.gridy = 0;
            patientPanel.add(new JLabel("Name:"), pg);
            pg.gridx = 1;
            pNameLabel = new JLabel("-");
            pNameLabel.setFont(pNameLabel.getFont().deriveFont(Font.BOLD));
            patientPanel.add(pNameLabel, pg);

            pg.gridx = 0;
            pg.gridy++;
            patientPanel.add(new JLabel("Date of Birth:"), pg);
            pg.gridx = 1;
            pDobLabel = new JLabel("-");
            patientPanel.add(pDobLabel, pg);

            pg.gridx = 0;
            pg.gridy++;
            patientPanel.add(new JLabel("Insurance Number:"), pg);
            pg.gridx = 1;
            pHinLabel = new JLabel("-");
            pHinLabel.setFont(pHinLabel.getFont().deriveFont(Font.BOLD));
            patientPanel.add(pHinLabel, pg);


            pg.gridx = 0;
            pg.gridy++;
            patientPanel.add(new JLabel("Sex:"), pg);
            pg.gridx = 1;
            pSexLabel = new JLabel("-");
            patientPanel.add(pSexLabel, pg);

            // Diagnosis info panel (center/right)
            JPanel infoPanel = new JPanel(new GridBagLayout());
            infoPanel.setBackground(new Color(171, 191, 234));
            GridBagConstraints g = new GridBagConstraints();
            g.insets = new Insets(6, 6, 6, 6);
            g.anchor = GridBagConstraints.WEST;
            g.gridx = 0;
            g.gridy = 0;

            infoPanel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createTitledBorder("Selected Diagnosis Info"),
                    BorderFactory.createEmptyBorder(8, 8, 8, 8)
            ));

            infoPanel.add(new JLabel("Symptoms:"), g);
            g.gridx = 1;
            symptomsLabel = new JLabel();
            symptomsLabel.setFont(symptomsLabel.getFont().deriveFont(Font.BOLD));
            infoPanel.add(symptomsLabel, g);

            g.gridx = 0;
            g.gridy++;
            infoPanel.add(new JLabel("Diagnosis:"), g);
            g.gridx = 1;
            diagnosisLabel = new JLabel();
            diagnosisLabel.setFont(diagnosisLabel.getFont().deriveFont(Font.BOLD));
            infoPanel.add(diagnosisLabel, g);

            g.gridx = 0;
            g.gridy++;
            infoPanel.add(new JLabel("Medication:"), g);
            g.gridx = 1;
            medicationLabel = new JLabel();
            medicationLabel.setFont(medicationLabel.getFont().deriveFont(Font.BOLD));
            infoPanel.add(medicationLabel, g);

            g.gridx = 0;
            g.gridy++;
            infoPanel.add(new JLabel("Date:"), g);
            g.gridx = 1;
            dateLabel = new JLabel();
            dateLabel.setFont(dateLabel.getFont().deriveFont(Font.BOLD));
            infoPanel.add(dateLabel, g);

            JPanel center = new JPanel(new BorderLayout(10, 10));
            center.setOpaque(false);
            center.add(patientPanel, BorderLayout.NORTH);
            center.add(infoPanel, BorderLayout.CENTER);

            add(center, BorderLayout.CENTER);

            // Bottom buttons
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

        /*
        public void showDiagnosis(DiagnosisFile df, Patient p) {
            try {
                if (p != null) {
                    pNameLabel.setText((p.getNamePatient() == null ? "-" : p.getNamePatient()) +
                            " " + (p.getSurnamePatient() == null ? "" : p.getSurnamePatient()));
                    if (p.getDobPatient() != null) {
                        pDobLabel.setText(String.format("%02d-%02d-%d",
                                p.getDobPatient().getDay(),
                                p.getDobPatient().getMonth(),
                                p.getDobPatient().getYear()));
                    } else {
                        pDobLabel.setText("-");
                    }
                    pHinLabel.setText(String.valueOf(p.getHealthInsuranceNumberPatient() == 0 ? "-" : p.getHealthInsuranceNumberPatient()));
                    pSexLabel.setText(p.getSexPatient() == null ? "-" : p.getSexPatient().toString());
                } else {
                    pNameLabel.setText("-");
                    pDobLabel.setText("-");
                    pHinLabel.setText("-");
                    pSexLabel.setText("-");
                }
            } catch (Throwable ignored) {
                pNameLabel.setText("-");
                pDobLabel.setText("-");
                pHinLabel.setText("-");
                pSexLabel.setText("-");
            }


            currentDiagnosisFileId = -1;
            if (df == null) {
                symptomsLabel.setText("-");
                diagnosisLabel.setText("-");
                medicationLabel.setText("-");
                dateLabel.setText("-");
                return;
            }

            try {
                if (df.getSymptoms() != null && !df.getSymptoms().isEmpty()) {
                    symptomsLabel.setText(String.join(", ", df.getSymptoms()));
                } else {
                    symptomsLabel.setText("None");
                }
            } catch (Throwable ignored) {
                symptomsLabel.setText("None");
            }

            try {
                diagnosisLabel.setText(df.getDiagnosis() == null ? "-" : df.getDiagnosis());
            } catch (Throwable ignored) {
                diagnosisLabel.setText("-");
            }

            try {
                medicationLabel.setText(df.getMedication() == null ? "-" : df.getMedication());
            } catch (Throwable ignored) {
                medicationLabel.setText("-");
            }

            try {
                if (df.getDate() != null) {
                    Object d = df.getDate();
                    if (d instanceof LocalDateTime) {
                        LocalDateTime ldt = (LocalDateTime) d;
                        dateLabel.setText(String.format("%02d-%02d-%04d", ldt.getDayOfMonth(), ldt.getMonthValue(), ldt.getYear()));
                    } else if (d instanceof LocalDate) {
                        LocalDate ld = (LocalDate) d;
                        dateLabel.setText(String.format("%02d-%02d-%04d", ld.getDayOfMonth(), ld.getMonthValue(), ld.getYear()));
                    } else {
                        dateLabel.setText(d.toString());
                    }
                } else {
                    dateLabel.setText("unknown");
                }
            } catch (Throwable ignored) {
                dateLabel.setText("unknown");
            }

            try {
                currentDiagnosisFileId = df.getId();
            } catch (Throwable ignored) {
                currentDiagnosisFileId = -1;
            }
        }

        public int getSelectedDiagnosisId() {
            return currentDiagnosisFileId;
        }
    }*/

        public void showDiagnosis(DiagnosisFile df, Patient p) {

            // Show patient info
            if (p != null) {
                pNameLabel.setText((p.getNamePatient() == null ? "-" : p.getNamePatient()) +
                        " " + (p.getSurnamePatient() == null ? "" : p.getSurnamePatient()));
                pDobLabel.setText(p.getDobPatient() == null ? "-" :
                        String.format("%02d-%02d-%04d",
                                p.getDobPatient().getDay(),
                                p.getDobPatient().getMonth(),
                                p.getDobPatient().getYear()));
                pHinLabel.setText(p.getHealthInsuranceNumberPatient() == 0 ? "-" : String.valueOf(p.getHealthInsuranceNumberPatient()));
                pSexLabel.setText(p.getSexPatient() == null ? "-" : p.getSexPatient().toString());
            } else {
                pNameLabel.setText("-");
                pDobLabel.setText("-");
                pHinLabel.setText("-");
                pSexLabel.setText("-");
            }

            if (df == null) {
                symptomsLabel.setText("-");
                diagnosisLabel.setText("-");
                medicationLabel.setText("-");
                dateLabel.setText("-");
                return;
            }

            // Symptoms
            symptomsLabel.setText(
                    (df.getSymptoms() == null || df.getSymptoms().isEmpty())
                            ? "None"
                            : String.join(", ", df.getSymptoms())
            );

            // Diagnosis (multi-line neatly displayed)
            diagnosisLabel.setText(
                    df.getDiagnosis() == null ? "-" :
                            "<html>" + df.getDiagnosis().replace("\n", "<br>") + "</html>"
            );

            // Medication (clean)
            medicationLabel.setText(
                    df.getMedication() == null ? "-" : df.getMedication()
            );

            // Date
            if (df.getDate() != null) {
                if (df.getDate() instanceof LocalDate ld) {
                    dateLabel.setText(String.format("%02d-%02d-%04d",
                            ld.getDayOfMonth(), ld.getMonthValue(), ld.getYear()));
                } else {
                    dateLabel.setText(df.getDate().toString());
                }
            } else {
                dateLabel.setText("unknown");
            }

            currentDiagnosisFileId = df.getId();
        }
        public int getSelectedDiagnosisId() {
            return currentDiagnosisFileId;
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
            backButton.addActionListener(e -> handleBackToViewPatientFromViewDiagnosisFile());

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

            JLabel label = new JLabel("Diagnosis To Complete", JLabel.CENTER);
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
            backButton.addActionListener(e -> handleBackToMenuFromLoadRecentlyFinished());

            buttonPanel.add(completeButton);
            buttonPanel.add(refreshButton);
            buttonPanel.add(backButton);
            add(buttonPanel, BorderLayout.SOUTH);

            // Cargar lista al inicializar
            //loadRecentlyFinished();
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

                        /*String response = in.readUTF();
                        if ("RECENTLY_FINISH_LIST".equals(response)) {
                            recentData = in.readUTF();
                        }*/

                        StringBuilder sb = new StringBuilder();
                        while (true) {
                            String line = in.readUTF();
                            if ("RECENTLY_FINISHED".equals(line)) {
                                break; // end of list from server
                            }
                            if (sb.length() > 0) sb.append(" ");
                            sb.append(line);
                        }
                        recentData = sb.toString();

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
            diagnosisText.setText("Please enter Diagnosis information.\n\nObservations:\n\nTreatment Plan:\n\nMedications\n\nFollow-up::");
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
                        serverMsg = in.readUTF();
                        serverMsg = serverMsg+ "-> "+ in.readUTF();
                        if (success) currentUsername = username;
                    } else {
                        serverMsg = "Unexpected server response: " + response;
                        serverMsg = serverMsg+ "->"+ in.readUTF();
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
            out.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        changeState("SEARCH_PATIENT");
    }

    private void handleSelectPatient() {
        String selectedPatient = searchPatientPanel.getSelectedPatient();
        System.out.println("The selected patient is:" + selectedPatient);

        if (selectedPatient == null || selectedPatient.equals("No patients available")) {
            JOptionPane.showMessageDialog(this, "Please select a patient", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }

        new SwingWorker<Void, Void>() {
            private String patientInfo = null;
            private boolean success = false;
            private Patient patient = null; // Inicializar como null

            @Override
            protected Void doInBackground() {
                try {
                    // Limpiar el buffer de entrada si hay datos disponibles
                    if (socket.isClosed()) {
                        System.out.println("socket closed");
                    }

                    out.writeUTF("VIEW_PATIENT");
                    out.writeInt(Integer.parseInt(selectedPatient));
                    out.flush();

                    String patientString = in.readUTF();
                    System.out.println("Patient data received: " + patientString); // Debug

                    if (patientString == null || patientString.isBlank()) {
                        patientInfo = "Received empty patient data";
                        return null;
                    }

                    String resp = in.readUTF();
                    System.out.println("selected patient sent to server");
                    System.out.println(in.available());


                    System.out.println("Server response: " + resp); // Debug

                    if (!"PATIENT_OVERVIEW_SENT".equals(resp)) {
                        patientInfo = "Unexpected response: " + resp;
                        return null;
                    }



                    // Parsear el paciente
                    List<Patient> patients = parsePatientList(patientString);
                    if (patients == null || patients.isEmpty()) {
                        patientInfo = "No patient data could be parsed";
                        return null;
                    }

                    this.patient = patients.get(0);
                    currentPatient = patient;
                    debugPatientData(this.patient);// Tomar el primer paciente
                    if (this.patient == null) {
                        patientInfo = "Parsed patient is null";
                        return null;
                    }

                    // Verificar que los datos esenciales no sean null
                    if (this.patient.getDobPatient() == null) {
                        patientInfo = "Patient date of birth is null";
                        return null;
                    }

                    patientInfo = "Patient data loaded successfully";
                    success = true;

                } catch (IOException e) {
                    patientInfo = "I/O error while getting patient info: " + e.getMessage();
                    e.printStackTrace();
                } catch (NumberFormatException e) {
                    patientInfo = "Invalid patient ID format: " + selectedPatient;
                    e.printStackTrace();
                } catch (Exception e) {
                    patientInfo = "Unexpected error: " + e.getMessage();
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void done() {
                try {
                    if (success && patient != null) {
                        viewPatientPanel.setPatientInfo(patient);
                        currentPatientInfo = patientInfo;
                        changeState("VIEW_PATIENT");
                    } else {
                        JOptionPane.showMessageDialog(DoctorApplicationGUI.this,
                                patientInfo != null ? patientInfo : "Unknown error occurred",
                                "Error", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(DoctorApplicationGUI.this,
                            "Error displaying patient: " + ex.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE);
                    ex.printStackTrace();
                }
            }
        }.execute();
    }

    private void debugPatientData(Patient patient) {
        if (patient == null) {
            System.out.println("Patient is NULL");
            return;
        }
        System.out.println("Patient name: " + patient.getNamePatient());
        System.out.println("Patient surname: " + patient.getSurnamePatient());
        System.out.println("Patient HIN: " + patient.getHealthInsuranceNumberPatient());
        System.out.println("Patient sex: " + patient.getSexPatient());
        if (patient.getDobPatient() != null) {
            System.out.println("Patient DoB: " + patient.getDobPatient().getDay() +
                    "-" + patient.getDobPatient().getMonth() +
                    "-" + patient.getDobPatient().getYear());
        } else {
            System.out.println("Patient DoB is NULL");
        }
        if (patient.getDiagnosisList() != null) {
            System.out.println("Diagnosis count: " + patient.getDiagnosisList().size());
        } else {
            System.out.println("Diagnosis list is NULL");
        }
    }

    public static List<Patient> parsePatientList(String payload) {
        List<Patient> patients = new ArrayList<>();
        if (payload == null) return patients;
        payload = payload.trim();
        if (payload.isEmpty()) return patients;

        int pos = 0;
        while (true) {
            int start = payload.indexOf("Patient{", pos);
            if (start < 0) break;
            start = payload.indexOf('{', start);
            if (start < 0) break;
            int brace = start + 1;
            int depth = 1;
            while (brace < payload.length() && depth > 0) {
                char c = payload.charAt(brace);
                if (c == '{') depth++;
                else if (c == '}') depth--;
                brace++;
            }
            if (depth != 0) break;
            String body = payload.substring(start + 1, brace - 1).trim();
            pos = brace;

            // parse key=value pairs
            Map<String, String> map = new LinkedHashMap<>();
            int i = 0;
            while (i < body.length()) {
                int eq = body.indexOf('=', i);
                if (eq < 0) break;
                String key = body.substring(i, eq).trim();
                i = eq + 1;

                String value;
                if (i < body.length() && body.charAt(i) == '\'') {
                    // quoted string with single quote
                    i++; // skip '
                    StringBuilder sb = new StringBuilder();
                    while (i < body.length()) {
                        char ch = body.charAt(i++);
                        if (ch == '\'') break;
                        if (ch == '\\' && i < body.length()) {
                            sb.append(body.charAt(i++));
                        } else {
                            sb.append(ch);
                        }
                    }
                    value = sb.toString();
                    while (i < body.length() && (body.charAt(i) == ',' || Character.isWhitespace(body.charAt(i)))) i++;
                } else if (i < body.length() && body.charAt(i) == '[') {
                    // bracketed list
                    int j = i;
                    int d = 0;
                    StringBuilder sb = new StringBuilder();
                    while (j < body.length()) {
                        char ch = body.charAt(j);
                        sb.append(ch);
                        if (ch == '[') d++;
                        else if (ch == ']') {
                            d--;
                            if (d == 0) { j++; break; }
                        }
                        j++;
                    }
                    value = sb.toString();
                    i = j;
                    while (i < body.length() && (body.charAt(i) == ',' || Character.isWhitespace(body.charAt(i)))) i++;
                } else {
                    // plain until comma
                    int comma = i;
                    while (comma < body.length() && body.charAt(comma) != ',') comma++;
                    value = body.substring(i, comma).trim();
                    i = comma + 1;
                    while (i < body.length() && Character.isWhitespace(body.charAt(i))) i++;
                }
                map.put(key, value);
            }

            // build Patient
            Patient p = new Patient();
            try {
                if (map.containsKey("idPatient")) {
                    String v = map.get("idPatient").replace("'", "").trim();
                    try { setFieldIfExists(p, "idPatient", Integer.parseInt(v)); } catch (Exception e) { setFieldIfExists(p, "idPatient", v); }
                }
                if (map.containsKey("namePatient")) setFieldIfExists(p, "namePatient", map.get("namePatient").replace("'", "").trim());
                if (map.containsKey("surnamePatient")) setFieldIfExists(p, "surnamePatient", map.get("surnamePatient").replace("'", "").trim());
                if (map.containsKey("dniPatient")) setFieldIfExists(p, "dniPatient", map.get("dniPatient").replace("'", "").trim());
                if (map.containsKey("dobPatient")) {
                    String dob = map.get("dobPatient").replace("'", "").trim();
                    if (!dob.isEmpty()) {
                        // prova ISO_LOCAL_DATE e fallback a stringa
                        try {
                            java.time.LocalDate ld = java.time.LocalDate.parse(dob, DateTimeFormatter.ISO_LOCAL_DATE);
                            setFieldIfExists(p, "dobPatient", ld);
                        } catch (Exception ex) {
                            setFieldIfExists(p, "dobPatient", dob);
                        }
                    }
                }
                if (map.containsKey("emailPatient")) setFieldIfExists(p, "emailPatient", map.get("emailPatient").replace("'", "").trim());
                if (map.containsKey("sexPatient")) setFieldIfExists(p, "sexPatient", map.get("sexPatient").replace("'", "").trim());
                if (map.containsKey("phoneNumberPatient")) {
                    String v = map.get("phoneNumberPatient").replace("'", "").trim();
                    try { setFieldIfExists(p, "phoneNumberPatient", Long.parseLong(v)); } catch (Exception e) { setFieldIfExists(p, "phoneNumberPatient", v); }
                }
                if (map.containsKey("healthInsuranceNumberPatient")) {
                    String v = map.get("healthInsuranceNumberPatient").replace("'", "").trim();
                    try { setFieldIfExists(p, "healthInsuranceNumberPatient", Integer.parseInt(v)); } catch (Exception e) { setFieldIfExists(p, "healthInsuranceNumberPatient", v); }
                }
                if (map.containsKey("emergencyContactPatient")) setFieldIfExists(p, "emergencyContactPatient", map.get("emergencyContactPatient").replace("'", "").trim());
                if (map.containsKey("doctorId")) {
                    String v = map.get("doctorId").replace("'", "").trim();
                    try { setFieldIfExists(p, "doctorId", Integer.parseInt(v)); } catch (Exception e) { setFieldIfExists(p, "doctorId", v); }
                }
                if (map.containsKey("MACadress")) setFieldIfExists(p, "MACadress", map.get("MACadress").replace("'", "").trim());

                // diagnosisList: usa il parser dedicato per ottenere List<DiagnosisFile>
                if (map.containsKey("diagnosisFile") || map.containsKey("diagnosisList")) {
                    String key = map.containsKey("diagnosisFile") ? "diagnosisFile" : "diagnosisList";
                    String raw = map.get(key).trim();
                    List<DiagnosisFile> diagFiles = parseDiagnosisFileList(raw);
                    setFieldIfExists(p, "diagnosisList", diagFiles);
                }

                if (map.containsKey("userId")) {
                    String v = map.get("userId").replace("'", "").trim();
                    try { setFieldIfExists(p, "userId", Integer.parseInt(v)); } catch (Exception e) { setFieldIfExists(p, "userId", v); }
                }
            } catch (Throwable ignored) {}

            patients.add(p);
        }

        return patients;
    }
    private static void setFieldIfExists(Object target, String name, Object value) {
        if (target == null || name == null) return;
        Class<?> cls = target.getClass();
        String setterName = "set" + name;

        try {
            // cerca setter (case-insensitive)
            for (Method m : cls.getMethods()) {
                if (m.getName().equalsIgnoreCase(setterName) && m.getParameterCount() == 1) {
                    Object arg = convertToParameterType(value, m.getParameterTypes()[0]);
                    m.invoke(target, arg);
                    return;
                }
            }

            try {
                java.lang.reflect.Field f = cls.getDeclaredField(name);
                f.setAccessible(true);
                Object arg = convertToParameterType(value, f.getType());
                f.set(target, arg);
            } catch (NoSuchFieldException ignored) {
            }

        } catch (Throwable ignored) {
        }
    }

    @SuppressWarnings("unchecked")
    public static Object convertToParameterType(Object value, Class<?> targetType) {
        if (value == null) return null;
        if (targetType.isAssignableFrom(value.getClass())) return value;

        String s = value.toString();
        try {
            if (targetType == String.class) return s;

            if (targetType == Integer.class || targetType == int.class) {
                if (value instanceof Number) return ((Number) value).intValue();
                return Integer.parseInt(s);
            }
            if (targetType == Long.class || targetType == long.class) {
                if (value instanceof Number) return ((Number) value).longValue();
                return Long.parseLong(s);
            }
            if (targetType == java.time.LocalDateTime.class) {
                if (value instanceof java.time.LocalDateTime) return value;
                if (value instanceof java.time.LocalDate) return ((java.time.LocalDate) value).atStartOfDay();
                return java.time.LocalDateTime.parse(s, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            }
            if (targetType == java.time.LocalDate.class) {
                if (value instanceof java.time.LocalDate) return value;
                if (value instanceof java.time.LocalDateTime) return ((java.time.LocalDateTime) value).toLocalDate();
                return java.time.LocalDate.parse(s, DateTimeFormatter.ISO_LOCAL_DATE);
            }
            if (java.util.Date.class.isAssignableFrom(targetType)) {
                if (value instanceof java.util.Date) return value;
                if (value instanceof java.time.LocalDateTime) {
                    return java.util.Date.from(((java.time.LocalDateTime) value).atZone(java.time.ZoneId.systemDefault()).toInstant());
                }
                if (value instanceof java.time.LocalDate) {
                    return java.util.Date.from(((java.time.LocalDate) value).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant());
                }

                java.time.LocalDateTime ldt = java.time.LocalDateTime.parse(s, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                return java.util.Date.from(ldt.atZone(java.time.ZoneId.systemDefault()).toInstant());
            }

            if (java.util.List.class.isAssignableFrom(targetType)) {
                if (value instanceof java.util.List) return value;
                // se è una stringa separata da virgole, creiamo una ArrayList
                java.util.List<String> list = new java.util.ArrayList<>();
                if (s != null && !s.isBlank()) {
                    for (String part : s.split(",")) {
                        String t = part.trim();
                        if (!t.isEmpty()) list.add(t);
                    }
                }
                return list;
            }

            if (targetType.isEnum()) {
                @SuppressWarnings({"rawtypes", "unchecked"})
                Object e = Enum.valueOf((Class<Enum>) targetType, s);
                return e;
            }

        } catch (Exception ignored) {
            // fallback al valore originale
        }
        return value;
    }

    // java
    public static List<DiagnosisFile> parseDiagnosisFileList(String payload) {
        List<DiagnosisFile> files = new ArrayList<>();
        if (payload == null) return files;
        payload = payload.trim();
        if (payload.isEmpty() || payload.equals("[]")) return files;

        int pos = 0;
        while (true) {
            int start = payload.indexOf("MedicalRecord{", pos);
            if (start < 0) break;
            start = payload.indexOf('{', start);
            if (start < 0) break;
            int brace = start + 1;
            int depth = 1;
            while (brace < payload.length() && depth > 0) {
                char c = payload.charAt(brace);
                if (c == '{') depth++;
                else if (c == '}') depth--;
                brace++;    // brace increases until I reach the } position that closes the MedicalRecord{
            }
            if (depth != 0) break;
            String body = payload.substring(start + 1, brace - 1).trim();
            pos = brace;

            // parse key=value pairs in body
            Map<String, String> map = new LinkedHashMap<>();
            int i = 0;
            while (i < body.length()) {
                // read key
                int eq = body.indexOf('=', i);
                if (eq < 0) break;
                String key = body.substring(i, eq).trim();
                i = eq + 1;

                // read value: could start with ' (quoted), [ (list), or plain until comma
                String value;
                if (i < body.length() && body.charAt(i) == '\'') {
                    // quoted string
                    i++; // skip '
                    StringBuilder sb = new StringBuilder();
                    while (i < body.length()) {
                        char c = body.charAt(i++);
                        if (c == '\'') break;
                        if (c == '\\' && i < body.length()) { // support escaped chars
                            sb.append(body.charAt(i++));
                        } else {
                            sb.append(c);
                        }
                    }
                    value = sb.toString();
                    // skip optional comma and space
                    while (i < body.length() && (body.charAt(i) == ',' || Character.isWhitespace(body.charAt(i)))) i++;
                } else if (i < body.length() && body.charAt(i) == '[') {
                    // bracketed list - read until matching ]
                    int j = i;
                    int d = 0;
                    StringBuilder sb = new StringBuilder();
                    while (j < body.length()) {
                        char c = body.charAt(j);
                        if (c == '[') d++;
                        else if (c == ']') {
                            d--;
                            if (d == 0) { sb.append(c); j++; break; }
                        }
                        sb.append(c);
                        j++;
                    }
                    value = sb.toString();
                    i = j;
                    // skip optional comma and spaces
                    while (i < body.length() && (body.charAt(i) == ',' || Character.isWhitespace(body.charAt(i)))) i++;
                } else {
                    // plain token until comma
                    int comma = i;
                    while (comma < body.length() && body.charAt(comma) != ',') comma++;
                    value = body.substring(i, comma).trim();
                    i = comma + 1;
                    while (i < body.length() && Character.isWhitespace(body.charAt(i))) i++;
                }
                map.put(key, value);
            }

            // costruisci DiagnosisFile usando setFieldIfExists
            DiagnosisFile df = new DiagnosisFile();
            try {
                // id (può essere "id='123'" oppure "id=123")
                String idStr = map.get("id");
                if (idStr != null) {
                    idStr = idStr.replace("'", "").trim();
                    try { setFieldIfExists(df, "id", Integer.parseInt(idStr)); } catch (Exception e) { setFieldIfExists(df, "id", idStr); }
                }

                // symptoms: può essere "['a','b']" o "[a, b]" o "a,b"
                String symptomsRaw = map.get("symptoms");
                if (symptomsRaw != null) {
                    String s = symptomsRaw.trim();
                    List<String> symptoms = new ArrayList<>();
                    if (s.startsWith("[")) {
                        // rimuovi [ ]
                        String inner = s.substring(1, Math.max(1, s.length()-1));
                        // split rispettando apici
                        StringBuilder cur = new StringBuilder();
                        boolean inQuote = false;
                        for (int k = 0; k < inner.length(); k++) {
                            char c = inner.charAt(k);
                            if (c == '\'' || c == '\"') {
                                inQuote = !inQuote;
                                continue;
                            }
                            if (c == ',' && !inQuote) {
                                String t = cur.toString().trim();
                                if (!t.isEmpty()) symptoms.add(t);
                                cur.setLength(0);
                            } else {
                                cur.append(c);
                            }
                        }
                        String last = cur.toString().trim();
                        if (!last.isEmpty()) symptoms.add(last);
                    } else {
                        // comma separated plain
                        for (String part : s.split(",")) {
                            String t = part.replace("'", "").replace("\"","").trim();
                            if (!t.isEmpty()) symptoms.add(t);
                        }
                    }
                    setFieldIfExists(df, "symptoms", symptoms);
                }

                // diagnosis, medication
                if (map.containsKey("diagnosis")) setFieldIfExists(df, "diagnosis", map.get("diagnosis").replace("'", "").trim());
                if (map.containsKey("medication")) setFieldIfExists(df, "medication", map.get("medication").replace("'", "").trim());

                // date
                if (map.containsKey("date")) {
                    String dateRaw = map.get("date").replace("'", "").trim();
                    if (!dateRaw.isEmpty()) {
                        // tenta ISO_LOCAL_DATE_TIME e fallback a stringa
                        try {
                            LocalDateTime ldt = LocalDateTime.parse(dateRaw, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                            setFieldIfExists(df, "date", ldt);
                        } catch (Exception ex) {
                            setFieldIfExists(df, "date", dateRaw);
                        }
                    }
                }

                // patient id
                if (map.containsKey("patient id")) {
                    String pid = map.get("patient id").replace("'", "").trim();
                    try { setFieldIfExists(df, "patientId", Integer.parseInt(pid)); } catch (Exception e) { setFieldIfExists(df, "patientId", pid); }
                } else if (map.containsKey("patientId")) {
                    String pid = map.get("patientId").replace("'", "").trim();
                    try { setFieldIfExists(df, "patientId", Integer.parseInt(pid)); } catch (Exception e) { setFieldIfExists(df, "patientId", pid); }
                }

                // status
                if (map.containsKey("status")) {
                    String statusStr = map.get("status").replace("'", "").trim();
                    try { setFieldIfExists(df, "status", Boolean.parseBoolean(statusStr)); } catch (Exception e) { setFieldIfExists(df, "status", statusStr); }
                }

            } catch (Throwable ignored) {}

            files.add(df);
        }

        // ordina come prima
        files.sort(java.util.Comparator.comparing(DiagnosisFile::getDate,
                java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder())).reversed());

        return files;
    }

    private void handleBackToMenuFromSearchPatient(){
        try {
            out.writeUTF("BACK_TO_MENU");
            System.out.println();
            out.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        currentUsername = null;
        //cleanupResources();
        changeState("DOCTOR_MENU");
    }

    private void handleBackToSearchPatientFromViewPatient(){
        try {
            out.writeUTF("BACK_TO_SEARCH_PATIENT");
            out.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        currentUsername = null;
        //cleanupResources();
        changeState("SEARCH_PATIENT");
    }

    private void handleBackToMenuFromLoadRecentlyFinished(){
        try {
            out.writeUTF("BACK_TO_MENU");
            System.out.println();
            out.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        currentUsername = null;
        //cleanupResources();
        changeState("DOCTOR_MENU");
    }

    // Back from COMPLETE_DIAGNOSISFILE to RECENTLY_FINISH
    private void handleBackToRecentlyFinishFromComplete() {
        try {
            out.writeUTF("BACK_TO_MENU");   // server: COMPLETE_DIAGNOSISFILE -> RECENTLY_FINISH
            out.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        changeState("DOCTOR_MENU");
    }

    private void handleBackToViewPatientFromViewDiagnosisFile() {
        try {
            out.writeUTF("BACK_TO_MENU");
            out.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        changeState("VIEW_PATIENT");
    }

    private void handleViewDiagnosisFile() {
        if (currentPatient == null) {
            JOptionPane.showMessageDialog(this, "No patient loaded", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        int selectedIndex = viewPatientPanel.getSelectedDiagnosisIndex();
        if (selectedIndex < 0) {
            JOptionPane.showMessageDialog(this, "Please select a diagnosis from the list", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (currentPatient.getDiagnosisList() == null || selectedIndex >= currentPatient.getDiagnosisList().size()) {
            JOptionPane.showMessageDialog(this, "Selected diagnosis not available", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        DiagnosisFile chosen = currentPatient.getDiagnosisList().get(selectedIndex);
        viewDiagnosisFilePanel.showDiagnosis(chosen, currentPatient);
        changeState("VIEW_DIAGNOSISFILE");
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
        recentlyFinishPanel.loadRecentlyFinished();
        changeState("RECENTLY_FINISH");
    }

    private void handleCompleteDiagnosis() {
        int diagnosisId = recentlyFinishPanel.getSelectedDiagnosisId();
        if (diagnosisId == -1) {
            JOptionPane.showMessageDialog(this, "Please select a diagnosis to complete", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }

        currentDiagnosisFileId = diagnosisId;
        completeDiagnosisFilePanel.setDiagnosisText("\n\nObservations:\n\nTreatment Plan:\n\nMedications:\n\nFollow-up:");
        changeState("COMPLETE_DIAGNOSISFILE");
    }

    private void handleSaveDiagnosis() {
        String diagnosis = completeDiagnosisFilePanel.getDiagnosisText();

        if (diagnosis.isBlank()) {
            JOptionPane.showMessageDialog(this, "Please enter diagnosis details", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }

        DiagnosisParts parts = parseDiagnosisText(diagnosis);

        String finalDiagnosisText = formatDiagnosisForStorage(parts);

        new SwingWorker<Void, Void>() {
            private boolean success = false;
            private String message = null;

            @Override
            protected Void doInBackground() {
                try {
                    out.writeUTF("COMPLETE_DIAGNOSISFILE");
                    out.writeUTF(String.valueOf(currentDiagnosisFileId));
                    System.out.println(finalDiagnosisText);
                    out.writeUTF(finalDiagnosisText);
                    out.writeUTF(parts.medications);  //to store the medications separately
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
                    recentlyFinishPanel.loadRecentlyFinished();
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

    public static class DiagnosisParts {
        public String observations = "";
        public String treatmentPlan = "";
        public String medications = "";
        public String followUp = "";
    }

    public static DiagnosisParts parseDiagnosisText(String fullText) {
        DiagnosisParts parts = new DiagnosisParts();

        // Normalize
        String text = fullText.replace("\r", "");

        // Split into blocks based on labels
        String[] obsSplit = text.split("Observations:", 2);
        if (obsSplit.length < 2) return parts;
        String[] planSplit = obsSplit[1].split("Treatment Plan:", 2);

        parts.observations = planSplit[0].trim();

        if (planSplit.length < 2) return parts;
        String[] medSplit = planSplit[1].split("Medications:", 2);

        parts.treatmentPlan = medSplit[0].trim();

        if (medSplit.length < 2) return parts;
        String[] followSplit = medSplit[1].split("Follow-up:", 2);

        parts.medications = followSplit[0].trim();

        if (followSplit.length < 2) return parts;
        parts.followUp = followSplit[1].trim();

        return parts;
    }

    public static String formatDiagnosisForStorage(DiagnosisParts p) {
        return
                "Observations:\n    " + p.observations + "\n\n" +
                        "Treatment Plan:\n    " + p.treatmentPlan + "\n\n" +
                        "Follow-up:\n    " + p.followUp;
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
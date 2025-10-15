package executable;

import javax.swing.*;
import javax.swing.plaf.basic.BasicButtonUI;
import java.awt.*;

public class MenuDoctorSwing extends JFrame {

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel cards = new JPanel(cardLayout);

    public MenuDoctorSwing() {
        super("App for Doctors");
        initUI();
    }

    private void initUI() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(720, 560);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // Barra superior
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
        btnExit.addActionListener(e -> System.exit(0));

        JPanel leftWrap = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        leftWrap.setOpaque(false);
        leftWrap.add(btnExit);
        topBar.add(leftWrap, BorderLayout.WEST);
        add(topBar, BorderLayout.NORTH);

        // PANTALLA 1: Home
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

        // PANTALLA 2: Auth
        JPanel auth = new JPanel();
        auth.setLayout(new BoxLayout(auth, BoxLayout.Y_AXIS));
        auth.setBackground(new Color(90, 136, 111));
        auth.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));

        JLabel choose = new JLabel("Choose an option", SwingConstants.CENTER);
        choose.setAlignmentX(Component.CENTER_ALIGNMENT);
        choose.setFont(choose.getFont().deriveFont(Font.BOLD, 22f));

        JButton btnLogin = new JButton("Login");
        btnLogin.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnLogin.setFont(btnLogin.getFont().deriveFont(Font.BOLD, 15f));
        btnLogin.setBackground(new Color(220, 186, 76));
        btnLogin.setForeground(Color.WHITE);
        btnLogin.setOpaque(true);
        btnLogin.setBorderPainted(false);
        btnLogin.setFocusPainted(false);
        btnLogin.setUI(new BasicButtonUI());

        JButton btnRegister = new JButton("Register");
        btnRegister.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnRegister.setFont(btnRegister.getFont().deriveFont(Font.BOLD, 15f));
        btnRegister.setBackground(new Color(220, 152, 76));
        btnRegister.setForeground(Color.WHITE);
        btnRegister.setOpaque(true);
        btnRegister.setBorderPainted(false);
        btnRegister.setFocusPainted(false);
        btnRegister.setUI(new BasicButtonUI());

        JButton btnBackHome = new JButton("Return");
        btnBackHome.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnBackHome.addActionListener(e -> cardLayout.show(cards, "home"));

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

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MenuDoctorSwing().setVisible(true));
    }
}

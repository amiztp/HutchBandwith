import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.URL;
import java.util.prefs.Preferences;

public class PingUI extends JFrame {
    // App version for update check
    private static final String CURRENT_VERSION = "1.0.0";

    // Change this to control how many launches trigger the subscription popup
    private static final int MAX_LAUNCH_COUNT = 100;

    private JTextArea logArea;
    private JButton toggleButton;
    private JPanel titleBar;
    private volatile boolean running = false;
    private Thread pingThread;
    private Point initialClick;

    // Theme colors
    private final Color bgColor = new Color(30, 30, 30);
    private final Color fgColor = new Color(200, 200, 200);
    private final Color activeColor = new Color(0, 150, 0);
    private final Color borderColor = new Color(80, 80, 80);

    // Lock state
    private boolean locked = false;

    public PingUI() {
        // Frame setup
        setUndecorated(true);
        setSize(400, 350);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        getRootPane().setBorder(BorderFactory.createLineBorder(borderColor, 3));

        // Title bar with SPEEDX label
        titleBar = new JPanel(new BorderLayout());
        titleBar.setBackground(bgColor);

        JLabel titleLabel = new JLabel("SPEEDX");
        titleLabel.setForeground(fgColor);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 16));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 0));
        titleBar.add(titleLabel, BorderLayout.WEST);

        JButton closeButton = createCircleIconButton(Color.RED, "X");
        JButton minimizeButton = createCircleIconButton(Color.YELLOW, "_");
        JButton maximizeButton = createCircleIconButton(Color.GREEN, "⬜");

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 5));
        buttonPanel.setBackground(bgColor);
        buttonPanel.add(minimizeButton);
        buttonPanel.add(maximizeButton);
        buttonPanel.add(closeButton);

        titleBar.add(buttonPanel, BorderLayout.EAST);

        // Make window draggable
        titleBar.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                initialClick = e.getPoint();
            }
        });
        titleBar.addMouseMotionListener(new MouseAdapter() {
            public void mouseDragged(MouseEvent e) {
                int thisX = getLocation().x;
                int thisY = getLocation().y;
                int xMoved = e.getX() - initialClick.x;
                int yMoved = e.getY() - initialClick.y;
                setLocation(thisX + xMoved, thisY + yMoved);
            }
        });

        // Log area
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setBackground(bgColor);
        logArea.setForeground(fgColor);
        JScrollPane scrollPane = new JScrollPane(logArea);

        // Auto-clear logs every 30 seconds
        Timer clearTimer = new Timer(30000, e -> {
            logArea.append("Auto Clearing Logs\n");
            logArea.setText("");
        });
        clearTimer.start();

        // Circle toggle button
        toggleButton = new JButton("Start") {
            @Override
            public Dimension getPreferredSize() {
                return new Dimension(80, 80);
            }
        };
        toggleButton.setFocusPainted(false);
        toggleButton.setBorder(BorderFactory.createEmptyBorder());
        toggleButton.setFont(new Font("Arial", Font.BOLD, 14));
        toggleButton.setContentAreaFilled(false);
        toggleButton.setOpaque(false);

        // Custom circle paint
        toggleButton.setUI(new javax.swing.plaf.basic.BasicButtonUI() {
            @Override
            public void paint(Graphics g, JComponent c) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int diameter = Math.min(c.getWidth(), c.getHeight());
                g2.setColor(running ? activeColor : new Color(70, 70, 70));
                g2.fillOval(0, 0, diameter, diameter);
                g2.setColor(fgColor);
                FontMetrics fm = g2.getFontMetrics();
                String text = ((JButton) c).getText();
                int x = (c.getWidth() - fm.stringWidth(text)) / 2;
                int y = (c.getHeight() + fm.getAscent()) / 2 - 4;
                g2.drawString(text, x, y);
                g2.dispose();
            }
        });

        JPanel controlPanel = new JPanel();
        controlPanel.setBackground(bgColor);
        controlPanel.add(toggleButton);

        // Add components
        add(titleBar, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        add(controlPanel, BorderLayout.SOUTH);

        // Button actions
        toggleButton.addActionListener(e -> {
            if (locked) return;
            if (!running) {
                running = true;
                toggleButton.setText("Stop");
                titleBar.setBackground(activeColor);
                pingThread = new Thread(this::pingLoop);
                pingThread.start();
            } else {
                running = false;
                toggleButton.setText("Start");
                titleBar.setBackground(bgColor);
            }
            toggleButton.repaint();
        });

        closeButton.addActionListener(e -> System.exit(0));
        minimizeButton.addActionListener(e -> setState(JFrame.ICONIFIED));
        maximizeButton.addActionListener(e -> {
            if (getExtendedState() == JFrame.MAXIMIZED_BOTH) {
                setExtendedState(JFrame.NORMAL);
            } else {
                setExtendedState(JFrame.MAXIMIZED_BOTH);
            }
        });

        // Delayed update check (2 minutes after launch)
        Timer updateTimer = new Timer(120000, e -> checkForUpdates());
        updateTimer.setRepeats(false);
        updateTimer.start();

        // Launch counter check
        checkLaunchCount();
    }

    // ===== Launch counter with subscription popup =====
    private void checkLaunchCount() {
        Preferences prefs = Preferences.userNodeForPackage(PingUI.class);
        int launchCount = prefs.getInt("launchCount", 0);
        launchCount++;
        prefs.putInt("launchCount", launchCount);

        if (launchCount >= MAX_LAUNCH_COUNT) {
            JTextArea message = new JTextArea(
                "⚠️ Subscription Required ⚠️\n\n" +
                "If you want to continue using this tweak application on your device,\n" +
                "make a lifetime subscription once for just 500 LKR.\n\n" +
                "Account Details: 92353798\n" +
                "Account Name: MAT PERERA\n" +
                "Send receipt to: 0762383636\n\n" +
                "The program will be sent to you within 24 Hours."
            );
            message.setEditable(false);
            message.setBackground(Color.RED);
            message.setForeground(Color.WHITE);
            message.setFont(new Font("Arial", Font.BOLD, 14));

            JOptionPane.showMessageDialog(this, message,
                "Subscription Required", JOptionPane.WARNING_MESSAGE);

            // Lock the app for further usage
            lockApp();

            // Optionally reset counter
            prefs.putInt("launchCount", 0);
        }
    }

    // Lock the app for further usage
    private void lockApp() {
        locked = true;
        toggleButton.setEnabled(false);
        logArea.setText("Application locked. Please subscribe to continue.");
        logArea.setBackground(Color.DARK_GRAY);
        logArea.setForeground(Color.RED);
        titleBar.setBackground(Color.DARK_GRAY);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            PingUI ui = new PingUI();
            ui.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            ui.setVisible(true);
        });
    }

    // Create Apple-style circular buttons
    private JButton createCircleIconButton(Color circleColor, String iconText) {
        JButton b = new JButton(iconText);
        b.setPreferredSize(new Dimension(25, 25));
        b.setFocusPainted(false);
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setOpaque(false);
        b.setFont(new Font("Arial", Font.BOLD, 12));
        b.setForeground(Color.BLACK);

        b.setUI(new javax.swing.plaf.basic.BasicButtonUI() {
            @Override
            public void paint(Graphics g, JComponent c) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int diameter = Math.min(c.getWidth(), c.getHeight());
                g2.setColor(circleColor);
                g2.fillOval(0, 0, diameter, diameter);
                g2.setColor(Color.BLACK);
                FontMetrics fm = g2.getFontMetrics();
                String text = ((JButton) c).getText();
                int x = (c.getWidth() - fm.stringWidth(text)) / 2;
                int y = (c.getHeight() + fm.getAscent()) / 2 - 3;
                g2.drawString(text, x, y);
                g2.dispose();
            }
        });
        return b;
    }

    // Ping loop
    private void pingLoop() {
        String host = "www.hutch.lk";
        while (running) {
            try {
                InetAddress inet = InetAddress.getByName(host);
                boolean reachable = inet.isReachable(5000);
                String result = reachable
                        ? "Speeding Up Your Connection"
                        : "Retrying To Speed Your Connection";
                logArea.append(result + "\n");
                Thread.sleep(10000);
            } catch (IOException | InterruptedException ex) {
                logArea.append("Error: " + ex.getMessage() + "\n");
            }
        }
    }

    // Check for updates from GitHub
    private void checkForUpdates() {
        try {
            URL url = new URL("https://raw.githubusercontent.com/<your-username>/<repo-name>/main/version.txt");
            BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
            String latestVersion = in.readLine().trim();
            in.close();

            if (!CURRENT_VERSION.equals(latestVersion)) {
                JOptionPane.showMessageDialog(this,
                        "A new version (" + latestVersion + ") is available!\nPlease update the app.",
                        "Update Available",
                        JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (Exception e) {
            logArea.append("Update check failed: " + e.getMessage() + "\n");
        }
    }
}
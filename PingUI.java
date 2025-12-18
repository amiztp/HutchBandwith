import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.URL;

public class PingUI extends JFrame {
    private static final String CURRENT_VERSION = "1.0.0"; // your app version
    private JTextArea logArea;
    private JButton toggleButton;
    private JPanel titleBar;
    private volatile boolean running = false;
    private Thread pingThread;
    private Point initialClick;

    // Colors
    private final Color bgColor = new Color(30, 30, 30);
    private final Color fgColor = new Color(200, 200, 200);
    private final Color activeColor = new Color(0, 150, 0); // green
    private final Color borderColor = new Color(80, 80, 80);

    public PingUI() {
        setUndecorated(true);
        setSize(400, 350); // reduced width
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // Custom border
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
        Timer clearTimer = new Timer(180000, e -> {
            
            logArea.setText(""); // clear all logs
            logArea.append("Logs Auto Cleared\n");
        });
        clearTimer.start();

        // Circle toggle button
        toggleButton = new JButton("Start") {
            @Override
            public Dimension getPreferredSize() {
                return new Dimension(80, 80); // fixed square
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

                int diameter = Math.min(c.getWidth(), c.getHeight()); // always circle
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

        add(titleBar, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        add(controlPanel, BorderLayout.SOUTH);

        // Actions
        toggleButton.addActionListener(e -> {
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

        // Check for updates on startup
        checkForUpdates();
    }

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

    // Update check feature
    private void checkForUpdates() {
        try {
            // Replace <user>/<repo> with your GitHub repo path
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

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            PingUI ui = new PingUI();
            ui.setVisible(true);
        });
    }
}

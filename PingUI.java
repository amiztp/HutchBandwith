import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.net.InetAddress;

public class PingUI extends JFrame {
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
        setSize(500, 350);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // Custom border
        getRootPane().setBorder(BorderFactory.createLineBorder(borderColor, 3));

        // Title bar (buttons aligned right)
        titleBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 5));
        titleBar.setBackground(bgColor);

        JButton closeButton = createCircleIconButton(Color.RED, "X");
        JButton minimizeButton = createCircleIconButton(Color.YELLOW, "_");
        JButton maximizeButton = createCircleIconButton(Color.GREEN, "⬜");

        titleBar.add(minimizeButton);
        titleBar.add(maximizeButton);
        titleBar.add(closeButton);

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

        // Round toggle button
        toggleButton = new JButton("Start");
        toggleButton.setPreferredSize(new Dimension(80, 80));
        toggleButton.setFocusPainted(false);
        toggleButton.setBorder(BorderFactory.createEmptyBorder());
        toggleButton.setFont(new Font("Arial", Font.BOLD, 14));
        toggleButton.setContentAreaFilled(false);
        toggleButton.setOpaque(false);

        // Custom round paint
        toggleButton.setUI(new javax.swing.plaf.basic.BasicButtonUI() {
            @Override
            public void paint(Graphics g, JComponent c) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                g2.setColor(running ? activeColor : new Color(70, 70, 70));
                g2.fillOval(0, 0, c.getWidth(), c.getHeight());

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
                g2.setColor(circleColor);
                g2.fillOval(0, 0, c.getWidth(), c.getHeight());

                // Draw icon text centered
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

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            PingUI ui = new PingUI();
            ui.setVisible(true);
        });
    }
}

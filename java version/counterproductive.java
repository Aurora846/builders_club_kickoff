import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.WindowConstants;
import java.awt.GridLayout;

public class counterproductive {
    // The flower illustration is drawn at this base size and then scaled per popup.
    private static final int FLOWER_BASE_WIDTH = 190;
    private static final int FLOWER_BASE_HEIGHT = 210;

    // Shared state for choosing popup locations, colors, and tracking open windows.
    private final Random random = new Random();
    private final List<javax.swing.JWindow> openPopups = new ArrayList<>();
    private final Map<javax.swing.JWindow, Timer> dodgeTimers = new HashMap<>();

    // Main application controls and the timer that schedules the next flower.
    private JFrame frame;
    private JLabel status;
    private JButton toggleButton;
    private JCheckBox randomButtonPosition;
    private JCheckBox multipleClicks;
    private JCheckBox cursorAvoidance;
    private Timer popupTimer;
    private boolean running;

    public static void main(String[] args) {
        // Swing components should be created on Swing's event-dispatch thread.
        SwingUtilities.invokeLater(() -> new counterproductive().createWindow());
    }

    private void createWindow() {
        // Build the small control window used to start and pause interruptions.
        frame = new JFrame("Counterproductive");
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        status = new JLabel("Ready to interrupt your focus.", SwingConstants.CENTER);
        toggleButton = new JButton("Start interruptions");
        toggleButton.addActionListener(event -> toggleInterruptions());
        // These options control how each newly created flower behaves.
        randomButtonPosition = new JCheckBox("Randomize close button position", true);
        multipleClicks = new JCheckBox("Some flowers need multiple clicks", true);
        cursorAvoidance = new JCheckBox("Flowers move away from the cursor", true);

        JPanel content = new JPanel(new GridLayout(0, 1, 4, 4));
        content.add(status);
        content.add(randomButtonPosition);
        content.add(multipleClicks);
        content.add(cursorAvoidance);
        content.add(toggleButton);
        frame.setContentPane(content);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        // This one-shot timer is restarted with a new random delay after each flower.
        popupTimer = new Timer(0, event -> randomFlower());
        popupTimer.setRepeats(false);
    }

    private void toggleInterruptions() {
        // Starting schedules flowers; stopping only prevents future ones from appearing.
        running = !running;
        toggleButton.setText(running ? "Stop interruptions" : "Start interruptions");
        updateStatus();
        if (running) {
            scheduleNextFlower();
        } else {
            popupTimer.stop();
        }
    }

    private void updateStatus() {
        // Keep the status label in sync with the run state and open popup count.
        String message = running ? "Interruptions running." : "Interruptions paused.";
        if (!openPopups.isEmpty()) {
            message += " Flowers to close: " + openPopups.size() + ".";
        }
        status.setText(message);
    }

    private void scheduleNextFlower() {
        if (running) {
            // Choose a delay from 2,000 through 10,000 milliseconds (2 to 10 seconds).
            popupTimer.setInitialDelay(2000 + random.nextInt(8001));
            popupTimer.restart();
        }
    }

    private void randomFlower() {
        // Create a flower window with its own button so it remains until dismissed.
        javax.swing.JWindow popup = new javax.swing.JWindow(frame);
        FlowerPanel flower = new FlowerPanel(random.nextInt(5), randomFlowerScale());
        // Draggability is decided separately for every flower.
        boolean draggable = random.nextBoolean();
        if (draggable) {
            makeDraggable(flower, popup);
            flower.setToolTipText("Drag this flower around");
        } else {
            flower.setToolTipText("This flower cannot be moved");
        }
        JPanel popupContent = new JPanel(new BorderLayout());

        JButton closeButton = new JButton("Close flower");
        // Snapshot the click mode and choose this flower's required number of clicks.
        boolean thisFlowerNeedsMultipleClicks = multipleClicks.isSelected();
        int clicksRemaining = thisFlowerNeedsMultipleClicks ? 1 + random.nextInt(4) : 1;
        updateCloseButtonText(closeButton, clicksRemaining);
        closeButton.addActionListener(event -> {
            int remaining = Integer.parseInt(closeButton.getActionCommand());
            remaining--;
            if (remaining == 0) {
                stopDodgeAnimation(popup);
                popup.dispose();
                openPopups.remove(popup);
                updateStatus();
            } else {
                updateCloseButtonText(closeButton, remaining);
                // A flower that needs more clicks jumps after a successful attempt.
                if (thisFlowerNeedsMultipleClicks) {
                    movePopupToRandomScreenPosition(popup);
                }
            }
        });
        if (randomButtonPosition.isSelected() && random.nextBoolean()) {
            popupContent.add(closeButton, BorderLayout.NORTH);
        } else {
            popupContent.add(closeButton, BorderLayout.SOUTH);
        }
        popupContent.add(flower, BorderLayout.CENTER);
        if (cursorAvoidance.isSelected()) {
            makeAvoidCursor(flower, closeButton, popup);
        }

        popup.setContentPane(popupContent);
        popup.pack();

        movePopupToRandomScreenPosition(popup);
        popup.setAlwaysOnTop(true);
        popup.setVisible(true);
        openPopups.add(popup);
        updateStatus();
        scheduleNextFlower();
    }

    private void updateCloseButtonText(JButton closeButton, int clicksRemaining) {
        closeButton.setActionCommand(Integer.toString(clicksRemaining));
        closeButton.setText(clicksRemaining == 1
            ? "Close flower"
            : "Close flower (" + clicksRemaining + " clicks left)");
    }

    private void movePopupToRandomScreenPosition(javax.swing.JWindow popup) {
        stopDodgeAnimation(popup);
        // Keep the complete popup inside the usable screen bounds.
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        int maxX = Math.max(0, screen.width - popup.getWidth());
        int maxY = Math.max(0, screen.height - popup.getHeight());
        popup.setLocation(random.nextInt(maxX + 1), random.nextInt(maxY + 1));
    }

    private void stopDodgeAnimation(javax.swing.JWindow popup) {
        Timer dodgeTimer = dodgeTimers.remove(popup);
        if (dodgeTimer != null) {
            dodgeTimer.stop();
        }
    }

    private void makeAvoidCursor(JPanel flower, JButton closeButton, javax.swing.JWindow popup) {
        // The cooldown prevents constant movement while the cursor remains nearby.
        long[] lastDodgeTime = {0};
        MouseMotionAdapter dodgeListener = new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent event) {
                if (!cursorAvoidance.isSelected()) {
                    return;
                }
                Point pointer = event.getLocationOnScreen();
                Point button = closeButton.getLocationOnScreen();
                button.translate(closeButton.getWidth() / 2, closeButton.getHeight() / 2);
                double dx = button.x - pointer.x;
                double dy = button.y - pointer.y;
                double distance = Math.hypot(dx, dy);
                long now = System.currentTimeMillis();
                if (distance > 85 || now - lastDodgeTime[0] < 650) {
                    return;
                }

                lastDodgeTime[0] = now;
                double directionX = distance == 0 ? (random.nextBoolean() ? 1 : -1) : dx / distance;
                double directionY = distance == 0 ? 0 : dy / distance;
                Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
                int startX = popup.getX();
                int startY = popup.getY();
                int maxX = Math.max(0, screen.width - popup.getWidth());
                int maxY = Math.max(0, screen.height - popup.getHeight());
                int targetX = Math.max(0, Math.min((int) Math.round(startX + directionX * 48), maxX));
                int targetY = Math.max(0, Math.min((int) Math.round(startY + directionY * 48), maxY));

                stopDodgeAnimation(popup);
                // Ease the popup toward its destination over roughly 420 milliseconds.
                long started = System.currentTimeMillis();
                Timer dodgeTimer = new Timer(16, timerEvent -> {
                    double progress = Math.min(1.0, (System.currentTimeMillis() - started) / 420.0);
                    double easedProgress = 1 - Math.pow(1 - progress, 3);
                    int x = (int) Math.round(startX + (targetX - startX) * easedProgress);
                    int y = (int) Math.round(startY + (targetY - startY) * easedProgress);
                    popup.setLocation(x, y);
                    if (progress >= 1.0) {
                        Timer completedTimer = (Timer) timerEvent.getSource();
                        completedTimer.stop();
                        if (dodgeTimers.get(popup) == completedTimer) {
                            dodgeTimers.remove(popup);
                        }
                    }
                });
                dodgeTimers.put(popup, dodgeTimer);
                dodgeTimer.start();
            }
        };
        flower.addMouseMotionListener(dodgeListener);
        closeButton.addMouseMotionListener(dodgeListener);
    }

    private double randomFlowerScale() {
        // Convert one centimeter to screen pixels and cap the adjustment at 25 percent.
        int pixelsPerCentimeter = (int) Math.round(Toolkit.getDefaultToolkit().getScreenResolution() / 2.54);
        int maxAdjustment = Math.min(pixelsPerCentimeter, FLOWER_BASE_WIDTH / 4);
        int widthAdjustment = random.nextInt(maxAdjustment * 2 + 1) - maxAdjustment;
        return (double) (FLOWER_BASE_WIDTH + widthAdjustment) / FLOWER_BASE_WIDTH;
    }

    private void makeDraggable(JPanel flower, javax.swing.JWindow popup) {
        flower.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
        MouseAdapter dragHandler = new MouseAdapter() {
            private Point dragOffset;

            @Override
            public void mousePressed(MouseEvent event) {
                Point popupLocation = popup.getLocation();
                Point pointerLocation = event.getLocationOnScreen();
                dragOffset = new Point(
                    pointerLocation.x - popupLocation.x,
                    pointerLocation.y - popupLocation.y
                );
            }

            @Override
            public void mouseReleased(MouseEvent event) {
                dragOffset = null;
            }

            @Override
            public void mouseDragged(MouseEvent event) {
                if (dragOffset == null) {
                    return;
                }
                // Preserve the original grab point and clamp movement to the screen.
                Point pointerLocation = event.getLocationOnScreen();
                Dimension screen = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
                int maxX = Math.max(0, screen.width - popup.getWidth());
                int maxY = Math.max(0, screen.height - popup.getHeight());
                int x = Math.max(0, Math.min(pointerLocation.x - dragOffset.x, maxX));
                int y = Math.max(0, Math.min(pointerLocation.y - dragOffset.y, maxY));
                popup.setLocation(x, y);
            }
        };
        flower.addMouseListener(dragHandler);
        flower.addMouseMotionListener(dragHandler);
    }

    // A custom Swing panel that paints a simple flower using Java2D shapes.
    private static class FlowerPanel extends JPanel {
        // Each popup chooses one of these colors for its petals.
        private final Color[] palettes = {
            new Color(239, 91, 119), new Color(246, 174, 45),
            new Color(155, 113, 191), new Color(235, 220, 104),
            new Color(238, 130, 73)
        };
        private final Color petalColor;
        private final double scale;

        FlowerPanel(int paletteIndex, double scale) {
            petalColor = palettes[paletteIndex];
            this.scale = scale;
            setPreferredSize(new Dimension(
                (int) Math.round(FLOWER_BASE_WIDTH * scale),
                (int) Math.round(FLOWER_BASE_HEIGHT * scale)
            ));
            setBackground(new Color(255, 250, 235));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            // Use a copied graphics context so drawing settings do not leak to Swing.
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.scale(scale, scale);

            // Draw the stem and leaves first so the flower head appears in front.
            g.setColor(new Color(67, 135, 83));
            g.setStroke(new BasicStroke(5, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.drawLine(95, 102, 95, 190);
            g.fillOval(63, 143, 34, 14);
            g.fillOval(94, 159, 34, 14);

            // Rotate the drawing context around the flower center to arrange the petals.
            Point center = new Point(95, 82);
            for (int petal = 0; petal < 10; petal++) {
                AffineTransform oldTransform = g.getTransform();
                g.rotate(Math.PI * 2 * petal / 10, center.x, center.y);
                g.setColor(petalColor);
                g.fillOval(center.x - 12, center.y - 43, 24, 39);
                g.setTransform(oldTransform);
            }

            // Finish with the center of the flower, then release the copied context.
            g.setColor(new Color(245, 195, 57));
            g.fillOval(center.x - 15, center.y - 15, 30, 30);
            g.setColor(new Color(155, 105, 31));
            g.fillOval(center.x - 3, center.y - 5, 3, 3);
            g.fillOval(center.x + 4, center.y + 2, 3, 3);
            g.fillOval(center.x - 6, center.y + 4, 3, 3);
            g.dispose();
        }
    }
}
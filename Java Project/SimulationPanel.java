import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.geom.Ellipse2D;
import java.util.List;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;

//Draws the celestial bodies, and animates them using real gravity each frame
public class SimulationPanel extends JPanel {

    private static final double DISTANCE_SCALE = 0.14;
    private static final double SIZE_SCALE = 0.3;
    private static final int MIN_RADIUS_PX = 2;
    private static final double ZOOM_PER_NOTCH = 1.2;
    private static final double MIN_ZOOM = 0.5;
    private static final double MAX_ZOOM = 30;

    private static final int TICK_DELAY_MS = 30;
    private static final int STEPS_PER_TICK = 400;
    // Sim time per physics step; at 400 steps per 30ms tick, Earth orbits in ~15s on screen
    private static final double DT = 5000;
    private static final int MAX_EVENTS_SHOWN = 5;
    private static final int LINE_HEIGHT = 16;
    private static final Color HABITABLE_ZONE = new Color(60, 200, 90, 45);
    // Same mass as the Sun, so a black hole dropped nearby disrupts the system without instantly swallowing it
    private static final double SPAWNED_BLACK_HOLE_MASS = 1000000;

    private final List<Body> bodies;
    private final CollisionRules collisionRules;
    private final Deque<String> recentEvents = new ArrayDeque<>();
    private int meteorsDropped = 0;
    private int blackHolesDropped = 0;

    private double zoom = 1;
    // Screen offset in pixels, changed by dragging
    private double panX = 0;
    private double panY = 0;

    SimulationPanel(List<? extends Body> bodies, CollisionRules collisionRules){
        this.bodies = new ArrayList<>(bodies);
        this.collisionRules = collisionRules;
        setBackground(Color.BLACK);
        setPreferredSize(new Dimension(1000,600));

        MouseAdapter mouse = new MouseAdapter() {
            private int lastX;
            private int lastY;

            @Override
            public void mouseClicked(MouseEvent e) {
                double simX = toSimX(e.getX());
                double simY = toSimY(e.getY());
                if (SwingUtilities.isRightMouseButton(e)){
                    dropBlackHole(simX, simY);
                } else {
                    dropMeteor(simX, simY);
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                lastX = e.getX();
                lastY = e.getY();
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                panX += e.getX() - lastX;
                panY += e.getY() - lastY;
                lastX = e.getX();
                lastY = e.getY();
                repaint();
            }

            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                zoomAround(e.getX(), e.getY(), Math.pow(ZOOM_PER_NOTCH, -e.getPreciseWheelRotation()));
            }
        };
        addMouseListener(mouse);
        addMouseMotionListener(mouse);
        addMouseWheelListener(mouse);

        Timer timer = new Timer(TICK_DELAY_MS, e -> tick());
        timer.start();
    }

    private void tick(){
        // Collisions are checked every step so fast bodies can't skip past each other between ticks
        for (int step = 0; step < STEPS_PER_TICK; step++){
            Physics.step(bodies, DT);
            collisionRules.resolveAll(bodies).forEach(collision -> logEvent(collision.description()));
        }
        bodies.forEach(body -> body.update(bodies, STEPS_PER_TICK * DT).ifPresent(this::logEvent));
        repaint();
    }

    private void logEvent(String event){
        System.out.println(event);
        recentEvents.addFirst(event);
        if (recentEvents.size() > MAX_EVENTS_SHOWN){
            recentEvents.removeLast();
        }
    }

    private void dropMeteor(double simX, double simY){
        meteorsDropped++;
        spawn(() -> new Meteor("Meteor " + meteorsDropped, 0.5, new Location(simX, simY), 2, new Velocity(0, 0)));
    }

    private void dropBlackHole(double simX, double simY){
        blackHolesDropped++;
        spawn(() -> new BlackHole("Black Hole " + blackHolesDropped, SPAWNED_BLACK_HOLE_MASS, new Location(simX, simY), 8, new Velocity(0, 0)));
    }

    private void spawn(Supplier<Body> create){
        try {
            bodies.add(create.get());
            repaint();
        } catch (InvalidBodyException ex) {
            System.err.println("Could not create body: " + ex.getMessage());
        }
    }

    // Keeps the point under the mouse fixed while zooming
    private void zoomAround(int screenX, int screenY, double factor){
        double simX = toSimX(screenX);
        double simY = toSimY(screenY);
        zoom = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, zoom * factor));
        panX = screenX - getWidth() / 2.0 - simX * scale();
        panY = screenY - getHeight() / 2.0 - simY * scale();
        repaint();
    }

    private double scale(){
        return DISTANCE_SCALE * zoom;
    }

    private int toScreenX(double simX){
        return (int) Math.round(getWidth() / 2.0 + panX + simX * scale());
    }

    private int toScreenY(double simY){
        return (int) Math.round(getHeight() / 2.0 + panY + simY * scale());
    }

    private double toSimX(int screenX){
        return (screenX - getWidth() / 2.0 - panX) / scale();
    }

    private double toSimY(int screenY){
        return (screenY - getHeight() / 2.0 - panY) / scale();
    }

    @Override
    protected void paintComponent(Graphics g){
        super.paintComponent(g);
        Graphics2D g2=(Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);

        drawHabitableZones(g2);
        for (Body body : bodies){
            int r=Math.max(MIN_RADIUS_PX,(int) Math.round(body.radius * SIZE_SCALE * zoom));
            body.draw(g2, toScreenX(body.l.x), toScreenY(body.l.y), r);
        }

        drawBarycentre(g2);
        drawStats(g2);
    }

    //A translucent band from HABITABLE_MIN to HABITABLE_MAX around every star
    private void drawHabitableZones(Graphics2D g2){
        double midRadius = (Star.HABITABLE_MIN + Star.HABITABLE_MAX) / 2 * scale();
        float width = (float) ((Star.HABITABLE_MAX - Star.HABITABLE_MIN) * scale());
        Stroke original = g2.getStroke();
        g2.setStroke(new BasicStroke(width));
        g2.setColor(HABITABLE_ZONE);
        bodies.stream()
                .filter(Star.class::isInstance)
                .forEach(star -> {
                    int x = toScreenX(star.l.x);
                    int y = toScreenY(star.l.y);
                    g2.draw(new Ellipse2D.Double(x - midRadius, y - midRadius, midRadius * 2, midRadius * 2));
                });
        g2.setStroke(original);
    }

    private void drawBarycentre(Graphics2D g2){
        Location barycentre = bodies.stream().collect(SimulationStats.barycentre());
        int x = toScreenX(barycentre.x);
        int y = toScreenY(barycentre.y);
        g2.setColor(Color.RED);
        g2.drawLine(x - 5, y, x + 5, y);
        g2.drawLine(x, y - 5, x, y + 5);
        g2.drawString("barycentre", x + 8, y + 4);
    }

    private void drawStats(Graphics2D g2){
        int y = LINE_HEIGHT;
        g2.setColor(Color.WHITE);

        String counts = SimulationStats.countByType(bodies).entrySet().stream()
                .map(entry -> entry.getKey() + ": " + entry.getValue())
                .collect(Collectors.joining("   "));
        g2.drawString(counts, 10, y);

        Map<Boolean, Long> life = SimulationStats.planetsByLife(bodies);
        g2.drawString("Planets with life: " + life.get(true) + " / " + (life.get(true) + life.get(false)), 10, y += LINE_HEIGHT);

        g2.setColor(Color.LIGHT_GRAY);
        for (String event : recentEvents){
            g2.drawString(event, 10, y += LINE_HEIGHT);
        }

        g2.setColor(Color.GRAY);
        g2.drawString("Scroll to zoom  |  drag to pan  |  left-click: drop a meteor  |  right-click: drop a black hole", 10, getHeight() - 10);
    }
}

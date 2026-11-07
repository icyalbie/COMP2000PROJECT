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
import java.util.stream.Collectors;
import javax.swing.JPanel;
import javax.swing.Timer;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

//Draws the celestial bodies, and animates them using real gravity each frame
public class SimulationPanel extends JPanel {

    private static final double DISTANCE_SCALE = 0.14;
    private static final double SIZE_SCALE = 0.3;
    private static final int MIN_RADIUS_PX = 2;

    private static final int TICK_DELAY_MS = 30;
    private static final int STEPS_PER_TICK = 400;
    // Sim time per physics step; at 400 steps per 30ms tick, Earth orbits in ~15s on screen
    private static final double DT = 5000;
    private static final int MAX_EVENTS_SHOWN = 5;
    private static final int LINE_HEIGHT = 16;
    private static final Color HABITABLE_ZONE = new Color(60, 200, 90, 45);

    private final List<Body> bodies;
    private final CollisionRules collisionRules;
    private final Deque<String> recentEvents = new ArrayDeque<>();

    SimulationPanel(List<? extends Body> bodies, CollisionRules collisionRules){
        this.bodies = new ArrayList<>(bodies);
        this.collisionRules = collisionRules;
        setBackground(Color.BLACK);
        setPreferredSize(new Dimension(1000,600));

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int cx = getWidth() / 2;
                int cy = getHeight() / 2;
                double simX = (e.getX() - cx) / DISTANCE_SCALE;
                double simY = (e.getY() - cy) / DISTANCE_SCALE;

                try {
                    Meteor meteor = new Meteor(
                            "Meteor",
                            0.5,
                            new Location(simX, simY),
                            2,
                            new Velocity(0, 0)
                    );
                    SimulationPanel.this.bodies.add(meteor);
                    repaint();
                } catch (InvalidBodyException ex) {
                    System.err.println("Could not create meteor: " + ex.getMessage());
                }
            }
        });

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

    @Override
    protected void paintComponent(Graphics g){
        super.paintComponent(g);
        Graphics2D g2=(Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);

        int centreX=getWidth()/2;
        int centreY=getHeight()/2;

        drawHabitableZones(g2, centreX, centreY);
        for (Body body : bodies){
            int x=centreX + (int) Math.round(body.l.x * DISTANCE_SCALE);
            int y=centreY + (int) Math.round(body.l.y * DISTANCE_SCALE);
            int r=Math.max(MIN_RADIUS_PX,(int) Math.round(body.radius * SIZE_SCALE));
            body.draw(g2, x, y, r);
        }

        drawBarycentre(g2, centreX, centreY);
        drawStats(g2);
    }

    //A translucent band from HABITABLE_MIN to HABITABLE_MAX around every star
    private void drawHabitableZones(Graphics2D g2, int centreX, int centreY){
        double midRadius = (Star.HABITABLE_MIN + Star.HABITABLE_MAX) / 2 * DISTANCE_SCALE;
        float width = (float) ((Star.HABITABLE_MAX - Star.HABITABLE_MIN) * DISTANCE_SCALE);
        Stroke original = g2.getStroke();
        g2.setStroke(new BasicStroke(width));
        g2.setColor(HABITABLE_ZONE);
        bodies.stream()
                .filter(Star.class::isInstance)
                .forEach(star -> {
                    double x = centreX + star.l.x * DISTANCE_SCALE;
                    double y = centreY + star.l.y * DISTANCE_SCALE;
                    g2.draw(new Ellipse2D.Double(x - midRadius, y - midRadius, midRadius * 2, midRadius * 2));
                });
        g2.setStroke(original);
    }

    private void drawBarycentre(Graphics2D g2, int centreX, int centreY){
        Location barycentre = bodies.stream().collect(SimulationStats.barycentre());
        int x = centreX + (int) Math.round(barycentre.x * DISTANCE_SCALE);
        int y = centreY + (int) Math.round(barycentre.y * DISTANCE_SCALE);
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
    }
}
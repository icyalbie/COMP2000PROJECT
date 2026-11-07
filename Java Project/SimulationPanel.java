import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
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

    private final List<Body> bodies;

    SimulationPanel(List<? extends Body> bodies){
        this.bodies = new ArrayList<>(bodies);
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
        for (int step = 0; step < STEPS_PER_TICK; step++){
            applyGravity();
        }
        handleBlackHoleCollisions();
        repaint();
    }

    private void applyGravity(){
        for (Body body : bodies){
            double ax = 0;
            double ay = 0;
            for (Body source : bodies){
                if (source == body) continue;
                Velocity a = Physics.calculateGravity(body, source);
                ax += a.vx;
                ay += a.vy;
            }
            Physics.updateBody(body, new Velocity(ax, ay), DT);
        }
    }

    private void handleBlackHoleCollisions(){
        Iterator<Body> it = bodies.iterator();
        while (it.hasNext()){
            Body body = it.next();
            if (body instanceof BlackHole) continue;

            for (Body source : bodies){
                if (!(source instanceof BlackHole)) continue;

                double dx = source.l.x - body.l.x;
                double dy = source.l.y - body.l.y;
                double distance = Math.sqrt(dx * dx + dy * dy);

                if (distance < source.radius + body.radius){
                    try {
                        throw new CollisionException(body.name + " was consumed by " + source.name);
                    } catch (CollisionException ex) {
                        System.out.println(ex.getMessage());
                        it.remove();
                    }
                    break;
                }
            }
        }
    }

    @Override
    protected void paintComponent(Graphics g){
        super.paintComponent(g);
        Graphics2D g2=(Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);

        int centreX=getWidth()/2;
        int centreY=getHeight()/2;

        for (Body body : bodies){
            int x=centreX + (int) Math.round(body.l.x * DISTANCE_SCALE);
            int y=centreY + (int) Math.round(body.l.y * DISTANCE_SCALE);
            int r=Math.max(MIN_RADIUS_PX,(int) Math.round(body.radius * SIZE_SCALE));

            g2.setColor(body.colour());
            g2.fillOval(x-r,y-r,r*2,r*2);
            g2.drawString(body.name,x+r+4,y-r-4);
        }
    }
}
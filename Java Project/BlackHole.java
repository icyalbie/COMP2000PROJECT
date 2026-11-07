import java.awt.Color;
import java.awt.Graphics2D;

public class BlackHole extends Body {

    private static final Color GLOW = new Color(180, 100, 255);
    private static final int GLOW_GAP_PX = 4;

    BlackHole(String name, double mass, Location l, double radius, Velocity v){
        super(name, mass, l, radius, v);
    }

    @Override
    Color colour(){
        return Color.BLACK;
    }

    //A black body is invisible on the black background, so outline it with a glowing ring
    @Override
    void draw(Graphics2D g2, int x, int y, int r){
        int ring = r + GLOW_GAP_PX;
        g2.setColor(GLOW);
        g2.drawOval(x-ring,y-ring,ring*2,ring*2);
        g2.setColor(colour());
        g2.fillOval(x-r,y-r,r*2,r*2);
        g2.setColor(GLOW);
        g2.drawString(name,x+ring+4,y-ring-4);
    }
}
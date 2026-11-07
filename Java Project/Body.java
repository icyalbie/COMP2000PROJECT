import java.awt.Color;
import java.awt.Graphics2D;

public class Body {
       String  name;
       double mass;
       Location l;
       double radius ;
       Velocity v;

       Body(String name , double mass, Location l, double radius ,Velocity v){
        if (name == null || name.trim().isEmpty()){
            throw new InvalidBodyException("A body needs a name");
        }
        if (mass <= 0){
            throw new InvalidBodyException(name + " needs a positive mass, got " + mass);
        }
        if (radius <= 0){
            throw new InvalidBodyException(name + " needs a positive radius, got " + radius);
        }
        this.name=name;
        this.mass=mass;
        this.l=l;
        this.radius=radius;
        this.v=v;

    }

    Color colour(){
        return Color.LIGHT_GRAY;
    }

    //x, y and r are already in screen pixels
    void draw(Graphics2D g2, int x, int y, int r){
        g2.setColor(colour());
        g2.fillOval(x-r,y-r,r*2,r*2);
        g2.drawString(name,x+r+4,y-r-4);
    }
}

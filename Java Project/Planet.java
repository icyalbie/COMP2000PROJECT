import java.awt.Color;

public class Planet extends Body{

    boolean hasLife;

    Planet(String name, double mass, Location l, double radius, Velocity v, boolean hasLife){
        super(name, mass, l, radius, v);
        this.hasLife = hasLife;
    }

    Planet(String name, double mass, Location l, double radius, Velocity v){
        this(name, mass, l, radius, v, false);
    }

    @Override
    Color colour(){
        if (hasLife){
            return new Color(60, 200, 90);
        }
        return new Color(120,170,255);
    }
}
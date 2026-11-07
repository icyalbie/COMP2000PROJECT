import java.awt.Color;

public class Star  extends Body{

    //Distances from a star where a planet is neither too hot nor too cold for life (Earth orbits at 100)
    static final double HABITABLE_MIN = 85;
    static final double HABITABLE_MAX = 140;


    Star(String name , double mass, Location l, double radius,Velocity v ){
        super(name,mass,l,radius,v);
    }

    @Override
    Color colour(){
        return new Color(255,214,102);
    }
}

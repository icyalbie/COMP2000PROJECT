import javax.swing.SwingUtilities;

public class Main{
    public static void main (String []args){

        Star sun = new Star ("Sun",1000000,new Location(0,0),10,new Velocity(0,0));
        Planet mercury=new Planet("Mercury",1.660,new Location(38.7,0),2,new Velocity(0,0));
        Planet venus=new Planet("Venus",24.47,new Location(72.3,0),2.5,new Velocity(0,0));
        Planet earth=new Planet("Earth",30.03,new Location(100,0),2.7,new Velocity(0,0), true);
        Planet mars=new Planet("Mars",3.227,new Location(152.4,0),2.2,new Velocity(0,0));
        Planet jupiter=new Planet("Jupiter",954.6,new Location(520.4,0),6,new Velocity(0,0));
        Planet saturn=new Planet("Saturn",285.8,new Location(957.4,0),5,new Velocity(0,0));
        Planet uranus=new Planet("Uranus",43.66,new Location(1921,0),4,new Velocity(0,0));
        Planet neptune=new Planet("Neptune",51.51,new Location(3002,0),4, new Velocity(0,0));
        BlackHole sagA = new BlackHole("Sagittarius A*", 5000000, new Location(-1500,0), 8, new Velocity(0,0));

        Planet[] planets = {mercury, venus, earth, mars, jupiter, saturn, uranus, neptune};
        for (Planet planet : planets){
            planet.v = Physics.circularOrbit(sun, planet.l);
        }

        Galaxy<Body> galaxy = new Galaxy<>();
        try {
            galaxy.add(sun);
            galaxy.add(mercury);
            galaxy.add(venus);
            galaxy.add(earth);
            galaxy.add(mars);
            galaxy.add(jupiter);
            galaxy.add(saturn);
            galaxy.add(uranus);
            galaxy.add(neptune);
            galaxy.add(sagA);
        } catch (DuplicateBodyException e) {
            System.err.println("Could not build the galaxy: " + e.getMessage());
            return;
        }

    // checking newobjects
    //     System.out.println(jupiter.mass);
        
    // checking physics gravity method
    //     Velocity acceleration =Physics.calculateGravity(earth,sun);
    //     System.out.println("X acceleration: "+ acceleration.vx);
    //     System.out.println("Y acceleration: "+ acceleration.vy);
    //     
    //  //checking acceleration  and if it changes the location aspects
    //      for (int i=0;i<10000;i++){
    //         Velocity acceleration= Physics.calculateGravity(earth,sun);
    //         Physics.updateBody(earth,acceleration,1);
    //         System.out.println("Earths x:"+ earth.l.x);
    //         System.out.println("Earths y:"+ earth.l.y);
    //         System.out.println("Earths vx:"+ earth.v.vx);
    //         System.out.println("Earths vy:"+ earth.v.vy);
    //         

    //     }
    //      System.out.println("Earths x:"+ earth.l.x);
    //      System.out.println("Earths y:"+ earth.l.y);
    //      System.out.println("Earths vx:"+ earth.v.vx);
    //      System.out.println("Earths vy:"+ earth.v.vy);
    

        SwingUtilities.invokeLater(() -> new SimulationWindow("Solar System", galaxy.byDescendingRadius(), CollisionRules.standard()).start());
    }

}

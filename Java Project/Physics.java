public class Physics {
    static final double G=3.96e-17;// Please dont change as this is my final calculation for gravitational constant acording to scale ~ Leon Prince :)
    // Keeps acceleration finite (instead of infinite/NaN) when two bodies overlap
    static final double SOFTENING=1.0;

    static Velocity calculateGravity(Body body,Body source){
        double dx=source.l.x-body.l.x;
        double dy=source.l.y-body.l.y;
        double distanceSq=dx*dx+dy*dy+SOFTENING*SOFTENING;
        double distance= Math.sqrt(distanceSq);
        double acceleration=G * source.mass / distanceSq;
        double ax =acceleration * dx/distance;
        double ay=acceleration * dy/distance;
        return new Velocity(ax,ay);

    }
    static void updateBody(Body body ,Velocity acceleration, double dt){
        body.v.vx+=acceleration.vx * dt;
        body.v.vy+=acceleration.vy * dt ;

        body.l.x+=body.v.vx * dt;
        body.l.y+=body.v.vy * dt;
    }

    // Velocity needed at the given location to circle the centre body, v = sqrt(GM/r)
    static Velocity circularOrbit(Body centre, Location at){
        double dx=at.x-centre.l.x;
        double dy=at.y-centre.l.y;
        double r=Math.sqrt(dx*dx+dy*dy);
        double speed=Math.sqrt(G * centre.mass / r);
        return new Velocity(centre.v.vx - dy/r*speed, centre.v.vy + dx/r*speed);
    }

    
    
    
}

import java.util.List;
import java.util.stream.Stream;

public class Physics {
    static final double G=3.96e-17;// Please dont change as this is my final calculation for gravitational constant acording to scale ~ Leon Prince :)
    // Keeps acceleration finite (instead of infinite/NaN) when two bodies overlap
    static final double SOFTENING=1.0;
    // Measured: below ~150 bodies, splitting work across threads costs more than it saves
    static final int PARALLEL_THRESHOLD=150;

    // Phase 1 works out every acceleration from the same positions before anything moves, so the
    // result doesn't depend on list order, and it only reads shared state so it is safe to run in parallel.
    // Phase 2 then moves the bodies one at a time.
    static void step(List<? extends Body> bodies, double dt){
        List<Velocity> accelerations = accelerations(bodies, bodies.size() >= PARALLEL_THRESHOLD);
        for (int i = 0; i < bodies.size(); i++){
            updateBody(bodies.get(i), accelerations.get(i), dt);
        }
    }

    static List<Velocity> accelerations(List<? extends Body> bodies, boolean parallel){
        Stream<? extends Body> stream = parallel ? bodies.parallelStream() : bodies.stream();
        return stream.map(body -> netAcceleration(body, bodies)).toList();
    }

    static Velocity netAcceleration(Body body, List<? extends Body> bodies){
        return bodies.stream()
                .filter(source -> source != body)
                .map(source -> calculateGravity(body, source))
                .reduce(new Velocity(0, 0), (a, b) -> new Velocity(a.vx + b.vx, a.vy + b.vy));
    }

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

    static boolean touching(Body a, Body b){
        double dx=b.l.x-a.l.x;
        double dy=b.l.y-a.l.y;
        return Math.sqrt(dx*dx+dy*dy) < a.radius + b.radius;
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

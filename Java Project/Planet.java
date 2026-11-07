import java.awt.Color;
import java.util.List;
import java.util.Optional;

public class Planet extends Body{

    //Earth's orbital period in sim time (~15s on screen)
    static final double YEAR = 1e9;
    static final double YEARS_FOR_LIFE_TO_EMERGE = 2;
    static final double YEARS_FOR_LIFE_TO_DIE = 0.5;

    boolean hasLife;
    private boolean wasHabitable;
    //How long the planet has been inside (or outside) the habitable zone without a change
    private double timeInConditions;

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

    //Restarts the clock, so life has to emerge from scratch afterwards
    void wipeOutLife(){
        hasLife = false;
        timeInConditions = 0;
    }

    @Override
    Optional<String> update(List<? extends Body> bodies, double dt){
        Optional<Double> starDistance = nearestStarDistance(bodies);
        boolean habitable = starDistance
                .map(d -> d >= Star.HABITABLE_MIN && d <= Star.HABITABLE_MAX)
                .orElse(false);

        if (habitable != wasHabitable){
            wasHabitable = habitable;
            timeInConditions = 0;
        }
        timeInConditions += dt;

        if (habitable && !hasLife && timeInConditions >= YEARS_FOR_LIFE_TO_EMERGE * YEAR){
            hasLife = true;
            return Optional.of("Life emerged on " + name);
        }
        if (!habitable && hasLife && timeInConditions >= YEARS_FOR_LIFE_TO_DIE * YEAR){
            wipeOutLife();
            String cause = starDistance
                    .map(d -> d < Star.HABITABLE_MIN ? " burned away" : " froze")
                    .orElse(" froze with no star to warm it");
            return Optional.of("Life on " + name + cause);
        }
        return Optional.empty();
    }

    //Empty once every star has been destroyed
    private Optional<Double> nearestStarDistance(List<? extends Body> bodies){
        return bodies.stream()
                .filter(Star.class::isInstance)
                .map(star -> Math.hypot(star.l.x - l.x, star.l.y - l.y))
                .min(Double::compare);
    }
}

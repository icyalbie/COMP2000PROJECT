import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collector;
import java.util.stream.Collectors;

//Summaries of the current bodies, shown on screen by SimulationPanel
public class SimulationStats {

    //Sorted by type name so the on-screen order doesn't jump around
    static Map<String, Long> countByType(List<? extends Body> bodies){
        return bodies.stream()
                .collect(Collectors.groupingBy(body -> body.getClass().getSimpleName(), TreeMap::new, Collectors.counting()));
    }

    //true -> planets with life, false -> planets without
    static Map<Boolean, Long> planetsByLife(List<? extends Body> bodies){
        return bodies.stream()
                .filter(Planet.class::isInstance)
                .map(Planet.class::cast)
                .collect(Collectors.partitioningBy(planet -> planet.hasLife, Collectors.counting()));
    }

    //Custom collector for the barycentre: the mass-weighted centre that the whole system orbits around.
    //Accumulates {mass * x, mass * y, total mass}; the combiner merges partial sums so it also works in parallel
    static Collector<Body, double[], Location> barycentre(){
        return Collector.of(
                () -> new double[3],
                (sums, body) -> {
                    sums[0] += body.mass * body.l.x;
                    sums[1] += body.mass * body.l.y;
                    sums[2] += body.mass;
                },
                (a, b) -> {
                    a[0] += b[0];
                    a[1] += b[1];
                    a[2] += b[2];
                    return a;
                },
                sums -> new Location(sums[0] / sums[2], sums[1] / sums[2]));
    }
}

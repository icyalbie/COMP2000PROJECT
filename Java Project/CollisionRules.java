import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

//Strategy pattern: each collision behaviour is a CollisionRule lambda,
//chosen at runtime by the types of the two bodies that touched
public class CollisionRules {

    record Collision(Body survivor, Body lost, String description) {}

    private record Entry<S extends Body, L extends Body>(Class<S> survivorType, Class<L> lostType, CollisionRule<S, L> rule) {
        boolean matches(Body survivor, Body lost){
            return survivorType.isInstance(survivor) && lostType.isInstance(lost);
        }

        Collision apply(Body survivor, Body lost){
            String description = rule.resolve(survivorType.cast(survivor), lostType.cast(lost));
            return new Collision(survivor, lost, description);
        }
    }

    private final List<Entry<?, ?>> entries = new ArrayList<>();

    //Rules are tried in the order they were added, so add specific rules before general ones
    <S extends Body, L extends Body> CollisionRules when(Class<S> survivorType, Class<L> lostType, CollisionRule<S, L> rule){
        entries.add(new Entry<>(survivorType, lostType, rule));
        return this;
    }

    //Empty when no rule covers the pair, in which case the bodies pass through each other
    Optional<Collision> resolve(Body a, Body b){
        Body heavier = a.mass >= b.mass ? a : b;
        Body lighter = heavier == a ? b : a;
        for (Entry<?, ?> entry : entries){
            if (entry.matches(heavier, lighter)) return Optional.of(entry.apply(heavier, lighter));
            if (entry.matches(lighter, heavier)) return Optional.of(entry.apply(lighter, heavier));
        }
        return Optional.empty();
    }

    //Resolves every touching pair, removes the lost bodies and returns what happened
    List<Collision> resolveAll(List<Body> bodies){
        List<Collision> collisions = new ArrayList<>();
        List<Body> lost = new ArrayList<>();
        for (int i = 0; i < bodies.size(); i++){
            Body a = bodies.get(i);
            for (int j = i + 1; j < bodies.size() && !lost.contains(a); j++){
                Body b = bodies.get(j);
                if (lost.contains(b) || !Physics.touching(a, b)) continue;
                resolve(a, b).ifPresent(collision -> {
                    collisions.add(collision);
                    lost.add(collision.lost());
                });
            }
        }
        bodies.removeAll(lost);
        return collisions;
    }

    static CollisionRules standard(){
        return new CollisionRules()
                .when(BlackHole.class, Body.class, (hole, body) -> {
                    absorb(hole, body);
                    return body.name + " was consumed by " + hole.name;
                })
                .when(Star.class, Body.class, (star, body) -> {
                    absorb(star, body);
                    return body.name + " burned up in " + star.name;
                })
                .when(Planet.class, Meteor.class, (planet, meteor) -> {
                    absorb(planet, meteor);
                    if (planet.hasLife){
                        planet.wipeOutLife();
                        return meteor.name + " struck " + planet.name + " and wiped out all life";
                    }
                    return meteor.name + " struck " + planet.name;
                })
                .when(Planet.class, Planet.class, (bigger, smaller) -> {
                    absorb(bigger, smaller);
                    bigger.wipeOutLife();
                    return smaller.name + " smashed into " + bigger.name;
                })
                .when(Body.class, Body.class, (bigger, smaller) -> {
                    absorb(bigger, smaller);
                    return smaller.name + " merged into " + bigger.name;
                });
    }

    //Conserves momentum and volume so the merged body keeps moving realistically, and merges at the
    //pair's centre of mass so the system's barycentre doesn't jump
    private static void absorb(Body survivor, Body lost){
        double totalMass = survivor.mass + lost.mass;
        survivor.l.x = (survivor.mass * survivor.l.x + lost.mass * lost.l.x) / totalMass;
        survivor.l.y = (survivor.mass * survivor.l.y + lost.mass * lost.l.y) / totalMass;
        survivor.v.vx = (survivor.mass * survivor.v.vx + lost.mass * lost.v.vx) / totalMass;
        survivor.v.vy = (survivor.mass * survivor.v.vy + lost.mass * lost.v.vy) / totalMass;
        survivor.mass = totalMass;
        survivor.radius = Math.cbrt(Math.pow(survivor.radius, 3) + Math.pow(lost.radius, 3));
    }
}

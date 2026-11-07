//What happens when two bodies touch. The survivor stays in the simulation, the lost body is removed
//Returns a description of the collision
@FunctionalInterface
interface CollisionRule<S extends Body, L extends Body> {
    String resolve(S survivor, L lost);
}

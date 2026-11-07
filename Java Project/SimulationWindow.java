import java.util.List;
import javax.swing.JFrame;

//Simulation Window - add redraw loop later
public class SimulationWindow extends JFrame {

    SimulationWindow(String title, List<? extends Body> bodies, CollisionRules collisionRules){
        super(title);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        add(new SimulationPanel(bodies, collisionRules));
        pack();
        setLocationRelativeTo(null);
    }

    void start(){
        setVisible(true);
    }
}

package codelets.perception;

import br.unicamp.cst.core.entities.Codelet;
import br.unicamp.cst.core.entities.MemoryObject;
import ws3dproxy.model.Thing;
import ws3dproxy.util.Constants;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class WallDetector extends Codelet {
    private MemoryObject visionMO;
    private MemoryObject knownWallsMO;

    @Override
    public void accessMemoryObjects() {
        synchronized (this) {
            this.visionMO = (MemoryObject) this.getInput("VISION");
        }
        this.knownWallsMO = (MemoryObject) this.getOutput("WALLS");
    }

    @Override
    public void calculateActivation() {

    }

    @Override
    public void proc() {
        CopyOnWriteArrayList<Thing> vision;
        List<Thing> known;
        synchronized (visionMO) {
            //vision = Collections.synchronizedList((List<Thing>) visionMO.getI());
            vision = new CopyOnWriteArrayList((List<Thing>) visionMO.getI());
            known = Collections.synchronizedList((List<Thing>) knownWallsMO.getI());
            synchronized (vision) {
                for (Thing t : vision) {
                    boolean found = false;
                    synchronized (known) {
                        CopyOnWriteArrayList<Thing> myknown = new CopyOnWriteArrayList<>(known);
                        for (Thing e : myknown)
                            if (t.getName().equals(e.getName())) {
                                found = true;
                                break;
                            }
                        if (found == false && t.getCategory() == Constants.categoryBRICK) known.add(t);
                    }

                }
            }
        }
        knownWallsMO.setI(known);
    }
}

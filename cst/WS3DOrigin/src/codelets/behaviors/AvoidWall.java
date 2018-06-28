package codelets.behaviors;

import br.unicamp.cst.core.entities.Codelet;
import br.unicamp.cst.core.entities.MemoryContainer;
import br.unicamp.cst.core.entities.MemoryObject;
import memory.CreatureInnerSense;
import org.json.JSONObject;
import ws3dproxy.model.Thing;
import ws3dproxy.model.WorldPoint;
import ws3dproxy.util.Constants;

import java.util.ArrayList;
import java.util.List;

public class AvoidWall extends Codelet {
    private MemoryObject insMO;
    private MemoryObject wallsMO;
    private Thing wall = null;
    private MemoryContainer legsMC;
    private int legsMCID = -1;
    private double eval = 0.9;

    private double wallX;
    private double wallY;

    @Override
    public void accessMemoryObjects() {
        wallsMO = (MemoryObject)this.getInput("WALLS");
        insMO = (MemoryObject)this.getInput("INNER");
        legsMC=(MemoryContainer)this.getOutput("LEGS_CONTAINER");
    }

    @Override
    public void calculateActivation() {

    }

    @Override
    public void proc() {
        List<Thing> walls = (List<Thing>) wallsMO.getI();
        CreatureInnerSense cis = (CreatureInnerSense) insMO.getI();
        if(cis.position != null) {

            double cX = cis.position.getX();
            double cY = cis.position.getY();

            double x2 = 99999;
            eval = 0.9;
            for (Thing thing : walls) {
                WorldPoint creaturePosition = cis.position;
//            if (cis.position.distanceTo(thing.getCenterPosition()) <= 200 ) {
                if (thing.getX2() < x2) {
                    if (wall == null || (cX < wall.getX1() && thing.getX1() < cX)) {
                        wall = thing;
                        x2 = thing.getX2();
                    }

                }
//            }
            }

            if (wall != null) {
                double wX1 = wall.getX1();
                double wY1 = wall.getY1();
                double wX2 = wall.getX2();
                double wY2 = wall.getY2();
//            Avoid from the left
                if ((wall.getY1()) < 10) {
                    wallX = wall.getX2() + 60;
                    wallY = wall.getY2() + 60;
                    if (cY > wY2 + 30)
                        wallX = wall.getX1() - 50;

                }
//                Avoid from the right
                else {
                    wallX = wall.getX2() + 60;
                    wallY = wall.getY1() - 60;
                    if (cY < wY1 - 25)
                        wallX = wall.getX1() - 25;
                }
            }

            if (wall != null && wall.getX1() - 5 > cX) {
                wall = null;
                eval = 0.1;
            }


            try {
                JSONObject message = new JSONObject();
                message.put("ACTION", "GOTO");
                message.put("X", (int) wallX);
                message.put("Y", (int) wallY);
                message.put("SPEED", 2.0);
                if (legsMCID == -1)
                    legsMCID = legsMC.setI(message.toString(), eval);
                else
                    legsMC.setI(message.toString(), eval, legsMCID);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }
}

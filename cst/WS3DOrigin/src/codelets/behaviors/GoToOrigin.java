package codelets.behaviors;

import br.unicamp.cst.core.entities.Codelet;
import br.unicamp.cst.core.entities.MemoryContainer;
import org.json.JSONObject;

public class GoToOrigin extends Codelet {
    private MemoryContainer legsMC;
    private int legsMCID = -1;

    @Override
    public void accessMemoryObjects() {
        legsMC = (MemoryContainer) this.getOutput("LEGS_CONTAINER");
    }

    @Override
    public void calculateActivation() {

    }

    @Override
    public void proc() {
        try{
            JSONObject message = new JSONObject();
            message.put("ACTION", "GOTO");
            message.put("X", (int) 0);
            message.put("Y", (int) 0);
            message.put("SPEED", 2.0);
            if (legsMCID == -1)
                legsMCID = legsMC.setI(message.toString(), 0.8);
            else
                legsMC.setI(message.toString(), 0.8, legsMCID);
        } catch (Exception e){
            e.printStackTrace();
        }
    }
}

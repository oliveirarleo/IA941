/*****************************************************************************
 * Copyright 2007-2015 DCA-FEEC-UNICAMP
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *    Klaus Raizer, Andre Paraense, Ricardo Ribeiro Gudwin
 *****************************************************************************/

package codelets.behaviors;

import java.awt.Point;
import java.awt.geom.Point2D;

import org.json.JSONException;
import org.json.JSONObject;
import br.unicamp.cst.core.entities.Codelet;
import br.unicamp.cst.core.entities.MemoryContainer;
import br.unicamp.cst.core.entities.MemoryObject;
import memory.CreatureInnerSense;
import ws3dproxy.model.Thing;

public class GoToClosestThing extends Codelet {

    private MemoryObject closestAppleMO;
    private MemoryObject selfInfoMO;
    private MemoryContainer legsMC;
    private int legsMCID = -1;
    private int creatureBasicSpeed;
    private double reachDistance;

    public GoToClosestThing(int creatureBasicSpeed, int reachDistance) {
        this.creatureBasicSpeed = creatureBasicSpeed;
        this.reachDistance = reachDistance;
    }

    @Override
    public void accessMemoryObjects() {
        closestAppleMO = (MemoryObject) this.getInput("FOOD_LEAFLET_THING");
        selfInfoMO = (MemoryObject) this.getInput("INNER");
        legsMC = (MemoryContainer) this.getOutput("LEGS_CONTAINER");

    }

    @Override
    public void proc() {
        // Find distance between creature and closest apple
        //If far, go towards it
        //If close, stops

        Thing closestThing = (Thing) closestAppleMO.getI();
        CreatureInnerSense cis = (CreatureInnerSense) selfInfoMO.getI();

        if (closestThing != null) {
            double appleX = 0;
            double appleY = 0;
            try {
                appleX = closestThing.getX1();
                appleY = closestThing.getY1();

            } catch (Exception e) {
                e.printStackTrace();
            }

            double selfX = cis.position.getX();
            double selfY = cis.position.getY();

            Point2D pApple = new Point();
            pApple.setLocation(appleX, appleY);

            Point2D pSelf = new Point();
            pSelf.setLocation(selfX, selfY);

            double distance = pSelf.distance(pApple);
            JSONObject message = new JSONObject();
            try {
                if (distance > reachDistance) { //Go to it
                    message.put("ACTION", "GOTO");
                    message.put("X", (int) appleX);
                    message.put("Y", (int) appleY);
                    message.put("SPEED", creatureBasicSpeed);
                    if (legsMCID == -1)
                        legsMCID = legsMC.setI(message.toString(), 0.6);
                    else
                        legsMC.setI(message.toString(), 0.6, legsMCID);
                } else {//Stop
                    message.put("ACTION", "GOTO");
                    message.put("X", (int) appleX);
                    message.put("Y", (int) appleY);
                    message.put("SPEED", 0.0);
                    if (legsMCID == -1)
                        legsMCID = legsMC.setI(message.toString(), 0.1);
                    else
                        legsMC.setI(message.toString(), 0.1, legsMCID);
                }


            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        else {
            if (legsMCID == -1)
                legsMCID = legsMC.setI("", 0.0);
            else
                legsMC.setI("", 0.0, legsMCID);
        }
    }//end proc

    @Override
    public void calculateActivation() {

    }

}

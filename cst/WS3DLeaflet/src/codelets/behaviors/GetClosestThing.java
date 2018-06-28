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
import br.unicamp.cst.core.entities.MemoryObject;
import memory.CreatureInnerSense;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import ws3dproxy.model.Thing;
import ws3dproxy.util.Constants;

public class GetClosestThing extends Codelet {

    private MemoryObject closestThingMO;
    private MemoryObject innerSenseMO;
    private MemoryObject knownMO;
    private MemoryObject leafletJewelsIMO;
    private MemoryObject leafletJewelsMO;
    private int reachDistance;
    private MemoryObject handsMO;
    Thing closesThing;
    Thing lastThingRemoved = null;
    CreatureInnerSense cis;
    List<Thing> known;
    Map<String, Integer> lj;

    public GetClosestThing(int reachDistance) {
        setTimeStep(50);
        this.reachDistance = reachDistance;
    }

    @Override
    public void accessMemoryObjects() {
        closestThingMO = (MemoryObject) this.getInput("CLOSEST_THING");
        innerSenseMO = (MemoryObject) this.getInput("INNER");
        handsMO = (MemoryObject) this.getOutput("HANDS");
        knownMO = (MemoryObject) this.getOutput("KNOWN_THINGS");
        leafletJewelsMO = (MemoryObject) this.getInput("LEAFLET_JEWELS");
    }

    @Override
    public void proc() {
        String appleName = "";
        closesThing = (Thing) closestThingMO.getI();
        cis = (CreatureInnerSense) innerSenseMO.getI();
        known = (List<Thing>) knownMO.getI();
        lj = (Map<String, Integer>) leafletJewelsMO.getI();
        //Find distance between closest apple and self
        //If closer than reachDistance, eat the apple

        if (closesThing != null) {
            double thingX = 0;
            double thingY = 0;
            try {
                thingX = closesThing.getX1();
                thingY = closesThing.getY1();
                appleName = closesThing.getName();


            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            double selfX = cis.position.getX();
            double selfY = cis.position.getY();

            Point2D pThing = new Point();
            pThing.setLocation(thingX, thingY);

            Point2D pSelf = new Point();
            pSelf.setLocation(selfX, selfY);

            double distance = pSelf.distance(pThing);
            JSONObject message = new JSONObject();
            try {
                if (distance < reachDistance) {
                    message.put("OBJECT", appleName);
                    if(closesThing.getCategory() == Constants.categoryJEWEL)
                        message.put("ACTION", "PICKUP");
                    else
                        message.put("ACTION", "EATIT");
                    handsMO.updateI(message.toString());
                    DestroyClosestThing();
                } else {
                    handsMO.updateI("");    //nothing
                }

//				System.out.println(message);
            } catch (JSONException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        } else {
            handsMO.updateI("");    //nothing
        }
        //System.out.println("Before: "+known.size()+ " "+known);

        //System.out.println("After: "+known.size()+ " "+known);
        //System.out.println("EatClosestApple: "+ handsMO.getInfo());

    }

    @Override
    public void calculateActivation() {

    }

    public void DestroyClosestThing() {
        int r = -1;
        int i = 0;
        synchronized (known) {

                CopyOnWriteArrayList<Thing> myknown = new CopyOnWriteArrayList<>(known);
                for (Thing t : known) {
                    if (closesThing != null) {
                        if (t.getName().equals(closesThing.getName())) r = i;
                    }
                    i++;
                }
                if (r != -1) {
                    known.remove(r);


                }
                synchronized (lj) {
                    String colorName = closesThing.getMaterial().getColorName();
                    if(lj.containsKey(colorName) && (lastThingRemoved == null || !lastThingRemoved.getName().equals(closesThing.getName())))
                    {
                        int numJewels = lj.get(colorName);
                        lj.replace(colorName,  numJewels- 1);

                    }
                leafletJewelsMO.setI(lj);
                knownMO.setI(known);
                lastThingRemoved = closesThing;
                closesThing = null;
            }
        }
    }

}

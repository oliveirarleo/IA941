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

import br.unicamp.cst.core.entities.Codelet;
import br.unicamp.cst.core.entities.MemoryContainer;
import br.unicamp.cst.core.entities.MemoryObject;
import br.unicamp.cst.core.entities.Mind;
import codelets.behaviors.GetClosestThing;
import codelets.behaviors.Forage;
import codelets.behaviors.GoToClosestThing;
import codelets.motor.HandsActionCodelet;
import codelets.motor.LegsActionCodelet;
import codelets.perception.FoodOrLeafletJewelDetector;
import codelets.perception.ThingDetector;
import codelets.perception.ClosestThingDetector;
import codelets.sensors.InnerSense;
import codelets.sensors.Vision;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import memory.CreatureInnerSense;
import support.MindView;
import ws3dproxy.model.Leaflet;
import ws3dproxy.model.Thing;

/**
 * @author rgudwin
 */
public class AgentMind extends Mind {

    private static int creatureBasicSpeed = 3;
    private static int reachDistance = 50;

    public AgentMind(Environment env) {
        super();

        // Declare Memory Objects
        MemoryContainer legsMC;
        MemoryObject handsMO;
        MemoryObject visionMO;
        MemoryObject innerSenseMO;
        MemoryObject closestThingMO;
        MemoryObject knownThingsMO;
        MemoryObject leafletJewelsMO;
        MemoryObject foodLeafletMO;

        //Initialize Memory Objects
        legsMC = createMemoryContainer("LEGS_CONTAINER");
        handsMO = createMemoryObject("HANDS", "");
        List<Thing> vision_list = Collections.synchronizedList(new ArrayList<Thing>());
        visionMO = createMemoryObject("VISION", vision_list);
        CreatureInnerSense cis = new CreatureInnerSense();
        innerSenseMO = createMemoryObject("INNER", cis);
        Thing closestThing = null;
        closestThingMO = createMemoryObject("CLOSEST_THING", closestThing);
        List<Thing> knownThings = Collections.synchronizedList(new ArrayList<Thing>());
        knownThingsMO = createMemoryObject("KNOWN_THINGS", knownThings);
        Map<String, Integer> leafletJewels = Collections.synchronizedMap(new HashMap<String, Integer>());
        Thing foodLeaflet = null;
        foodLeafletMO = createMemoryObject("FOOD_LEAFLET_THING", foodLeaflet);

        env.c.updateState();
        List<Leaflet> leafs = env.c.getLeaflets();

        for (Leaflet l : leafs) {
            HashMap<String, Integer> missingTypes = l.getWhatToCollect();

//            for (String s : missingTypes.keySet()) {
//                System.out.println(s + " " + missingTypes.get(s));
//            }

            if (leafletJewels.isEmpty())
                leafletJewels.putAll(missingTypes);
            else {
                for (String key : missingTypes.keySet()) {
                    if (leafletJewels.containsKey(key)) {
                        leafletJewels.replace(key, leafletJewels.get(key) + missingTypes.get(key));
                    } else{
                        leafletJewels.put(key, missingTypes.get(key));
                    }
                }
            }
        }

//        for (String s : leafletJewels.keySet()) {
//            System.out.println(s + " " + leafletJewels.get(s));
//        }

        leafletJewelsMO = createMemoryObject("LEAFLET_JEWELS", leafletJewels);

        // Create and Populate MindViewer
        MindView mv = new MindView("MindView");
        mv.addMO(knownThingsMO);
        mv.addMO(foodLeafletMO);
        mv.addMO(visionMO);
        mv.addMO(closestThingMO);
        mv.addMO(innerSenseMO);
        mv.addMO(handsMO);

        mv.addMO(leafletJewelsMO);
        mv.addMC(legsMC);

        mv.StartTimer();
        mv.setVisible(true);

        // Create Sensor Codelets
        Codelet vision = new Vision(env.c);
        vision.addOutput(visionMO);
        insertCodelet(vision); //Creates a vision sensor

        Codelet innerSense = new InnerSense(env.c);
        innerSense.addOutput(innerSenseMO);
        insertCodelet(innerSense); //A sensor for the inner state of the creature

        // Create Actuator Codelets
        Codelet legs = new LegsActionCodelet(env.c);
        legs.addInput(legsMC);
        insertCodelet(legs);

        Codelet hands = new HandsActionCodelet(env.c);
        hands.addInput(handsMO);
        insertCodelet(hands);

        // Create Perception Codelets
        Codelet ad = new ThingDetector();
        ad.addInput(visionMO);
        ad.addOutput(knownThingsMO);
        insertCodelet(ad);

        Codelet closestThingDetector = new ClosestThingDetector();
        closestThingDetector.addInput(knownThingsMO);
        closestThingDetector.addInput(innerSenseMO);
        closestThingDetector.addOutput(closestThingMO);
        insertCodelet(closestThingDetector);

        Codelet foodLeafletDetector = new FoodOrLeafletJewelDetector();
        foodLeafletDetector.addInput(leafletJewelsMO);
        foodLeafletDetector.addInput(knownThingsMO);
        foodLeafletDetector.addInput(innerSenseMO);
        foodLeafletDetector.addOutput(foodLeafletMO);
        insertCodelet(foodLeafletDetector);

        // Create Behavior Codelets
        Codelet forage = new Forage();
        forage.addInput(knownThingsMO);
        forage.addOutput(legsMC);
        insertCodelet(forage);

        Codelet goToClosestThing = new GoToClosestThing(creatureBasicSpeed, reachDistance);
        goToClosestThing.addInput(foodLeafletMO);
        goToClosestThing.addInput(innerSenseMO);
        goToClosestThing.addOutput(legsMC);
        insertCodelet(goToClosestThing);

        Codelet eatApple = new GetClosestThing(reachDistance);
        eatApple.addInput(closestThingMO);
        eatApple.addInput(leafletJewelsMO);
        eatApple.addInput(innerSenseMO);
        eatApple.addOutput(handsMO);
        eatApple.addOutput(knownThingsMO);
        insertCodelet(eatApple);



        // sets a time step for running the codelets to avoid heating too much your machine
        for (Codelet c : this.getCodeRack().getAllCodelets())
            c.setTimeStep(200);

        // Start Cognitive Cycle
        start();
    }

}

package codelets.perception;

import br.unicamp.cst.core.entities.Codelet;
import br.unicamp.cst.core.entities.MemoryObject;
import memory.CreatureInnerSense;
import ws3dproxy.model.Thing;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import ws3dproxy.util.Constants;

public class FoodOrLeafletJewelDetector extends Codelet {

    private MemoryObject knownMO;
    private MemoryObject foodOrLeafletJewelMO;
    private MemoryObject innerSenseMO;
    private MemoryObject leafletJewelsMO;

    private List<Thing> known;
    private Map<String, Integer> leafletJewels;

    public FoodOrLeafletJewelDetector() {
    }


    @Override
    public void accessMemoryObjects() {
        this.knownMO = (MemoryObject) this.getInput("KNOWN_THINGS");
        this.innerSenseMO = (MemoryObject) this.getInput("INNER");
        this.leafletJewelsMO = (MemoryObject) this.getInput("LEAFLET_JEWELS");
        this.foodOrLeafletJewelMO = (MemoryObject) this.getOutput("FOOD_LEAFLET_THING");

    }

    @Override
    public void proc() {
        Thing closest_thing = null;
        known = Collections.synchronizedList((List<Thing>) knownMO.getI());

        CreatureInnerSense cis = (CreatureInnerSense) innerSenseMO.getI();
        leafletJewels = (Map<String, Integer>) leafletJewelsMO.getI();
        synchronized (known) {
            if (known.size() != 0) {
                //Iterate over objects in vision, looking for the closest apple
                CopyOnWriteArrayList<Thing> myknown = new CopyOnWriteArrayList<>(known);
                for (Thing t : myknown) {
                    int category = t.getAttributes().getCategory();
                    String color = t.getAttributes().getMaterial3D().getColorName();
//                    System.out.println(category + " " + color);
                    if ((category == Constants.categoryPFOOD && cis.fuel < 400) || (category == Constants.categoryJEWEL &&
                            leafletJewels.containsKey(color) && leafletJewels.get(color) > 0)) {
                        if (closest_thing == null) {
                            closest_thing = t;
                        } else {
                            double Dnew = calculateDistance(t.getX1(), t.getY1(), cis.position.getX(), cis.position.getY());
                            double Dclosest = calculateDistance(closest_thing.getX1(), closest_thing.getY1(), cis.position.getX(), cis.position.getY());
                            if (Dnew < Dclosest) {
                                closest_thing = t;
                            }

                        }
                    }
                }
//                System.out.println(closest_thing);

                if (closest_thing != null) {
                    if (foodOrLeafletJewelMO.getI() == null || !foodOrLeafletJewelMO.getI().equals(closest_thing)) {
                        foodOrLeafletJewelMO.setI(closest_thing);
                    }

                } else {
                    closest_thing = null;
                    foodOrLeafletJewelMO.setI(closest_thing);
                }
            } else {
                closest_thing = null;
                foodOrLeafletJewelMO.setI(closest_thing);
            }
        }
    }//end proc

    @Override
    public void calculateActivation() {

    }

    private double calculateDistance(double x1, double y1, double x2, double y2) {
        return (Math.sqrt(Math.pow(x1 - x2, 2) + Math.pow(y1 - y2, 2)));
    }

}

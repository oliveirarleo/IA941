package modules;

import edu.memphis.ccrg.lida.environment.EnvironmentImpl;
import edu.memphis.ccrg.lida.framework.tasks.FrameworkTaskImpl;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import ws3dproxy.WS3DProxy;
import ws3dproxy.model.Creature;
import ws3dproxy.model.Leaflet;
import ws3dproxy.model.Thing;
import ws3dproxy.model.World;
import ws3dproxy.model.WorldMap;
import ws3dproxy.util.Constants;

public class Environment extends EnvironmentImpl {

    private static final int DEFAULT_TICKS_PER_RUN = 100;
    private int ticksPerRun;
    private WS3DProxy proxy;
    private Creature creature;
    private Thing wall;
    private List<Thing> thingAhead;
    private String currentAction;   
    private String command;
    
    private double wallX;
    private double wallY;
    
    public Environment() {
        this.ticksPerRun = DEFAULT_TICKS_PER_RUN;
        this.proxy = new WS3DProxy();
        this.creature = null;
        this.wall = null;
        this.thingAhead = new ArrayList<>();
        this.currentAction = "gotoOrigin";
    }

    @Override
    public void init() {
        super.init();
        ticksPerRun = (Integer) getParam("environment.ticksPerRun", DEFAULT_TICKS_PER_RUN);
        taskSpawner.addTask(new BackgroundTask(ticksPerRun));
        
        try {
            System.out.println("Reseting the WS3D World ...");
            proxy.getWorld().reset();
            creature = proxy.createCreature(700, 0, 0);
            creature.start();
            System.out.println("Starting the WS3D Resource Generator ... ");
//            World.grow(1);
            World.createBrick(1, 100, 0, 150, 450);
            World.createBrick(2, 250, 150, 300, 600);
            World.createBrick(3, 400, 0, 450, 450);
            Thread.sleep(4000);
            creature.updateState();
            System.out.println("DemoLIDA has started...");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private class BackgroundTask extends FrameworkTaskImpl {

        public BackgroundTask(int ticksPerRun) {
            super(ticksPerRun);
        }

        @Override
        protected void runThisFrameworkTask() {
            updateEnvironment();
            performAction(currentAction);
        }
    }

    @Override
    public void resetState() {
        currentAction = "gotoOrigin";
    }

    @Override
    public Object getState(Map<String, ?> params) {
        Object requestedObject = null;
        String mode = (String) params.get("mode");
        switch (mode) {
            case "wall":
                requestedObject = wall;
                break;
            case "thingAhead":
                requestedObject = thingAhead;
                break;
            default:
                break;
        }
        return requestedObject;
    }

    
    public void updateEnvironment() {
        creature.updateState();
        thingAhead.clear();
        
        
        
        double x2 = 99999;
        for (Thing thing : creature.getThingsInVision()) {
            if (creature.calculateDistanceTo(thing) <= 200 ) {
                // Identifica o objeto proximo
                if(thing.getCategory() == Constants.categoryBRICK){
                    if(thing.getX2() < x2){
                        if(wall == null || wall.getX1()-40 > creature.getAttributes().getX1()){
                            wall = thing;
                            x2 = thing.getX2();
                        }
                    
                    }
                }else{
                    thingAhead.add(thing);
                }
            }
        }
        
        
        if(wall != null){
            double wX1 = wall.getX1();
            double wY1 = wall.getY1();
            double wX2 = wall.getX2();
            double wY2 = wall.getY2();
            double cX = creature.getAttributes().getX1();
            double cY = creature.getAttributes().getY1();
//            Avoid from the left
            if((wall.getY1()) < 10){
                wallX = wall.getX2() + 60;
                wallY = wall.getY2() + 60;
                if(cY > wY2)
                    wallX = wall.getX1()-50;
                    
            }
//                Avoid from the right
            else{
                wallX = wall.getX2() + 60;
                wallY = wall.getY1() - 120;
                if(cY < wY1)
                    wallX = wall.getX1()-50;
            }
        }
        
        if(wall != null && wall.getX1()-40 > creature.getAttributes().getX1())
            wall = null;
                    
    }
    
    
    
    @Override
    public void processAction(Object action) {
        String actionName = (String) action;
        currentAction = actionName.substring(actionName.indexOf(".") + 1);
    }

    public String getCommand()
    {
        return command;
    }
    
    private void performAction(String currentAction) {
        try {
            command = currentAction;
            switch (currentAction) {
                case "avoidWall":
                    if(wall != null)
                        creature.moveto(3.0, wallX, wallY);
                    break;
                case "gotoOrigin":
                    creature.moveto(6.0, 0, 0);
                    break;
                case "get":
                    creature.move(0.0, 0.0, 0.0);
                    if (thingAhead != null) {
                        for (Thing thing : thingAhead) {
                            if (thing.getCategory() == Constants.categoryJEWEL) {
                                creature.putInSack(thing.getName());
                            } else if (thing.getCategory() == Constants.categoryFOOD || thing.getCategory() == Constants.categoryNPFOOD || thing.getCategory() == Constants.categoryPFOOD) {
                                creature.eatIt(thing.getName());
                            }
                        }
                    }
                    this.resetState();
                    break;
                default:
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

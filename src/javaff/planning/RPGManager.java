package javaff.planning;

import javaff.JavaFF;
import javaff.data.Plan;

public class RPGManager {
    // Private static variable of the same type as the RPGManager class
    private static volatile RPGManager instance;

    private boolean[] RPGInUse;
    private RelaxedPlanningGraph[] RPGs;

    // Private constructor to prevent instantiation from outside the class
    private RPGManager() {
        // Prevent form the reflection api.
        if (instance != null) {
            throw new RuntimeException("Use getInstance() method to get the single instance of this class.");
        }
    }

    // Public static method that returns the RPGManager instance
    public static RPGManager getInstance() {
        // Double-checked locking principle is used
        if (instance == null) { // First check (no locking)
            synchronized (RPGManager.class) {
                if (instance == null) { // Second check (with locking)
                    instance = new RPGManager();
                }
            }
        }
        return instance;
    }

    public void initialiseRPGs(State s) {
        int maxThreads = JavaFF.useParallel ? (int) (Runtime.getRuntime().availableProcessors() * 2) : 1;
        RPGInUse = new boolean[maxThreads];
        RPGs = new RelaxedPlanningGraph[maxThreads];
        for (int i = 0; i < maxThreads; i++) {
            RelaxedPlanningGraph rpg = new RelaxedPlanningGraph(s.getActions(), s.goal);
            RPGs[i] = rpg;
            RPGInUse[i] = false;
        }
    }

    public synchronized RelaxedPlanningGraph requestRPG() {
        for (int i = 0; i < RPGInUse.length; i++) {
            if (!RPGInUse[i]) {
                RPGInUse[i] = true;
                RelaxedPlanningGraph rpg = RPGs[i].branch();
                // System.out.println(RPGs[i].getMyId() + " " + rpg.getMyId());
                return rpg;
            }
        }

        throw new RuntimeException("No free RPGs");
    }

    public RelaxedPlanningGraph getRPG(int id) {
        return RPGs[id];
        // if (RPGInUse[id]) {
        // return RPGs[id];
        // } else {
        // throw new RuntimeException("RPG not in use");
        // }
    }

    // public Plan getPlan(int id, State s) {
    // Plan plan = RPGs[id].branch().getPlan(s);
    // revokeRPG(id);
    // return plan;
    // }

    public synchronized void revokeRPG(RelaxedPlanningGraph rpg) {
        for (int i = 0; i < RPGs.length; i++) {
            // System.out.println(RPGs[i].getMyId() + " " + rpg.getMyId());
            if (RPGs[i].getMyId() == rpg.getMyId()) {
                RPGInUse[i] = false;
                return;
            }
        }
        throw new RuntimeException("RPG not found");
    }
}

/************************************************************************
 * Strathclyde Planning Group,
 * Department of Computer and Information Sciences,
 * University of Strathclyde, Glasgow, UK
 * http://planning.cis.strath.ac.uk/
 * 
 * Copyright 2007, Keith Halsey
 * Copyright 2008, Andrew Coles and Amanda Smith
 * Copyright 2015, David Pattison
 *
 * This file is part of JavaFF.
 * 
 * JavaFF is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 * 
 * JavaFF is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with JavaFF.  If not, see <http://www.gnu.org/licenses/>.
 * 
 ************************************************************************/

package javaff.search;

import javaff.JavaFF;
import javaff.data.Action;
import javaff.data.Fact;
import javaff.data.Plan;
import javaff.planning.STRIPSState;
import javaff.planning.State;
import utils.Pair;
import javaff.planning.Filter;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import bag.Bag;
import benchmarker.Benchmarker;

import java.util.LinkedList;
import java.util.Collections;
import java.util.Comparator;
import java.math.BigDecimal;

public class ParallelEnforcedHillClimbingSearch extends Search {
    protected BigDecimal bestHValue;

    // cache the hash codes rather than the states for memory efficiency
    protected Set<Integer> closed;
    protected LinkedList<State> open;
    protected Bag<State> currentBag;
    private final Lock lock = new ReentrantLock();
    protected boolean foundBetter = false;
    private int extraAvailableProcessors = Runtime.getRuntime().availableProcessors() - 1;

    public ParallelEnforcedHillClimbingSearch(State s) {
        this(s, new HValueComparator());
    }

    public ParallelEnforcedHillClimbingSearch(State s, Comparator c) {
        super(s);
        setComparator(c);

        closed = Collections.synchronizedSet(new HashSet<Integer>());
        open = new LinkedList<State>();
        currentBag = new Bag<>();
    }

    public void setFilter(Filter f) {
        filter = f;
    }

    public State removeNext() {

        return (State) (open).removeFirst();
    }

    public boolean needToVisit(State s) {
        if (closed.contains(s.hashCode()))
            return false;

        closed.add(s.hashCode()); // otherwise put it on
        return true; // and return true
    }

    class ProcessNeighboursTask extends RecursiveTask<Pair<Bag<State>, State>> {
        private static final int THRESHOLD = 6;
        // private static final int THRESHOLD = 9999999;
        private int start, end;
        private final List<Action> list;
        private javaff.planning.State curr;
        private AtomicInteger currentDepth, prevDepth, maxDepth;
        HashMap<javaff.planning.State, Integer> successorLayers;

        public ProcessNeighboursTask(List<Action> list, int start, int end, javaff.planning.State curr,
                HashMap<javaff.planning.State, Integer> successorLayers,
                AtomicInteger currentDepth,
                AtomicInteger prevDepth, AtomicInteger maxDepth) {
            // System.out.println("Creating neighbour task");
            this.list = list;
            this.start = start;
            this.end = end;
            this.curr = curr;
            this.currentDepth = currentDepth;
            this.prevDepth = prevDepth;
            this.maxDepth = maxDepth;
            this.successorLayers = successorLayers;
        }

        @Override
        protected Pair<Bag<javaff.planning.State>, javaff.planning.State> compute() {
            boolean split;
            lock.lock();
            if (end - start > 1 && extraAvailableProcessors > 0) {
                split = true;
                extraAvailableProcessors--;
                // System.out.println("SPLITTING neighbours");
            } else {
                // System.out.println("NOT splitting neighbours");
                split = false;
            }
            lock.unlock();
            if (!split) {
                // Process the chunk
                closed.add(curr.hashCode());
                if (successorLayers.get(curr) == null) {
                    // System.out.println("HERE ******************************");
                    // System.out.println(curr);
                    return new Pair<>(null, null);
                }
                currentDepth.set(successorLayers.get(curr));
                if (currentDepth.get() > maxDepth.get())
                    maxDepth.set(currentDepth.get());

                lock.lock();
                if (currentDepth.get() > prevDepth.get()) {
                    JavaFF.infoOutput.print("[" + (currentDepth) + "]");
                    prevDepth.set(currentDepth.get());
                }
                lock.unlock();

                Bag<javaff.planning.State> outputBag = new Bag<>();
                for (int i = start; i < end; i++) {
                    if (ProcessLayerTask.found.get()) {
                        // System.out.println("found goal, aborting neighbour thread");
                        return new Pair<>(null, null);
                    }

                    if (foundBetter) {
                        return null;
                    }

                    Action a = list.get(i);
                    // System.out.println("\nEvaluating action " + a + " from state " + curr);
                    javaff.planning.State succ = curr.getNextState(a);

                    if (closed.contains(succ.hashCode()) == true) {
                        continue;
                    }
                    // ++statesEvaluated;

                    BigDecimal succH = succ.getHValue();
                    if (ProcessLayerTask.found.get()) {
                        // System.out.println("found goal, aborting neighbour thread");
                        return new Pair<>(null, null);
                    }

                    if (foundBetter) {
                        return null;
                    }
                    // check we have no entered a dead-end
                    if (((STRIPSState) succ).getRelaxedPlan() == null) {
                        // System.out.println("Found dead end from action " + a);
                        continue;
                    }

                    // now do online goal-ordering check -- this is used by FF to prevent deleting
                    // goals early in the
                    // relaxed plan, which would then need negated again later on
                    boolean threatensGoal = isGoalThreatened(((STRIPSState) succ).getRelaxedPlan(), succ.goal);
                    if (threatensGoal) {
                        // System.out.println("Threatens goal");
                        // closed.add(succ); //The real FF says that this state should be "removed" from
                        // the state-space -- we just skip it
                        continue; // skip successor state
                    }

                    if (succ.goalReached()) { // if we've found a goal state -
                                              // return it as the
                                              // solution
                        // JavaFF.infoOutput
                        // .println("\nEvaluated " + statesEvaluated + " states to a max depth of " +
                        // maxDepth);

                        // System.out.println(
                        // "GOAL REACHED
                        // ***************************************************************");
                        ProcessLayerTask.found.set(true);
                        return new Pair<>(null, succ);
                    } else if (succH.compareTo(bestHValue) < 0) {
                        // if we've found a state with a better heuristic
                        // value
                        // than the best seen so far
                        lock.lock();

                        if (!foundBetter) {
                            // System.out.println("Found better, won the race");
                            foundBetter = true;
                            lock.unlock();
                        } else {
                            // System.out.println("Already found better, lost the race: " + a);
                            lock.unlock();
                            return null;
                        }

                        bestHValue = succH; // note the new best value
                        // open.clear();

                        // open.add(succ); // add it to the open list
                        currentBag = new Bag<>();
                        currentBag.insert(succ);

                        successorLayers.clear();
                        successorLayers.put(succ, 1);
                        prevDepth.set(0);
                        currentDepth.set(1);

                        // System.out.println("\nFOUND BETTER ACTION:" + a + "\nFROM STATE: " + curr
                        // + "\nWHICH LEADS TO STATE: " + succ);
                        JavaFF.infoOutput.print("\n" + bestHValue + " into depth ");
                        // if (bestHValue.compareTo(BigDecimal.ZERO) == 0) {
                        // System.out.println("STATE WITH H=0: " + succ);
                        // }

                        return null; // and skip looking at the other successors
                    } else {
                        outputBag.insert(succ); // add it to the open list
                        successorLayers.put(succ, currentDepth.get() + 1);
                        // System.out.println("Adding state to bag: " + succ);
                        // prevDepth = currentDepth;
                    }
                }

                return new Pair<>(outputBag, null);
            } else {
                if (ProcessLayerTask.found.get()) {
                    // System.out.println("found goal, aborting neighbour thread");
                    return new Pair<>(null, null);
                }

                // System.out.println("SPLITTING NEIGHBOURS");

                int mid = start + (end - start) / 2;

                ProcessNeighboursTask left = new ProcessNeighboursTask(list, start, mid, curr, successorLayers,
                        currentDepth, prevDepth, maxDepth);
                ProcessNeighboursTask right = new ProcessNeighboursTask(list, mid, end, curr, successorLayers,
                        currentDepth, prevDepth, maxDepth);

                left.fork();
                Pair<Bag<javaff.planning.State>, javaff.planning.State> out1 = right.compute();
                Pair<Bag<javaff.planning.State>, javaff.planning.State> out2 = left.join();
                extraAvailableProcessors++;

                if (out1 == null || out2 == null) {
                    // System.out.println("Already found better, aborting neighbour thread");
                    return null;
                }

                if (out1.getSecond() != null || out2.getSecond() != null) {
                    // System.out.println("Found goal, aborting neighbour thread");
                    return new Pair<>(null, out1.getSecond() != null ? out1.getSecond() : out2.getSecond());
                }

                if (out1.getFirst() == null || out2.getFirst() == null) {
                    // System.out.println("found goal, aborting neighbour thread");
                    return new Pair<>(null, null);
                }

                // System.out.println("Could not find better, merging bags");
                out1.getFirst().union((Bag<javaff.planning.State>) out2.getFirst());
                return new Pair<>(out1.getFirst(), null);
            }
        }
    }

    class ProcessLayerTask extends RecursiveTask<Pair<Bag<State>, State>> {
        private Bag<javaff.planning.State> inputBag;
        HashMap<javaff.planning.State, Integer> successorLayers;
        private AtomicInteger currentDepth, prevDepth, maxDepth;
        private ForkJoinPool pool;
        public static AtomicBoolean found = new AtomicBoolean(false);
        private static final int THRESHOLD = 16;

        public ProcessLayerTask(Bag<javaff.planning.State> inputBag,
                HashMap<javaff.planning.State, Integer> successorLayers,
                AtomicInteger currentDepth,
                AtomicInteger prevDepth, AtomicInteger maxDepth, ForkJoinPool pool) {
            this.inputBag = inputBag;
            this.pool = pool;
            this.successorLayers = successorLayers;
            this.currentDepth = currentDepth;
            this.prevDepth = prevDepth;
            this.maxDepth = maxDepth;
        }

        @Override
        protected Pair<Bag<javaff.planning.State>, javaff.planning.State> compute() {
            lock.lock();
            boolean split;
            // System.out.println(
            // "Layer size: " + inputBag.size() + " Extra available processors: " +
            // extraAvailableProcessors);
            if (inputBag.size() > Bag.GRAIN_SIZE && extraAvailableProcessors > 0) {
                split = true;
                extraAvailableProcessors--;
                // System.out.println("SPLITTING LAYER");
            } else {
                split = false;
                // System.out.println("NOT splitting layer");
            }
            lock.unlock();
            if (!split) {
                Bag<javaff.planning.State> outputBag = null;
                for (javaff.planning.State state : inputBag) {
                    if (ProcessLayerTask.found.get()) {
                        // System.out.println("1doof");
                        return new Pair<>(null, null);
                    }

                    List<Action> applicable = filter.getActions(state);
                    Benchmarker.addToNeighbourCount(applicable.size());
                    // System.out.println("Applicable actions: " + applicable.size());
                    // applicable.parallelStream().forEach(a -> {

                    // });
                    ProcessNeighboursTask task = new ProcessNeighboursTask(applicable, 0,
                            applicable.size(), state, successorLayers, currentDepth, prevDepth, maxDepth);

                    Pair<Bag<javaff.planning.State>, javaff.planning.State> out = pool.invoke(task);
                    if (out == null) {
                        return null;
                    }

                    if (out.getSecond() != null) {
                        // System.out.println("1Found goal, aborting layer thread");
                        return new Pair<>(null, out.getSecond());
                    }

                    if (out.getFirst() == null) {
                        // System.out.println("1Found goal somewhere else, aborting layer thread");
                        return new Pair<>(null, null);
                    }

                    if (outputBag == null) {
                        outputBag = out.getFirst();
                    } else {
                        outputBag.union((Bag<javaff.planning.State>) out.getFirst());
                    }
                }

                return new Pair<>(outputBag, null);
            } else {
                if (ProcessLayerTask.found.get()) {
                    // System.out.println("doof");
                    return new Pair<>(null, null);
                }
                // System.out.println("SPLITTING LAYER");
                // Split the bag and create new subtasks
                // System.out.println("Splitting bag of size " + inputBag.size());
                // for (javaff.planning.State state : inputBag) {
                // System.out.println(state);
                // }

                Bag<javaff.planning.State> leftBag = (Bag<javaff.planning.State>) inputBag.split();
                // System.out.println("Left bag size: " + leftBag.size());
                // System.out.println("Right bag size: " + inputBag.size());

                // System.out.println(leftBag.size() + " " + inputBag.size());
                extraAvailableProcessors--;
                ProcessLayerTask leftTask = new ProcessLayerTask(leftBag, successorLayers, currentDepth,
                        prevDepth, maxDepth, pool);
                ProcessLayerTask rightTask = new ProcessLayerTask(inputBag, successorLayers, currentDepth,
                        prevDepth, maxDepth, pool);

                leftTask.fork();
                Pair<Bag<javaff.planning.State>, javaff.planning.State> out1 = rightTask.compute();
                Pair<Bag<javaff.planning.State>, javaff.planning.State> out2 = leftTask.join();
                extraAvailableProcessors++;

                if (out1 == null || out2 == null) {
                    // System.out.println("Found better, aborting layer thread");
                    return null;
                }

                if (out1.getSecond() != null || out2.getSecond() != null) {
                    // System.out.println("Found goal, aborting layer thread");
                    return new Pair<>(null, out1.getSecond() != null ? out1.getSecond() : out2.getSecond());
                }

                if (out1.getFirst() == null || out2.getFirst() == null) {
                    System.out.println(out1.getFirst() + " " + out2.getFirst());
                    // System.out.println("Found goal somewhere else, aborting layer thread");
                    return new Pair<>(null, null);
                }

                // System.out.println("Did not find goal, merging bags");
                out1.getFirst().union((Bag<javaff.planning.State>) out2.getFirst());
                return new Pair<>(out1.getFirst(), null);
            }
        }
    }

    public State search() {
        if (start.goalReached()) { // wishful thinking
            return start;
        }

        needToVisit(start); // dummy call (adds start to the list of 'closed'
        // states so we don't visit it again

        // Bag<State> currentBag = new Bag<>();
        currentBag.insert(start);
        // open.add(start); // add it to the open list
        bestHValue = start.getHValue(); // and take its heuristic value as the
        // best so far

        javaff.JavaFF.infoOutput.print(bestHValue + " into depth ");
        HashMap<State, Integer> successorLayers = new HashMap<>();
        successorLayers.put(start, 1);

        AtomicInteger currentDepth = new AtomicInteger(1), prevDepth = new AtomicInteger(0),
                maxDepth = new AtomicInteger(1);
        ProcessLayerTask.found.set(false);
        try (ForkJoinPool pool = new ForkJoinPool()) {
            while (!currentBag.isEmpty()) // whilst still states to consider
            {
                extraAvailableProcessors = Runtime.getRuntime().availableProcessors() - 1;
                // System.out.println("Extra available processors: " +
                // extraAvailableProcessors);
                Benchmarker.addToLayerCount(currentBag.size());
                // System.out.println("Layer size: " + currentBag.size());
                foundBetter = false;
                ProcessLayerTask task = new ProcessLayerTask(currentBag, successorLayers,
                        currentDepth,
                        prevDepth,
                        maxDepth, pool);
                Pair<Bag<State>, State> result = pool.invoke(task);
                if (result == null) {
                    // System.out.println("Found better, resetting");
                    // System.out.println("Current bag size: " + currentBag.size());
                    continue;
                }

                if (result.getSecond() != null) {
                    return result.getSecond();
                }

                // System.out.println("Did not find a better state in this layer, continuing");
                // System.out.println(result.getFirst());
                currentBag = result.getFirst();
            }
        }
        JavaFF.infoOutput.println();

        return null;

    }

    /**
     * Tests whether any of the actions in the RELAXED plan associated with this
     * state
     * delete a goal.
     * 
     * @param relaxedPlan The relaxed plan which will be used to detect goal
     *                    orderings
     * @param goal        The goal to check
     * @return
     */
    private boolean isGoalThreatened(Plan relaxedPlan, Fact goal) {
        // maintain a list of achieved goals as we go through the RP in-order. These
        // will
        // be checked at each timestep to see if any already achieved goals are deleted.
        HashSet<Fact> achieved = new HashSet<Fact>();
        List<Action> actions = relaxedPlan.getActions();

        for (Action a : actions) {
            for (Fact g : goal.getFacts()) {
                // if this action deletes a goal and that goal has already been achieved by
                // a previous action in the RP, immediately return
                if (a.deletes(g) && achieved.contains(g))
                    return true;
            }

            achieved.addAll(a.getAddPropositions());
            achieved.addAll(a.getDeletePropositions()); // needed for ADL goals
        }

        return false;
    }
}

package javaff.search;

import javaff.planning.State;
import javaff.JavaFF;
import javaff.data.Action;
import javaff.planning.Filter;
import javaff.planning.STRIPSState;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.TreeSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

import benchmarker.Benchmarker;

public class ParallelBestFirstSearch extends Search {

    protected Hashtable closed;
    protected SortedSet<State> open;
    private final ReentrantLock lock = new ReentrantLock();

    private static final AtomicBoolean correctResultFound = new AtomicBoolean(false);

    public ParallelBestFirstSearch(State s) {
        this(s, new HValueComparator());
    }

    public ParallelBestFirstSearch(State s, Comparator c) {
        super(s);
        setComparator(c);

        closed = new Hashtable();
        open = Collections.synchronizedSortedSet(new TreeSet(comp));
    }

    public void updateOpen(State S) {
        List<Action> applicable = filter.getActions(S);
        // Benchmarker.addToActionCount(applicable.size());
        Set<State> uniqueSuccessors = Collections.synchronizedSet(new HashSet<>());
        applicable.parallelStream().forEach(action -> {
            State succ = S.getNextState(action);
            if (!uniqueSuccessors.add(succ)) {
                return;
            }

            succ.getHValue();
            open.add(succ);
        });
    }

    public State removeNext() {
        if (open.isEmpty())
            return null;

        State S = (State) (open).first();
        open.remove(S);
        /*
         * System.out.println("================================");
         * S.getSolution().print(System.out); System.out.println("----Helpful
         * Actions-------------"); javaff.planning.TemporalMetricState ms =
         * (javaff.planning.TemporalMetricState) S;
         * System.out.println(ms.helpfulActions);
         * System.out.println("----Relaxed Plan----------------");
         * ms.RelaxedPlan.print(System.out);
         */
        return S;
    }

    public synchronized boolean needToVisit(State s) {
        Integer Shash = new Integer(s.hashCode());
        State D = (State) closed.get(Shash);

        if (closed.containsKey(Shash) && D.equals(s))
            return false;

        closed.put(Shash, s);
        return true;
    }

    private State searchV1() {
        open.add(start);

        while (!open.isEmpty()) {
            State s = removeNext();
            if (needToVisit(s)) {
                ++nodeCount;
                if (s.goalReached()) {
                    return s;
                } else {
                    updateOpen(s);
                }
            }

        }

        return null;
    }

    private State searchV2() {
        correctResultFound.set(false);
        int count = 0;
        int[] boundLimits = { 50, 400, 3000, 20000, 100000 };
        int[] threadLimits = { 1, 4, 8, 16, 32, 64 };
        HashMap<STRIPSState, BigDecimal> heuristics = new HashMap<>();
        open.add(start);
        while (!correctResultFound.get()) {
            int bound = boundLimits[Math.min(count, 4)];
            int K = threadLimits[Math.min(count++, 5)];
            nodeCount = 0;

            // open.clear();
            // closed.clear();
            // open.add(start);
            List<Callable<State>> tasks = new ArrayList<>();
            for (int i = 0; i < K; i++) {
                tasks.add(() -> {
                    while (nodeCount <= bound && !correctResultFound.get()) {
                        State s = removeNext();

                        if (s == null) {
                            // System.out.println("Open is empty");
                            continue;
                        }

                        if (!needToVisit(s)) {
                            // System.out.println("Already visited");
                            continue;
                        }

                        if (s.goalReached()) {
                            // System.out.println("Goal reached");
                            correctResultFound.set(true);
                            return s;
                        }

                        System.out.println(s.getHValue());

                        List<Action> actions = filter.getActions(s);
                        Set<State> uniqueSuccessors = Collections.synchronizedSet(new HashSet<>());
                        AtomicBoolean goalFound = new AtomicBoolean(false);
                        AtomicReference<State> goalState = new AtomicReference<>();
                        actions.parallelStream().forEach(action -> {
                            if (correctResultFound.get() || goalFound.get() || nodeCount > bound) {
                                return;
                            }

                            State succ = s.getNextState(action);
                            if (!uniqueSuccessors.add(succ)) {
                                return;
                            }

                            if (succ.goalReached()) {
                                goalFound.set(true);
                                correctResultFound.set(true);
                                goalState.set(succ);
                            }

                            STRIPSState stripsSucc = (STRIPSState) succ;
                            if (heuristics.containsKey(succ)) {
                                stripsSucc.RPCalculated = true;
                                stripsSucc.HValue = heuristics.get(succ);
                            } else {
                                heuristics.put(stripsSucc, succ.getHValue());
                            }

                            // succ.getHValue();
                            if (nodeCount > bound) {
                                return;
                            }

                            open.add(succ);
                        });

                        if (goalFound.get()) {
                            return goalState.get();
                        }

                        ++nodeCount;
                    }

                    // Return a dummy value if the correct result was found by another task
                    // System.out.println("Task cancelled");
                    throw new CancellationException("Task cancelled");
                });
            }

            try {
                return JavaFF.executorService.invokeAny(tasks);
            } catch (InterruptedException e) {
                System.out.println("ERROR OCCURED");
                e.printStackTrace();
                return null;
            } catch (ExecutionException e) {
                System.out.println("Bound exceeded, retrying with higher bound and thread count");
                // e.printStackTrace();
            }
        }

        return null;
    }

    public State search() {
        // return searchV1();
        return searchV2();
    }

}

package javaff.search;

import javaff.planning.State;
import javaff.planning.Filter;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.TreeSet;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

public class ParallelBestFirstSearch extends Search {

    protected Hashtable closed;
    protected TreeSet open;
    private final ReentrantLock lock = new ReentrantLock();

    private static final ExecutorService executorService = Executors
            .newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    private static final AtomicBoolean correctResultFound = new AtomicBoolean(false);

    public ParallelBestFirstSearch(State s) {
        this(s, new HValueComparator());
    }

    public ParallelBestFirstSearch(State s, Comparator c) {
        super(s);
        setComparator(c);

        closed = new Hashtable();
        open = new TreeSet(comp);
    }

    public void updateOpen(State S) {
        open.addAll(S.getNextStates(filter.getActions(S)));
    }

    public State removeNext() {
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

    public boolean needToVisit(State s) {
        Integer Shash = new Integer(s.hashCode());
        State D = (State) closed.get(Shash);

        if (closed.containsKey(Shash) && D.equals(s))
            return false;

        closed.put(Shash, s);
        return true;
    }

    private State findGoal() throws InterruptedException, ExecutionException {
        List<Callable<State>> tasks = new ArrayList<>();
        for (int i = 0; i < Runtime.getRuntime().availableProcessors(); i++) {
            tasks.add(() -> {
                // while (!open.isEmpty()) {
                // State s = removeNext();
                // if (needToVisit(s)) {
                // ++nodeCount;
                // if (s.goalReached()) {
                // return s;
                // } else {
                // updateOpen(s);
                // }
                // }

                // }
                while (!correctResultFound.get()) {
                    if (Math.random() < 0.5) {
                        State s;
                        lock.lock();
                        try {
                            s = removeNext();
                        } finally {
                            lock.unlock();
                        }

                        for (State neighbour : start.getNextStates(filter.getActions(s))) {
                            if (neighbour.goalReached()) {
                                return neighbour;
                            }

                            lock.lock();
                            try {
                                if (needToVisit(s)) {
                                    ++nodeCount;
                                    updateOpen(s);
                                }
                            } finally {
                                lock.unlock();
                            }
                        }
                    } else {
                        State t = null;
                        lock.lock();

                        try {
                            int desiredIndex = (int) Math.ceil(open.size() * 0.3) - 1;
                            int currentIndex = 0;
                            for (State element : (TreeSet<State>) open) {
                                if (currentIndex == desiredIndex) {
                                    t = element;
                                    needToVisit(t);
                                    break;
                                }
                                currentIndex++;
                            }
                        } finally {
                            lock.unlock();
                        }

                        TreeSet<State> localOpen = new TreeSet<>(comp);
                        localOpen.add(t);
                        for (int j = 0; j < 20; j++) {
                            State s = localOpen.first();
                            localOpen.remove(s);
                            open.remove(s);

                            for (State neighbour : s.getNextStates(filter.getActions(s))) {
                                if (neighbour.goalReached()) {
                                    return neighbour;
                                }
                                lock.lock();
                                try {
                                    if (needToVisit(s)) {
                                        open.add(neighbour);
                                        localOpen.add(neighbour);
                                    }
                                } finally {
                                    lock.unlock();
                                }
                            }
                        }
                    }
                    // Your task logic here
                    // Simulate task work
                    // TimeUnit.MILLISECONDS.sleep(100);

                    // // Example condition to check for the correct result
                    // if (new java.util.Random().nextInt(100) == 50) {
                    // correctResultFound.set(true);
                    // return "Processor " + Thread.currentThread().getName() + " found the correct
                    // result!";
                    // }
                }

                // Return a dummy value if the correct result was found by another task
                throw new CancellationException("Task cancelled");
            });
        }
        // The invokeAny method returns the result of the first successfully completed
        // task and cancels all others
        try {
            return executorService.invokeAny(tasks);
        } finally {
            // Ensure all tasks are cancelled after finding the correct result
            correctResultFound.set(true);
            executorService.shutdownNow(); // Attempt to stop all actively executing tasks
        }
    }

    public State search() {
        if (start.goalReached())
            return start; // wishful thinking v2

        for (State neighbour : start.getNextStates(filter.getActions(start))) {
            if (neighbour.goalReached()) {
                return neighbour; // more wishful thinking
            }

            open.add(neighbour);
        }

        try {
            // Submit tasks and wait for one to complete with the correct result
            return findGoal();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            return null;
        } finally {
            // Shutdown the executor service
            executorService.shutdownNow();
        }
    }

}

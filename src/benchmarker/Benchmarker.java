package benchmarker;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Scanner;

import org.json.JSONObject;

import javaff.JavaFF;
import javaff.data.Plan;
import utils.LogManager;

public class Benchmarker {
    public static final int benchmarkSetId = 11;
    public static int layerSizeSum = 0;
    public static int neighbourSizeSum = 0;
    public static int actionsSum = 0;
    private static int numLayers = 0;
    private static int numNodes = 0;
    private static int maxLayerSize = Integer.MIN_VALUE;
    private static int minLayerSize = Integer.MAX_VALUE;
    private static int maxNeighbourSize = Integer.MIN_VALUE;
    private static int minNeighourSize = Integer.MAX_VALUE;
    private static int maxActions = Integer.MIN_VALUE;
    private static int minActions = Integer.MAX_VALUE;

    private static int localLayerSizeSum = 0;
    private static int localMaxLayerSize = Integer.MIN_VALUE;
    private static int localMinLayerSize = Integer.MAX_VALUE;
    private static int localNumLayers = 0;

    private static int localNumNodes = 0;
    private static int localNeighbourSizeSum = 0;
    private static int localActionsSum = 0;
    private static int localMaxNeighbourSize = Integer.MIN_VALUE;
    private static int localMinNeighbourSize = Integer.MAX_VALUE;
    private static int localMaxActions = Integer.MIN_VALUE;
    private static int localMinActions = Integer.MAX_VALUE;

    public static void addToLayerCount(int count) {
        Benchmarker.layerSizeSum += count;
        Benchmarker.localLayerSizeSum += count;

        Benchmarker.maxLayerSize = Math.max(Benchmarker.maxLayerSize, count);
        Benchmarker.localMaxLayerSize = Math.max(Benchmarker.localMaxLayerSize, count);

        Benchmarker.minLayerSize = Math.min(Benchmarker.minLayerSize, count);
        Benchmarker.localMinLayerSize = Math.min(Benchmarker.localMinLayerSize, count);

        Benchmarker.numLayers++;
        Benchmarker.localNumLayers++;
    }

    private static void resetLocals() {
        Benchmarker.localLayerSizeSum = 0;
        Benchmarker.localMaxLayerSize = Integer.MIN_VALUE;
        Benchmarker.localMinLayerSize = Integer.MAX_VALUE;
        Benchmarker.localNumLayers = 0;
        Benchmarker.localNumNodes = 0;
        Benchmarker.localNeighbourSizeSum = 0;
        Benchmarker.localActionsSum = 0;
        Benchmarker.localMaxNeighbourSize = Integer.MIN_VALUE;
        Benchmarker.localMinNeighbourSize = Integer.MAX_VALUE;
        Benchmarker.localMaxActions = Integer.MIN_VALUE;
        Benchmarker.localMinActions = Integer.MAX_VALUE;
    }

    public static void addToNeighbourCount(int count) {
        Benchmarker.neighbourSizeSum += count;
        Benchmarker.localNeighbourSizeSum += count;

        Benchmarker.maxNeighbourSize = Math.max(Benchmarker.maxNeighbourSize, count);
        Benchmarker.localMaxNeighbourSize = Math.max(Benchmarker.localMaxNeighbourSize, count);

        Benchmarker.minNeighourSize = Math.min(Benchmarker.minNeighourSize, count);
        Benchmarker.localMinNeighbourSize = Math.min(Benchmarker.localMinNeighbourSize, count);

        Benchmarker.numNodes++;
        Benchmarker.localNumNodes++;
    }

    public static void addToActionCount(int count) {
        Benchmarker.actionsSum += count;
        Benchmarker.localActionsSum += count;

        Benchmarker.maxActions = Math.max(Benchmarker.maxActions, count);
        Benchmarker.localMaxActions = Math.max(Benchmarker.localMaxActions, count);

        Benchmarker.minActions = Math.min(Benchmarker.minActions, count);
        Benchmarker.localMinActions = Math.min(Benchmarker.localMinActions, count);
    }

    private static double getAverageActions() {
        return (double) Benchmarker.actionsSum / Benchmarker.numNodes;
    }

    private static double getAverageLocalActions() {
        return (double) Benchmarker.localActionsSum / Benchmarker.localNumNodes;
    }

    private static double getAverageLayerSize() {
        return (double) Benchmarker.layerSizeSum / Benchmarker.numLayers;
    }

    private static double getAverageLocalLayerSize() {
        return (double) Benchmarker.localLayerSizeSum / Benchmarker.localNumLayers;
    }

    private static double getAverageNeighbourSize() {
        return (double) Benchmarker.neighbourSizeSum / Benchmarker.numNodes;
    }

    private static double getAverageLocalNeighbourSize() {
        return (double) Benchmarker.localNeighbourSizeSum / Benchmarker.localNumNodes;
    }

    public static void main(String[] args) throws IOException {
        LogManager.logImportant("Loading domains...");
        List<Domain> domains = Loader.loadDomains();
        int problemsSolved = 0;
        int ehcSuccess = 0;
        Scanner scanner = new Scanner(System.in);

        String domainSeparator = "*".repeat(40);
        String problemSeprator = "-".repeat(20);
        String typeSeparator = "_".repeat(10);

        // long logId = Files.list(Paths.get("logs"))
        // .filter(Files::isDirectory) // Filter to get only directories
        // .count() + 1;
        long logId = 13;
        LogManager.toggleOutput();
        boolean wait = false;
        boolean temp = false;
        int tempProblemIndexOffset = 0;
        for (int i = 0; i < domains.size(); i++) {
            LogManager.logImportant("\n");
            LogManager.logImportant(domainSeparator);
            LogManager.logImportant("Domain " + i + ": " + domains.get(i).getName());
            Domain domain = domains.get(i);
            // if (!domain.getName().equals("logistics98"))
            // continue;
            List<Problem> problems = domain.getProblems();
            int j = 0;
            // int j = problems.size() - 10;
            // int j = (problems.size() - 10) / 2;
            j += tempProblemIndexOffset;
            tempProblemIndexOffset = 0;
            for (; j < problems.size(); j++) {
                LogManager.logImportant(problemSeprator);

                Problem problem = problems.get(j);
                // JavaFF ff = new JavaFF(domain.getDomainFile(), problem.getProblemFile(),
                // null);
                int k = 0;
                if (temp) {
                    k = 1;
                    temp = false;
                }
                for (; k < 2; k++) {
                    LogManager.logImportant("Problem: " + problem.getProblemFile());
                    LogManager.logImportant("Domain " + i + ": " + domains.get(i).getName());
                    LogManager.logImportant("Lower bound: " + problem.getLowerBound());
                    // JavaFF ff = new JavaFF(new File("domain.pddl"), new File("problem.pddl"),
                    // null);
                    JavaFF ff = new JavaFF(domain.getDomainFile(), problem.getProblemFile(),
                            null);

                    Plan plan = null;

                    JavaFF.useParallel = k == 1;
                    ff.setUseEHC(true);
                    ff.setUseBFS(true);
                    LogManager.logImportant(typeSeparator);
                    LogManager.logImportant("PARALLEL: " + JavaFF.useParallel);
                    if (wait) {
                        LogManager.logImportant("Press any key to begin solving");
                        LogManager.logImportant(">", false);
                        scanner.nextLine(); // Waits for user to press Enter
                    }
                    try {
                        plan = ff.plan();
                    } catch (Exception e) {
                        // totalPlanningTime += (endTime - startTime) / 1e9;
                        LogManager.logImportant("An error occured while solving problem " +
                                problem.getProblemFile() +
                                ":");
                        e.printStackTrace();
                        continue;
                    }

                    LogManager.logImportant("------------RESULTS------------");
                    if (ff.planningBFSTime == 0) {
                        ehcSuccess++;
                    }
                    LogManager.logImportant("EHC plan time: " + ff.planningEHCTime);
                    LogManager.logImportant("BFS plan time: " + ff.planningBFSTime);
                    LogManager.logImportant("");

                    problemsSolved++;

                    Path logFilePath = Paths
                            .get("logs/" + logId + "/" + domain.getName() + "/" + (JavaFF.useParallel ? "concurrent"
                                    : "serial") + ".json");

                    JSONObject logContents;
                    if (Files.exists(logFilePath)) {
                        String content = new String(Files.readAllBytes(logFilePath));
                        logContents = new JSONObject(content);
                    } else {
                        Files.createDirectories(logFilePath.getParent());
                        logContents = new JSONObject();
                    }

                    JSONObject problemLog = new JSONObject();
                    problemLog.put("completed", true);
                    problemLog.put("totalTimeGrounding", ff.groundingTime);
                    problemLog.put("totalTimeReachabilityAnalysis", ff.reachabilityAnalysisTime);
                    problemLog.put("totalTimeEHC", ff.planningEHCTime);
                    problemLog.put("totalTimeBFS", ff.planningBFSTime);
                    problemLog.put("cost", plan.getCost());
                    problemLog.put("lowerBound", problem.getLowerBound());
                    logContents.put(problem.getProblemDirName(), problemLog);

                    Files.write(logFilePath, logContents.toString(4).getBytes());
                }

                // BigDecimal cost = plan.getCost();
                // LogManager.logImportant("Problem solved successfully.");
                // LogManager.logImportant("Took " + ff.timeTaken + " seconds of planning.");
                // LogManager.logImportant("Took " + (endTime - startTime) / 1e9 + " seconds of
                // realtime.");
                // LogManager.logImportant("Plan cost: " + cost);
                // totalPlanningTime += ff.timeTaken;
                // totalRealTime += (endTime - startTime) / 1e9;

                // LogManager.logImportant("Max layer size: " + Benchmarker.localMaxLayerSize);
                // LogManager.logImportant("Min layer size: " + Benchmarker.localMinLayerSize);
                // LogManager.logImportant("Average layer size: " + getAverageLocalLayerSize());
                // LogManager.logImportant("Max neighbour size: " +
                // Benchmarker.localMaxNeighbourSize);
                // LogManager.logImportant("Min neighbour size: " +
                // Benchmarker.localMinNeighbourSize);
                // LogManager.logImportant("Average neighbour size: " +
                // getAverageLocalNeighbourSize());
                // LogManager.logImportant("Max actions: " + Benchmarker.localMaxActions);
                // LogManager.logImportant("Min actions: " + Benchmarker.localMinActions);
                // LogManager.logImportant("Average actions: " + getAverageLocalActions());

                // resetLocals();
            }
        }

        LogManager.logImportant("EHC was successful in " + ehcSuccess + " out of " + problemsSolved + " problems.");
        scanner.close();

        // LogManager.logImportant("Solved " + problemsSolved + "");
        // LogManager.logImportant("Total planning time: " + totalPlanningTime +
        // "seconds.");
        // LogManager.logImportant("Total real time: " + totalRealTime + " seconds.");
        // LogManager.logImportant("Max layer size: " + Benchmarker.maxLayerSize);
        // LogManager.logImportant("Min layer size: " + Benchmarker.minLayerSize);
        // LogManager.logImportant("Average layer size: " + getAverageLayerSize());
        // LogManager.logImportant("Max neighbour size: " +
        // Benchmarker.maxNeighbourSize);
        // LogManager.logImportant("Min neighbour size: " +
        // Benchmarker.minNeighourSize);
        // LogManager.logImportant("Average neighbour size: " +
        // getAverageNeighbourSize());
        // LogManager.logImportant("Max actions: " + Benchmarker.maxActions);
        // LogManager.logImportant("Min actions: " + Benchmarker.minActions);
        // LogManager.logImportant("Average actions: " + getAverageActions());

        JavaFF.executorService.shutdown();

        // Max layer size: 22
        // Average layer size: 2.0835214446952595
        // Max neighbour size: 46
        // Average neighbour size: 9.857142857142858

    }

}

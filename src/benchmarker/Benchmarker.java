package benchmarker;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

import org.json.JSONArray;
import org.json.JSONObject;

import javaff.JavaFF;
import javaff.data.Plan;

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

    public static void main(String[] args) {
        URI uri = URI.create("https://api.planning.domains/json/classical/collection/" + benchmarkSetId);

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .uri(uri)
                .build();
        try {
            System.out.println("Fetching the FF Benchmark Set from " + uri.toString() + "...");
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            JSONObject json = new JSONObject(response.body());

            JSONObject result = json.getJSONObject("result");
            String name = result.getString("collection_name");
            // JSONArray domains = result.getJSONArray("domain_set");
            String domains = result.getString("domain_set");
            domains = domains.substring(1, domains.length() - 1);
            // System.out.println(domains);
            ArrayList<String> domainSet = new ArrayList<String>(Arrays.asList(domains.split(",")));
            // System.out.println(domainSet.get(0));
            // System.out.println(domains);

            ArrayList<Domain> domainList = new ArrayList<Domain>();

            int problemsSolved = 0;
            double startTime, endTime, totalTime = 0;
            System.out.println("Found " + domains.length() + " domains in the FF benchmark set.");
            for (int i = 3; i < 4; i++) {
                String domainId = domainSet.get(i);
                HashSet<String> badDomains = new HashSet<String>(Arrays.asList("15", "29"));
                if (badDomains.contains(domainId)) {
                    System.out.println("Skipping domain " + domainId + "...");
                    continue;
                }
                uri = URI.create("https://api.planning.domains/json/classical/problems/" + domainId);

                request = HttpRequest.newBuilder()
                        .GET()
                        .uri(uri)
                        .build();

                System.out.println("Fetching problems from domain " + domainId + "...");
                response = client.send(request, HttpResponse.BodyHandlers.ofString());
                json = new JSONObject(response.body());

                JSONArray problems = json.getJSONArray("result");
                System.out.println("Found " + problems.length() + " problems in domain " + domainId + ".");
                JSONObject first = problems.getJSONObject(0);
                String domainUrl = first.getString("domain_url");
                String domainName = first.getString("domain");

                request = HttpRequest.newBuilder()
                        .GET()
                        .uri(URI.create(domainUrl))
                        .build();

                System.out.println("Fetching definition of domain " + domainId + "...");
                String domainStr = client.send(request, HttpResponse.BodyHandlers.ofString()).body();

                Domain domain = new Domain(domainName, domainUrl);

                System.out.println("Solving problems in domain " + domainId + "...");
                for (int j = 3; j < 4; j++) {
                    JSONObject problem = problems.getJSONObject(j);
                    String problemUrl = problem.getString("problem_url");
                    String problemName = problem.getString("problem");
                    int problemLowerBound = problem.getInt("lower_bound");
                    int problemUpperBound = problem.getInt("upper_bound");

                    request = HttpRequest.newBuilder()
                            .GET()
                            .uri(URI.create(problemUrl))
                            .build();

                    System.out.println("Fetching definition of problem " + j + "/" + problems.length() + ": "
                            + problemName + " of domain " + i + "/" + domainSet.size() + ":" + domainName + "...");
                    String problemStr = client.send(request, HttpResponse.BodyHandlers.ofString()).body();

                    Problem p = new Problem(problemUrl, problemName, problemLowerBound, problemUpperBound);

                    System.out.println("Problem fetched successfully. Solving...");
                    JavaFF ff = new JavaFF(domainStr, problemStr);

                    Plan plan = null;

                    startTime = System.nanoTime();
                    try {
                        plan = ff.plan();
                    } catch (Exception e) {
                        endTime = System.nanoTime();
                        totalTime += (endTime - startTime) / 1e9;
                        System.out.println("An error occured while solving problem " + problemName + ":");
                        e.printStackTrace();
                        continue;
                    }

                    p.timeTaken = ff.timeTaken;
                    BigDecimal cost = plan.getCost();
                    System.out.println("Problem solved successfully.");
                    System.out.println("Took " + p.timeTaken + " seconds.");
                    System.out.println("Plan cost: " + cost);
                    totalTime += p.timeTaken;

                    System.out.println("Max layer size: " + Benchmarker.localMaxLayerSize);
                    System.out.println("Min layer size: " + Benchmarker.localMinLayerSize);
                    System.out.println("Average layer size: " + getAverageLocalLayerSize());
                    System.out.println("Max neighbour size: " + Benchmarker.localMaxNeighbourSize);
                    System.out.println("Min neighbour size: " + Benchmarker.localMinNeighbourSize);
                    System.out.println("Average neighbour size: " + getAverageLocalNeighbourSize());
                    System.out.println("Max actions: " + Benchmarker.localMaxActions);
                    System.out.println("Min actions: " + Benchmarker.localMinActions);
                    System.out.println("Average actions: " + getAverageLocalActions());

                    domain.addProblem(p);
                    problemsSolved++;
                    resetLocals();

                    // Get the Java runtime
                    // Runtime runtime = Runtime.getRuntime();
                    // // Run the garbage collector
                    // runtime.gc();
                    // // Calculate memory usage
                    // long memory = runtime.totalMemory() - runtime.freeMemory();
                    // System.out.println("Used memory is bytes: " + memory);
                }

                domainList.add(domain);
            }

            System.out.println("Solved " + problemsSolved + "");
            System.out.println("Total time: " + totalTime + " seconds.");
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("Max layer size: " + Benchmarker.maxLayerSize);
        System.out.println("Min layer size: " + Benchmarker.minLayerSize);
        System.out.println("Average layer size: " + getAverageLayerSize());
        System.out.println("Max neighbour size: " + Benchmarker.maxNeighbourSize);
        System.out.println("Min neighbour size: " + Benchmarker.minNeighourSize);
        System.out.println("Average neighbour size: " + getAverageNeighbourSize());
        System.out.println("Max actions: " + Benchmarker.maxActions);
        System.out.println("Min actions: " + Benchmarker.minActions);
        System.out.println("Average actions: " + getAverageActions());

        // Get the Java runtime
        // Runtime runtime = Runtime.getRuntime();
        // // Run the garbage collector
        // runtime.gc();
        // // Calculate memory usage
        // long memory = runtime.totalMemory() - runtime.freeMemory();
        // System.out.println("Total used memory is bytes: " + memory);

        // System.out.println(response.body());

        // Max layer size: 22
        // Average layer size: 2.0835214446952595
        // Max neighbour size: 46
        // Average neighbour size: 9.857142857142858

    }

}

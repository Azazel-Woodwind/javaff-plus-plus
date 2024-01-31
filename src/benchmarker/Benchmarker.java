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
    private static int numLayers = 0;
    private static int numNodes = 0;
    private static int maxLayerSize = Integer.MIN_VALUE;
    private static int maxNeighbourSize = Integer.MIN_VALUE;

    public static void addToLayerCount(int count) {
        Benchmarker.layerSizeSum += count;
        Benchmarker.maxLayerSize = Math.max(Benchmarker.maxLayerSize, count);

        Benchmarker.numLayers++;
    }

    public static void addToNeighbourCount(int count) {
        Benchmarker.neighbourSizeSum += count;
        Benchmarker.maxNeighbourSize = Math.max(Benchmarker.maxNeighbourSize, count);

        Benchmarker.numNodes++;
    }

    private static double getAverageLayerSize() {
        return (double) Benchmarker.layerSizeSum / Benchmarker.numLayers;
    }

    private static double getAverageNeighbourSize() {
        return (double) Benchmarker.neighbourSizeSum / Benchmarker.numNodes;
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
            long startTime, endTime, totalTime = 0;
            System.out.println("Found " + domains.length() + " domains in the FF benchmark set.");
            for (int i = 0; i < 5; i++) {
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
                for (int j = 0; j < 10; j++) {
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

                    try {
                        startTime = System.nanoTime();
                        plan = ff.plan();
                        endTime = System.nanoTime();
                    } catch (Throwable e) {
                        System.out.println("An error occured while solving problem " + problemName + ":");
                        System.out.println(e.getMessage());
                        continue;
                    }

                    p.timeTaken = ff.timeTaken;
                    BigDecimal cost = plan.getCost();
                    System.out.println("Problem solved successfully.");
                    System.out.println("Took " + p.timeTaken + " seconds.");
                    System.out.println("Plan cost: " + cost);
                    totalTime += p.timeTaken;

                    domain.addProblem(p);
                    problemsSolved++;
                }

                domainList.add(domain);
            }

            System.out.println("Solved " + problemsSolved + " problems in " + totalTime + " seconds.");
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("Max layer size: " + Benchmarker.maxLayerSize);
        System.out.println("Average layer size: " + getAverageLayerSize());
        System.out.println("Max neighbour size: " + Benchmarker.maxNeighbourSize);
        System.out.println("Average neighbour size: " + getAverageNeighbourSize());

        // System.out.println(response.body());

        // Max layer size: 22
        // Average layer size: 2.0835214446952595
        // Max neighbour size: 46
        // Average neighbour size: 9.857142857142858

    }

}

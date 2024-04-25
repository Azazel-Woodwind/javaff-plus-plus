package benchmarker;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

public class Loader {
    public static final String benchmarkCollectionURI = "https://api.planning.domains/json/classical/collection/11";
    public static final String domainProblemsBaseURI = "https://api.planning.domains/json/classical/problems/";
    public static final HashSet<String> badDomainIds = new HashSet<String>(Arrays.asList(
            "15",
            "29",
            "62",
            "99",
            "110",
            "112",
            "114",
            "121",
            "123",
            "125")); // contain unsupported requirements for JavaFF2.1

    public static void main(String[] args) throws IOException, InterruptedException {
        URI uri = URI.create(benchmarkCollectionURI);

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .uri(uri)
                .build();

        System.out.println("Fetching the FF Benchmark Set from " + uri.toString() +
                "...");

        HttpResponse<String> response = client.send(request,
                HttpResponse.BodyHandlers.ofString());
        JSONObject json = new JSONObject(response.body());

        JSONObject result = json.getJSONObject("result");
        String domains = result.getString("domain_set");
        domains = domains.substring(1, domains.length() - 1);
        List<String> domainSet = new ArrayList<String>(Arrays.asList(domains.split(",")));

        System.out.println("Found " + domainSet.size() + " domains in the FF benchmark set.");

        for (int i = 0; i < domainSet.size(); i++) {
            String domainId = domainSet.get(i);
            if (badDomainIds.contains(domainId)) {
                System.out.println("Skipping domain " + domainId + "...");
                continue;
            }

            uri = URI.create(domainProblemsBaseURI +
                    domainId);
            request = HttpRequest.newBuilder()
                    .GET()
                    .uri(uri)
                    .build();

            System.out.println("Fetching problems from domain " + domainId + "...");
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
            json = new JSONObject(response.body());

            JSONArray problems = json.getJSONArray("result");
            JSONObject first = problems.getJSONObject(0);

            String domainPath = first.getString("domain_path");
            String domainName = domainPath.split("/")[1];

            System.out.println("Found " + problems.length() + " problems in domain " +
                    domainId + ": " + domainName + ".");

            System.out.println("Fetching domain definition...");
            String domainUrl = first.getString("domain_url");
            request = HttpRequest.newBuilder()
                    .GET()
                    .uri(URI.create(domainUrl))
                    .build();

            String domainStr = client.send(request,
                    HttpResponse.BodyHandlers.ofString()).body();

            // write to file
            System.out.println("Writing domain definition to file...");
            Path domainFilePath = Paths.get("domains/" + domainName + "/domain.pddl");

            Files.createDirectories(domainFilePath.getParent());
            Files.write(domainFilePath, domainStr.getBytes());

            System.out.println("Sorting problems by cost lower bound...");
            List<JSONObject> problemsList = new ArrayList<>();
            int minLowerBound = Integer.MAX_VALUE;
            int maxLowerBound = Integer.MIN_VALUE;
            for (int j = 0; j < problems.length(); j++) {
                JSONObject problem = problems.getJSONObject(j);
                if (!problem.isNull("lower_bound")) {
                    int lowerBound = problem.getInt("lower_bound");
                    minLowerBound = Math.min(minLowerBound, lowerBound);
                    maxLowerBound = Math.max(maxLowerBound, lowerBound);
                }
                problemsList.add(problem);
            }

            final int finalMinLowerBound = minLowerBound;
            final int finalMaxLowerBound = maxLowerBound;
            Collections.sort(problemsList, new Comparator<JSONObject>() {
                @Override
                public int compare(JSONObject problem1, JSONObject problem2) {
                    int lb1, lb2;
                    if (problem1.isNull("lower_bound")) {
                        lb1 = (finalMinLowerBound + finalMaxLowerBound) / 2;
                    } else {
                        lb1 = problem1.getInt("lower_bound");
                    }

                    if (problem2.isNull("lower_bound")) {
                        lb2 = (finalMinLowerBound + finalMaxLowerBound) / 2;
                    } else {
                        lb2 = problem2.getInt("lower_bound");
                    }

                    return Integer.compare(lb1, lb2);
                }
            });

            System.out.println("Writing problem definitions to files...");
            for (int j = 0; j < problemsList.size(); j++) {
                JSONObject problem = problemsList.get(j);
                String problemUrl = problem.getString("problem_url");

                request = HttpRequest.newBuilder()
                        .GET()
                        .uri(URI.create(problemUrl))
                        .build();

                String problemStr = client.send(request,
                        HttpResponse.BodyHandlers.ofString()).body();

                String problemName = problem.getString("problem");
                int problemLowerBound;
                if (!problem.isNull("lower_bound")) {
                    problemLowerBound = problem.getInt("lower_bound");
                } else {
                    problemLowerBound = -1;
                }

                String ID = (char) (j + 65) + "-" + j;
                String problemDirectoryPath = "domains/" + domainName + "/" + ID + "."
                        + problemName.split("\\.")[0];
                Path problemFilePath = Paths
                        .get(problemDirectoryPath + "/problem.pddl");
                Files.createDirectories(problemFilePath.getParent());
                Files.write(problemFilePath, problemStr.getBytes());

                JSONObject metadata = new JSONObject();
                metadata.put("lowerBound", problemLowerBound);

                Path metadataFilePath = Paths
                        .get(problemDirectoryPath + "/metadata.json");
                Files.write(metadataFilePath, metadata.toString(4).getBytes());
            }

            System.out.println("Finished processing domain " + domainId + ": " + domainName + ".");
        }
    }

    public static List<Domain> loadDomains() {
        if (!Files.exists(Paths.get("domains"))) {
            throw new RuntimeException("No domains found in the 'domains' directory.");
        }

        List<Domain> domains = new ArrayList<Domain>();
        try {
            Path start = Paths.get("domains");
            Files.list(start)
                    .filter(Files::isDirectory)
                    .map(path -> path.getFileName().toString())
                    .sorted()
                    .forEach(domainName -> domains.add(new Domain(domainName)));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return domains;
    }
}

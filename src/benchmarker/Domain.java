package benchmarker;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Domain {

    private String name;
    private List<Problem> problems;
    private File domainFile;

    public Domain(String name) {
        this.name = name;
        this.domainFile = new File("domains/" + name + "/domain.pddl");
        this.problems = loadProblems();
    }

    private List<Problem> loadProblems() {
        Path start = Paths.get("domains/" + name);
        try {
            return Files.list(start)
                    .filter(Files::isDirectory) // Filter to get only directories
                    .map(path -> path.getFileName().toString()) // Convert Path to String
                    .sorted() // Sort alphabetically
                    .map(problemDirName -> new Problem(name, problemDirName)) // Create a Problem object
                    .collect(Collectors.toList()); // Collect into a list

        } catch (IOException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public String getName() {
        return name;
    }

    public List<Problem> getProblems() {
        return problems;
    }

    public File getDomainFile() {
        return domainFile;
    }
}

package benchmarker;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.json.JSONObject;

public class Problem {
    private int lowerBound;
    private File problemFile;
    private String domainName, problemDirName;

    public Problem(String domainName, String problemDirName) {
        this.domainName = domainName;
        this.problemDirName = problemDirName;
        this.problemFile = new File("domains/" + domainName + "/" + problemDirName + "/problem.pddl");

        initialiseMetadata();
    }

    private void initialiseMetadata() {
        try {
            String content = new String(
                    Files.readAllBytes(Paths.get("domains/" + domainName + "/" + problemDirName + "/metadata.json")));
            JSONObject jsonObject = new JSONObject(content);
            this.lowerBound = jsonObject.getInt("lowerBound");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public int getLowerBound() {
        return lowerBound;
    }

    public File getProblemFile() {
        return problemFile;
    }

    public String getDomainName() {
        return domainName;
    }

    public String getProblemDirName() {
        return problemDirName;
    }
}
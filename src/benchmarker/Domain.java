package benchmarker;

import java.util.ArrayList;

public class Domain {

    public String name, url;

    public ArrayList<Problem> problems;

    public double averageTime;

    public Domain(String name, String url) {
        this.name = name;
        this.url = url;
        this.problems = new ArrayList<Problem>();
        this.averageTime = 0;
    }

    public void addProblem(Problem problem) {
        this.averageTime = (this.averageTime * this.problems.size() + problem.timeTaken) / (this.problems.size() + 1);
        this.problems.add(problem);
    }
    
}

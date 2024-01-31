package benchmarker;

public class Problem {
    public String name, domainName, url;

    public int lowerBound, upperBound;

    public double timeTaken;

    public Problem(String url, String domainName, int lowerBound, int upperBound) {
        this.domainName = domainName;
        this.url = url;
        this.lowerBound = lowerBound;
        this.upperBound = upperBound;
    }
}

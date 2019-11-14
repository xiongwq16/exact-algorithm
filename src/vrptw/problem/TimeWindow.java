package vrptw.problem;

public class TimeWindow {
    private double earliestTime;
    private double latestTime;
    
    public TimeWindow(double earliestTime, double latestTime) {
        this.earliestTime = earliestTime;
        this.latestTime = latestTime;
    }
    
    public double getEarliestTime() {
        return earliestTime;
    }
    
    public double getLatestTime() {
        return latestTime;
    }
    
}

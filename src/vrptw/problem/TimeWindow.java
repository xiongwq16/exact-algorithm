package vrptw.problem;

/**
 * Time window.
 * 
 * @author Xiong Wangqi
 * @version V1.0
 * @since JDK1.8
 */
public class TimeWindow {
    private double earliestTime;
    private double latestTime;
    
    TimeWindow(double earliestTime, double latestTime) {
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

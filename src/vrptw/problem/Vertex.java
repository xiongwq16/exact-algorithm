package vrptw.problem;

/**
 * 节点类，作为 Customer 和 Depot 类的父类.
 * 
 * @author Xiong Wangqi
 * @version V1.0
 * @since JDK1.8
 */
public class Vertex {
    /** 节点索引号，方便设计算法. */
    private int index;
    
    /** 算例数据中的节点 ID. */
    private String id;
    
    private double demand;
    private double serviceTime;
    
    private double x;
    private double y;
    
    private TimeWindow timeWindow;
    
    private class TimeWindow {
        double earliestTime;
        double latestTime;
        
        TimeWindow(double earliestTime, double latestTime) {
            this.earliestTime = earliestTime;
            this.latestTime = latestTime;
        }
    }
    
    Vertex(int index, String id, double x, double y, double demand, 
            double serviceTime, double earliestTime, double latestTime) {
        this.index = index;
        this.id = id;
        this.x = x;
        this.y = y;
        this.demand = demand;;
        this.serviceTime = serviceTime;
        timeWindow = new TimeWindow(earliestTime, latestTime);
    }
    
    Vertex(Vertex v) {
        this.index = v.index;
        this.id = v.id;
        this.x = v.x;
        this.y = v.y;
        this.demand = v.demand;
        this.serviceTime = v.serviceTime;
        this.timeWindow = new TimeWindow(v.timeWindow.earliestTime, v.timeWindow.latestTime);
    }
    
    double getDistanceTo(Vertex v) {
        int distToRound = (int)(10 * Math.sqrt((this.x - v.x) * (this.x - v.x) + (this.y - v.y) * (this.y - v.y)));
        return distToRound / 10.0;
    }
    
    public int getIndex() {
        return index;
    }
    
    public String getId() {
        return id;
    }
    
    public double getDemand() {
        return demand;
    }
    
    public double getServiceTime() {
        return serviceTime;
    }
    
    public double getEarliestTime() {
        return timeWindow.earliestTime;
    }
    
    public double getLatestTime() {
        return timeWindow.latestTime;
    }
}
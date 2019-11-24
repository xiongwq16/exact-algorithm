package vrptw.problem;

/**
 * 节点类，作为 Customer 和 Depot 类的父类.
 * 
 * @author Xiong Wangqi
 * @version V1.0
 * @since JDK1.8
 */
public class Vertex {
    /** 节点 id，方便设计算法. */
    private int id;
    
    /** 算例数据中的节点编号. */
    private String number;
    private double demand;
    private double serviceTime;
    
    private double x;
    private double y;
    
    private TimeWindow timeWindow;
    
    Vertex(int id, String number, double x, double y, double demand, 
            double serviceTime, double earliestTime, double latestTime) {
        this.id = id;
        this.number = number;
        this.x = x;
        this.y = y;
        this.demand = demand;
        this.serviceTime = serviceTime;
        timeWindow = new TimeWindow(earliestTime, latestTime);
    }
    
    Vertex(Vertex v, TimeWindow tw) {
        this.id = v.id;
        this.number = v.number;
        this.x = v.x;
        this.y = v.y;
        this.demand = v.demand;
        this.serviceTime = v.serviceTime;
        
        this.timeWindow = tw;
    }
    
    double getDistanceTo(Vertex v) {
        // 保留两位小数
        int distToRound = (int)(100 * Math.sqrt((this.x - v.x) * (this.x - v.x) + (this.y - v.y) * (this.y - v.y)));
        return distToRound / 100.0;
    }
    
    public int getId() {
        return id;
    }
    
    public String getNumber() {
        return number;
    }
    
    public double getDemand() {
        return demand;
    }
    
    public double getServiceTime() {
        return serviceTime;
    }
    
    public double getEarliestTime() {
        return timeWindow.getEarliestTime();
    }
    
    public double getLatestTime() {
        return timeWindow.getLatestTime();
    }
}
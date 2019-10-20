package vrptw.solution;

import java.util.ArrayList;

/**
 * 路径.
 * 
 * @author Xiong Wangqi
 * @version V1.0
 * @since JDK1.8
 */
public class Path {
    /** 路径上的节点的索引数组. */
    private ArrayList<Integer> nodeIndices;
    /** 该路径上各个节点的访问次数. */
    private int[] nodeVisitTime;
    
    /** 路径的实际成本. */
    private double cost;
    
    /**
     * Create a Instance Path.
     * 
     * @param nodeIndices 路径上的节点的索引数组
     * @param cost 路径的成本
     * @param totalNodeNum 问题中的节点总数
     */
    public Path(ArrayList<Integer> nodeIndices, double cost, int totalNodeNum) {        
        this.nodeIndices = new ArrayList<>(nodeIndices);
        this.nodeVisitTime = new int[totalNodeNum];
        for (int i = 0; i < nodeIndices.size(); i++) {
            this.nodeVisitTime[nodeIndices.get(i)] = 1;
        }
        
        this.cost = cost;
    }
        
    /**
     * 计算并返回 sValue
     * Svalue[i] = 1 − |K| * (sum[i][j], for j in V)，其中 V 表示客户和配送中心，<br>
     * 对应原模型中的 sum (x[i][j][k], for j in nodes, k in |K|) = 1.
     * 
     * @param vehNum 车辆数
     * @return Svalue
     */
    public double[] calSvalue(int vehNum) {
        int totalNodeNum = this.nodeVisitTime.length;
        double[] svalue = new double[totalNodeNum];
        // 初始化
        for (int i = 0; i < totalNodeNum; i++) {
            svalue[i] = 1;
        }
        
        // 更新在路径上的点对应的 sValue[i]
        for (int i = 0; i < nodeIndices.size() -  1; i++) {
            svalue[nodeIndices.get(i)] -= vehNum;
        }
        
        return svalue;
    }
    
    public String pathToString() {   
        StringBuilder sb = new StringBuilder();
        
        this.nodeIndices.forEach(index -> sb.append(index + "-"));
        
        return sb.substring(0, sb.length() - 1).toString();
    }
    
    public int[] getNodeVisitTime() {
        return nodeVisitTime;
    }
    
    public double getCost() {
        return cost;
    }

    public ArrayList<Integer> getNodeIndices() {
        return nodeIndices;
    }
}

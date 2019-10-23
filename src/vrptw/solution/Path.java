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
    private ArrayList<Integer> vertexIndices;
    /** 该路径上各个节点的访问次数. */
    private int[] vertexVisitTime;
    
    /** 路径的实际成本. */
    private double cost;
    
    /**
     * Create a Instance Path.
     * 
     * @param vertexIndices 路径上的节点的索引数组
     * @param cost 路径的成本
     * @param totalVertexNum 问题中的节点总数
     */
    public Path(ArrayList<Integer> vertexIndices, double cost, int totalVertexNum) {        
        this.vertexIndices = new ArrayList<>(vertexIndices);
        this.vertexVisitTime = new int[totalVertexNum];
        for (int i = 0; i < vertexIndices.size(); i++) {
            this.vertexVisitTime[vertexIndices.get(i)] = 1;
        }
        
        this.cost = cost;
    }
        
    /**
     * 计算并返回 sValue
     * Svalue[i] = 1 − |K| * (sum[i][j], for j in V)，其中 V 表示客户和配送中心，<br>
     * 对应原模型中的 sum (x[i][j][k], for j in vertexes, k in |K|) = 1.
     * 
     * @param vehNum 车辆数
     * @return Svalue
     */
    public double[] calSvalue(int vehNum) {
        int totalVertexNum = this.vertexVisitTime.length;
        double[] svalue = new double[totalVertexNum];
        // 初始化
        for (int i = 0; i < totalVertexNum; i++) {
            svalue[i] = 1;
        }
        
        // 更新在路径上的点对应的 sValue[i]
        for (int i = 0; i < vertexIndices.size() -  1; i++) {
            svalue[vertexIndices.get(i)] -= vehNum;
        }
        
        return svalue;
    }
    
    public String pathToString() {   
        StringBuilder sb = new StringBuilder();
        
        this.vertexIndices.forEach(index -> sb.append(index + "-"));
        
        return sb.substring(0, sb.length() - 1).toString();
    }
    
    public int[] getVertexVisitTime() {
        return vertexVisitTime;
    }
    
    public double getCost() {
        return cost;
    }

    public ArrayList<Integer> getVertexIndices() {
        return vertexIndices;
    }
}

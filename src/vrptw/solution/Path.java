package vrptw.solution;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import vrptw.parameter.Parameters;
import vrptw.problem.Vertex;
import vrptw.problem.Vrptw;

/**
 * 路径.
 * 
 * @author Xiong Wangqi
 * @version V1.0
 * @since JDK1.8
 */
public class Path {
    /** 路径上的节点的 ID 数组. */
    private ArrayList<Integer> vertexIds;
    /** 该路径上各个客户被访问的次数. */
    private Map<Integer, Integer> cusVisitedTime;
    
    /** 路径的实际成本. */
    private double cost;
    
    /**
     * 根据节点访问序列生成路径.
     * 
     * @param vrptwIns VRPTW instance
     * @param vertexIds 路径上的节点的 ID 数组
     */
    public Path(Vrptw vrptwIns, List<Integer> vertexIds) {        
        int visitVertexNum = vertexIds.size();
        if (vertexIds.get(0) != 0 || vertexIds.get(visitVertexNum - 1) != vrptwIns.getVertexNum() - 1) {
            throw new IllegalArgumentException("Path without start depot and en depot is illegal.");
        }
        
        this.vertexIds = new ArrayList<>(vertexIds);
        
        int cusNum = vrptwIns.getCusNum();
        cusVisitedTime = new HashMap<Integer, Integer>((int) (cusNum / Parameters.LOADER_FACTOR) + 1);
        // 初始化为 0
        for (Vertex cus: vrptwIns.getCustomers()) {
            cusVisitedTime.put(cus.getId(), 0);
        }
        
        cost = vrptwIns.getDistMatrix()[0][vertexIds.get(1)];
        for (int i = 1; i < visitVertexNum - 1; i++) {
            int vertexId = vertexIds.get(i);
            cusVisitedTime.put(vertexId, cusVisitedTime.get(vertexId) + 1);
            cost += vrptwIns.getDistMatrix()[vertexId][vertexIds.get(i + 1)];
        }
        
    }
    
    /**
     * 计算并返回 sValue, Svalue[i] = 1 − |K| * sum(x[i][j], for j in vertexes)，<br>
     * 对应原模型中的 sum (x[i][j][k], for j in vertexes, k in |K|) = 1，拉格朗日松弛时可用.
     * 
     * @param vrptwIns VRPTW 算例
     * @return Svalue
     */
    public Map<Integer, Integer> getSvalue(Vrptw vrptwIns) {
        int cusNum = vrptwIns.getCusNum();
        Map<Integer, Integer> svalue = new HashMap<>((int) (cusNum / Parameters.LOADER_FACTOR) + 1);
        // 初始化
        for (Vertex v: vrptwIns.getCustomers()) {
            svalue.put(v.getId(), 1);
        }
        
        // 更新在路径上的点对应的 sValue[i]
        int vehNum = vrptwIns.getVehNum();
        for (int vertexId: this.vertexIds) {
            svalue.put(vertexId, svalue.get(vertexId) - vehNum);
        }
        
        return svalue;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        
        this.vertexIds.forEach(id -> sb.append(id + "-"));
        
        return sb.substring(0, sb.length() - 1).toString();
    }
    
    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        
        if (other == null || !(other instanceof Path)) {
            return false;
        }
        
        Path that = (Path) other;
        if (this.cost != that.cost) {
            return false;
        }
        
        int vertexNum1 = this.vertexIds.size();
        int vertexNum2 = that.vertexIds.size();
        if (vertexNum1 != vertexNum2) {
            return false;
        }
        
        for (int i = 0; i < vertexNum1; i++) {
            if (this.vertexIds.get(i) != that.vertexIds.get(i)) {
                return false;
            }
        }
        
        return true;
    }
    
    public ArrayList<Integer> getVertexIds() {
        return vertexIds;
    }
    
    public Map<Integer, Integer> getCusVisitedTime() {
        return cusVisitedTime;
    }
    
    public double getCost() {
        return cost;
    }
    
}

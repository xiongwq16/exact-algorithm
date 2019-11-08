package vrptw.algorithm.subproblem;

import java.util.ArrayList;
import java.util.Map;

import vrptw.parameter.Parameters;
import vrptw.problem.Vrptw;
import vrptw.solution.Path;

/**
 * VRPTW 的 Price Problem（ESPPTWCC / SPPTWCC）抽象类.
 * 
 * @author Xiong Wangqi
 * @version V1.0
 * @since JDK1.8
 */
public abstract class AbstractPriceProblem {
    protected final double capacity;
    protected final int vertexNum;
    protected final Vrptw vrptwIns;

    protected double[][] timeMatrix;
    protected double[][] revisedCostMatrix;
    
    protected ArrayList<Path> shortestPaths;
    /** 被对偶变量修改后的最短路径的成本. */
    protected double revisedCostOfShortestPath;
    
    /**
     * 构造函数.
     * 
     * @param vrptwIns VRPTW 问题实例
     */
    public AbstractPriceProblem(Vrptw vrptwIns) {
        this.vrptwIns = vrptwIns;
        vertexNum = vrptwIns.getVertexNum();
        capacity = vrptwIns.getVehicle().getCapacity();
        shortestPaths = new ArrayList<>(Parameters.INITIAL_CAPACITY);
    }

    /**
     * Solve the price problem.
     * 
     * @param dualValues dual values of RMLP
     * @param timeMatrix time matrix
     */
    public abstract void solve(Map<Integer, Double> dualValues, double[][] timeMatrix);
        
    /**
     * 重置相关变量，准备下一次求解.
     */
    protected abstract void reset();
    
    /**
     * update the dual values and the revisedCost.
     * 
     * @param newDualValues new dual values
     */
    protected void updateDistAndCostMatrix(Map<Integer, Double> newDualValues) {
        // startDepot and endDepot has no lambda
        if (newDualValues.size() != vrptwIns.getCusNum()) {
            throw new IllegalArgumentException(
                    String.format("The lenght of lambda should be %d", vrptwIns.getCusNum()));
        }
        
        revisedCostMatrix = new double[vertexNum][vertexNum];
        for (int i = 0; i < vertexNum; i++) {
            for (int j = 0; j < vertexNum; j++) {
                revisedCostMatrix[i][j] = vrptwIns.getDistMatrix()[i][j];
            }
        }
        
        for (Map.Entry<Integer, Double> entry: newDualValues.entrySet()) {
            for (int j = 0; j < vertexNum; j++) {
                revisedCostMatrix[entry.getKey()][j] = revisedCostMatrix[entry.getKey()][j] - entry.getValue();
            }
        }
        
    }
    
    public ArrayList<Path> getShortestPath() {
        return shortestPaths;
    }
    
    public double getRevisedCostOfShortestPath() {
        return revisedCostOfShortestPath;
    }
    
}

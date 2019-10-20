package vrptw.algorithm.subproblem;

import java.util.ArrayList;

import vrptw.problem.Vrptw;
import vrptw.solution.Path;

/**
 * VRPTW 的子问题（ESPPTWCC / SPPTWCC）抽象类.
 * 
 * @author Xiong Wangqi
 * @version V1.0
 * @since JDK1.8
 */
abstract public class AbstractSubProblem {
    protected final int nodeNum;
    protected final Vrptw vrptwIns;

    protected final double[][] revisedCostMatrix;
    
    protected Path shortestPath;
    /** 路径对应的 reduced cost. */
    protected double reducedCost;
    
    /**
     * 构造函数.
     * 
     * @param vrptwIns VRPTW 问题实例
     * @param lambda   Dual prices of sum(x[i][j][k] for j in V, k in K) = 1
     */
    public AbstractSubProblem(Vrptw vrptwIns, double[] lambda) {
        // startDepot and endDepot has no lambda
        if (lambda.length != vrptwIns.getNodeNum() - 2) {
            throw new IllegalArgumentException(
                    String.format("The lenght of lambda should be %d", vrptwIns.getNodeNum() - 2));
        }

        this.vrptwIns = vrptwIns;

        nodeNum = vrptwIns.getNodeNum();
        revisedCostMatrix = new double[nodeNum][nodeNum];
        // startDepot and endDepot(dummy)
        for (int j = 0; j < nodeNum; j++) {
            revisedCostMatrix[0][j] = vrptwIns.getDistanceBetween(0, j);
            revisedCostMatrix[j][nodeNum - 1] = vrptwIns.getDistanceBetween(j, nodeNum - 1);
        }

        // customers
        for (int i = 1; i < nodeNum - 1; i++) {
            for (int j = 0; j < nodeNum; j++) {
                revisedCostMatrix[i][j] = vrptwIns.getDistanceBetween(i, j) - lambda[i - 1];
            }

        }

    }

    /**
     * 求解子问题.
     */
    abstract public void solve();

    /**
     * 基于节点访问序列，reduced cost 生成路径.
     * 
     * @param nodeIndices 节点访问序列（包括 startDepot 和 endDepot）
     * @return 给定节点访问序列对应的路径
     */
    protected Path createPath(ArrayList<Integer> nodeIndices) {
        double cost = 0;
        for (int i = 0; i < nodeIndices.size() - 1; i++) {
            cost += vrptwIns.getDistanceBetween(nodeIndices.get(i), nodeIndices.get(i + 1));
        }
        
        return new Path(nodeIndices, cost, vrptwIns.getNodeNum());
    }
    
    public Path getShortestPath() {
        return shortestPath;
    }
    
    public double getReducedCost() {
        return reducedCost;
    }
    
}

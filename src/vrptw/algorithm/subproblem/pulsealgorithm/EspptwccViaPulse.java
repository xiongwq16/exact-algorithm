package vrptw.algorithm.subproblem.pulsealgorithm;

import java.util.ArrayList;
import java.util.Map;

import vrptw.algorithm.subproblem.AbstractSubProblem;
import vrptw.problem.Vrptw;
import vrptw.solution.Path;

/**
 * Solve an ESPPTWCC via Pulse Algorithm.
 * 
 * @author Xiong Wangqi
 * @version V1.0
 * @since JDK1.8
 */
public class EspptwccViaPulse extends AbstractSubProblem {
    /**
     * Lower bound matrix, store the minimum reduced cost that can be achieved <br>
     * by any partial path that reaches node v[i] with a given amount of consumed resource.
     */
    ArrayList<Map<Integer, Double>> lbMatrix;
    
    // TODO 需要添加的变量（从初始化的情况去看）
    
    public EspptwccViaPulse(Vrptw vrptwIns, double[] lambda) {
        super(vrptwIns, lambda);
        // TODO Auto-generated constructor stub
        
    }

    /**
     * Solve an ESPPTWCC via Pulse algorithm.
     * Step 0: Initialization
     * Step 1: Bounding Scheme(containing the Pulse Procedures from all nodes)
     * Step 2: Pulse from startDepot
     */
    @Override
    public void solve() {
        // TODO Auto-generated method stub 需要完成最短路的节点序列设置，子问题的reduced cost 设置
        
    }
    
    // 需要输入有向图，也就是 vrptw 实例，这里也将其作为类变量，不直接输入
    // bound step，时间上下界限，军方在参数类的全局静态变量中
    // 需要输出 bound matrix，这里作为类变量，不设返回值
    private void boundingScheme() {
        
    }
    
    // TODO 是否为从 startDepot 出发的脉冲和从其他点出发的脉冲设置不同的 Method
    private void pulseProcedure(int currNodeIndex, double pathLoad, double pathTime, double pathReducedCost, Path path) {
        // Check Feasibility
        
        // Pulse through the outgoing arcs of current node to the neighborhoods
        
    }
    
    // TODO 需要的函数有哪些，输入输出分别是什么
    /**
     * Description.
     * Note that these lower bounds solely focus on the time resource consumption 
     * and consider a (relaxed) initial capacity consumption of zero 
     * for the partial path P that reaches node v i ∈ N.
     * @return
     */
    private boolean isFeasible(int currNodeIndex, double pathLoad, double pathTime) {
        return true;
    }
    
    /**
     * The function checkBounds will prune a partial path if r(Path) + lbMatrix(v[i], t(Path)) >= UB(r). <br>
     * 
     * @param currNodeIndex
     * @param pathTime
     * @param pathReducedCost
     * @return
     */
    private boolean checkBounds(int currNodeIndex, double pathTime, double pathReducedCost) {
        return true;
    }
    
    private boolean rollBack(int currNodeIndex, double pathTime, double pathReducedCost, Path path) {
        // TODO Path 中包含各个节点的访问次数信息，后面可能用得上
        return true;
    }
    
    // 如何记录脉冲过程
    
    /**
     * 脉冲线程类.
     * 
     * @author Xiong Wangqi
     * @version V1.0
     * @since JDK1.8
     */
    private class PulseThread implements Runnable {
        /** 脉冲出发节点的索引. */
        private int startNodeIndex;
        /** 脉冲出发节点的 Partial Path 对应的路径总需求量. */
        private double pathLoad;
        /** 脉冲出发节点的 Partial Path 对应的路径耗时. */
        private double pathTime;
        /** 脉冲出发节点的 Partial Path 对应的 reduced cost. */
        private double pathReducedCost;
        /** 脉冲出发节点的 Partial Path 对应的节点访问序列. */
        private ArrayList<Integer> nodeIndices;
        
        
        /**
         * 创建脉冲线程.
         * 
         * @param startNodeIndex 发出脉冲的节点索引
         * @param pathLoad 脉冲出发节点的 Partial Path 对应的路径总需求量
         * @param pathTime 脉冲出发节点的 Partial Path 对应的路径耗时
         * @param pathReducedCost 脉冲出发节点的 Partial Path 对应的 reduced cost
         * @param nodeIndices 脉冲出发节点的 Partial Path 对应的节点访问序列
         */
        PulseThread(int startNodeIndex, double pathLoad, double pathTime, 
                double pathReducedCost, ArrayList<Integer> nodeIndices) {
            this.startNodeIndex = startNodeIndex;
            this.pathLoad = pathLoad;
            this.pathTime = pathTime;
            this.pathReducedCost = pathReducedCost;
            this.nodeIndices = nodeIndices;
        }
        
        @Override
        public void run() {
            // TODO Auto-generated method stub
            
        }
        
    }
    
}

package fctp;

import ilog.concert.IloException;
import ilog.cplex.IloCplex.CplexStatus;

/**
 * FCTP's Manual Benders Decomposition Algorithm.
 * 
 * @author Xiong Wangqi
 * @version V1.0
 * @since JDK1.8
 */
class ManualBenders {
    private final Fctp fctpIns;
    private FctpMasterProblem masterProblem;
    
    ManualBenders(int warehouseNum, int customerNum, 
            double meanCapMulti, double meanFixedCost, int seed) throws IloException {

        fctpIns = new Fctp(warehouseNum, customerNum, meanCapMulti, meanFixedCost, seed);
        
        // Create the master MIP
        masterProblem = new FctpMasterProblem(fctpIns);
    }

    /**
     * 求解 FCTP 并输出解.
     */
    void solve() throws IloException {
        // 新建 BendersCallback 通用回调对象，并添加到主问题对应的求解器中
        int numThreads = masterProblem.getNumCores();
        BendersCallback callback = new BendersCallback(fctpIns, masterProblem, numThreads);
        
        // 将自定义的基于通用回调的 BenderCallback 添加到主问题的求解器中
        masterProblem.attach(callback);
        
        /* 
         * 求解主问题
         * Cplex 会在求解过程中调用 BendersCallback
         * 用于添加割平面的子问题在 BendersCallback 的 invoke 方法中求解
         */
        FctpSolution fctpSol = masterProblem.solve();
        
        if (fctpSol.getStatus() == CplexStatus.Optimal
                || fctpSol.getStatus() == CplexStatus.Feasible) {
            // 使用 BendersCallback 中的方法添加 flows 信息
            callback.addFlows(fctpSol);
        }
        
        fctpSol.output();
    }
}

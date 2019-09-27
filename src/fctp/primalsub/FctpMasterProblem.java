package fctp.primalsub;

import ilog.concert.IloException;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;

import java.util.ArrayList;

import fctp.Fctp;
import fctp.FctpSolution;

/**
 * UFLP 主问题.
 * 
 * @author Xiong Wangqi
 * @version V1.0
 * @since JDK1.8
 */
class FctpMasterProblem {
    /** The cutoff for rounding values of binary variables up. */
    static final double ROUNDUP = 0.5;

    private final int warehouseNum;
    private final int customerNum;

    private final IloCplex masterSolver;

    /** open[j] = 1 表示仓库 j 开始，否则不开设. */
    private final IloNumVar[] open;

    /** 子问题的最优值 - 流量成本，也是“最优割”的右侧项. */
    private final IloNumVar estFlowCost;

    FctpMasterProblem(Fctp fctpIns) throws IloException {
        warehouseNum = fctpIns.getWarehouseNum();
        customerNum = fctpIns.getCustomerNum();

        // 主问题初始化
        masterSolver = new IloCplex();
        open = new IloNumVar[warehouseNum];
        for (int j = 0; j < warehouseNum; j++) {
            open[j] = masterSolver.boolVar("open_" + j);
        }
        estFlowCost = masterSolver.numVar(0.0, Double.MAX_VALUE, "estFlowCost");

        double[] fixedCost = fctpIns.getFixedCost();
        masterSolver.addMinimize(masterSolver.sum(
                estFlowCost, masterSolver.scalProd(fixedCost, open)), "totalCost");
        
        // Suppress master problem output by default.
        masterSolver.setOut(null);
    }

    /**
     * Solve the MasterProblem.
     * 
     * @return the solution whose "flows" should be set based on the subProblem
     */
    FctpSolution solve() throws IloException {
        long start = System.currentTimeMillis();
        
        boolean isSolved = masterSolver.solve();

        FctpSolution fctpSol = new FctpSolution(warehouseNum, customerNum);
        fctpSol.setStatus(masterSolver.getCplexStatus());
        fctpSol.setSolveTime(System.currentTimeMillis() - start);
        
        if (isSolved) {
            fctpSol.setTotalCost(masterSolver.getObjValue());

            ArrayList<Integer> openWarehouses = new ArrayList<>();
            double[] warehouseOpenValue = masterSolver.getValues(open);
            for (int j = 0; j < warehouseNum; j++) {
                if (warehouseOpenValue[j] > ROUNDUP) {
                    openWarehouses.add(j);
                }
            }
            fctpSol.setOpenWarehouses(openWarehouses);
        }
        masterSolver.end();

        return fctpSol;
    }
    
    IloCplex getMasterSolver() {
        return masterSolver;
    }
    
    /**
     * 获取核心数量（用户 BendersCallback 定义线程数）.
     * 
     * @return the number of logical cores.
     */
    int getNumCores() throws IloException {
        return masterSolver.getNumCores();
    }
    
    /*
     * 由于 masterSolver 被封装在 masterProblem 对象中，
     * 因此在 Callback 中对masterSolver 的操作需要借助“类方法”实现。
     * 根据 Benders 分解中 MasterProblem 与 SubProblem 的关系，需要实现以下几个方法：
     * 1. 主问题中的仓库开关变量 - 用于“最优割”以及“可行割”的生成
     * 2. 主问题中的流量成本变量 - 用于“最优割”的生成
     * 将上述变量作为自定义的 BensersCallback 的属性，同时可以结合回调上下文的获取变量值
     */
    IloNumVar[] getOpenVar() {
        return open;
    }
    
    IloNumVar getEstFlowCostVar() {
        return estFlowCost;
    }
}

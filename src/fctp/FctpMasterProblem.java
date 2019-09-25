package fctp;

import ilog.concert.IloException;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;

import java.util.ArrayList;

/**
 * UFLP 主问题.
 * 
 * @author Xiong Wangqi
 * @version V1.0
 * @since JDK1.8
 */
class FctpMasterProblem {
    /** The cutoff for rounding values of binary variables up. */
    private static final double ROUNDUP = 0.5;

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
    
    /**
     * 获取核心数量（用户 BendersCallback 定义线程数）.
     * 
     * @return the number of logical cores.
     */
    int getNumCores() throws IloException {
        return masterSolver.getNumCores();
    }
    
    /**
     * 添加自定义的 BenderCallback 到主问题的求解器中.
     * 
     * @param callback 自定义的基于通用回调的 BenderCallback
     */
    void attach(BendersCallback callback) throws IloException {
        long contextmask = IloCplex.Callback.Context.Id.Candidate 
                | IloCplex.Callback.Context.Id.ThreadUp
                | IloCplex.Callback.Context.Id.ThreadDown;
        masterSolver.use(callback, contextmask);
    }
    
    /*
     * 由于 masterSolver 被封装在 masterProblem 对象中，
     * 因此在 Callback 中对masterSolver 的操作需要借助“类方法”实现。
     * 根据 Benders 分解中 MasterProblem 与 SubProblem 的关系，需要实现以下几个方法：
     * 1. 主问题中的流量成本值 - 用于判断
     * 2. 主问题中的流量成本变量 - 用于生成“最优割”
     * 3. 主问题中的仓库开关值 - 用于更新子问题的目标函数
     * 4. 主问题中的仓库开关变量 - 用于生成“可行割”
     * 方法实现见下方代码。
     */
    
    /**
     * 获取流量成本 - “最优割”的右侧项 {@link #estFlowCost}.
     * 
     * @param context 通用回调的上下文 
     * @return 流量成本
     */
    double getFlowCost(final IloCplex.Callback.Context context) throws IloException {
        return context.getCandidatePoint(estFlowCost);
    }
    
    /**
     * 获取仓库开设信息 - 被“固定”的变量值.
     * 
     * @param context 通用回调的上下文 
     * @return 仓库开设信息 {@link #open}
     * @throws IloException
     */
    double[] getOpenValus(final IloCplex.Callback.Context context) throws IloException {
        return context.getCandidatePoint(open);
    }
    
    public IloNumVar[] getOpenVar() {
        return open;
    }
    
    public IloNumVar getEstFlowCostVar() {
        return estFlowCost;
    }

}

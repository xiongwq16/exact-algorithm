package fctp;

import ilog.concert.IloException;
import ilog.concert.IloNumVar;
import ilog.concert.IloRange;
import ilog.cplex.IloCplex;
import ilog.cplex.IloCplex.CplexStatus;
import ilog.cplex.IloCplex.UnknownObjectException;

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
    /** 主问题中的流量成本变量. */
    private final IloNumVar[] open;
    private final IloNumVar estFlowCost;
    
    /*
     * 为了避免再次求解子问题来获取流量信息 flows[j][k]，
     * 这里根据求解过程中的主问题的当前最优目标值的更新，来保存对应的流量信息。
     */
    /** MasterProblem objective value. */
    private double incumbentValue;
    /** subProblem flow values. */
    private double[][] flowValues;
    
    ManualBenders(int warehouseNum, int customerNum, 
            double meanCapMulti, double meanFixedCost, int seed) throws IloException {
        // 算例
        fctpIns = new Fctp(warehouseNum, customerNum, meanCapMulti, meanFixedCost, seed);        
        
        // 主问题相关信息
        masterProblem = new FctpMasterProblem(fctpIns);
        this.estFlowCost = masterProblem.getEstFlowCostVar();
        this.open = masterProblem.getOpenVar();

        incumbentValue = Double.POSITIVE_INFINITY;
        flowValues = new double[warehouseNum][customerNum];
        
    }

    /**
     * 求解 FCTP 并输出解.
     */
    void solve() throws IloException {
        // 新建 BendersCallback 通用回调对象，并添加到主问题对应的求解器中
        int threadNum = masterProblem.getNumCores();
        BendersCallback callback = new BendersCallback(threadNum);
        long contextmask = IloCplex.Callback.Context.Id.Candidate 
                | IloCplex.Callback.Context.Id.ThreadUp
                | IloCplex.Callback.Context.Id.ThreadDown;
        masterProblem.getMasterSolver().use(callback, contextmask);
        
        /* 
         * 求解主问题
         * Cplex 会在求解过程中调用 BendersCallback
         * 用于添加割平面的子问题在 BendersCallback 的 invoke 方法中求解
         */
        FctpSolution fctpSol = masterProblem.solve();
        
        if (fctpSol.getStatus() == CplexStatus.Optimal
                || fctpSol.getStatus() == CplexStatus.Feasible) {
            // 使用 BendersCallback 中的方法添加 flows 信息
            callback.addFlowInfo(fctpSol);
        }
        
        fctpSol.output();
    }
    
    private class BendersCallback implements IloCplex.Callback.Function {
        private final FctpSubProblem[] subProblems;
        
        BendersCallback(int threadNum) {
            this.subProblems = new FctpSubProblem[threadNum];
        }
        
        /**
         * Benders Decomposition Algorithm 核心步骤.
         * 
         * @param context 主问题的回调上下文
         * @see ilog.cplex.IloCplex.Callback.Function#invoke(ilog.cplex.IloCplex.Callback.Context)
         */
        @Override
        public void invoke(IloCplex.Callback.Context context) throws IloException {
            // 获取 ThreadId
            int threadNo = context.getIntInfo(IloCplex.Callback.Context.Info.ThreadId);
            
            // setup
            if (context.inThreadUp()) {
                System.out.printf("\n>>> subProblem %d set up.\n", threadNo);
                subProblems[threadNo] = new FctpSubProblem(fctpIns, masterProblem);
                return;
            }
            
            // teardown
            if (context.inThreadDown()) {
                // Clear the subproblem
                System.out.printf("\n>>> subProblem %d tear down.\n", threadNo);
                subProblems[threadNo].end();
                subProblems[threadNo] = null;
                return;
            }
            
            // 获取当前解
            if (context.inCandidate()) {
                /*
                 * 获取主问题的解
                 * 注意 BendersCallback 中的 masterProblem 是正在使用 invoke 的主问题模型的引用 
                 * 可直接调用主问题的求解器的信息
                 */
                double[] openValues = context.getCandidatePoint(open);
                
                // 获取当前的线程对应的子问题，并求解
                FctpSubProblem subProblem = subProblems[threadNo];
                IloCplex.Status status = subProblem.solve(openValues, fctpIns.getCapacity());
                
                // 对偶问题无界
                if (status == IloCplex.Status.Unbounded) {
                    System.out.print("\n>>> Adding feasibility cut.\n");
                    
                    // 构造可行割
                    IloRange feasibilityCut =  subProblem.createFeasibilityCut();
                    context.rejectCandidate(feasibilityCut);
                    return;
                }
                // 对偶问题最优
                if (status == IloCplex.Status.Optimal) {
                    double zmaster = context.getCandidatePoint(estFlowCost);
                    System.out.println(zmaster);
                    if (zmaster < subProblem.getObjValue() - FctpSolution.EPS) {
                        System.out.print("\n>>> Adding optimality cut.\n");
                        // 构造最优割
                        IloRange optimalityCut = subProblem.createOptimalityCut(estFlowCost);
                        context.rejectCandidate(optimalityCut);
                    } else {
                        double masterObjValue = context.getCandidateObjective();
                        System.out.println("\n>>> Finding new solution with value "
                                + masterObjValue + "\n");
                        storeFlows(subProblem, masterObjValue);
                    }
                    return;
                }
                
            } else {
                throw new IloException("Callback was called from an invalid context: "
                        + context.getId() + ".\n");
            }
            
        }
        
        /**
         * 为求解主问题得到的解添加流量信息 flows.
         * 
         * @param fctpSol FCTP 的解
         */
        void addFlowInfo(FctpSolution fctpSol) {
            for (int k = 0; k < fctpIns.getCustomerNum(); k++) {
                for (int j = 0; j < fctpIns.getWarehouseNum(); j++) {
                    fctpSol.setFlowBetween(j, k, flowValues[j][k]);
                }
                
            }
            
        }
        
        /**
         * 当主问题找到了更好的解时，存储对应的流量信息 flows.
         *
         * @param subProblem 当前处理的子问题 
         * @param masterObjValue 新的历史最优值
         * @throws IloException 
         * @throws UnknownObjectException 
         */
        private synchronized void storeFlows(FctpSubProblem subProblem, double masterObjValue) 
                throws UnknownObjectException, IloException {
            if (masterObjValue < incumbentValue) {
                incumbentValue = masterObjValue;
                for (int k = 0; k < fctpIns.getCustomerNum(); k++) {
                    for (int j = 0; j < fctpIns.getWarehouseNum(); j++) {
                        flowValues[j][k] = subProblem.getFlowsBetween(j, k);
                    }
                }
            }
        }
        
    }
}

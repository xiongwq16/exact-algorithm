package fctp;

import ilog.concert.IloException;
import ilog.concert.IloRange;
import ilog.cplex.IloCplex;

class BendersCallback implements IloCplex.Callback.Function {
    private final int warehouseNum;
    private final int customerNum;
    /** 子问题的初始化需要反复调用 fctpIns，因此直接作为类属性. */
    private final Fctp fctpIns;
    
    private FctpMasterProblem masterProblem;
    private FctpSubProblem[] subProblems;
    
    /*
     * 为了避免再次求解子问题来获取流量信息 flows[j][k]，
     * 这里根据求解过程中的主问题的当前最优目标值的更新，来保存对应的流量信息。
     * 需要注意的需要使用线程安全的方式进行处理。
     */
    /** MasterProblem objective value. */
    private double incumbentValue;
    /** subProblem flow values. */
    private double[][] flowValues;
    
    BendersCallback(Fctp fctpIns, FctpMasterProblem masterProblem, int threadNum) {
        warehouseNum = fctpIns.getWarehouseNum();
        customerNum = fctpIns.getCustomerNum();
        
        this.fctpIns = fctpIns;
        this.masterProblem = masterProblem;
        this.subProblems = new FctpSubProblem[threadNum];
        
        incumbentValue = Double.POSITIVE_INFINITY;
        flowValues = new double[warehouseNum][customerNum];
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
            subProblems[threadNo] = new FctpSubProblem(fctpIns);
            System.out.printf(">>> subProblem %d set up.\n", threadNo);
            return;
        }
        
        // teardown
        if (context.inThreadDown()) {
            // Clear the subproblem
            subProblems[threadNo] = null;
            subProblems[threadNo].end();
            System.out.printf(">>> subProblem %d tear down.\n", threadNo);
            return;
        }
        
        // 获取当前解
        if (context.inCandidate()) {
            /*
             * 获取主问题的解
             * 注意 BendersCallback 中的 masterProblem 是正在使用 invoke 的主问题模型的引用 
             * 可直接调用主问题的求解器的信息
             */
            double[] openValues = masterProblem.getOpenValus(context);
            
            // 获取当前的线程对应的子问题，并求解
            FctpSubProblem subProblem = subProblems[threadNo];
            IloCplex.Status status = subProblem.solve(openValues);
            
            // 对偶问题无界，原问题无解
            if (status == IloCplex.Status.Unbounded) {
                // 添加可行割
                IloRange feasibilityCut =  subProblem.getFeasibilityCut(masterProblem.getOpenVar());
                context.rejectCandidate(feasibilityCut);
                
                System.out.print(">>> Adding feasibility cut.\n");
            }
            
            if (status == IloCplex.Status.Optimal) {
                double estFlowCost = masterProblem.getFlowCost(context);
                if (estFlowCost < subProblem.getObjValue() - FctpSolution.EPS) {
                    // 添加最优割
                    IloRange optimalityCut = subProblem.getOptimalityCut(
                            masterProblem.getEstFlowCostVar(), masterProblem.getOpenVar());
                    context.rejectCandidate(optimalityCut);
                    
                    System.out.print(">>> Adding optimality cut.\n");
                    
                } else {
                    storeFlows(subProblem, context.getCandidateObjective());
                }
                
            }
            return;
        } else {
            System.err.println("Callback was called from an invalid context: "
                    + context.getId() + ".\n");
        }
        
    }
    
    /**
     * 为求解主问题得到的解添加流量信息 flows.
     * 
     * @param fctpSol FCTP 的解
     */
    void addFlows(FctpSolution fctpSol) {
        for (int k = 0; k < customerNum; k++) {
            for (int j = 0; j < warehouseNum; j++) {
                fctpSol.setFlowBetween(j, k, flowValues[j][k]);
            }
            
        }
        
    }
    
    /**
     * 当主问题找到了更好的解时，存储对应的流量信息 flows.
     *
     * @param subProblem 当前进程处理的子问题 
     * @param masterObjValue 新的历史最优值
     */
    private synchronized void storeFlows(FctpSubProblem subProblem, double masterObjValue) 
            throws IloException {
        if (masterObjValue < incumbentValue) {
            incumbentValue = masterObjValue;
            for (int k = 0; k < customerNum; k++) {
                for (int j = 0; j < warehouseNum; j++) {
                    flowValues[j][k] = subProblem.getFlowsBetween(j, k);
                }
            }
        }
    }
    
}

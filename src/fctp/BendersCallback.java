package fctp;

import ilog.concert.IloException;
import ilog.concert.IloNumVar;
import ilog.concert.IloRange;
import ilog.cplex.IloCplex;

class BendersCallback implements IloCplex.Callback.Function {
    private final int warehouseNum;
    private final int customerNum;
    private final double[] demand;
    private final double[] capacity;
    private final double[][] flowCost;
    
    private final FctpMasterProblem masterProblem;
    /** 主问题中的流量成本变量. */
    private final IloNumVar[] open;
    private final IloNumVar estFlowCost;
    private final FctpSubProblem[] subProblems;
    
    /*
     * 为了避免再次求解子问题来获取流量信息 flows[j][k]，
     * 这里根据求解过程中的主问题的当前最优目标值的更新，来保存对应的流量信息。
     */
    /** MasterProblem objective value. */
    private double incumbentValue;
    /** subProblem flow values. */
    private double[][] flowValues;
    
    BendersCallback(Fctp fctpIns, FctpMasterProblem masterProblem, int threadNum) {
        // 算例信息
        warehouseNum = fctpIns.getWarehouseNum();
        customerNum = fctpIns.getCustomerNum();
        demand = fctpIns.getDemand();
        capacity = fctpIns.getCapacity();
        flowCost = fctpIns.getFlowCost();
        
        // 主问题相关信息
        this.masterProblem = masterProblem;
        this.estFlowCost = masterProblem.getEstFlowCostVar();
        this.open = masterProblem.getOpenVar();
        
        // 子问题
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
            System.out.printf("\n>>> subProblem %d set up.\n", threadNo);
            subProblems[threadNo] = new FctpSubProblem(warehouseNum, customerNum, flowCost);
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
            double[] openValues = masterProblem.getOpenValus(context);
            
            // 获取当前的线程对应的子问题，并求解
            FctpSubProblem subProblem = subProblems[threadNo];
            IloCplex.Status status = subProblem.solve(openValues, demand, capacity);
            
            // 对偶问题无界
            if (status == IloCplex.Status.Unbounded) {
                System.out.print("\n>>> Adding feasibility cut.\n");
                
                // 构造可行割
                IloRange feasibilityCut =  subProblem.createFeasibilityCut(open, demand, capacity);
                context.rejectCandidate(feasibilityCut);
                return;
            }
            // 对偶问题最优
            if (status == IloCplex.Status.Optimal) {
                double zmaster = masterProblem.getFlowCost(context);
                if (zmaster < subProblem.getObjValue() - FctpSolution.EPS) {
                    // 添加最优割
                    System.out.print("\n>>> Adding optimality cut.\n");
                    IloRange optimalityCut = subProblem.createOptimalityCut(
                            estFlowCost, open, demand, capacity);
                    context.rejectCandidate(optimalityCut);
                } else {
                    System.out.println("\n>>> Accepting new incumbent with value "
                            + context.getCandidateObjective() + "\n");
                    storeFlows(subProblem, context.getCandidateObjective());
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
     * @param subProblem 当前处理的子问题 
     * @param masterObjValue 新的历史最优值
     */
    private void storeFlows(FctpSubProblem subProblem, double masterObjValue) 
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

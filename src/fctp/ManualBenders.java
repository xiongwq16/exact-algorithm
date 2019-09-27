package fctp;

import fctp.Fctp;
import fctp.FctpMasterProblem;
import fctp.FctpSolution;
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
    
    /** MasterProblem incumbent value. */
    private double incumbentValue;
    /** store subProblem flow values. */
    private double[][] flowValues;
    
    /** 子问题是否采用对偶形式. */
    private final boolean isSubProblemDual;

    ManualBenders(boolean isSubProblemDual, int warehouseNum, int customerNum, 
            double meanCapMulti, double meanFixedCost, int seed) throws IloException {
        this.isSubProblemDual = isSubProblemDual;
        
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
        
        // 求解主问题，Cplex 会基于在 BendersCallback 的 invoke 中子问题的求解结果添加割平面
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
                if (isSubProblemDual) {
                    subProblems[threadNo] = new FctpSubProblemDual(fctpIns, masterProblem);
                } else {
                    subProblems[threadNo] = new FctpSubProblemPrimal(fctpIns, masterProblem);
                }
                return;
            }

            // teardown
            if (context.inThreadDown()) {
                // Clear the subproblem
                System.out.printf("\n>>> subProblem %d tear down.\n", threadNo);
                subProblems[threadNo] = null;
                subProblems[threadNo].end();
                return;
            }

            // 获取当前解
            if (context.inCandidate()) {
                // 获取主问题中的仓库开设决策变量的取值
                double[] openValues = context.getCandidatePoint(open);                
                // 获取当前的线程对应的子问题，并求解
                FctpSubProblem subProblem = subProblems[threadNo];
                IloCplex.Status status = subProblem.solve(openValues, fctpIns.getCapacity());

                // 原问题无解或对偶问题无界
                if ((isSubProblemDual && status == IloCplex.Status.Unbounded) 
                        || (!isSubProblemDual && status == IloCplex.Status.Infeasible)) {
                    
                    System.out.print("\n>>> Adding feasibility cut.\n");
                    
                    // 构造可行割
                    IloRange feasibilityCut = subProblem.createFeasibilityCut();
                    context.rejectCandidate(feasibilityCut);
                    return;
                }
                
                // 原问题或对偶问题最优
                if (status == IloCplex.Status.Optimal) {
                    double zStar = context.getCandidatePoint(estFlowCost);
                    if (zStar < subProblem.getObjValue() - FctpSolution.EPS) {
                        System.out.print("\n>>> Adding optimality cut.\n");
                        // 构造最优割
                        IloRange optimalityCut = subProblem.createOptimalityCut(estFlowCost);
                        context.rejectCandidate(optimalityCut);
                        
                    } else {
                        double masterObj = context.getCandidateObjective();
                        System.out.print("\n>>> Finding solution with cost " + masterObj + "\n");
                        storeFlows(subProblem, masterObj);
                    }
                    return;
                }

            } else {
                throw new IloException(
                        "Callback from an invalid context: " + context.getId() + ".\n");
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
         * @param subProblem     当前处理的子问题
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
                        flowValues[j][k] = subProblem.getFlowBetween(j, k);
                    }
                }
                
            }
        }

    }
}

package fctp;

import ilog.concert.IloException;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloLinearNumExprIterator;
import ilog.concert.IloNumExpr;
import ilog.concert.IloNumVar;
import ilog.concert.IloRange;
import ilog.cplex.IloCplex;
import ilog.cplex.IloCplex.UnknownObjectException;

import java.util.HashMap;

/**
 * FCTP 子问题（对偶形式）.
 * 
 * @author Xiong Wangqi
 * @version V1.0
 * @since JDK1.8
 */
class FctpSubProblem {
    private final int warehouseNum;
    private final int customerNum;
    private final double[] capacity;
    private final double[] demand;
    
    private final IloCplex dualLpSolver;
    
    /**
     * 需求量约束的对偶变量，名称记为 u[k]， <br>
     * 需求量约束：for k in customers: sum(flow[j][k], for j in warehouses) >= demand[k].
     */
    private final IloNumVar[] u;
    /**
     * 容量约束的对偶变量名，名称记为 v[j]， <br>
     * 容量约束：for j in warehouses: -sum(flow[j][k], for k in customers) >= -capacity[j] * open[j].
     */
    private final IloNumVar[] v;
    
    /** 
     * 确定极射线的值时需要查询对应变量的位置，使用 HashMap 可提高查询效率 O(1)，<br>
     * Key：对偶形式的子问题的决策变量 u and v, <br>
     * Value：u 的 Value 为原索引号，v 的 Value 为 原索引号 + customerNum.
     */
    private final HashMap<IloNumVar, Integer> varMap;
    
    /** 对偶问题约束，constraints[j][k] 对应的对偶变量即为原问题中的 仓库 j 供应给客户 k 的货量. */
    private IloRange[][] constraints;
    
    FctpSubProblem(Fctp fctpIns) throws IloException {
        warehouseNum = fctpIns.getWarehouseNum();
        customerNum = fctpIns.getCustomerNum();
        capacity = fctpIns.getCapacity();
        demand = fctpIns.getDemand();
        double[][] flowCost = fctpIns.getFlowCost();
        
        dualLpSolver = new IloCplex();
        // Create dual variables and set the objective
        u = new IloNumVar[customerNum];
        v = new IloNumVar[warehouseNum];
        varMap = new HashMap<>(customerNum + warehouseNum);
        IloLinearNumExpr expr = dualLpSolver.linearNumExpr();
        for (int k = 0; k < customerNum; k++) {
            u[k] = dualLpSolver.numVar(0.0, Double.MAX_VALUE, "u_" + k);
            varMap.put(u[k], k);
            expr.addTerm(demand[k], u[k]);
        }
        for (int j = 0; j < warehouseNum; j++) {
            v[j] = dualLpSolver.numVar(0.0, Double.MAX_VALUE, "v_" + j);
            varMap.put(v[j], customerNum + j);
            // 初始 open[j] 全部为0，因此初始目标函数中不添加这部分
        }
        // Minimize the total flow cost
        dualLpSolver.addMaximize(expr);
        
        // Dual Constrains
        constraints = new IloRange[warehouseNum][customerNum];
        for (int j = 0; j < warehouseNum; j++) {
            for (int k = 0; k < customerNum; k++) {
                // u[k] - v[j] >= flowCost[j][k]
                constraints[j][k] = dualLpSolver.addLe(
                        dualLpSolver.diff(u[k], v[j]), flowCost[j][k]);
            }
        }

        /*
         * Turn off the "presolve" reductions
         * if the presolver recognizes that the subproblem is infeasible, we do not get a dual ray
         */
        dualLpSolver.setParam(IloCplex.Param.Preprocessing.Reduce, 0);
        // Solve the subProblem(Dual Type) with dual simplex method
        dualLpSolver.setParam(IloCplex.Param.RootAlgorithm, IloCplex.Algorithm.Dual);
        
        // Suppress master and subproblem output by default.
        dualLpSolver.setOut(null);
    }

    /**
     * Releases all Cplex objects attached to the SubProblem.
     */
    void end() {
        dualLpSolver.end();
    }
    
    /**
     * 求解的是子问题的对偶问题，需要通过获取约束对偶变量的方式获取流量信息. <br>
     * @return
     */
    double getFlowsBetween(int warehouseIndex, int customerIndex) 
            throws UnknownObjectException, IloException {
        return dualLpSolver.getDual(constraints[warehouseIndex][customerIndex]);
    }
    
    /*
     * 由于 dualLpSolver 被封装在 subProblem 对象中，
     * 因此外部变量想要对 dualLpSolver 对应的对偶形式的子问题模型进行更新需要借助“类方法”实现。
     * 根据 Benders 分解中 MasterProblem 与 SubProblem 的关系，需要实现以下几个方法：
     * 1. 基于主问题的解中的开设仓库信息 open，更新目标函数，并求解
     * 2. 添加“可行割”
     * 3. 添加“最优割”
     * 4. 获取子问题目标函数值
     * 方法实现见下方代码。
     */
    
    /**
     * 求解子问题，并返回解的状态（用于判定可行割/最优割）.
     * 
     * @param openValues 仓库开设信息
     * @return 子问题的解的状态
     */
    IloCplex.Status solve(double[] openValues) throws IloException {
        // 更新子问题的目标函数
        IloLinearNumExpr objExpr = dualLpSolver.linearNumExpr();
        for (int k = 0; k < customerNum; k++) {
            objExpr.addTerm(demand[k], u[k]);
        }
        for (int j = 0; j < warehouseNum; j++) {
            objExpr.addTerm(-capacity[j] * openValues[j], v[j]);
        }
        dualLpSolver.getObjective().setExpr(objExpr);
        
        dualLpSolver.solve();
        return dualLpSolver.getStatus();
    }
    
    IloRange getFeasibilityCut(IloNumVar[] open) throws IloException {
        IloLinearNumExpr ray = dualLpSolver.getRay();
        IloLinearNumExprIterator it = ray.linearIterator();
        
        double constant = 0;
        IloNumExpr expr = dualLpSolver.numExpr();
        
        while (it.hasNext()) {
            IloNumVar var = it.nextNumVar();
            double value = it.getValue();
            
            int index = varMap.get(var);
            // 极射线中的变量对应的系数
            
            if (index < customerNum) {
                constant += demand[index] * value;
            } else {
                index = index - customerNum;
                expr = dualLpSolver.sum(
                        expr, dualLpSolver.prod(-capacity[index] * value, open[index]));
            }
        }
        return dualLpSolver.le(dualLpSolver.sum(constant, expr), 0);
    }
    
    IloRange getOptimalityCut(IloNumVar estFlowCost, IloNumVar[] open) throws IloException {        
        double constant = 0;
        IloNumExpr expr = dualLpSolver.numExpr();
        for (int k = 0; k < customerNum; k++) {
            constant += demand[k] * dualLpSolver.getValue(u[k]);
        }
        for (int j = 0; j < warehouseNum; j++) {
            expr = dualLpSolver.sum(expr, 
                    dualLpSolver.prod(-capacity[j] * dualLpSolver.getValue(v[j]), open[j]));
        }
        return dualLpSolver.le(dualLpSolver.diff(
                dualLpSolver.sum(constant, expr), estFlowCost), 0);
    }
    
    double getObjValue() throws IloException {
        return dualLpSolver.getObjValue();
    }
}

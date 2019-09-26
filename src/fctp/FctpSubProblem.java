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
    
    private final IloCplex dualSolver;
    
    /**
     * 需求量约束的对偶变量，名称记为 demandDualVars[k]， <br>
     * 需求量约束：for k in customers: sum(flow[j][k], for j in warehouses) >= demand[k].
     */
    private final IloNumVar[] demandDualVars;
    /**
     * 容量约束的对偶变量名，名称记为 capacityDualVars[j]， <br>
     * 容量约束：for j in warehouses: -sum(flow[j][k], for k in customers) >= -capacity[j] * open[j].
     */
    private final IloNumVar[] capacityDualVars;
    
    /**
     * 确定极射线的值时需要查询对应变量的位置，使用 HashMap 可提高查询效率 O(1)，<br>
     * Key：对偶形式的子问题的决策变量 {@link #demandDualVars} and {@link #capacityDualVars}， <br>
     * Value：注意 {@link #capacityDualVars} 的 Value 为 原索引号 + customerNum.
     */
    private final HashMap<IloNumVar, Integer> varMap;
    
    /** 对偶问题约束，constraints[j][k] 对应的对偶变量即为原问题中的 仓库 j 供应给客户 k 的货量. */
    private IloRange[][] constraints;
    
    FctpSubProblem(int warehouseNum, int customerNum, double[][] flowCost) throws IloException {
        this.warehouseNum = warehouseNum;
        this.customerNum = customerNum;
                
        dualSolver = new IloCplex();
        // Create dual variables
        demandDualVars = new IloNumVar[customerNum];
        capacityDualVars = new IloNumVar[warehouseNum];
        varMap = new HashMap<>(customerNum + warehouseNum);
        for (int k = 0; k < customerNum; k++) {
            demandDualVars[k] = dualSolver.numVar(0.0, Double.MAX_VALUE, "u_" + k);
            varMap.put(demandDualVars[k], k);
        }
        for (int j = 0; j < warehouseNum; j++) {
            capacityDualVars[j] = dualSolver.numVar(0.0, Double.MAX_VALUE, "v_" + j);
            varMap.put(capacityDualVars[j], customerNum + j);
        }
        
        // Dual Constrains
        constraints = new IloRange[warehouseNum][customerNum];
        for (int j = 0; j < warehouseNum; j++) {
            for (int k = 0; k < customerNum; k++) {
                // u[k] - v[j] >= flowCost[j][k]
                constraints[j][k] = dualSolver.addLe(
                        dualSolver.diff(demandDualVars[k], capacityDualVars[j]), flowCost[j][k]);
            }
        }
        
        // Initial objective
        dualSolver.addMaximize();
        
        /*
         * Turn off the "presolve" reductions
         * if the presolver recognizes that the subproblem is infeasible, we do not get a dual ray
         */
        dualSolver.setParam(IloCplex.Param.Preprocessing.Reduce, 0);
        // Solve the subProblem(Dual Type) with dual simplex method
        dualSolver.setParam(IloCplex.Param.RootAlgorithm, IloCplex.Algorithm.Dual);
        
        dualSolver.setOut(null);
    }

    /**
     * Releases all Cplex objects attached to the SubProblem.
     */
    void end() {
        dualSolver.end();
    }
    
    /**
     * 求解的是子问题的对偶问题，需要通过获取约束对偶变量的方式获取流量信息. <br>
     * @return
     */
    double getFlowsBetween(int warehouseIndex, int customerIndex) 
            throws UnknownObjectException, IloException {
        return dualSolver.getDual(constraints[warehouseIndex][customerIndex]);
    }
    
    /*
     * 由于 dualLpSolver 被封装在 subProblem 对象中，
     * 因此外部变量想要对 dualLpSolver 对应的对偶形式的子问题模型进行更新需要借助“类方法”实现。
     * 根据 Benders 分解中 MasterProblem 与 SubProblem 的关系，需要实现以下几个方法：
     * 1. 基于主问题的解中的开设仓库信息，更新目标函数，并求解
     * 2. 生成“可行割”
     * 3. 生成“最优割”
     * 4. 获取子问题目标函数值
     * 方法实现见下方代码。
     */
    
    /**
     * 求解子问题，并返回解的状态（用于判定可行割/最优割）.
     * 
     * @param openValues 仓库开设信息
     * @param demand 客户需求
     * @param capacity 仓库容量
     * @return
     * @throws IloException
     */
    IloCplex.Status solve(double[] openValues, double[] demand, double[] capacity) throws IloException {
        // 根据仓库开设情况更新的容量
        double[] openCapacity = new double[warehouseNum];
        for (int j = 0; j < warehouseNum; j++) {
            openCapacity[j] = -openValues[j] * capacity[j];
        }
        
        // 更新子问题的目标函数
        IloLinearNumExpr objExpr = dualSolver.linearNumExpr();
        
        objExpr.addTerms(demand, demandDualVars);
        objExpr.addTerms(openCapacity, capacityDualVars);
        
        dualSolver.getObjective().setExpr(objExpr);
        
        dualSolver.solve();
        
        return dualSolver.getStatus();
    }
    
    IloRange createFeasibilityCut(IloNumVar[] open, 
            double[] demand, double[] capacity) throws IloException {
        IloLinearNumExpr ray = dualSolver.getRay();
        IloLinearNumExprIterator it = ray.linearIterator();
        
        double constant = 0;
        IloNumExpr expr = dualSolver.numExpr();
        while (it.hasNext()) {
            IloNumVar var = it.nextNumVar();
            double value = it.getValue();
            
            int index = varMap.get(var);
            if (index < customerNum) {
                constant += demand[index] * value;
            } else {
                index = index - customerNum;
                expr = dualSolver.sum(
                        expr, dualSolver.prod(-capacity[index] * value, open[index]));
            }
        }
        return dualSolver.le(dualSolver.sum(constant, expr), 0);
    }
    
    IloRange createOptimalityCut(IloNumVar estFlowCost, IloNumVar[] open, 
            double[] demand, double[] capacity) throws IloException {        
        double constant = 0;
        for (int k = 0; k < customerNum; k++) {
            constant += demand[k] * dualSolver.getValue(demandDualVars[k]);
        }
        IloLinearNumExpr expr = dualSolver.linearNumExpr(constant);
        
        for (int j = 0; j < warehouseNum; j++) {
            expr.addTerm(-capacity[j] * dualSolver.getValue(capacityDualVars[j]), open[j]);
        }
        
        return dualSolver.le(dualSolver.diff(expr, estFlowCost), 0);
    }
    
    double getObjValue() throws IloException {
        return dualSolver.getObjValue();
    }
    
}

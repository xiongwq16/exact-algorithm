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
    private static int warehouseNum;
    private static int customerNum;
    
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
    /** 记录目标函数系数，便于添加割平面. */
    private final HashMap<IloNumVar, IloLinearNumExpr> objCoeff;
    private static final double loaderFactor = 0.75;
    
    /** 对偶问题约束，constraints[j][k] 对应的对偶变量即为原问题中的 仓库 j 供应给客户 k 的货量. */
    private final IloRange[][] constraints;
    
    FctpSubProblem(Fctp fctpIns, FctpMasterProblem masterProblem) throws IloException {
        warehouseNum = fctpIns.getWarehouseNum();
        customerNum = fctpIns.getCustomerNum();
        double[] demand = fctpIns.getDemand();
        double[] capacity = fctpIns.getCapacity();
                
        dualSolver = new IloCplex();
        // Create dual variables
        demandDualVars = new IloNumVar[customerNum];
        capacityDualVars = new IloNumVar[warehouseNum];
        objCoeff = new HashMap<>((int)((customerNum + warehouseNum) / loaderFactor) + 1);
        for (int k = 0; k < customerNum; k++) {
            demandDualVars[k] = dualSolver.numVar(0.0, Double.MAX_VALUE, "u_" + k);
            
            // 保存需求量约束对应的对偶变量在子问题目标函数中的系数
            objCoeff.put(demandDualVars[k], dualSolver.linearNumExpr(demand[k]));
        }
        
        IloLinearNumExpr expr = dualSolver.linearNumExpr();
        for (int j = 0; j < warehouseNum; j++) {
            capacityDualVars[j] = dualSolver.numVar(0.0, Double.MAX_VALUE, "v_" + j);
            
            /*
             * 保存容量约束对应的对偶变量在子问题目标函数中的系数
             * 注意这里的右侧项存放的变量 -capacity[j] * open[j]
             */
            expr.clear();
            expr.addTerm(-capacity[j], masterProblem.getOpenVar()[j]);
            objCoeff.put(capacityDualVars[j], expr);
        }
        
        // Dual Constrains
        double[][] flowCost = fctpIns.getFlowCost();
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
     * @return 指定仓库和客户之间的流量
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
     * @return 解的状态
     */
    IloCplex.Status solve(double[] openValues, double[] capacity) throws IloException {
        IloNumExpr objExpr = dualSolver.numExpr();
        
        // 根据仓库开设情况更新的容量
        double[] openCapacity = new double[warehouseNum];
        for (int j = 0; j < warehouseNum; j++) {
            openCapacity[j] = 0;
            if (openValues[j] > FctpMasterProblem.ROUNDUP) {
                openCapacity[j] = -openValues[j] * capacity[j];
            }
            objExpr = dualSolver.sum(objExpr, dualSolver.prod(openCapacity[j], capacityDualVars[j]));
        }
        
        for (int k = 0; k < customerNum; k++) {
            objExpr = dualSolver.sum(objExpr, dualSolver.prod(objCoeff.get(demandDualVars[k]), demandDualVars[k]));
        }
        
        dualSolver.getObjective().setExpr(objExpr);
        
        dualSolver.solve();
        
        return dualSolver.getStatus();
    }
    
    IloRange createFeasibilityCut() throws IloException {
        IloLinearNumExpr ray = dualSolver.getRay();
        IloLinearNumExprIterator it = ray.linearIterator();
        
        IloNumExpr expr = dualSolver.numExpr();
        while (it.hasNext()) {
            IloNumVar var = it.nextNumVar();
            double value = it.getValue();
            expr = dualSolver.sum(expr, dualSolver.prod(value, objCoeff.get(var)));
        }
        return dualSolver.le(expr, 0);
    }
    
    IloRange createOptimalityCut(IloNumVar estFlowCost) throws IloException {        
        IloNumExpr expr = dualSolver.numExpr();
        
        double dualVal;
        for (int k = 0; k < customerNum; k++) {
            dualVal = dualSolver.getValue(demandDualVars[k]);
            expr = dualSolver.sum(expr, dualSolver.prod(objCoeff.get(demandDualVars[k]), dualVal));
        }
        
        for (int j = 0; j < warehouseNum; j++) {
            dualVal = dualSolver.getValue(capacityDualVars[j]);
            expr = dualSolver.sum(expr, dualSolver.prod(objCoeff.get(capacityDualVars[j]), dualVal));
        }
        
        return dualSolver.le(dualSolver.diff(expr, estFlowCost), 0);
    }
    
    double getObjValue() throws IloException {
        return dualSolver.getObjValue();
    }
    
}

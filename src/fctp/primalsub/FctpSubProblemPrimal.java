package fctp.primalsub;

import ilog.concert.IloConstraint;
import ilog.concert.IloException;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloLinearNumExprIterator;
import ilog.concert.IloNumExpr;
import ilog.concert.IloNumVar;
import ilog.concert.IloRange;
import ilog.cplex.IloCplex;
import ilog.cplex.IloCplex.UnknownObjectException;

import java.util.HashMap;

import fctp.Fctp;

/**
 * FCTP 子问题.
 * 
 * @author Xiong Wangqi
 * @version V1.0
 * @since JDK1.8
 */
class FctpSubProblem {
    private static int warehouseNum;
    private static int customerNum;
    
    private final IloCplex subSolver;
    /** flows[]j[k] 表示仓库 j 到 客户 k 的流量. */
    private final IloNumVar[][] flows;
    /** 客户需求量约束. */
    private final IloRange[] demandCstr;
    /** 仓库容量约束，关闭的仓库容量为 0. */
    private final IloRange[] capacityCstr;
    /** 约束右侧项. */
    private final HashMap<IloConstraint, IloLinearNumExpr> rhs;
    private static final double loaderFactor = 0.75;
    
    FctpSubProblem(Fctp fctpIns, FctpMasterProblem masterProblem) throws IloException {
        warehouseNum = fctpIns.getWarehouseNum();
        customerNum = fctpIns.getCustomerNum();
        double[] demand = fctpIns.getDemand();
        double[] capacity = fctpIns.getCapacity();
        double[][] flowCost = fctpIns.getFlowCost();
                
        subSolver = new IloCplex();
        // Create variables
        flows = new IloNumVar[warehouseNum][customerNum];
        IloLinearNumExpr expr = subSolver.linearNumExpr();
        for (int j = 0; j < warehouseNum; j++) {
            for (int k = 0; k < customerNum; k++) {
                flows[j][k] = subSolver.numVar(
                        0.0, Double.MAX_VALUE, "flow_" + j + "_" + k);
                expr.addTerm(flowCost[j][k], flows[j][k]);
            }
            
        }
        // Minimize total flow cost
        subSolver.addMinimize(expr, "flowCost");
        
        /* 
         * Demand constraints:
         * for k in customers: sum(flow[j][k], for j in warehouses) >= demand[k]
         */
        rhs = new HashMap<>((int)((warehouseNum + customerNum) / loaderFactor) + 1);
        demandCstr = new IloRange[customerNum];
        for (int k = 0; k < customerNum; k++) {
            expr.clear();
            for (int j = 0; j < warehouseNum; j++) {
                expr.addTerm(1.0, flows[j][k]);
            }
            demandCstr[k] = subSolver.addGe(expr, demand[k], "demand_" + k);
            rhs.put(demandCstr[k], subSolver.linearNumExpr(demand[k]));
        }
        
        /*
         * Capacity constraints:
         * sum(flow[j][k], for k in customers) <= capacity[j] * open[j]
         * Initially all zero, which makes the subproblem infeasible
         */
        capacityCstr = new IloRange[warehouseNum];
        for (int j = 0; j < warehouseNum; j++) {
            capacityCstr[j] = subSolver.addLe(
                    subSolver.sum(flows[j]), 0.0, "capacity_" + j);
            
            // 保存容量约束的右侧项，注意这里的右侧项存放的变量 open[j] * capacity[j]
            expr.clear();
            expr.addTerm(capacity[j], masterProblem.getOpenVar()[j]);
            rhs.put(capacityCstr[j], expr);
        }
        
        /*
         * Turn off the "presolve" reductions
         * if the presolver recognizes that the subproblem is infeasible, we do not get a dual ray
         */
        subSolver.setParam(IloCplex.Param.Preprocessing.Reduce, 0);
        // Solve the subProblem(Dual Type) with dual simplex method
        subSolver.setParam(IloCplex.Param.RootAlgorithm, IloCplex.Algorithm.Dual);
        
        subSolver.setOut(null);
    }
    
    double getFlowBetween(int warehouseIndex, int customerIndex) 
            throws UnknownObjectException, IloException {
        return subSolver.getValue(flows[warehouseIndex][customerIndex]);
    }

    /**
     * Releases all Cplex objects attached to the SubProblem.
     */
    void end() {
        subSolver.end();
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
        // 根据仓库开设情况更新的容量，并更新子问题的容量约束 - 右侧项
        for (int j = 0; j < warehouseNum; j++) {
            if (openValues[j] > FctpMasterProblem.ROUNDUP) {
                capacityCstr[j].setUB(capacity[j]);
            }
        }
        
        subSolver.solve();
        
        return subSolver.getStatus();
    }
    
    IloRange createFeasibilityCut() throws IloException {
        /*
         * dualFarkas 方法可获取对偶变量非零的子问题的约束及其对应的对偶变量，
         * 结果存放在输入的参数中，详细说明可参考：
         * https://orinanobworld.blogspot.com/2010/07/infeasible-lps-and-farkas-certificates.html
         */
        IloConstraint[] constraints = new IloConstraint[warehouseNum + customerNum];
        double[] dualVal = new double[warehouseNum + customerNum];
        subSolver.dualFarkas(constraints, dualVal);
        
        IloNumExpr expr = subSolver.numExpr();
        for (int i = 0; i < constraints.length; i++) {
            IloConstraint cstr = constraints[i];
            expr = subSolver.sum(expr, subSolver.prod(dualVal[i], rhs.get(cstr)));
        }
        
        return subSolver.le(expr, 0);
    }
    
    IloRange createOptimalityCut(IloNumVar estFlowCost) throws IloException {
        IloNumExpr expr = subSolver.numExpr();
        double[] demandDualVal = subSolver.getDuals(demandCstr);
        for (int k = 0; k < customerNum; k++) {
            expr = subSolver.sum(expr, subSolver.prod(demandDualVal[k], rhs.get(demandCstr[k])));
        }
        
        double[] capacityDualVal = subSolver.getDuals(capacityCstr);
        for (int j = 0; j < warehouseNum; j++) {
            expr = subSolver.sum(expr, subSolver.prod(capacityDualVal[j], rhs.get(capacityCstr[j])));
        }
        
        return subSolver.le(subSolver.diff(expr, estFlowCost), 0);
    }
    
    double getObjValue() throws IloException {
        return subSolver.getObjValue();
    }
    
}

package fctp;

import fctp.Fctp;
import fctp.FctpMasterProblem;

import ilog.concert.IloConstraint;
import ilog.concert.IloException;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloNumExpr;
import ilog.concert.IloNumVar;
import ilog.concert.IloRange;
import ilog.cplex.IloCplex;
import ilog.cplex.IloCplex.UnknownObjectException;

import java.util.HashMap;

/**
 * FCTP 子问题.
 * 
 * @author Xiong Wangqi
 * @version V1.0
 * @since JDK1.8
 */
class FctpSubProblemPrimal extends AbstractFctpSubProblem {
    /** flows[]j[k] 表示仓库 j 到 客户 k 的流量. */
    private IloNumVar[][] flows;
    /** 客户需求量约束. */
    private IloRange[] demandCstr;
    /** 仓库容量约束，关闭的仓库容量为 0. */
    private IloRange[] capacityCstr;
    /** 约束右侧项. */
    private HashMap<IloConstraint, IloLinearNumExpr> rhs;
    
    FctpSubProblemPrimal(Fctp fctpIns, FctpMasterProblem masterProblem) throws IloException {
        super(fctpIns);
        
        double[] demand = fctpIns.getDemand();
        double[] capacity = fctpIns.getCapacity();
        double[][] flowCost = fctpIns.getFlowCost();
        
        // 创建变量并设置最小化流量成本的目标函数
        flows = new IloNumVar[warehouseNum][customerNum];
        IloLinearNumExpr expr = subSolver.linearNumExpr();
        for (int j = 0; j < warehouseNum; j++) {
            for (int k = 0; k < customerNum; k++) {
                flows[j][k] = subSolver.numVar(
                        0.0, Double.MAX_VALUE, "flow_" + j + "_" + k);
                expr.addTerm(flowCost[j][k], flows[j][k]);
            }
            
        }
        subSolver.addMinimize(expr, "flowCost");
        
        /* 
         * 需求量约束:
         * for k in customers: sum(flow[j][k], for j in warehouses) >= demand[k]
         */
        rhs = new HashMap<>((int)((warehouseNum + customerNum) / LOADER_FACTOR) + 1);
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
         * 容量约束:
         * sum(flow[j][k], for k in customers) <= capacity[j] * open[j]
         * Initially all zero, which makes the subproblem infeasible
         */
        capacityCstr = new IloRange[warehouseNum];
        for (int j = 0; j < warehouseNum; j++) {
            capacityCstr[j] = subSolver.addLe(
                    subSolver.sum(flows[j]), 0.0, "capacity_" + j);
            
            // 保存容量约束的右侧项，注意这里的右侧项存放的变量 open[j] * capacity[j]
            IloLinearNumExpr rhsExpr = subSolver.linearNumExpr();
            rhsExpr.addTerm(capacity[j], masterProblem.getOpenVar()[j]);
            rhs.put(capacityCstr[j], rhsExpr);
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
    
    @Override
    double getFlowBetween(int warehouseIndex, int customerIndex) 
            throws UnknownObjectException, IloException {
        return subSolver.getValue(flows[warehouseIndex][customerIndex]);
    }
    
    @Override
    IloCplex.Status solve(double[] openValues, double[] capacity) throws IloException {
        // 根据仓库开设情况更新的容量，并更新子问题的容量约束 - 右侧项
        for (int j = 0; j < warehouseNum; j++) {
            if (openValues[j] >= FctpMasterProblem.ROUNDUP) {
                capacityCstr[j].setUB(capacity[j]);
            } else {
                capacityCstr[j].setUB(0);
            }
        }
        
        subSolver.solve();
                
        return subSolver.getStatus();
    }
    
    @Override
    IloRange createFeasibilityCut() throws IloException {
        /*
         * dualFarkas 方法可获取对偶变量非零的子问题的约束及其对应的对偶变量，
         * 两者的结果分别存放在第一、二个参数中，可用于获取对偶问题的极射线的值，详细说明可参考：
         * https://orinanobworld.blogspot.com/2010/07/infeasible-lps-and-farkas-certificates.html
         */
        IloConstraint[] constraints = new IloConstraint[warehouseNum + customerNum];
        double[] dualVal = new double[warehouseNum + customerNum];
        subSolver.dualFarkas(constraints, dualVal);
        
        // 约束的右侧项 * 对应的极射线的值
        IloNumExpr expr = subSolver.numExpr();
        for (int i = 0; i < constraints.length; i++) {
            IloConstraint cstr = constraints[i];
            expr = subSolver.sum(expr, subSolver.prod(dualVal[i], rhs.get(cstr)));
        }
        
        // 返回对偶问题的极射线对应的“可行割”：u^r(d - Gx) <= 0
        return subSolver.le(expr, 0);
    }
    
    @Override
    IloRange createOptimalityCut(IloNumVar estFlowCost) throws IloException {
        IloNumExpr expr = subSolver.numExpr();
        double[] demandDualVal = subSolver.getDuals(demandCstr);
        // 需求量约束的右侧项 * 对偶变量值
        for (int k = 0; k < customerNum; k++) {
            expr = subSolver.sum(expr, subSolver.prod(demandDualVal[k], rhs.get(demandCstr[k])));
        }
        
        // 容量约束的右侧项 * 对偶变量值
        double[] capacityDualVal = subSolver.getDuals(capacityCstr);
        for (int j = 0; j < warehouseNum; j++) {
            expr = subSolver.sum(expr, 
                    subSolver.prod(capacityDualVal[j], rhs.get(capacityCstr[j])));
        }
        
        // 返回对偶问题的极点对应的“最优割”：u^t(d - Gx) <= sigma
        return subSolver.le(subSolver.diff(expr, estFlowCost), 0);
    }
    
}

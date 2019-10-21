package fctp.subproblem;

import fctp.AbstractFctpSubProblem;
import fctp.Fctp;
import fctp.Parameters;

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
public class FctpSubProblemDual extends AbstractFctpSubProblem {
    /**
     * 需求量约束的对偶变量 <br>
     * 需求量约束：for k in customers: <br>
     * sum(flow[j][k], for j in warehouses) >= demand[k].
     */
    private IloNumVar[] demandDualVars;
    /**
     * 容量约束的对偶变量 <br>
     * 容量约束：for j in warehouses: <br>
     * -sum(flow[j][k], for k in customers) >= -capacity[j] * open[j].
     */
    private IloNumVar[] capacityDualVars;
    
    /** 记录目标函数系数，便于生成割平面（重要）. */
    private HashMap<IloNumVar, IloLinearNumExpr> objCoeff;
    
    /** 对偶问题约束，constraints[j][k] 对应的对偶变量即为原问题中的 仓库 j 供应给客户 k 的货量. */
    private IloRange[][] constraints;
    
    /**
     * Create a Instance FctpSubProblemDual.
     * 
     * @param fctpIns FCTP 实例
     * @param openVar 主问题中的 open 变量
     * @throws IloException
     */
    public FctpSubProblemDual(Fctp fctpIns, IloNumVar[] openVar) throws IloException {
        super(fctpIns);
        
        // 创建决策变量，并保存对应的目标函数系数
        demandDualVars = new IloNumVar[customerNum];
        capacityDualVars = new IloNumVar[warehouseNum];        
        objCoeff = new HashMap<>((int)((customerNum + warehouseNum) / Parameters.LOADER_FACTOR) + 1);
        
        double[] demand = fctpIns.getDemand();
        for (int k = 0; k < customerNum; k++) {
            demandDualVars[k] = subSolver.numVar(0.0, Double.MAX_VALUE, "u_" + k);
            // 保存需求量约束对应的对偶变量在子问题目标函数中的系数
            objCoeff.put(demandDualVars[k], subSolver.linearNumExpr(demand[k]));
        }
        
        double[] capacity = fctpIns.getCapacity();
        for (int j = 0; j < warehouseNum; j++) {
            capacityDualVars[j] = subSolver.numVar(0.0, Double.MAX_VALUE, "v_" + j);
            
            /*
             * 保存容量约束对应的对偶变量在子问题目标函数中的系数
             * 注意这里的右侧项存放的变量 -capacity[j] * open[j]
             */
            IloLinearNumExpr expr = subSolver.linearNumExpr();
            expr.addTerm(-capacity[j], openVar[j]);
            objCoeff.put(capacityDualVars[j], expr);
        }
        
        // 对偶约束，u[k] - v[j] >= flowCost[j][k]
        double[][] flowCost = fctpIns.getFlowCost();
        constraints = new IloRange[warehouseNum][customerNum];
        for (int j = 0; j < warehouseNum; j++) {
            for (int k = 0; k < customerNum; k++) {
                constraints[j][k] = subSolver.addLe(
                        subSolver.diff(demandDualVars[k], capacityDualVars[j]), flowCost[j][k]);
            }
        }
        
        // 最大化目标
        subSolver.addMaximize();
        
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
    protected double getFlowBetween(int warehouseIndex, int customerIndex) 
            throws UnknownObjectException, IloException {
        return subSolver.getDual(constraints[warehouseIndex][customerIndex]);
    }
    
    @Override
    protected IloCplex.Status solve(double[] openValues, double[] capacity) throws IloException {
        IloNumExpr objExpr = subSolver.numExpr();        
        // 根据仓库开设情况更新的容量
        double[] openCapacity = new double[warehouseNum];
        for (int j = 0; j < warehouseNum; j++) {
            openCapacity[j] = 0;
            if (openValues[j] > Parameters.ROUNDUP) {
                openCapacity[j] = -capacity[j];
            }
            objExpr = subSolver.sum(objExpr, 
                    subSolver.prod(openCapacity[j], capacityDualVars[j]));
        }
        
        for (int k = 0; k < customerNum; k++) {
            objExpr = subSolver.sum(objExpr, 
                    subSolver.prod(objCoeff.get(demandDualVars[k]), demandDualVars[k]));
        }
        
        subSolver.getObjective().setExpr(objExpr);
        
        subSolver.solve();
        
        return subSolver.getStatus();
    }
    
    @Override
    protected IloRange createFeasibilityCut() throws IloException {
        IloLinearNumExpr ray = subSolver.getRay();
        IloLinearNumExprIterator it = ray.linearIterator();
        
        // 约束的右侧项 * 对应的极射线的值
        IloNumExpr expr = subSolver.numExpr();
        while (it.hasNext()) {
            IloNumVar var = it.nextNumVar();
            double value = it.getValue();
            expr = subSolver.sum(expr, subSolver.prod(value, objCoeff.get(var)));
        }
        
        // 返回对偶问题的极射线对应的“可行割”：u^r(d - Gx) <= 0
        return subSolver.le(expr, 0);
    }

    @Override
    protected IloRange createOptimalityCut(IloNumVar estFlowCost) throws IloException {        
        IloNumExpr expr = subSolver.numExpr();
        
        // 需求量约束的右侧项 * 对偶变量值
        double dualVal;
        for (int k = 0; k < customerNum; k++) {
            dualVal = subSolver.getValue(demandDualVars[k]);
            expr = subSolver.sum(expr, subSolver.prod(objCoeff.get(demandDualVars[k]), dualVal));
        }
        
        // 容量约束的右侧项 * 对偶变量值
        for (int j = 0; j < warehouseNum; j++) {
            dualVal = subSolver.getValue(capacityDualVars[j]);
            expr = subSolver.sum(expr, 
                    subSolver.prod(objCoeff.get(capacityDualVars[j]), dualVal));
        }
        
        // 返回对偶问题的极点对应的“最优割”：u^t(d - Gx) <= sigma
        return subSolver.le(subSolver.diff(expr, estFlowCost), 0);
    }
    
}

package csp;

import ilog.concert.IloException;
import ilog.concert.IloNumVar;
import ilog.concert.IloNumVarType;
import ilog.concert.IloObjective;
import ilog.cplex.IloCplex;

/**
 * CutStock Problem 的 Price Problem.
 * 
 * @author Xiong Wangqi
 * @version V1.0
 * @since JDK1.8
 */
class CspPriceProblem {
    /** 需要的木材种类. */
    private final int kinds;
    
    private IloCplex pricingSolver;
    /** 切割方案对应的每种木材的份数. */
    private IloNumVar[] pattern;
    private double reducedCost;

    /**
     * Price Problem 模型初始化.
     */
    CspPriceProblem(Csp cspIns) throws IloException {
        kinds = cspIns.getAmount().length;
        pricingSolver = new IloCplex();
        pattern = new IloNumVar[kinds];

        pricingSolver.addMinimize();
        // 添加变量，即切割方案对应的每种长度的木材的份数
        pattern = pricingSolver.numVarArray(kinds, 0, Double.MAX_VALUE, IloNumVarType.Int);
        // 添加约束
        pricingSolver.addRange(-Double.MAX_VALUE, 
                pricingSolver.scalProd(pattern, cspIns.getSize()), 
                cspIns.getRollLength());

        reducedCost = -1;
    }

    /**
     * 求解 Price Problem，得到新的切割方案（对应的每种木材的份数）.
     */
    double[] solve(double[] dualPrice) throws IloException {
        // 更新目标值
        IloObjective obj = pricingSolver.getObjective();
        obj.setExpr(pricingSolver.diff(1.0, pricingSolver.scalProd(pattern, dualPrice)));

        pricingSolver.solve();

        this.reducedCost = pricingSolver.getObjValue();

        return pricingSolver.getValues(pattern);
    }
    
    public double getReducedCost() {
        return reducedCost;
    }
    
    /**
     * Releases all Cplex objects attached to the Price Problem.
     */
    void end() {
        pricingSolver.end();
    }

}

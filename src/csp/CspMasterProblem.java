package csp;

import ilog.concert.IloColumn;
import ilog.concert.IloException;
import ilog.concert.IloNumVar;
import ilog.concert.IloNumVarType;
import ilog.concert.IloObjective;
import ilog.concert.IloRange;
import ilog.cplex.IloCplex;

import java.io.IOException;
import java.util.Arrays;

/**
 * CutStock Problem 的主问题.
 * 
 * @author Xiong Wangqi
 * @version V1.0
 * @since JDK1.8
 */
class CspMasterProblem {
    /** 所需木材种类. */
    private int kinds;
    
    private IloCplex rmlpSolver;
    private IloRange[] amountCstrs;

    /** 求解过程中生成的切割方案数. */
    private int patternNum;
    /** 决策变量（切割方案使用的次数）. */
    private IloNumVar[] cutTimes;
    /** 决策变量对应的切割方案. */
    private double[][] patterns;

    /**
     * Master Problem 初始化.
     * 
     * @param cspIns CSP instance
     * @throws IloException
     */
    CspMasterProblem(Csp cspIns) throws IloException {
        kinds = cspIns.getAmount().length;

        rmlpSolver = new IloCplex();
        rmlpSolver.addMinimize();
        amountCstrs = new IloRange[kinds];

        patternNum = 0;
        cutTimes = new IloNumVar[kinds];
        patterns = new double[kinds][kinds];

        // 木材数量约束
        for (int i = 0; i < kinds; i++) {
            amountCstrs[i] = rmlpSolver.addRange(cspIns.getAmount()[i], Double.MAX_VALUE);
        }
        // 初始列，分别对应整根原料全部切成一种木材的方案
        for (int j = 0; j < kinds; j++) {
            // 切割方案（默认填充0）
            double[] pattern = new double[kinds];
            pattern[j] = (int) (cspIns.getRollLength() / cspIns.getSize()[j]);

            this.addColumn(1.0, pattern);
        }

        // 用原始单纯形法求解RMLP
        rmlpSolver.setParam(IloCplex.Param.RootAlgorithm, IloCplex.Algorithm.Primal);
    }

    /**
     * 生成新列，模型三要素更新：目标值，约束 ，变量.
     * 
     * @param coeffInObj 要生成的列对应的变量在目标函数中的系数
     * @param coeffInCstrs 要生成的列对应的变量在约束条件中的系数
     * @throws IloException
     */
    void addColumn(double coeffInObj, double[] coeffInCstrs) throws IloException {
        if (coeffInCstrs.length != amountCstrs.length) {
            throw new IllegalArgumentException("The coefficients for the constraints is wrong.");
        }

        // 1. 添加目标函数中的系数
        IloObjective rollUsed = rmlpSolver.getObjective();
        IloColumn col = rmlpSolver.column(rollUsed, coeffInObj);

        // 2. 添加约束中的系数
        for (int i = 0; i < kinds; i++) {
            col = col.and(rmlpSolver.column(amountCstrs[i], coeffInCstrs[i]));
        }

        // 3. 添加变量
        IloNumVar cutTime = rmlpSolver.numVar(col, 0, Double.MAX_VALUE);

        // 保存切割方案
        this.addPattern(coeffInCstrs, cutTime);
    }

    /**
     * 求解 RMLP 并返回对偶变量值.
     * 
     * @return 对偶变量的值
     * @throws IloException
     */
    double[] solveLp() throws IloException {
        rmlpSolver.solve();
        return rmlpSolver.getDuals(amountCstrs);
    }

    /**
     * 将 MLP 转换为 MIP 进行求解，并返回木材切割问题的解.
     * 
     * @return CSP 问题的解
     * @throws IloException
     */
    CspSolution solveIp() throws IloException {
        this.deleteNullPatterns();

        // 将变量转换为整型
        rmlpSolver.add(rmlpSolver.conversion(cutTimes, IloNumVarType.Int));

        CspSolution cspSol = new CspSolution();
        if (rmlpSolver.solve()) {
            cspSol.generateCutPlan(rmlpSolver.getValues(cutTimes), patterns, rmlpSolver.getObjValue());
        }
        cspSol.setStatus(rmlpSolver.getCplexStatus());

        return cspSol;
    }
    
    /**
     * Releases all Cplex objects attached to the RMLP.
     */
    void end() {
        rmlpSolver.end();
    }

    /**
     * 清除倍增数组导致的“空”的切割方案.
     */
    private void deleteNullPatterns() {
        IloNumVar[] tempVar = new IloNumVar[patternNum];
        double[][] tempPat = new double[patternNum][kinds];

        for (int i = 0; i < patternNum; i++) {
            tempVar[i] = cutTimes[i];
            tempPat[i] = Arrays.copyOf(patterns[i], kinds);
        }
        cutTimes = tempVar;
        patterns = tempPat;
    }

    /**
     * 添加切割方案及其对应的决策变量（使用次数）.
     * 
     * @param pattern 切割方案中每种木材的
     * @param cutTime 决策变量-切割方案使用的次数
     */
    private void addPattern(double[] pattern, IloNumVar cutTime) {
        if (patternNum == cutTimes.length) {
            resizing(2 * patternNum);
        }
        
        cutTimes[patternNum] = cutTime;
        patterns[patternNum] = Arrays.copyOf(pattern, kinds);

        patternNum++;
    }

    /**
     * 扩展 {@link #cutTimes} 和 {@link #patterns} 的容量.
     * 
     * @param capacity 新容量
     */
    private void resizing(int capacity) {
        assert capacity >= patternNum;
        IloNumVar[] tempVar = new IloNumVar[capacity];
        double[][] tempPat = new double[capacity][kinds];

        for (int i = 0; i < patternNum; i++) {
            tempVar[i] = cutTimes[i];
            tempPat[i] = Arrays.copyOf(patterns[i], kinds);
        }
        cutTimes = tempVar;
        patterns = tempPat;
    }
    
}

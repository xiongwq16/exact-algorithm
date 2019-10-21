package fctp;

import ilog.concert.IloException;
import ilog.concert.IloNumVar;
import ilog.concert.IloRange;
import ilog.cplex.IloCplex;
import ilog.cplex.IloCplex.UnknownObjectException;

/**
 * 子问题抽象类.
 * 
 * @author Xiong Wangqi
 * @version V1.0
 * @since JDK1.8
 */
public abstract class AbstractFctpSubProblem {
    protected int warehouseNum;
    protected int customerNum;

    protected IloCplex subSolver;
    
    protected AbstractFctpSubProblem(Fctp fctpIns) throws IloException {
        warehouseNum = fctpIns.getWarehouseNum();
        customerNum = fctpIns.getCustomerNum();

        subSolver = new IloCplex();
    }
    
    /**
     * 获取指定仓库和客户之间的流量.
     * 
     * @param warehouseIndex 仓库索引
     * @param customerIndex 客户索引
     * @return 指定的仓库和客户之间的流量
     * @throws UnknownObjectException 索引错误异常
     * @throws IloException
     */
    protected abstract double getFlowBetween(int warehouseIndex, int customerIndex) 
            throws UnknownObjectException, IloException;

    /*
     * 由于 subSolver 被封装在 subProblem 对象中， 因此外部变量想要对 subSolver
     * 对应的对偶形式的子问题模型进行更新需要借助“类方法”实现。 根据 Benders 分解中 MasterProblem 与 SubProblem
     * 的关系，需要实现以下几个方法：
     * 1. 基于主问题的解中的开设仓库信息，更新目标函数，并求解
     * 2. 生成“可行割”
     * 3. 生成“最优割”
     * 4. 获取子问题目标函数值
     * 5. 释放 Cplex 相关对象
     * 方法实现见下方代码。
     */
    
    /**
     * 求解子问题，并返回解的状态（用于判定可行割/最优割）.
     * 
     * @param openValues 仓库开设变量的取值
     * @param capacity 仓库容量
     * @return 解的状态
     * @throws IloException
     */
    protected abstract IloCplex.Status solve(double[] openValues, double[] capacity) throws IloException;
    
    /**
     * 生成“可行割”.
     * 
     * @return 可行割
     * @throws IloException
     */
    protected abstract IloRange createFeasibilityCut() throws IloException;

    /**
     * 生成“最优割”.
     * 
     * @param estFlowCost 主问题中的流量成本
     * @return 最优割
     * @throws IloException
     */
    protected abstract IloRange createOptimalityCut(IloNumVar estFlowCost) throws IloException;

    double getObjValue() throws IloException {
        return subSolver.getObjValue();
    }
    
    /**
     * Releases all Cplex objects attached to the SubProblem.
     */
    void end() {
        subSolver.end();
    }
    
}

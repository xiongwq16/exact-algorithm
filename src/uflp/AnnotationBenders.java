package uflp;

import ilog.concert.IloException;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Cplex 的 Benders Decomposition Algorithm 注释分解.
 * 
 * @author Xiong Wangqi
 * @version V1.0
 * @since JDK1.8
 */
public class AnnotationBenders {
    private IloCplex uflpSolver;
    /** use[j] = 1 if warehouse j is used, 0 if not. */
    private IloNumVar[] open;
    /** supply[j][k] = 1 if warehouse j supplies customer k, 0 if not. */
    private IloNumVar[][] supply;

    private int warehouseNum;
    private int customerNum;

    public AnnotationBenders(String filename, int randomSeed) throws IOException, IloException {
        Uflp uflpIns = new Uflp(filename);
        warehouseNum = uflpIns.getFalicityNum();
        customerNum = uflpIns.getCustomerNum();

        uflpSolver = new IloCplex();

        // Create variables and set the objective
        open = new IloNumVar[warehouseNum];
        supply = new IloNumVar[warehouseNum][customerNum];
        // Prepare for the objective expression
        IloLinearNumExpr expr = uflpSolver.linearNumExpr();
        double[] fixedCost = uflpIns.getFixedCost();
        double[][] supplyCost = uflpIns.getSupplyCost();

        for (int j = 0; j < warehouseNum; j++) {
            open[j] = uflpSolver.boolVar("open_" + j);
            expr.addTerm(fixedCost[j], open[j]);
            for (int k = 0; k < customerNum; k++) {
                supply[j][k] = uflpSolver.numVar(0, 1, "supply_" + j + "_" + k);
                expr.addTerm(supplyCost[j][k], supply[j][k]);
            }

        }
        uflpSolver.addMinimize(expr, "totalCost");

        // Add Constraints
        for (int k = 0; k < customerNum; k++) {
            expr.clear();
            for (int j = 0; j < warehouseNum; j++) {
                // Each customer k can only be assigned to the open warehouse
                uflpSolver.addLe(supply[j][k], open[j]);
                expr.addTerm(1.0, supply[j][k]);
            }
            // Each customer k must be assigned to exactly one warehouse
            uflpSolver.addEq(expr, 1.0);
        }

        // 注释
        this.setAnnotation();

        // 设置随机种子
        this.setRandomSeed(randomSeed);
    }

    public void setBendersStrategy(int strategy) throws IloException {
        uflpSolver.setParam(IloCplex.Param.Benders.Strategy, strategy);
    }
    
    public void solve() throws IloException {
        UflpSolution uflpSol = new UflpSolution(warehouseNum, customerNum);

        long start = System.currentTimeMillis();

        boolean isSolved = uflpSolver.solve();
        uflpSol.setSolveTime(System.currentTimeMillis() - start);
        uflpSol.setStatus(uflpSolver.getCplexStatus());

        if (isSolved) {
            uflpSol.setTotalCost(uflpSolver.getObjValue());

            double tolerance = uflpSolver.getParam(IloCplex.Param.MIP.Tolerances.Integrality);
            ArrayList<Integer> openWarehouses = new ArrayList<>();
            for (int j = 0; j < warehouseNum; j++) {
                // 对开设的仓库及其服务的客户进行处理
                if (uflpSolver.getValue(open[j]) > 1 - tolerance) {
                    openWarehouses.add(j);

                    for (int k = 0; k < customerNum; k++) {
                        if (uflpSolver.getValue(supply[j][k]) > 1 - tolerance) {
                            uflpSol.setSupply(j, k);
                        }

                    }

                }

            }
            uflpSol.setOpenWarehouses(openWarehouses);
        }
        uflpSol.output();
        
        // Releases all Cplex objects attached to the Annotation Benders
        uflpSolver.end();
    }
    
    /**
     * 添加 Benders 注释 <br>
     * 1 创建 IloCplex.LongAnnotation 注释对象 <br>
     * 2 为变量添加注释：0 对应 MasterProblem，n  大于 0 对应第 n 个 SubProblem <br>
     * 3 如果 SubProblem 不可再分解，则全部注释为 1 <br>
     * 4 如果某个变量没有被注释，则使用创建 IloCplex.LongAnnotation 对象时的默认值 <br>
     */
    private void setAnnotation() throws IloException {
        IloCplex.LongAnnotation benders = uflpSolver.newLongAnnotation(
                IloCplex.CPX_BENDERS_ANNOTATION);

        // Put the binary "open" variables in the master problem.
        for (int j = 0; j < warehouseNum; j++) {
            uflpSolver.setAnnotation(benders, open[j], 0);
        }

        // Attention: The LP portion can be decomposed into smaller problems
        // So put the "supply" variables in different subproblem
        for (int k = 0; k < customerNum; k++) {
            for (int j = 0; j < warehouseNum; j++) {
                uflpSolver.setAnnotation(benders, supply[j][k], k + 1);
            }
        }

        // Benders Strategy 采用 User 策略，即完全根据用户注释进行 Benders Decomposition
        uflpSolver.setParam(IloCplex.Param.Benders.Strategy, IloCplex.BendersStrategy.User);
    }
    
    private void setRandomSeed(int randomSeed) throws IloException {
        uflpSolver.setParam(IloCplex.Param.RandomSeed, randomSeed);
    }
    
}

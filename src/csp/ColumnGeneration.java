package csp;

import ilog.concert.IloException;

import java.io.IOException;

/**
 * Use column generation to solve Cut Stock Problem.
 * 
 * @author Xiong Wangqi
 * @version V1.0
 * @since JDK1.8
 */
public class ColumnGeneration {
    /** How small a value can be and still be treated as nonzero. */
    private static final double EPS = 1e-6;
    
    private CspMasterProblem masterProblem;
    private CspPriceProblem priceProblem;

    public ColumnGeneration(String filename) throws IOException, IloException {
        Csp cspIns = new Csp(filename);
        
        masterProblem = new CspMasterProblem(cspIns);
        priceProblem = new CspPriceProblem(cspIns);
    }
    
    /**
     * 使用列生成求解，并返回解.
     * 
     * @throws IloException
     */
    public void solve() throws IloException {
        // 列生成核心步骤
        while (true) {
            double[] dualPrice = masterProblem.solveLp();

            double[] pattern = priceProblem.solve(dualPrice);

            // 如果的目标值大于等于0，则已找到MLP问题的解；否则生成新列
            if (priceProblem.getReducedCost() > -EPS) {
                break;
            }

            // 添加“新列”到 MasterProblem
            masterProblem.addColumn(1.0, pattern);
        }

        CspSolution cspSol = masterProblem.solveIp();
        cspSol.output();
        
        // Releases all Cplex objects attached to the Cut Stock Problem
        masterProblem.end();
        priceProblem.end();
    }
}

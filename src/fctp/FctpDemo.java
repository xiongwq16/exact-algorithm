package fctp;

import ilog.concert.IloException;

/**
 * FTCP 的手动 Benders 分解调用模拟.
 * 
 * @author Xiong Wangqi
 * @version V1.0
 * @since JDK1.8
 */
class FctpDemo {
    public static void main(String[] args) throws IloException {
        int warehouseNum = 50;
        int customerNum = 4000;
        // mean capacity multiplier
        double meanCapMulti = 2.5;
        // mean fixed cost of warehouse
        double meanFixedCost = 500;
        // 用于生成算例的随机种子
        int randomSeed = 72612;
        
        // 是否采用子问题的对偶形式
        boolean isSubProblemDual = true;
        try {
            ManualBenders mb = new ManualBenders(isSubProblemDual, warehouseNum, customerNum, 
                    meanCapMulti, meanFixedCost, randomSeed);
            mb.solve();

        } catch (IloException e) {
            System.err.println("Concert exception '" + e + "' caught");
        }
    }
}

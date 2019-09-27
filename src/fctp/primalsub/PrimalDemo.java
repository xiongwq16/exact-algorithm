package fctp.primalsub;

import ilog.concert.IloException;

/**
 * FTCP 的手动 Benders 分解调用模拟.
 * 
 * @author Xiong Wangqi
 * @version V1.0
 * @since JDK1.8
 */
public class PrimalDemo {
    
    public static void main(String[] args) throws IloException {
        int warehouseNum = 50;
        int customerNum = 4000; 
        // mean capacity multiplier
        double meanCapMulti = 2.5;
        // mean fixed cost of warehouse
        double meanFixedCost = 500;
        // 用于生成算例的随机种子
        int randomSeed = 72612;
        
        try {
            ManualBenders mb = new ManualBenders(warehouseNum, customerNum, 
                    meanCapMulti, meanFixedCost, randomSeed);
            mb.solve();
            
        } catch (IloException e) {
            System.err.println("Concert exception '" + e + "' caught");
        }
    }
}

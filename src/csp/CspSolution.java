package csp;

import ilog.cplex.IloCplex.CplexStatus;

import java.util.Arrays;

/**
 * 木材切割问题的解.
 * 
 * @author Xiong Wangqi
 * @version V1.0
 * @since JDK1.8
 */
class CspSolution {
    private CplexStatus status;

    private int rollUsed;
    /** 各切割方案的使用次数. */
    private int[] cutTimes;
    /** 列生成过程中生成的切割方案. */
    private double[][] patterns;

    /**
     * 生成木材切割方案.
     * 
     * @param cutTimes  各切割方案的使用次数
     * @param patterns  生成的所有切割方案
     * @param rollsUsed 使用的木材数量
     */
    void generateCutPlan(double[] cutTimes, double[][] patterns, double rollsUsed) {
        this.cutTimes = new int[cutTimes.length];
        for (int i = 0; i < cutTimes.length; i++) {
            this.cutTimes[i] = (int) cutTimes[i];
        }
        this.patterns = patterns;
        this.rollUsed = (int) rollsUsed;
    }
    
    /**
     * 输出解决方案，包括解的状态，使用的木材数量及切割方案.
     */
    void output() {
        System.out.println("\n" + status + " solution is found");
        System.out.printf("%d rolls are cut and the cut plan is:\n", rollUsed);
        for (int i = 0; i < cutTimes.length; i++) {
            if (cutTimes[i] > 0) {
                System.out.printf("Pattern %s is used for %d times\n", 
                        Arrays.toString(patterns[i]), cutTimes[i]);
            }
        }
    }
    
    void setStatus(CplexStatus status) {
        this.status = status;
    }
}

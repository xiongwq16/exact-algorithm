package uflp;

import ilog.concert.IloException;
import ilog.cplex.IloCplex;

import java.io.IOException;

/**
 * Use Benders Decomposition to solve UFLP.
 * 
 * @author Xiong Wangqi
 * @version V1.0
 * @since JDK1.8
 */
class UflpDemo {
    private enum Strategy {
        /** 执行传统分支和界限；忽略任何 Benders 注释，也不使用 Benders 算法. */
        Off,

        /**
         * CPLEX自动分解模型，忽略可能提供的任何注释：<br>
         * 将所有整数变量放入到主问题，将所有连续变量放入到子问题，进一步分解此子问题（如果可能）.
         */
        Full,

        /** CPLEX 根据注释确定主问题， 并尝试将剩余的变量分解成不相关的子问题并交给不同的Worker. */
        Workers,
        
        /** CPLEX根据注释分解模型. */
        User
    }

    public static void main(String[] args) throws IOException, IloException {
        // 算例路径
        String filename = "./instances/uflp/Euclid/111EuclS.txt";
        // 随机种子
        int randomSeed = 1024;
        
        // 要使用的Benders分解策略
        Strategy bendersStrategy = Strategy.User;

        AnnotationBenders bdAlgorithm = new AnnotationBenders(filename, randomSeed);

        try {
            switch (bendersStrategy) {
                case Off:
                    bdAlgorithm.setBendersStrategy(IloCplex.BendersStrategy.Off);
                    break;
                case Full:
                    bdAlgorithm.setBendersStrategy(IloCplex.BendersStrategy.Full);
                    break;
                case Workers:
                    bdAlgorithm.setBendersStrategy(IloCplex.BendersStrategy.Workers);
                    break;
                case User:
                    // AnnotationBenders 代码中默认 User 分解
                    // 这里不进行任何操作
                    break;
                default:
                    throw new IllegalArgumentException("Invalid model type specified.");
            }

        } catch (IloException ex) {
            System.out.println("Failed to build the model:\n" + ex.getMessage());
        }

        try {
            bdAlgorithm.solve();
        } catch (IloException ex) {
            System.out.println("Solution failed:\n" + ex.getMessage());
        }

    }
}

package testdemo;

import ilog.concert.IloException;

import java.io.IOException;

import vrptw.algorithm.VrptwExactAlgorithm;
import vrptw.algorithm.branchandbound.BranchAndBound;
import vrptw.algorithm.branchandprice.BranchAndPrice;
import vrptw.problem.Vrptw;

/**
 * VRPTW Exact Algorithm Test.
 * 
 * @author Xiong Wangqi
 * @version V1.0
 * @since JDK1.8
 */
public class VrptwExactAlgDemo {
    public static void main(String[] args) throws IloException {
        String filename = "./instances/solomon_100/c101.txt";
        String exactAlgType = "BranchAndBound";
        
        try {
            Vrptw vrptwIns = new Vrptw(filename);

            VrptwExactAlgorithm vrptwExactAlg;
            switch (exactAlgType) {
                case "BranchAndBound":
                    // 可考虑在 BranchAndBound 的构造函数中，根据 Best known solution 中的车辆数修改车辆数约束，加快求解
                    vrptwExactAlg = new BranchAndBound(vrptwIns);
                    break;
                case "BranchAndPrice":
                    vrptwExactAlg = new BranchAndPrice(vrptwIns);
                    break;
                default:
                    throw new IllegalArgumentException(
                            String.format("%s algorithm is not supported yet.", exactAlgType));
            }
            
            vrptwExactAlg.solve();

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}

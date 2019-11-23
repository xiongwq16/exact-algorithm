package testdemo;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import vrptw.algorithm.subproblem.AbstractPriceProblem;
import vrptw.algorithm.subproblem.labelalgorithm.EspptwccViaLabelCorrecting;
import vrptw.algorithm.subproblem.labelalgorithm.SpptwccViaLabelSetting;
import vrptw.algorithm.subproblem.pulsealgorithm.EspptwccViaPulse;
import vrptw.parameter.Parameters;
import vrptw.problem.Vrptw;

/**
 * SPPTWCC/ESPPTWCC Algorithm Test.
 * 
 * @author Xiong Wangqi
 * @version V1.0
 * @since JDK1.8
 */
public class SpptwccDemo {
    public static void main(String[] args) {
        // 求解 ESPPTWCC or SPPTWCC
        String spptwccOrEspptwcc = Parameters.ESPPTWCC_PULSE;
        String filename = "./instances/solomon_100/r101.txt";
        try {
            Vrptw vrptwIns = new Vrptw(filename);
            double[] lambda = { 4.5800000000000125, 10.420000000000002, 0.7199999999999989, 14.940000000000012, -0.0,
                    20.31000000000001, 18.52000000000002, 18.939999999999984, 16.959999999999994, 9.180000000000003,
                    29.693333333333346, 18.48, 0.2600000000000193, 23.82, 22.500000000000007, 16.809999999999967,
                    15.130000000000003, 4.829999999999998, 20.483333333333356, 17.900000000000002, 6.950000000000074,
                    24.929999999999936, 10.70999999999998, 18.469999999999967, 18.540000000000006, 14.77,
                    0.1200000000000081, -0.0, 34.230000000000004, 16.370000000000022, 25.569999999999983,
                    15.090000000000035, 23.86000000000001, 10.049999999999983, 27.940000000000012, 14.409999999999997,
                    10.170000000000009, 51.62, 26.08, 14.66, 31.889999999999944, 7.91999999999998, 19.859999999999992,
                    5.280000000000001, 9.530000000000008, 18.50999999999999, 44.99666666666668, 1.3299999999999983,
                    32.74999999999998, 8.309999999999999, 16.609999999999978, 15.329999999999991, 7.689999999999998,
                    26.640000000000025, 16.14, 12.150000000000006, 14.370000000000005, 1.4600000000000009,
                    5.669999999999995, 4.979999999999997, 13.519999999999996, 31.346666666666653, 23.093333333333334,
                    50.146666666666704, 42.83000000000003, 31.099999999999994, 38.67, 16.44000000000001,
                    21.569999999999997, 0.23999999999997357, 27.809999999999988, 8.920000000000002, 10.759999999999962,
                    12.760000000000012, 12.510000000000048, 8.76000000000002, 2.819999999999993, 24.290000000000006,
                    18.719999999999988, 0.29999999999999716, 22.999999999999975, 37.37999999999998, 22.899999999999984,
                    31.220000000000013, 24.39, 36.04000000000002, 25.52999999999998, 9.839999999999996, -0.0,
                    7.069999999999993, 6.450000000000003, 5.860000000000028, 3.5, 22.7, 5.8799999999999955,
                    7.219999999999999, 10.56000000000001, 5.359999999999992, 9.780000000000005, 4.630000000000003 };
            
            Map<Integer, Double> dualPrices = new HashMap<>(lambda.length);
            for (int i = 1; i <= lambda.length; i++) {
                dualPrices.put(i, lambda[i - 1]);
            }

            AbstractPriceProblem subAlg;
            switch (spptwccOrEspptwcc) {
                case Parameters.SPPTWCC_LABEL_SETTING:
                    subAlg = new SpptwccViaLabelSetting(vrptwIns);
                    break;
                case Parameters.ESPPTWCC_LABEL_CORRECTING:
                    subAlg = new EspptwccViaLabelCorrecting(vrptwIns);
                    break;
                case Parameters.ESPPTWCC_PULSE:
                    subAlg = new EspptwccViaPulse(vrptwIns);
                    break;
                default:
                    throw new IllegalArgumentException(
                            String.format("%s algorithm is not supported yet.", spptwccOrEspptwcc));
            }

            double start = System.currentTimeMillis();
            subAlg.updateTimeMatrix(vrptwIns.getTimeMatrix());
            subAlg.solve(dualPrices);

            System.out.println("Time consumption: " + (System.currentTimeMillis() - start) / 1000.0);
            System.out.println("Reduced cost: " + subAlg.getRevisedCostOfShortestPath());
            System.out.println("Path: " + subAlg.getShortestPath().toString() + "\n");

        } catch (IOException e) {
            System.err.println("IOException '" + e + "' caught");
        }

    }
}

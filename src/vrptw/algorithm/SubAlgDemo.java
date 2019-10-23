package vrptw.algorithm;

import java.io.IOException;

import vrptw.algorithm.subproblem.AbstractSubProblem;
import vrptw.algorithm.subproblem.labelalgorithm.EspptwccViaLabel;
import vrptw.algorithm.subproblem.labelalgorithm.SpptwccViaLabel;
import vrptw.problem.Vertex;
import vrptw.problem.Vrptw;

public class SubAlgDemo {
    public static void main(String[] args) {
        String filename = "./instances/solomon/c101.txt";
        try {
            Vrptw vrptwIns = new Vrptw(filename);
            double[] lambda = { 37.36, 41.22, 32.24, 36.22, 30.26, 38.0, 32.0, 36.22, 40.18, 33.52, 39.28, 76.14, 61.6,
                    78.7, 72.1, 80.62, 66.6, 70.7, 78.1, 20.0, 20.38, 24.32, 26.0, 30.0, 30.26, 31.62, 34.22, 35.44,
                    40.0, 41.22, 67.08, 63.24, 67.04, 64.76, 76.14, 70.7, 78.58, 82.46, 80.62, 41.22, 37.36, 38.62,
                    33.1, 43.08, 44.72, 41.18, 36.04, 46.64, 38.4, 45.6, 50.0, 42.42, 90.34, 80.08, 70.1, 90.0, 70.0,
                    90.08, 70.1, 90.54, 44.72, 36.04, 28.28, 43.08, 25.6, 33.1, 24.4, 41.22, 31.62, 117.04, -789.36,
                    47.7, 111.42, 39.68, 31.62, 104.4, 104.0, 100.56, 102.14, 102.94, 94.86, 70.7, 64.76, 62.08, 59.46,
                    52.94, 50.98, 53.84, 48.7, 41.22, 44.72, 88.4, 86.02, 81.2, 74.4, 72.1, 80.62, 61.6, 67.08, 76.14 };

            String subAlgName = "EspptwccViaLabel";
            AbstractSubProblem subAlg;
            
            switch (subAlgName) {
                case "SpptwccViaLabel":
                    subAlg = new SpptwccViaLabel(vrptwIns, lambda);
                    break;
                    case "EspptwccViaLabel":
                        subAlg = new EspptwccViaLabel(vrptwIns, lambda);
                        break;
                default:
                    throw new IllegalArgumentException(String.format("%s algorithm is not supported yet.", subAlgName));
            }

//            int[] temp = {0, 20, 24, 25, 35, 37, 38, 39, 36, 34, 52, 49, 47, 101};
//            double cost = 0;
//            double time = 0;
//            double demand = 0;
//            for (int i = 0; i < temp.length - 1; i++) {
//                cost += subAlg.revisedCostMatrix[temp[i]][temp[i + 1]];
//                time += vrptwIns.getDistanceBetween(temp[i], temp[i + 1]);
//                Vertex vertex = vrptwIns.getVertexByIndex(temp[i + 1]);
//                demand += vertex.getDemand();
//                if (demand > vrptwIns.getVehicle().getCapacity()) {
//                    System.out.println("Demand Infeasible");
//                    break;
//                }
//                
//                if (time > vertex.getLatestTime()) {
//                    System.out.println(temp[i] + "Timewindow Infeasible");
//                    break;
//                }
//
//                if (time < vertex.getEarliestTime()) {
//                    time = vertex.getEarliestTime();
//                }
//
//                time += vertex.getServiceTime();
//            }
//            System.out.println(cost);

            subAlg.solve();
            
            System.out.println(subAlg.getReducedCost());
            System.out.println(subAlg.getShortestPath().getVertexIndices().toString());

        } catch (IOException e) {
            System.err.println("IOException '" + e + "' caught");
        }

    }

}

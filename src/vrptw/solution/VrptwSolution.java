package vrptw.solution;

import java.util.ArrayList;

import vrptw.parameter.Parameters;
import vrptw.problem.Vrptw;

/**
 * VRPTW 的解，包括成本及路径.
 * 
 * @author Xiong Wangqi
 * @version V1.0
 * @since JDK1.8
 */
public class VrptwSolution {
    private Vrptw vrptwIns;
        
    private int pathNum;
    private ArrayList<Path> pathsUsed;
    
    private double totalCost;
    
    /**
     * 根据 MIP 的解生成配送方案.
     * 
     * @param paths 生成的所有路径
     * @param usePath 主问题的解，即是否选用对应的路径
     * @param objective 已找到的最优下界
     */
    public VrptwSolution(Vrptw vrptwIns, Path[] paths, double[] usePath, double objective) {
        this.vrptwIns = vrptwIns;
        
        pathNum = 0;
        totalCost = objective;
        
        pathsUsed = new ArrayList<>(paths.length);
        for (int i = 0; i < paths.length; i++) {
            if (usePath[i] > 1 - Parameters.EPS) {
                pathsUsed.add(paths[i]);
                pathNum++;
            }
        }
        
    }
    
    /**
     * 输出成本，耗时及路径信息.
     * 
     * @param timeConsume 求解耗时
     */
    public void output(double timeConsume) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("算法耗时 %.3fs\n", timeConsume / 1000.0));
        sb.append(String.format("总成本为 %.3f，使用车辆 %d 辆:\n", totalCost, pathNum));
        
        for (Path p: pathsUsed) {
            ArrayList<Integer> vertexIds = p.getVertexIds();
            String pathSrt = "";
            for (int id: vertexIds) {
                pathSrt += vrptwIns.getVertexes().get(id).getNumber() + "-";
            }
            sb.append(pathSrt.subSequence(0, pathSrt.length() - 1) + "\n");
        }
                
        System.out.println(sb.toString());
    }
    
    
}

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
     * @param usedPathIndices 解中使用的路径的索引
     * @param objective 已找到的最优下界
     */
    public VrptwSolution(Vrptw vrptwIns, Path[] paths, ArrayList<Integer> usedPathIndices, double objective) {
        this.vrptwIns = vrptwIns;
        
        pathNum = 0;
        totalCost = objective;
        
        pathsUsed = new ArrayList<>(paths.length);
        for (int index: usedPathIndices) {
            pathsUsed.add(paths[index]);
            pathNum++;
        }
        
    }
    
    /**
     * 输出搜索的节点数量，求解耗时、成本、路径等信息.
     * 
     * @param timeConsume 求解耗时
     * @param nodeNum node number in the branch and bound tree
     */
    public void output(double timeConsume, int nodeNum) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("算法耗时 %.3fs，搜索节点数量 %d\n", timeConsume / 1000.0, nodeNum));
        sb.append(String.format("总成本为 %.3f，使用车辆 %d 辆:\n", totalCost, pathNum));
        
        ArrayList<Integer> cusVisited = new ArrayList<>((int) (vrptwIns.getVertexNum() / Parameters.LOADER_FACTOR) + 1);
        for (Path p: pathsUsed) {
            ArrayList<Integer> vertexIds = p.getVertexIds();
            int num = vertexIds.size();
            String pathSrt = "" + vertexIds.get(0);
            for (int i = 1; i < num - 1; i++) {
                cusVisited.add(vertexIds.get(i));
                pathSrt += vrptwIns.getVertexes().get(vertexIds.get(i)).getNumber() + "-";
            }
            pathSrt += vertexIds.get(num - 1);
            sb.append(pathSrt.subSequence(0, pathSrt.length() - 1) + "\n");
        }
        
        cusVisited.sort((i1, i2) -> Integer.compare(i1, i2));
        sb.append(String.format("共访问客户 %d 个\n", cusVisited.size()));
        
        System.out.println(sb.toString());
    }
    
}

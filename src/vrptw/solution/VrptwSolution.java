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
    private int routeNum;
    private ArrayList<Path> paths;
    
    private double totalCost;
    
    public VrptwSolution(Vrptw vrptwIns) {
        routeNum = 0;
        paths = new ArrayList<>(Parameters.INITIAL_CAPACITY);
        
        totalCost = 0;
    }
    
    public void addPath(Path p) {
        this.paths.add(p);
        routeNum++;
        
        totalCost += p.getCost();
    }
    
    public void output() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("总成本为 %.3f，使用车辆 %d 辆\n", totalCost, routeNum));
        this.paths.forEach(path -> sb.append(path.pathToString() + "\n"));
        
        System.out.println(sb.toString());
    }
}

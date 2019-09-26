package fctp;

import ilog.cplex.IloCplex.CplexStatus;

import java.util.ArrayList;

/**
 * FCTP的解.
 * 
 * @author Xiong Wangqi
 * @version V1.0
 * @since JDK1.8
 */
class FctpSolution {
    /**
     * EPS limits how small a flow can be and still be treated as nonzero.
    */
    public static final double EPS = 1e-6;
    
    /** The milliseconds to seconds conversion factor. */
    private static final double MS_TO_SEC = 0.001;
    
    private double totalCost;
    private final double[][] flows;
    private ArrayList<Integer> openWarehouses;
    
    private CplexStatus status;
    private double solveTime;
    
    FctpSolution(int warehouseNum, int customerNum) {
        flows = new double[warehouseNum][customerNum];
    }
    
    void setOpenWarehouses(ArrayList<Integer> openWarehouses) {
        this.openWarehouses = new ArrayList<>(openWarehouses);
    }
    
    void setTotalCost(double totalCost) {
        this.totalCost = totalCost;
    }
    
    void setFlowBetween(int warehouseIndex, int customerIndex, double flow) {
        this.flows[warehouseIndex][customerIndex] = flow;
    }
    
    void setStatus(CplexStatus status) {
        this.status = status;
    }
    
    void setSolveTime(long solveTime) {
        this.solveTime = solveTime * MS_TO_SEC;
    }
    
    /**
     * 最终的解由两部分构成：<br>
     * 1 open 开设的仓库（主问题) <br>
     * 2 flows仓库与客户的流量关系（子问题）<br>
     * 需要主问题的解的状态来判定是否需要子问题的 flows 结果，故增加该方法。 <br>
     * 当然也有其他的解决方案，这里不做讨论. <br>
     * 
     * @return 解的状态（主问题）
     */
    CplexStatus getStatus() {
        return status;
    }
    
    /**
     * 输出 FCTP 的解，包括解的类型，总成本，使用的仓库，供应的客户（及供应量）.
     */
    void output() {
        StringBuilder sb = new StringBuilder();
        
        sb.append(String.format("It takes %.2f seconds to find the %s solution with cost %.2f", 
                solveTime, status, totalCost));
        
        int openWarehouseNum = openWarehouses.size();
        int customerNum = flows[0].length;
        for (int j = 0; j < openWarehouseNum; j++) {
            int warehouseIndex = openWarehouses.get(j);
            sb.append("\nWarehouse" + warehouseIndex + " is open, it serves customters: ");
            for (int k = 0; k < customerNum; k++) {
                if (flows[warehouseIndex][k] > EPS) {
                    sb.append(String.format("%d(%.2f) \t", k, flows[warehouseIndex][k]));
                }
                
            }
            
        }
        System.out.println(sb.toString());
    }
}
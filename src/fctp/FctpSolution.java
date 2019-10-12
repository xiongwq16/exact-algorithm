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
    private double totalCost;
    private double[][] flows;
    private ArrayList<Integer> openWarehouses;
    
    private CplexStatus status;
    private double solveTime;
    
    FctpSolution(int warehouseNum, int customerNum) {
        flows = new double[warehouseNum][customerNum];
    }
    
    /**
     * 输出 FCTP 的解，包括解的类型，总成本，使用的仓库，供应的客户（及供应量）.
     */
    public void output() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("It takes %.3f seconds to find the %s solution.\n", 
                solveTime, status));
        
        int openWarehouseNum = openWarehouses.size();
        sb.append(String.format("%d warehouse is open and total cost is %.3f.", 
                openWarehouseNum, totalCost));
        
        int customerNum = flows[0].length;
        for (int j = 0; j < openWarehouseNum; j++) {
            int warehouseIndex = openWarehouses.get(j);
            sb.append("\nWarehouse" + warehouseIndex + " is open, it serves customters: ");
            for (int k = 0; k < customerNum; k++) {
                if (flows[warehouseIndex][k] > Parameters.EPS) {
                    sb.append(String.format("%d(%.2f) \t", k, flows[warehouseIndex][k]));
                }
                
            }
            
        }
        System.out.println(sb.toString());
    }
    
    void setFlowBetween(int warehouseIndex, int customerIndex, double flow) {
        this.flows[warehouseIndex][customerIndex] = flow;
    }
    
    void setOpenWarehouses(ArrayList<Integer> openWarehouses) {
        this.openWarehouses = new ArrayList<>(openWarehouses);
    }
    
    void setTotalCost(double totalCost) {
        this.totalCost = totalCost;
    }
    
    void setStatus(CplexStatus status) {
        this.status = status;
    }
    
    public void setSolveTime(long solveTime) {
        this.solveTime = solveTime * Parameters.MS_TO_SEC;
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
    public CplexStatus getStatus() {
        return status;
    }
}

package uflp;

import ilog.cplex.IloCplex.CplexStatus;

import java.util.ArrayList;

/**
 * UFLP的解.
 * 
 * @author Xiong Wangqi
 * @version V1.0
 * @since JDK1.8
 */
class UflpSolution {
    /** The milliseconds to seconds conversion factor. */
    private static final double MS_TO_SEC = 0.001;

    private CplexStatus status;
    private double solveTime;

    /** supply[j][k] = 1 if warehouse j supplies customer k, 0 if not. */
    private double[][] supply;
    /** 开设的仓库的编号数组. */
    private ArrayList<Integer> openWarehouses;
    private double totalCost;

    UflpSolution(int warehouseNum, int customerNum) {
        supply = new double[warehouseNum][customerNum];
    }

    void setTotalCost(double totalCost) {
        this.totalCost = totalCost;
    }
    
    void setSupply(int warehouseIndex, int customerindex) {
        supply[warehouseIndex][customerindex] = 1;
    }

    void setOpenWarehouses(ArrayList<Integer> openWarehouses) {
        this.openWarehouses = openWarehouses;
    }

    void setStatus(CplexStatus status) {
        this.status = status;
    }

    void setSolveTime(long solveTime) {
        this.solveTime = solveTime * MS_TO_SEC;
    }

    /**
     * 输出 UFLP 的解，包括解的类型，总成本，开设的仓库，客户的供应方案.
     */
    void output() {
        StringBuilder sb = new StringBuilder();

        sb.append(String.format("It takes %.2f seconds to find the %s solution with cost %.2f", 
                solveTime, status, totalCost));

        int openWarehouseNum = openWarehouses.size();
        int customerNum = supply[0].length;
        for (int j = 0; j < openWarehouseNum; j++) {
            int warehouseIndex = openWarehouses.get(j);
            sb.append("\nWarehouse" + warehouseIndex + " is open, it serves customters: ");
            for (int k = 0; k < customerNum; k++) {
                if (supply[warehouseIndex][k] == 1) {
                    sb.append(k + "\t");
                }
                
            }

        }
        System.out.println(sb.toString());
    }

}

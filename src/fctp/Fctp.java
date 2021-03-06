package fctp;

import java.util.Random;

/**
 * Fixed Charge Transportation Problem(FCTP) 的实例类. <br>
 * 
 * @author Xiong Wangqi
 * @version V1.0
 * @since JDK1.8
 */
public class Fctp {
    private int warehouseNum;
    private int customerNum;

    private double[] demand;
    private double[] capacity;
    private double[] fixedCost;
    private double[][] flowCost;

    Fctp(int warehouseNum, int customerNum, double meanCapMulti, double meanFixedCost, long seed) {
        this.warehouseNum = warehouseNum;
        this.customerNum = customerNum;

        // 设置用于生成算例的随机种子
        Random rng = new Random(seed);

        // 客户需求量随机设置
        demand = new double[customerNum];
        double totalDemand = 0;
        for (int k = 0; k < customerNum; k++) {
            demand[k] = rng.nextDouble();
            totalDemand += demand[k];
        }

        // Set the mean capacity of any one warehouse
        double meanCapacity = meanCapMulti * totalDemand / warehouseNum;
        // 仓库容量、固定成本、流量成本随机设置
        capacity = new double[warehouseNum];
        fixedCost = new double[warehouseNum];
        for (int j = 0; j < warehouseNum; j++) {
            capacity[j] = 2 * meanCapacity * rng.nextDouble();
            fixedCost[j] = 2 * meanFixedCost * rng.nextDouble();
        }

        flowCost = new double[warehouseNum][customerNum];
        for (int j = 0; j < warehouseNum; j++) {
            for (int k = 0; k < customerNum; k++) {
                flowCost[j][k] = rng.nextDouble();
            }
        }

    }

    public int getWarehouseNum() {
        return warehouseNum;
    }

    public int getCustomerNum() {
        return customerNum;
    }

    public double[] getDemand() {
        return demand;
    }

    public double[] getCapacity() {
        return capacity;
    }

    double[] getFixedCost() {
        return fixedCost;
    }

    public double[][] getFlowCost() {
        return flowCost;
    }
}

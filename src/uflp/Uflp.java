package uflp;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import ilog.concert.IloException;

/**
 * UFLP算例对应的Java对象.
 * 
 * @author Xiong Wangqi
 * @version V1.0
 * @since JDK1.8
 */
class Uflp {
    private int warehouseNum;
    private int customerNum;
    /** 仓库 j 的固定成本. */
    private double[] fixedCost;
    /** 连接成本，supplyCost[j]k] 由仓库 j 服务客户 k 的供应成本. */
    private double[][] supplyCost;

    /**
     * 读取算例数据新建 UFLP 实例，算例采用Kochetov and Ivanenko 生成的 UFLP's Euclidean benchmark.
     * 
     * @param filename 文件路径名
     * @throws IOException
     */
    Uflp(String filename) throws IOException {
        BufferedReader bfr = new BufferedReader(new FileReader(filename));

        // 第一行为文件名，第二行为仓库数量、客户数量
        bfr.readLine();
        String line = bfr.readLine();
        String[] num = line.split("\\s+");
        warehouseNum = Integer.parseInt(num[0]);
        customerNum = Integer.parseInt(num[1]);

        fixedCost = new double[warehouseNum];
        supplyCost = new double[warehouseNum][customerNum];

        // 成本数据，依次为仓库编号（从 1 开始），仓库固定成本，客户的供应成本
        while ((line = bfr.readLine()) != null) {
            String[] costData = line.split("\\s+");
            int warehouseIndex = Integer.parseInt(costData[0]) - 1;
            fixedCost[warehouseIndex] = Double.parseDouble(costData[1]);
            
            for (int k = 2; k < costData.length; k++) {
                supplyCost[warehouseIndex][k - 2] = Double.parseDouble(costData[k]);
            }
        }
        bfr.close();
    }
    
    int getFalicityNum() {
        return warehouseNum;
    }
    
    int getCustomerNum() {
        return customerNum;
    }
    
    double[] getFixedCost() {
        return fixedCost;
    }
    
    double[][] getSupplyCost() {
        return supplyCost;
    }
    
}

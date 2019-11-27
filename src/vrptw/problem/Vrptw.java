package vrptw.problem;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import vrptw.parameter.Parameters;

/**
 * VRPTW 算例的 Java 对象.
 * 
 * @author Xiong Wangqi
 * @version V1.0
 * @since JDK1.8
 */
public class Vrptw {
    private int cusNum;
    private int vertexNum;
    /** 存放节点及其对应的序号，Key 为 0 和 n + 1 代表配送中心，其他代表客户. */
    private ArrayList<Vertex> vertexes;
    
    private int vehNum;
    private Vehicle vehicle;
    
    /** 距离矩阵，索引顺序从配送中心到客户. */
    private double[][] distMatrix;
    /** 时间矩阵，索引顺序从配送中心到客户. */
    private double[][] timeMatrix;
    
    /**
     * Create a Instance VRPTW if branch on time windows.
     * 
     * @param vrptwIns VRPTW 实例
     * @param tws 新的时间窗数组
     */
    public Vrptw(Vrptw vrptwIns, TimeWindow[] tws) {
        if (tws.length != vertexNum) {
            throw new IllegalArgumentException(
                    String.format("The length of para tws should be %d", vrptwIns.vertexNum));
        }
        
        this.cusNum = vrptwIns.cusNum;
        this.vertexNum = vrptwIns.vertexNum;
        
        vertexes = new ArrayList<>((int) (vertexNum / 0.75) + 1);
        for (int i = 0; i < vertexNum; i++) {
            vertexes.add(new Vertex(vrptwIns.vertexes.get(i), tws[i]));
        }
        
        this.vehNum = vrptwIns.vehNum;
        this.vehicle = vrptwIns.vehicle;
        
        this.distMatrix = vrptwIns.distMatrix;
        this.timeMatrix = vrptwIns.timeMatrix;
    }
    
    /**
     * Create a Instance Vrptw.
     * 
     * @param filename Solomon 算例文件名
     * @throws IOException
     */
    public Vrptw(String filename) throws IOException {
        BufferedReader bfr = new BufferedReader(new FileReader(filename));
        
        vertexes = new ArrayList<>(Parameters.INITIAL_CAPACITY);
        
        String line = null;
        int count = 0;
        while ((line = bfr.readLine()) != null) {
            count++;

            // 第 5 行是车辆数量及容量信息
            if (count == 5) {
                String[] str = line.trim().split("\\s+");
                vehNum = Integer.parseInt(str[0]);
                int vehicleCapacity = Integer.parseInt(str[1]);

                // 车辆信息初始化
                double speed = 1.0;
                vehicle = new Vehicle(speed, vehicleCapacity);
                continue;
            }

            if (count >= 10) {
                String[] str = line.trim().split("\\s+");
                if (str.length <= 1) {
                    // 空行跳出
                    break;
                }
                
                int id = count - 10;
                String number = str[0];
                double xcoor = Double.parseDouble(str[1]);
                double ycoor = Double.parseDouble(str[2]);
                double demand = Double.parseDouble(str[3]);
                double earliestTime = Double.parseDouble(str[4]);
                double latestTime = Double.parseDouble(str[5]);
                double serviceTime = Double.parseDouble(str[6]);
                
                Vertex vertex = new Vertex(id, number, xcoor, ycoor, demand, serviceTime, earliestTime, latestTime);
                vertexes.add(vertex);
            }

        }
        cusNum = vertexes.size() - 1;
        
        // add dummy end depot
        Vertex depot = vertexes.get(0);
        vertexes.add(depot);

        vertexNum = vertexes.size();

        distMatrix = new double[vertexNum][vertexNum];
        timeMatrix = new double[vertexNum][vertexNum];
        setDistAndTimeMatrix();
        
        bfr.close();
    }
    
    private void setDistAndTimeMatrix() {
        for (int i = 0; i < vertexNum; i++) {
            Vertex v1 = vertexes.get(i);
            for (int j = 0; j < vertexNum; j++) {
                Vertex v2 = vertexes.get(j);
                if (i == vertexNum - 1 || j == 0) {
                    distMatrix[i][j] = Parameters.BIG_TRAVEL_TIME;
                } else {
                    distMatrix[i][j] = v1.getDistanceTo(v2);
                }
                                
                timeMatrix[i][j] = distMatrix[i][j] / vehicle.getSpeed();
            }
            
        }
        
    }
        
    public ArrayList<Vertex> getVertexes() {
        return vertexes;
    }
    
    public List<Vertex> getCustomers() {
        return vertexes.subList(1, vertexNum - 1);
    }
    
    public double[][] getDistMatrix() {
        return distMatrix;
    }
    
    public double[][] getTimeMatrix() {
        return timeMatrix;
    }
    
    public int getCusNum() {
        return cusNum;
    }
    
    public int getVertexNum() {
        return vertexNum;
    }
    
    public int getVehNum() {
        return vehNum;
    }

    public Vehicle getVehicle() {
        return vehicle;
    }
    
}

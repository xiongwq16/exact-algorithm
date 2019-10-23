package vrptw.problem;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import vrptw.parameter.Parameters;

/**
 * VRPTW 算例的 Java 对象.
 * 
 * @author Xiong Wangqi
 * @version V1.0
 * @since JDK1.8
 */
public class Vrptw {
    private int vertexNum;
    /** 存放节点及其对应的序号，Key 为 0 代表配送中心，其他代表客户. */
    private Map<Integer, Vertex> vertexes;
    
    private int vehNum;
    private Vehicle vehicle;

    /** 距离矩阵，索引顺序从配送中心到客户. */
    private double[][] distMatrix;
    /** 时间矩阵，索引顺序从配送中心到客户. */
    private double[][] timeMatrix;

    public Vrptw(String filename) throws IOException {
        BufferedReader bfr = new BufferedReader(new FileReader(filename));

        vertexes = new LinkedHashMap<>(Parameters.INITIAL_CAPACITY);

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
                int index = count - 10;
                String[] str = line.trim().split("\\s+");
                String id = str[0];
                double xcoor = Double.parseDouble(str[1]);
                double ycoor = Double.parseDouble(str[2]);
                double demand = Double.parseDouble(str[3]);
                double earliestTime = Double.parseDouble(str[4]);
                double latestTime = Double.parseDouble(str[5]);
                double serviceTime = Double.parseDouble(str[6]);

                Vertex vertex = new Vertex(index, id, xcoor, ycoor, demand, serviceTime, earliestTime, latestTime);
                vertexes.put(index, vertex);
            }

        }

        // add dummy end depot
        Vertex depot = vertexes.get(0);
        vertexes.put(vertexes.size(), depot);

        vertexNum = vertexes.size();

        distMatrix = new double[vertexNum][vertexNum];
        timeMatrix = new double[vertexNum][vertexNum];
        setDistAndTimeMatrix();

        bfr.close();
    }

    public Vertex getVertexByIndex(int index) {
        return vertexes.get(index);
    }

    private void setDistAndTimeMatrix() {
        for (Map.Entry<Integer, Vertex> first : vertexes.entrySet()) {
            int index1 = first.getKey();
            Vertex vertex1 = first.getValue();

            for (Map.Entry<Integer, Vertex> second : vertexes.entrySet()) {
                int index2 = second.getKey();
                Vertex vertex2 = second.getValue();

                if (index1 == vertexNum - 1 || index2 == 0) {
                    distMatrix[index1][index2] = Double.MAX_VALUE;
                } else {
                    distMatrix[index1][index2] = vertex1.getDistanceTo(vertex2);
                }

                timeMatrix[index1][index2] = distMatrix[index1][index2] / vehicle.getSpeed();
            }

        }

    }
    
    public double[][] getDistMatrix() {
        return distMatrix;
    }
    
    public double[][] getTimeMatrix() {
        return timeMatrix;
    }
    
    public double getDistanceBetween(int firstIndex, int secondIndex) {
        return distMatrix[firstIndex][secondIndex];
    }

    public double getTimeBetween(int firstIndex, int secondIndex) {
        return timeMatrix[firstIndex][secondIndex];
    }

    public int getVehNum() {
        return vehNum;
    }

    public Vehicle getVehicle() {
        return vehicle;
    }

    public int getVertexNum() {
        return vertexNum;
    }
}

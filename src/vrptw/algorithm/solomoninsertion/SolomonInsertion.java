package vrptw.algorithm.solomoninsertion;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import vrptw.parameter.Parameters;
import vrptw.problem.Vertex;
import vrptw.problem.Vrptw;
import vrptw.solution.Path;

/**
 * Use Solomon I1 Algorithm to generate initial routes.
 * 
 * @author Xiong Wangqi
 * @version V1.0
 * @since JDK1.8
 */
public class SolomonInsertion {
    /** VRPTW instance，对时间窗进行分支的过程中 time window 会更新，并且需要调用 Solomon Insertion 生成初始解. */
    static Vrptw vrptwIns;
    
    /** 时间矩阵，对 Arc 进行分支的过程中 timeMatrix 会更新，并且需要调用 Solomon Insertion 生成初始解. */
    static double[][] timeMatrix;
    
    private int cusNum;
    private int vertexNum;
    
    private int routeNum;
    private ArrayList<Route> routes;
    
    /** is the vertex visited. */
    private boolean[] isVisited;
    
    /**
     * Create a Instance SolomonInsertion.
     * 
     * @param vrptwIns VRPTW instance
     */
    public SolomonInsertion(Vrptw vrptwIns, double[][] timeMatrix) {
        SolomonInsertion.vrptwIns = vrptwIns;
        SolomonInsertion.timeMatrix = timeMatrix;
        
        cusNum = vrptwIns.getCusNum();
        vertexNum = vrptwIns.getVertexNum();
        
        routes = new ArrayList<>(Parameters.INITIAL_CAPACITY);
        
        // false default，为了简化编程，把 depot 也放进来了且默认为 true
        isVisited = new boolean[vrptwIns.getVertexNum()];  
        isVisited[0] = true;
        isVisited[vertexNum - 1] = true;
    }
        
    /**
     * 用I1算法构造路径.
     * 
     * @return 无可行路径则返回 False
     */
    public boolean constructRoutes() {
        // TODO 对于 Branch and Bound 中分支节点无初始可行解的尝试，需要下一阶段的优化
        // 为不能第一个被访问、不能最后一个被访问的客户生成路径，可能存在同一个客户在多条路径上的情况
        for (int i = 1; i < vertexNum - 1; i++) {
            Route r;
            if (timeMatrix[0][i] == Double.MAX_VALUE) {
                if (timeMatrix[i][vertexNum - 1] == Double.MAX_VALUE) {
                    // 不能第一个被访问，也不能最后一个访问，找一条形如 startDepot-v1-i-v2-endDepot 的可行路径
                    r = this.generateRouteForMiddleCus(i);
                } else {
                    // 不能第一个被访问，找一条形如 startDepot-v1-i-endDepot 的可行路径
                    r = this.generateRouteWithForLastCus(i);
                }
                
                if (r == null) {
                    return false;
                }
                
                this.addRoute(r);
                routeNum++;
                continue;
            }
            
            if (timeMatrix[i][vertexNum - 1] == Double.MAX_VALUE) {
                // 不能最后一个被访问，找一条形如 startDepot-v1-i-endDepot 的可行路径
                r = this.generateRouteWithForFirstCus(i);
                
                if (r == null) {
                    // 无解
                    return false;
                }
                this.addRoute(r);
                routeNum++;
            }
            
        }
        
        int visitedCusNum = 0;
        visitedCusNum = this.getVisitedCusNum();
        Random rand = new Random(Parameters.RANDOM_SEED);
        
        // 准备构造一条新的路径，在剩余未被服务的客户中随机选择一个客户
        int randInt = 1 + rand.nextInt(cusNum - visitedCusNum);
        this.createRouteForSeedCus(randInt);
        
        // 核心步骤，依次构造每一条路径
        while (true) {
            // 判断是否所有的客户都已分配
            visitedCusNum = this.getVisitedCusNum();
            if (visitedCusNum == cusNum) {
                break;
            }
            
            // 得到最优插入顾客和插入位置
            BestInsert bestInsert = this.getBestInsertCusInfo();
            if (bestInsert.bestCusId == -1 || bestInsert.bestInsertPos == -1) {
                // 准备构造一条新的路径，在剩余未被服务的客户中随机选择一个客户
                randInt = 1 + rand.nextInt(cusNum - visitedCusNum);
                this.createRouteForSeedCus(randInt);
                
            } else {
                Route r = routes.get(routeNum - 1);
                r.insertCustomer(bestInsert.bestInsertPos, bestInsert.bestCusId);
                isVisited[bestInsert.bestCusId] = true;
            }
            
        }
        
        // 为所有路径添加 end depot
        routes.forEach(route -> route.addEndDepot());
        
        return true;
    }
    
    /**
     * 获取 Solomon Insertion Algorithm 生成的初始路径.
     * @return
     */
    public Path[] getPaths() {
        Path[] paths = new Path[routeNum];
        for (int i = 0; i < routeNum; i++) {
            List<Integer> ids = routes.get(i).getVertexIds();
            paths[i] = new Path(vrptwIns, ids);
        }
        
        return paths;
    }
    
    public double getCost() {
        return routes.stream().mapToDouble(route -> route.getCost()).sum(); 
    }
    
    /**
     * 输出 Solomon Insertion Algorithm 生成的初始解.
     */
    public void output() {
        double totalCost = routes.stream().mapToDouble(route -> route.getCost()).sum();
        
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("总成本为 %.3f，使用车辆 %d 辆:\n", totalCost, routeNum));
        
        for (int i = 0; i < routeNum; i++) {
            if (!routes.get(i).isFeasible()) {
                return;
            }
        }
        
        for (Route r: routes) {
            sb.append(r.toString() + "\n");
        }
        
        System.out.println(sb.toString());
    }
    
    private void createRouteForSeedCus(int randInt) {        
        int count = 0;
        int seedCusId = -1;
        for (int id = 1; id < vertexNum - 1; id++) {
            if (isVisited[id]) {
                continue;
            }
            
            // 找到第 randInt 个未被服务的客户
            count++;
            if (count == randInt) {
                seedCusId = id;
                break;
            }
        }
        
        // add seed customer
        Route r = new Route();
        // add seed customer
        r.addVertexToEnd(seedCusId);
        isVisited[seedCusId] = true;
        
        routes.add(r);
        routeNum++;
    }
    
    private int getVisitedCusNum() {
        int visitedCusNum = 0;
        for (int i = 1; i <= cusNum; i++) {
            if (isVisited[i] == true) {
                visitedCusNum++;
            }
        }
        
        return visitedCusNum;
    }
    
    /**
     * 针对当前路径，在未被服务的客户中选出最佳插入客户及插入位置信息，插入位置 = 0 表示插入到depot之后.
     * 
     * @return 最佳的插入客户的 ID 和插入位置
     */
    private BestInsert getBestInsertCusInfo() {
        int bestCusId = -1;
        int bestInsertPos = -1;
        
        double saving2 = 0;
        double maxSaving = -100000000;
        
        // 0 为 start depot，vertexNum - 1 为 end depot(dummy)
        for (int cusId = 1; cusId < vertexNum - 1; cusId++) {
            if (isVisited[cusId]) {
                continue;
            }
            
            //获得该顾客的最佳插入位置
            int currCusbestPos = this.getBestInsertPos(cusId);
            if (currCusbestPos == -1) {
                continue;
            }
            
            saving2 = this.getSaving2(currCusbestPos, cusId);
            if (saving2 > maxSaving) {
                maxSaving = saving2;
                bestCusId = cusId;
                bestInsertPos = currCusbestPos;
            }
        }
        
        return new BestInsert(bestInsertPos, bestCusId);
    }
    
    /**
     * 获得给定客户的最佳插入位置，pos = 0 表示插入到depot之后，如果没有可行的插入位置返回 -1.
     * 
     * @param cusId 待插入的客户的 ID
     * @return 最佳插入位置
     */
    private int getBestInsertPos(int cusId) {
        int bestPos = -1;
        
        double cost1 = 0;
        double minSaving = 100000000;
        
        int totalPosNum = routes.get(routeNum - 1).getCusNum();
        for (int pos = 0; pos <= totalPosNum; pos++) {
            cost1 = this.getCost1(pos, cusId);
            if (cost1 < minSaving) {
                minSaving = cost1;
                bestPos = pos;
            }
            
        }
        
        return bestPos;
    }
    
    /**
     * 获得费用节省值1，将某客户插入到当前线路上的某位置时，空间距离节省值和等待时间节省值的加权求和.
     * 
     * @param pos 待插入位置
     * @param cusId 待插入客户的 ID
     * @return
     */
    private double getCost1(int pos, int cusId) {
        // 取当前路径
        Route r = routes.get(routeNum - 1);
        Vertex cus = vrptwIns.getVertexes().get(cusId);
        
        // 载重约束，插入不可行则返回一个足够大的值
        if (cus.getDemand() + r.getLoad() > vrptwIns.getVehicle().getCapacity()) {
            return Double.MAX_VALUE;
        }
        // 满足容量约束，接下来计算插入费用（如不满足时间窗约束，则返回一个足够大的费用）
        
        int preVertexId = -1;
        int nextVertexId = -1;
        if (pos == r.getCusNum()) {
            // 插入到最后一个顾客和depot之间
            preVertexId = r.getVertexIds().get(r.getCusNum());
            nextVertexId = vertexNum - 1;
        } else {
            preVertexId = r.getVertexIds().get(pos);
            nextVertexId = r.getVertexIds().get(pos + 1);
        }
        
        if (!r.isInsertFeasibleOnTw(pos, cusId)) {
            return Double.MAX_VALUE;
        }
        
        // 记录在插入新客户前，插入位置后面的第一个顾客的开始服务时间
        double nextCusStartTimeBefore = r.getVertexStartTime(pos + 1);
        // 插入顾客，计算等待时间
        r.insertCustomer(pos, cusId);
        
        // 记录在插入新客户后，插入位置后面的第一个顾客的开始服务时间（现在的位置为 pos + 2）
        double nextCusStartTime = r.getVertexStartTime(pos + 2);
        
        if (pos == r.getCusNum()) {
            r.removeCustomer(r.getCusNum());
        } else {
            // 计算完毕后删除该顾客，注意客户插入后对应的位置为 pos + 1
            r.removeCustomer(pos + 1);
        }
        
        double[][] distMatrix = vrptwIns.getDistMatrix();
        double cost11 = distMatrix[preVertexId][cusId] + distMatrix[cusId][nextVertexId] 
                - Parameters.MIU * distMatrix[preVertexId][nextVertexId];
        
        double cost1 = Parameters.ALPHA1 * cost11 + Parameters.ALPHA2 * (nextCusStartTime - nextCusStartTimeBefore);
        
        return cost1;
    }
    
    /**
     * 获得费用节省值2，将某客户插入到线路某位置时，总的节省值，即 depot 到该客户的距离减去 saving1.
     * 
     * @param cusId 待插入客户的序号
     * @param pos 待插入位置
     * @return 费用节省值2
     */
    private double getSaving2(int pos, int cusId) {
        double cost1 = this.getCost1(pos, cusId);
        if (cost1 == Double.MAX_VALUE) {
            return Double.NEGATIVE_INFINITY;
        }
        
        double saving2 = Parameters.LAMBDA * vrptwIns.getDistMatrix()[0][cusId] - cost1;
        return saving2;
    }
        
    /**
     * 最优插入类，记录最佳插入客户和对应的插入位置.
     * 
     * @author Xiong Wangqi
     * @version V1.0
     * @since JDK1.8
     */
    private class BestInsert {
        final int bestInsertPos;
        final int bestCusId;
        
        BestInsert(int bestInsertPos, int bestCusId) {
            this.bestInsertPos = bestInsertPos;
            this.bestCusId = bestCusId;
        }
        
    }
    
    /**
     * 暴力搜索生成形如 startDepot-v1-cusId-v2-endDepot 的可行路径.
     * 
     * @param cusId 给定的客户 ID
     * @return 生成的路径，如果不存在这样的路径，返回 null
     */
    private Route generateRouteForMiddleCus(int cusId) {
        Route r = new Route();
        // startDepot-cusId
        r.addVertexToEnd(cusId);
        
        for (int i = 1; i < vertexNum - 1; i++) {
            // startDepot-v1-cusId
            r.insertCustomer(0, i);
            
            for (int j = 1; j < vertexNum - 1; j++) {
                if (i == j) {
                    continue;
                }
                
                // startDepot-v1-cusId-v2
                r.addVertexToEnd(j);
                if (r.isFeasible()) {
                    isVisited[cusId] = true;
                    isVisited[i] = true;
                    isVisited[j] = true;
                    return r;
                }
                
                r.removeCustomer(r.getCusNum());
            }
            r.removeCustomer(1);
        }
        
        return null;
    }
    
    /**
     * 暴力搜索生成形如 startDepot-v1-cusId-endDepot 的可行路径.
     * 
     * @param cusId 给定的客户 ID
     * @return 生成的路径，如果不存在这样的路径，返回 null
     */
    private Route generateRouteWithForLastCus(int cusId) {
        Route r = new Route();
        // startDepot-cusId
        r.addVertexToEnd(cusId);
        
        for (int i = 1; i < vertexNum - 1; i++) {
            // startDepot-v1-cusId
            r.insertCustomer(0, i);
            if (r.isFeasible()) {
                isVisited[cusId] = true;
                isVisited[i] = true;
                return r;
            }
            r.removeCustomer(1);
        }
        
        return null;
    }
    
    /**
     * 暴力搜索生成形如 startDepot-cusId-v1-endDepot 的可行路径.
     * 
     * @param cusId 给定的客户 ID
     * @return 生成的路径，如果不存在这样的路径，返回 null
     */
    private Route generateRouteWithForFirstCus(int cusId) {
        Route r = new Route();
        // startDepot-cusId
        r.addVertexToEnd(cusId);
        
        for (int id = 1; id < vertexNum - 1; id++) {
            // startDepot-v1-cusId
            r.addVertexToEnd(id);
            if (r.isFeasible()) {
                isVisited[cusId] = true;
                isVisited[id] = true;
                return r;
            }
            r.removeCustomer(1);
        }
        
        return null;
    }
    
    private void addRoute(Route r) {
        routes.add(r);
        List<Integer> ids = r.getVertexIds();
        for (int id: ids) {
            isVisited[id] = true;
        }
    }
        
}

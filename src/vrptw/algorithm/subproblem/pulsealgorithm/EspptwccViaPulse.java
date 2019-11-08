package vrptw.algorithm.subproblem.pulsealgorithm;

import java.util.ArrayList;
import java.util.Map;

import vrptw.algorithm.subproblem.AbstractPriceProblem;
import vrptw.parameter.Parameters;
import vrptw.problem.Vertex;
import vrptw.problem.Vrptw;
import vrptw.solution.Path;

/**
 * Solve a SPPTWCC via Pulse Algorithm, <br>
 * see Leonardo Lozano, et al. (2015): <br>
 * An Exact Algorithm for the Elementary Shortest Path Problem with Resource Constraints, <br>
 * The code is based on https://github.com/dengfaheng/CGVRPTW/tree/master/CGVRPTW_DFH
 * 
 * @author Xiong Wangqi
 * @version V1.0
 * @since JDK1.8
 */
public class EspptwccViaPulse extends AbstractPriceProblem {
    /** 用于记录 Pulse Algorithm 中各个节点的信息. */
    private PulseVertex[] pulseVertexes;
    
    /** 存储所有的到达了 end depot 且 revised cost 最小的 path. */
    private ArrayList<ArrayList<Integer>> allFinalShortestPaths;
    
    /** lowest bound found in bound scheme without considering initial demand at start node. */
    private double relaxationBound;
    /** minimum of cost/time ratio among all arcs. */
    private double naiveBound;
    
    /** time limit lower bound in bound scheme. */
    private double timeLimitLb;
    /** time step in bound scheme. */
    private double timeStep;
    /** every node have an initial time consumption in each iteration of bound scheme. */
    private double initialTimeConsumption;
    
    /** max time index to store the bound matrix. */
    private int maxTimeIndex;
    
    /** threads for pulse. */
    private Thread[] threads;
    private int threadNum;

    /**
     * Create a Instance EspptwccViaPulse.
     * 
     * @param vrptwIns VRPTW 算例
     */
    public EspptwccViaPulse(Vrptw vrptwIns) {
        super(vrptwIns);
                
        timeStep = Parameters.TIME_STEP;
        timeLimitLb = Parameters.TIME_LIMIT_LB;
        
        double timeUb = vrptwIns.getVertexes().get(vertexNum - 1).getLatestTime();
        timeUb += timeStep;
        timeUb -= timeUb % timeStep;
        maxTimeIndex = (int) (timeUb / timeStep);
        // iteration times in bound scheme
        int boundNum = (int) ((timeUb - timeLimitLb) / timeStep) + 1;
       
        pulseVertexes = new PulseVertex[vertexNum];        
        int count = 0;
        for (Vertex v: vrptwIns.getVertexes()) {
            pulseVertexes[count] = new PulseVertex(v, boundNum);
            count++;
        }
        
        allFinalShortestPaths = new ArrayList<>(Parameters.INITIAL_CAPACITY);
        
        threadNum = Parameters.THREAD_NUM;
        threads = new Thread[threadNum];
        for (int i = 0; i < threadNum; i++) {
            threads[i] = new Thread();
        }
        
    }
    
    @Override
    public void solve(Map<Integer, Double> dualValues, double[][] timeMatrix) {
        // 清空 bundMarix，准备下次调用
        this.reset();
        
        this.timeMatrix = timeMatrix;
        this.updateDistAndCostMatrix(dualValues);
        
        // Step 1: Bound Scheme
        boundingScheme();
        
        // 恢复 PulseVertex 的 isVisited[0] = false，准备开始 Pulse
        for (int i = 0; i < vertexNum; i++) {
            pulseVertexes[i].isVisited[0] = false;
        }
        
        // Step 2: Pulse from start depot
        // set initialTimeConsumption to the last value solved
        this.initialTimeConsumption += this.timeStep;
        // propagate the pulse from the start depot
        this.pulse(0, 0, 0, 0, 0, new ArrayList<Integer>());
        
        // Step 3: get shortest path
        for (ArrayList<Integer> vertexIds : allFinalShortestPaths) {
            this.shortestPaths.add(new Path(vrptwIns, vertexIds));
        }
    }
    
    @Override
    protected void reset() {
        // 重置 pulseVertexes 信息，包括各个节点的 minCost 和 bound matrix
        for (int i = 0; i < vertexNum; i++) {
            pulseVertexes[i].reset();
        }
        
        // 重置 navieBound
        naiveBound = Double.MAX_VALUE;
        // 重置 relaxation Bound
        relaxationBound = 0;
        
        // 重置 initialTimeConsumption
        initialTimeConsumption = pulseVertexes[vertexNum - 1].latestTime;
        initialTimeConsumption += timeStep;
        initialTimeConsumption -= initialTimeConsumption % timeStep;

        // 清空 shortest path 信息
        this.revisedCostOfShortestPath = 0;
        this.allFinalShortestPaths.clear();
        this.shortestPaths.clear();
    }

    /**
     * Calculate bounds store the minimum revised cost that can be achieved, by any partial path P that
     * reaches node v[i] with a given amount of consumed resource. <br>
     */
    private void boundingScheme() {
        this.calNaiveBound();
        
        // index to store the bound matrix
        int timeIndex = 0;
        
        while (initialTimeConsumption >= timeLimitLb) {
            timeIndex = (int) Math.ceil(initialTimeConsumption / timeStep);
            
            for (int currVertexId = 0; currVertexId < vertexNum - 1; currVertexId++) {
                // 从 ID = i 的节点处发出脉冲
                pulseInBound(currVertexId, 0, 0, initialTimeConsumption, new ArrayList<Integer>(), currVertexId);

                // 更新该节点的 bound
                PulseVertex v = pulseVertexes[currVertexId];
                v.bounds[maxTimeIndex - timeIndex] = v.minCost;
            }
            
            initialTimeConsumption -= timeStep;
        }

    }

    /**
     * Pulse for bound scheme.
     * 
     * @param currVertexId 当前节点 ID
     * @param cost         到达当前节点的 revised Cost
     * @param demand       到达当前节点时已服务的需求量
     * @param time         到达当前节点的时间
     * @param partialPath  路径上的节点（不包含 rootVertexId）
     * @param rootVertexId 路径的出发节点
     */
    private void pulseInBound(int currVertexId, double cost, double demand, double time, ArrayList<Integer> partialPath,
            int rootVertexId) {
        PulseVertex currPulseVertex = pulseVertexes[currVertexId];
        
        // check time window feasibility and cycle to prune
        if (time > currPulseVertex.latestTime || currPulseVertex.isVisited[0]) {
            return;
        }

        if (time < currPulseVertex.earliestTime) {
            time = currPulseVertex.earliestTime;
        }

        // check bound to prune
        if (calBoundPhase1(currVertexId, time, rootVertexId) + cost >= pulseVertexes[rootVertexId].minCost) {
            return;
        }

        // roll back to prune
        if (rollBack(currVertexId, cost, time, partialPath)) {
            return;
        }

        currPulseVertex.isVisited[0] = true;
        partialPath.add(currVertexId);

        // Propagate the pulse through all the outgoing arcs
        for (int j = 1; j < vertexNum; j++) {
            if (j == currVertexId) {
                continue;
            }

            double newCost = cost + revisedCostMatrix[currVertexId][j];
            // 注意增加的是 j 点的需求量
            double newDemand = demand + pulseVertexes[j].demand;
            double newTime = time + currPulseVertex.serviceTime + timeMatrix[currVertexId][j];

            // Check demand and time window feasibility
            if (newDemand > capacity || newTime > pulseVertexes[j].latestTime) {
                continue;
            }
            
            if (j == vertexNum - 1) {
                tryToUpdateRelaxationBound(newCost, newDemand, newTime, partialPath, rootVertexId);
            } else {
                pulseInBound(j, newCost, newDemand, newTime, partialPath, rootVertexId);
            }

        }

        // Remove the explored node from the path
        partialPath.remove(partialPath.size() - 1);
        currPulseVertex.isVisited[0] = false;
    }

    /**
     * Pulse stage.
     * 
     * @param threadId     线程 ID
     * @param currVertexId 当前节点 ID
     * @param cost         到达当前节点的 revised cost
     * @param demand       到达当前节点时已服务的需求量
     * @param time         到达当前节点的时间
     * @param partialPath  partialPath 路径上的节点（不包含 rootVertexId）
     */
    private void pulse(int threadId, int currVertexId, double cost, double demand, double time,
            ArrayList<Integer> partialPath) {

        PulseVertex currPulseVertex = pulseVertexes[currVertexId];
        if (time < currPulseVertex.earliestTime) {
            time = currPulseVertex.earliestTime;
        }

        // check cycle, bound and roll back to prune
        if (currPulseVertex.isVisited[threadId] 
                || calBoundPhase2(currVertexId, time) + cost >= revisedCostOfShortestPath
                || rollBack(currVertexId, cost, time, partialPath)) {
            return;
        }

        currPulseVertex.isVisited[threadId] = true;
        partialPath.add(currVertexId);

        // Propagate the pulse through all the outgoing arcs
        double newCost;
        double newDemand;
        double newTime;
        for (int j = 1; j < vertexNum; j++) {
            if (j == currVertexId) {
                continue;
            }
            
            newCost = cost + revisedCostMatrix[currVertexId][j];
            newDemand = demand + pulseVertexes[j].demand;
            newTime = time + currPulseVertex.serviceTime + timeMatrix[currVertexId][j];

            // Check demand and time window feasibility
            if (newDemand > capacity || newTime > pulseVertexes[j].latestTime) {
                continue;
            }

            if (j == vertexNum - 1) {
                tryToUpdateShortestPath(threadId, newCost, newDemand, newTime, partialPath);
                continue;
            }

            // If not in the start node continue the exploration on the current thread
            if (currVertexId != 0) {
                pulse(threadId, j, newCost, newDemand, newTime, partialPath);
                continue;
            }

            // If standing in the start node, wait for the next available thread to trigger the exploration
            boolean stopLooking = false;
            for (int t = 1; t < threadNum; t++) {
                if (!threads[t].isAlive()) {
                    threads[t] = new Thread(new PulseTask(t, j, newCost, newDemand, newTime, partialPath));
                    threads[t].start();

                    stopLooking = true;
                    break;
                }
            }

            if (!stopLooking) {
                try {
                    threads[1].join();
                    threads[1] = new Thread(new PulseTask(1, j, newCost, newDemand, newTime, partialPath));
                    threads[1].start();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

        }

        // Wait for all active threads to finish
        if (currVertexId == 0) {
            try {
                for (int t = 1; t < threadNum; t++) {
                    this.threads[t].join();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        // Remove the explored node from the path
        partialPath.remove(partialPath.size() - 1);
        currPulseVertex.isVisited[threadId] = false;
    }

    /**
     * set {@link #naiveBound} to be minimum of cost/time ratio among all arcs.
     */
    private void calNaiveBound() {
        for (int i = 0; i < vertexNum; i++) {
            for (int j = 0; j < vertexNum; j++) {
                double time = pulseVertexes[i].serviceTime + timeMatrix[i][j];
                if (time == 0) {
                    continue;
                }
                
                double temp = revisedCostMatrix[i][j] / time;
                if (temp <= this.naiveBound) {
                    this.naiveBound = temp;
                }
            }

        }

    }

    /**
     * Roll back pruning strategy.
     * 
     * @param currVertexId 当前节点
     * @param cost         到达当前节点的 revised cost
     * @param time         到达当前节点的时间
     * @param partialPath  给定路径（不包含 currVertexId）
     * @return 是否“剪枝”
     */
    private boolean rollBack(int currVertexId, double cost, double time, ArrayList<Integer> partialPath) {
        int size = partialPath.size();
        if (size <= 1) {
            return false;
        }
        
        int preVertexId = partialPath.get(size - 1);
        int directVertexId = partialPath.get(size - 2);
        
        double directCost = cost - revisedCostMatrix[preVertexId][currVertexId]
                - revisedCostMatrix[directVertexId][preVertexId] + revisedCostMatrix[directVertexId][currVertexId];
        if (directCost <= cost) {
            return true;
        }
        
        return false;
    }

    /**
     * Calculate a lower bound given a time consumption at a given node in bound scheme.
     * 
     * @param currVertexId 当前所在节点
     * @param time         到达当前节点的时间
     * @param rootVertexId 路径的出发节点
     * @return bound of currVertex，即 rootVertex 之后的路径的理想成本
     */
    private double calBoundPhase1(int currVertexId, double time, int rootVertexId) {
        double bound = 0;
        // 在bound scheme 的每一次迭代中，id 小的节点的 bound 会先计算完成
        // 每次迭代每个点的初始耗时都等于当前的 initialTimeConsumption 值，且相邻两次迭代的节点初始耗时相差 timeStep
        if (currVertexId >= rootVertexId && time < initialTimeConsumption + timeStep) {
            // currVertex 的 bound 还未计算完成，且到达当前节点的时间较小
            // bound = 后续路径的理想成本 + 目前得到的最优路径 revised cost
            bound = (initialTimeConsumption + timeStep - time) * naiveBound + relaxationBound;
        } else {
            // 到达当前节点的时间较大，并且 直接使用 bound scheme 的上一次迭代的得到的 bound 值
            // 或者 currVertex 的 bound 在之前的 bound scheme 迭代中已经完成
            int index = (int) (time / timeStep);
            bound = pulseVertexes[currVertexId].bounds[maxTimeIndex - index];
        }
        
        return bound;
    }

    /**
     * {@link #boundingScheme} 阶段找到了到达 end depot 的路径，尝试更新 {@link #revisedCostOfShortestPath}.
     * 
     * @param cost         到达 end depot 的 revised Cost
     * @param demand       到达 end depot 时已服务的需求量
     * @param time         到达 end depot 的时间
     * @param partialPath  路径上的节点（不包含 rootVertexId）
     * @param rootVertexId 路径的出发节点
     */
    private void tryToUpdateRelaxationBound(double cost, double demand, double time, ArrayList<Integer> partialPath,
            int rootVertexId) {
        // 是否可行
        if (demand > capacity || time > pulseVertexes[vertexNum - 1].latestTime) {
            return;
        }
        
        // 更新从 rootVertex 出发的最短路径的 revised cost
        if (cost < pulseVertexes[rootVertexId].minCost) {
            pulseVertexes[rootVertexId].minCost = cost;

            // 更新 relaxationBound
            if (cost < relaxationBound) {
                relaxationBound = cost;
            }
        }
        
    }

    /**
     * Calculate a lower bound given a time consumption at a given node in pulse.
     * 
     * @param currVertexId 当前所在节点
     * @param time         到达当前节点的时间
     * @return 当前节点的 bound，即当前节点之后的的剩余路径的理想成本
     */
    private double calBoundPhase2(int currVertexId, double time) {
        double bound = 0;
        // Pulse 阶段的 initialTimeConsumption 等于 bound scheme 阶段的最后一次迭代所使用的值
        // 需要注意的是每个节点不再设初始耗时，initialTimeConsumption 在此处只用于 boundsMatrix 的选择
        if (time < initialTimeConsumption) {
            bound = (initialTimeConsumption - time) * naiveBound + relaxationBound;
        } else {
            int index = (int) (time / this.timeStep);
            bound = pulseVertexes[currVertexId].bounds[maxTimeIndex - index];
        }
        
        return bound;
    }

    /**
     * {@link #pulse} 阶段找到了到达 end depot 的路径，尝试更新最短路径.
     * 
     * @param threadId    线程 ID
     * @param cost        到达 end depot 的 revised cost
     * @param demand      到达 end depot 时已经服务的需求量
     * @param time        到达 end depot 的时间
     * @param partialPath 路径上的节点（不包含 end depot ID）
     */
    private void tryToUpdateShortestPath(int threadId, double cost, double demand, double time,
            ArrayList<Integer> partialPath) {
        
        // 是否可行
        if (demand > capacity || time > pulseVertexes[vertexNum - 1].latestTime) {
            return;
        }

        if (cost < revisedCostOfShortestPath) {
            revisedCostOfShortestPath = cost;
            ArrayList<Integer> finalPath = new ArrayList<Integer>(partialPath);
            finalPath.add(vertexNum - 1);
            this.allFinalShortestPaths.clear();
            this.allFinalShortestPaths.add(finalPath);

            return;
        }

        // 记录所有 revised cost 最小的路径的节点信息
        if (cost == revisedCostOfShortestPath) {
            ArrayList<Integer> finalPath = new ArrayList<Integer>(partialPath);
            finalPath.add(vertexNum - 1);
            this.allFinalShortestPaths.add(finalPath);
        }
    }

    /**
     * Thread for pulse stage.
     * 
     * @author Xiong Wangqi
     * @version V1.0
     * @since JDK1.8
     */
    private class PulseTask implements Runnable {
        /** 线程索引. */
        int threadId;
        /** 当前节点 ID. */
        int currVertexId;
        /** 到达当前节点的 revised cost. */
        double cost;
        /** 到达当前节点时已服务的需求量. */
        double demand;
        /** 到达当前节点的时间. */
        double time;
        /** 路径经过的节点（不包含当前节点）. */
        ArrayList<Integer> partialPath;

        /**
         * Create a Instance PulseTask.
         * 
         * @param threadId     线程 ID
         * @param currVertexId 当前节点 ID
         * @param cost         到达当前节点的 revised cost
         * @param demand       到达当前节点时已服务的需求量
         * @param time         到达当前节点的时间
         * @param partialPath  路径经过的节点（不包含当前节点）
         */
        PulseTask(int threadId, int currVertexId, double cost, double demand, double time,
                ArrayList<Integer> partialPath) {
            this.threadId = threadId;
            this.currVertexId = currVertexId;
            this.cost = cost;
            this.demand = demand;
            this.time = time;
            this.partialPath = new ArrayList<>(partialPath);
        }

        @Override
        public void run() {
            // TODO Auto-generated method stub
            pulse(threadId, currVertexId, cost, demand, time, partialPath);
        }
    }

    /**
     * Pulse Vertex class for pulse algorithm.
     * 
     * @author Xiong Wangqi
     * @version V1.0
     * @since JDK1.8
     */
    private class PulseVertex {
        final double demand;
        final double serviceTime;
        final double earliestTime;
        final double latestTime;
        
        /** 对应 Vrptw 类中的节点. */
        /** 是否被访问过，在算法的 bound 阶段仅使用isVisited[0]，Pulse 阶段各元素分别对应不同的线程. */
        boolean[] isVisited;
        
        /** 记录从改点出发的“最短”路径对应的 revised cost. */
        double minCost;
        /** contains the lower bounds calculated for every discrete time step. */
        double[] bounds;

        PulseVertex(Vertex v, int boundNum) {
            demand = v.getDemand();
            serviceTime = v.getServiceTime();
            earliestTime = v.getEarliestTime();
            latestTime = v.getLatestTime();
            
            isVisited = new boolean[Parameters.THREAD_NUM];
            minCost = Double.MAX_VALUE;
            bounds = new double[boundNum];
        }
        
        void reset() {
            for (int i = 0; i < threadNum; i++) {
                isVisited[i] = false;
            }
            minCost = Double.MAX_VALUE;
            
            int boundNum = bounds.length;
            bounds = new double[boundNum];
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();

            sb.append(String.format("minRevisedCost %f, bounds [", minCost));
            for (double bound: bounds) {
                sb.append(bound + " ");
            }
            sb.append("]\n");

            return sb.toString();
        }
    }

}
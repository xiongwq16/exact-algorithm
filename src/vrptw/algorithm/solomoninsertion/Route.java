package vrptw.algorithm.solomoninsertion;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import vrptw.parameter.Parameters;
import vrptw.problem.Vertex;
import vrptw.problem.Vrptw;

/**
 * 路径.
 * 
 * @author Xiong Wangqi
 * @version V1.0
 * @since JDK1.8
 */
class Route {
    static Vrptw vrptwIns;
    
    /** 路径上的客户的数量. */
    private int cusNum;

    /** 路径上的总需求量. */
    private double load;
    /** 路径的实际成本. */
    private double cost;
    
    /** 途径各个节点的信息，包括车辆到达时间、等待时间、开始服务时间、结束服务（离开）时间. */
    private ArrayList<Activity> activities;
    
    /**
     * Create a Instance Path with start depot.
     * 
     * @param vrptwIns VRPTW instance
     */
    Route() {
        cusNum = 0;
        load = 0;
        cost = 0;

        activities = new ArrayList<>(Parameters.INITIAL_CAPACITY);
        // add start depot
        activities.add(new Activity(0, 0));
    }
    
    void addEndDepot() {
        int endDepotId = vrptwIns.getVertexNum() - 1;
        
        cost += vrptwIns.getDistMatrix()[activities.get(cusNum).vertexId][endDepotId];

        // 获取最后一个节点的信息
        Activity lastActivity = activities.get(cusNum);
        // 到达时间 = 离开上一个客户的时间 + 行驶时间getTimeBetween
        double arrTime = lastActivity.departTime + vrptwIns.getTimeMatrix()[lastActivity.vertexId][endDepotId];
        Activity newActivity = new Activity(endDepotId, arrTime);
        activities.add(newActivity);
    }

    /**
     * 将给定的客户加入到路径最后.
     * 
     * @param addVertexId 给定客户的 ID
     */
    void addVertexToEnd(int addVertexId) {
        // 注意返回配送中心的部分暂不计算
        cost += vrptwIns.getDistMatrix()[activities.get(cusNum).vertexId][addVertexId];
        load += vrptwIns.getVertexes().get(addVertexId).getDemand();
        
        // 获取最后一个节点的信息
        Activity lastActivity = activities.get(cusNum);
        // 到达时间 = 离开上一个客户的时间 + 行驶时间getTimeBetween
        double arrTime = lastActivity.departTime + vrptwIns.getTimeMatrix()[lastActivity.vertexId][addVertexId];
        Activity newActivity = new Activity(addVertexId, arrTime);
        activities.add(newActivity);

        cusNum++;
    }

    /**
     * 插入一个客户到指定位置，共有 cusNum + 1 个位置可选，<br>
     * 其中 0 表示插入到起始配送中心后面，cusNum 表示插入到返回配送中心前面.
     * 
     * @param pos      待插入位置
     * @param cusId 待插入客户的 ID
     */
    void insertCustomer(int pos, int cusId) {
        if (pos > cusNum) {
            throw new IllegalArgumentException("The insertion pos should be in [0, cusNum]");
        }

        // 插入到路径最后的情况（包括 cusNum = 0 的情况）
        if (pos == cusNum) {
            this.addVertexToEnd(cusId);
            return;
        }

        // 到达时间 = 离开上一个客户的时间 + 行驶时间
        Activity preActivity = activities.get(pos);
        double arrTime = preActivity.departTime + vrptwIns.getTimeMatrix()[preActivity.vertexId][cusId];
        Activity newActivity = new Activity(cusId, arrTime);
        
        // 删除一条弧，增加两条弧
        Activity nextActivity = activities.get(pos + 1);
        cost = cost - vrptwIns.getDistMatrix()[preActivity.vertexId][nextActivity.vertexId]
                + vrptwIns.getDistMatrix()[preActivity.vertexId][cusId]
                + vrptwIns.getDistMatrix()[cusId][nextActivity.vertexId];

        load += vrptwIns.getVertexes().get(cusId).getDemand();

        // 对于 ArrayLis 来说，实际上是添加到 pos 之后一个位置
        activities.add(pos + 1, newActivity);
        cusNum++;
        
        // pos + 1 位置的客户及其之前的客户的时间都不用再更新
        this.updateServicesFrom(pos + 1);
    }

    /**
     * 从路径上移除在 pos 处的客户（注意 pos 应从 1 开始，pos = 0 对应 depot）.
     * 
     * @param pos 对应客户在路径上的位置
     */
    void removeCustomer(int pos) {
        if (cusNum == 0) {
            throw new IllegalArgumentException(String.format("No removeing when the cusNum is %d", cusNum));
        }

        if (pos == 0 || pos > cusNum) {
            throw new IllegalArgumentException(String.format("The parameter \"ith\" should be in [1, %d]", cusNum));
        }

        int vertexToRemoveId = activities.get(pos).vertexId;
        int preVertexId = activities.get(pos - 1).vertexId;

        // 删除最后一个被服务的客户
        if (pos == cusNum) {
            // 删除一条弧
            cost = cost - vrptwIns.getDistMatrix()[preVertexId][vertexToRemoveId];
        } else {
            // 删除两条弧，增加一条狐
            int nextVertexId = activities.get(pos + 1).vertexId;
            cost = cost - vrptwIns.getDistMatrix()[preVertexId][vertexToRemoveId]
                    - vrptwIns.getDistMatrix()[vertexToRemoveId][nextVertexId]
                    + vrptwIns.getDistMatrix()[preVertexId][nextVertexId];
        }
        
        load -= vrptwIns.getVertexes().get(vertexToRemoveId).getDemand();
        
        activities.remove(pos);
        cusNum--;
        
        // pos - 1 位置的客户及其之前的客户的时间都是没有变化的
        this.updateServicesFrom(pos - 1);
    }

    /**
     * 判断将一个客户插入到指定位置后路径是否可行，共有 cusNum + 1 个位置可选，<br>
     * 其中 0 表示插入到起始配送中心后面，cusNum 表示插入到返回配送中心前面.
     * 
     * @param pos      待插入位置
     * @param cusId 待插入客户的 ID
     */
    boolean isInsertFeasibleOnTw(int pos, int cusId) {
        if (pos > cusNum) {
            throw new IllegalArgumentException(String.format("The insertion pos should be in [0, %d]", cusNum));
        }        
        
        Vertex cus = vrptwIns.getVertexes().get(cusId);
        Activity preActivity = activities.get(pos);
        double time = preActivity.departTime + vrptwIns.getTimeMatrix()[preActivity.vertexId][cusId];
        
        // 判断插入的客户本身的时间窗是否被违反
        if (time > cus.getLatestTime()) {
            return false;
        }
        
        // 模拟开始服务该客户的时间
        if (time < cus.getEarliestTime()) {
            time = cus.getEarliestTime();
        }
        
        // 模拟离开该客户的时间，准备验证其他客户和配送中心的时间窗
        time += cus.getServiceTime();
        
        // 继续验证
        for (int i = pos + 1; i <= cusNum; i++) {
            // 模拟到达 i 处客户的时间
            if (i == pos + 1) {
                time += vrptwIns.getTimeMatrix()[cusId][activities.get(pos + 1).vertexId];
            } else {
                time += vrptwIns.getTimeMatrix()[activities.get(i - 1).vertexId][activities.get(i).vertexId];
            }
            
            int currVertexId = activities.get(i).vertexId;
            Vertex currVertex = vrptwIns.getVertexes().get(currVertexId);
            
            if (time > currVertex.getLatestTime()) {
                return false;
            }
            
            // 被插入的后一个客户的到达时间小于其“最早服务时间”则后面所有客户的时间窗都不会违反
            if (time <= currVertex.getEarliestTime()) {
                return true;
            }
            
            // 模拟服务完成时间 - 离开时间
            time += currVertex.getServiceTime();
        }
        
        // 模拟返回配送中心的时间
        if (pos != cusNum) {
            time += vrptwIns.getTimeMatrix()[activities.get(cusNum).vertexId][vrptwIns.getVertexNum() - 1];
        } else {
            time += vrptwIns.getTimeMatrix()[cusId][vrptwIns.getVertexNum() - 1];
        }
        
        if (time > vrptwIns.getVertexes().get(vrptwIns.getVertexNum() - 1).getLatestTime()) {
            return false;
        }
        
        return true;
    }
        
    boolean isFeasible() {
        List<Integer> vertexIds = this.getVertexIds();

        double time = 0;
        double demand = 0;
        for (int i = 1; i <= cusNum; i++) {
            // 到达第 i 个客户的时间
            time += vrptwIns.getTimeMatrix()[vertexIds.get(i - 1)][vertexIds.get(i)];
            Vertex vertex = vrptwIns.getVertexes().get(vertexIds.get(i));
            
            demand += vertex.getDemand();
            if (demand > vrptwIns.getVehicle().getCapacity()) {
                System.out.println("Demand Infeasible");
                return false;
            }
            
            if (time > vertex.getLatestTime()) {
                System.out.println(i + "th Customer TimeWindow Infeasible");
                return false;
            }
            
            if (time < vertex.getEarliestTime()) {
                time = vertex.getEarliestTime();
            }
            
            // 离开第 i 个客户的时间
            time += vertex.getServiceTime();
        }
        
        // 模拟到达配送中心的时间
        time += vrptwIns.getTimeMatrix()[activities.get(cusNum).vertexId][vrptwIns.getVertexNum() - 1];
        if (time > vrptwIns.getVertexes().get(vrptwIns.getVertexNum() - 1).getLatestTime()) {
            System.out.println("EndDepot TimeWindow Infeasible");
            return false;
        }
        
        return true;
    }
    
    public List<Integer> getVertexIds() {
        return activities.stream().map(activity -> activity.vertexId).collect(Collectors.toList());
    }
    
    /**
     * 获取路径上 pos 处的节点的开始服务时间，注意 start depot 对应 0，end depot 对应 {@link #cusNum} + 1）.
     * 
     * @param pos 对应路径上的位置
     * @return 开始服务时间
     */
    double getVertexStartTime(int pos) {
        if (pos == cusNum + 1) {
            // 获取最后一个节点的信息
            Activity lastActivity = activities.get(cusNum);
            // 到达时间 = 离开上一个客户的时间 + 行驶时间getTimeBetween
            int endDepotId = vrptwIns.getVertexNum() - 1;
            return lastActivity.departTime + vrptwIns.getTimeMatrix()[lastActivity.vertexId][endDepotId];
        }

        return activities.get(pos).startTime;
    }

    /**
     * 更新 pos 之后访问的客户的服务信息.
     * 
     * @param pos 最后一个不用更新时间的客户的位置
     */
    private void updateServicesFrom(int pos) {
        if (pos > cusNum) {
            throw new IllegalArgumentException(String.format("The pos should be in [0, %d]", cusNum));
        }
        
        boolean isDepartTimeChanged = true;
        for (int i = pos + 1; i <= cusNum && isDepartTimeChanged; i++) {
            Activity preActivity = activities.get(i - 1);
            Activity currActivity = activities.get(i);
            
            double changedArrTime = preActivity.departTime
                    + vrptwIns.getTimeMatrix()[preActivity.vertexId][currActivity.vertexId];

            isDepartTimeChanged = currActivity.isDepartTimeChanged(changedArrTime);
        }

    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        
        this.getVertexIds().forEach(id -> sb.append(id + "-"));
        
        return sb.substring(0, sb.length() - 1).toString();
    }
    
    double getCost() {
        return cost;
    }
    
    int getCusNum() {
        return cusNum;
    }

    double getLoad() {
        return load;
    }

    /**
     * 经过某个节点的信息，包括车辆到达时间、等待时间、开始服务时间、结束服务（离开）时间.
     * 
     * @author Xiong Wangqi
     * @version V1.0
     * @since JDK1.8
     */
    private class Activity {
        int vertexId;
        /** 到达时间. */
        double arrTime;
        /** 开始服务时间. */
        double startTime;
        /** 结束服务时间，对于 VRPTW 来说也是离开时间. */
        double departTime;

        Activity(int vertexId, double arrTime) {
            this.vertexId = vertexId;
            this.setTimeInfo(arrTime);
        }
        
        /**
         * 根据改变后的到达时间更新服务信息，并判断离开时间是否改变.
         * 
         * @param changedArrTime 改变后的到达时间
         * @return 离开时间是否改变
         */
        boolean isDepartTimeChanged(double changedArrTime) {
            double departTimeBefore = this.departTime;
            
            setTimeInfo(changedArrTime);

            return departTimeBefore != this.departTime;
        }

        /**
         * 基于到达时间设置服务信息，包括开始服务信息、时间窗违反信息、结束服务信息.
         * 
         * @param preCumTwViolence 上一个节点活动对应的累计时间窗违反量
         */
        private void setTimeInfo(double arrTime) {            
            this.arrTime = arrTime;
            // 默认状态 - 在时间窗内到达
            this.startTime = arrTime;

            // 早于时间窗开启时刻到达，需等待
            Vertex v = vrptwIns.getVertexes().get(vertexId);
            if (arrTime <= v.getEarliestTime()) {
                this.startTime = v.getEarliestTime();
            }
            
            this.departTime = this.startTime + v.getServiceTime();
        }
        
        @Override
        public String toString() {
            return String.format("%.3f-%.3f-%.3f(%.3f-%.3f)", arrTime, startTime, departTime, 
                    vrptwIns.getVertexes().get(vertexId).getEarliestTime(), 
                    vrptwIns.getVertexes().get(vertexId).getLatestTime());
        }

    }

}

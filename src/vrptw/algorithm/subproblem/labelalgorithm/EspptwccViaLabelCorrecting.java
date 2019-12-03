package vrptw.algorithm.subproblem.labelalgorithm;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

import vrptw.algorithm.subproblem.AbstractPriceProblem;
import vrptw.parameter.Parameters;
import vrptw.problem.Vertex;
import vrptw.problem.Vrptw;
import vrptw.solution.Path;

/**
 * Solve an ESPPTWCC via dynamic programming labeling approach, <br>
 * see "3.4 Description of the Algorithm"in Feillet et al. (2006): <br>
 * An Exact Algorithm for the Elementary Shortest Path Problem with Resource Constraints: <br>
 * Application to Some Vehicle Routing Problems.
 * 
 * @author Xiong Wangqi
 * @version V1.0
 * @since JDK1.8
 */
public class EspptwccViaLabelCorrecting extends AbstractPriceProblem implements LabelAlgorithm {
    private Vertex[] vertexes;
    
    /** 待处理的节点队列. */
    private Queue<Integer> vertexeToTreat;
    /** set of labels extended from vertex chosen to its successors. */
    private ArrayList<EsppcctwLabel> labelExtendedFromCurrToNext;
    
    /** labels on every vertex，外层索引对应节点 ID. */
    private ArrayList<ArrayList<AbstractLabel>> labelList;
    
    /**
     * Create a Instance ESPPTWCC.
     * 
     * @param vrptwIns VRPTW 问题实例
     */
    public EspptwccViaLabelCorrecting(Vrptw vrptwIns) {
        super(vrptwIns);
        vertexeToTreat = new LinkedList<>();
        
        vertexes = new Vertex[vertexNum];
        for (int i = 0; i < vertexNum; i++) {
            vertexes[i] = vrptwIns.getVertexes().get(i);
        }
        
        labelList = new ArrayList<>((int) (vertexNum / Parameters.LOADER_FACTOR) + 1);
        for (int i = 0; i < vertexNum; i++) {
            labelList.add(new ArrayList<>(Parameters.INITIAL_CAPACITY));
        }
    }
    
    @Override
    public void updateVrptwIns(Vrptw newVrptwIns) {
        vertexes = new Vertex[vertexNum];
        for (int i = 0; i < vertexNum; i++) {
            vertexes[i] = newVrptwIns.getVertexes().get(i);
        }
    }
        
    /**
     * Solve an ESPPTWCC via dynamic programming labeling approach: <br>
     * Step 0: Initialization <br>
     * Step 1: Selection of the vertex to be treated <br>
     * Step 2: Exploration of the successor of the current vertex <br>
     * Step 3: add non-dominated labels to labelList add update vertexToTreat <br>
     * Step 4: Filtering.
     * 
     * @param lambda dual values
     */
    @Override
    public void solve(Map<Integer, Double> lambda) {
        // 清空 vertexeToTreat，labelList
        this.reset();
        
        this.updateDistAndCostMatrix(lambda);
        
        // Step 0: Initialization
        EsppcctwLabel initialLabel = new EsppcctwLabel(0, 0, 0, 0);
        labelList.get(0).add(initialLabel);
        vertexeToTreat.offer(0);
        
        while (!vertexeToTreat.isEmpty()) {
            // Step 1: Selection of the vertex to be treated, I choose FIFO rules
            int currVertexId = vertexeToTreat.poll();
            
            labelExtendedFromCurrToNext = new ArrayList<>(Parameters.INITIAL_CAPACITY);
            for (int j = 0; j < vertexNum; j++) {
                // Step 2: Exploration of the successor for all label on current vertex
                // all vertexes except the arc with Double.Max_Value
                if (timeMatrix[currVertexId][j] == Double.MAX_VALUE) {
                    continue;
                }
                for (AbstractLabel label : labelList.get(currVertexId)) {
                    // Extend to the reachable vertexes
                    this.labelExtension(label, j);
                }
                
                // Step 3: add non-dominated labels to labelList add update vertexToTreat
                for (EsppcctwLabel labelExtended : labelExtendedFromCurrToNext) {
                    this.useDominanceRules(labelExtended);
                }
                
                // Clear the set of labels extended from vertex chosen to its successors
                labelExtendedFromCurrToNext.clear();
            }
            
        }
        
        // Step 4: Filtering
        ArrayList<AbstractLabel> optLabels = this.filtering(labelList.get(vertexNum - 1));
        
        // 设置最短路径信息
        this.revisedCostOfShortestPath = optLabels.get(0).cost;
        for (AbstractLabel label: optLabels) {
            ArrayList<Integer> vertexIds = label.getVisitVertexes();
            this.shortestPaths.add(new Path(vrptwIns, vertexIds));
        }
        
    }
    
    @Override
    public void labelExtension(AbstractLabel currLabel, int nextVertexId) {
        if (!(currLabel instanceof EsppcctwLabel)) {
            throw new IllegalArgumentException("输入标签类型错误，请校验！");
        }
        
        // Extend to the reachable vertexes
        EsppcctwLabel currentLabel = (EsppcctwLabel) currLabel;
        
        if (currentLabel.isVertexUnreachable[nextVertexId]) {
            return;
        }
        
        // whether the extension is feasible
        double demand = currentLabel.demand + vertexes[nextVertexId].getDemand();
        
        // Attention: add service time
        double time = currentLabel.time + vertexes[currentLabel.vertexId].getServiceTime()
                + timeMatrix[currentLabel.vertexId][nextVertexId];
        
        if (time < vertexes[nextVertexId].getEarliestTime()) {
            time = vertexes[nextVertexId].getEarliestTime();
        }
        
        double cost = currentLabel.cost + revisedCostMatrix[currentLabel.vertexId][nextVertexId];
        EsppcctwLabel labelExtended = new EsppcctwLabel(cost, time, demand, nextVertexId, currentLabel);
        
        this.labelExtendedFromCurrToNext.add(labelExtended);
    }
    
    @Override
    public void useDominanceRules(AbstractLabel labelToCompare) {
        if (!(labelToCompare instanceof EsppcctwLabel)) {
            throw new IllegalArgumentException("输入标签类型错误，请校验！");
        }
        
        int currVertexId = labelToCompare.vertexId;
        ArrayList<AbstractLabel> labels = labelList.get(currVertexId);
        
        // Is the labels on current vertex changed?
        boolean isLabelsChanged = false;
        
        // whether the new label dominates or dominated by other labels
        boolean isLabelToCompareDominatedByOther = false;

        // labelToCompare 是否可能被接下来的标签“优超”
        boolean isPossibleDominatedByNextLabel = true;
        
        // 注意不要在 forEach 循环中使用 remove
        Iterator<AbstractLabel> iterator = labels.iterator();
        while (iterator.hasNext()) {
            AbstractLabel other = iterator.next();
                        
            if (labelToCompare.dominate(other)) {
                // 排除两者相等的情况
                if (isPossibleDominatedByNextLabel && labelToCompare.equals(other)) {
                    return;
                }
                
                /*
                 * label 是按顺序添加的，留下来的 label 都是没有被后面的 label “优超” 的，
                 * 也就是说排在后面的 lable 至少有一项资源是大于当前的 label 的；
                 * 
                 * 而 labelToCompare “优超”当前的 label 说明其所有资源都小于当前的 label，
                 * 所以 labelToCompare 不可能被更后面的 label “优超”
                 */
                isPossibleDominatedByNextLabel = false;
                
                iterator.remove();
                isLabelsChanged = true;
            }
            
            if (isPossibleDominatedByNextLabel && other.dominate(labelToCompare)) {
                isLabelToCompareDominatedByOther = true;
                /*
                 * label 是按顺序添加的，添加的前提就是不能被之前的label “优超”，
                 * 也就是说排在后面的 label 至少有一项资源是小于当前的 label 的；
                 * 
                 * 而 labelToCompare 被“优超” 说明其所有资源都大于当前的 label，
                 * 所以 labelToCompare 不可能“优超”更后面的 label，可以跳出循环。
                 */
                break;
            }

        }
        
        // add only if labelToCompare is non-dominated
        if (!isLabelToCompareDominatedByOther) {
            labels.add(labelToCompare);
            isLabelsChanged = true;
        }
        
        // Is current vertex not in vertexToTreat? Is the labels on current vertex j changed?
        if (!vertexeToTreat.contains(currVertexId) && isLabelsChanged) {
            vertexeToTreat.offer(currVertexId);
        }
        
    }
    
    @Override
    protected void reset() {
        this.vertexeToTreat.clear();
        // 清空 labelList 内层数组
        this.labelList.forEach(labels -> labels.clear());
        this.shortestPaths.clear();
    }
    
    /**
     * ESPPTWCC 专用 Label，增加两类资源：<br>
     * 1 访问过的节点的数量 <br>
     * 2 visitation vector，已经访问过的点为 1，未访问的为 0.
     * 
     * @author Xiong Wangqi
     * @version V1.0
     * @since JDK1.8
     */
    private class EsppcctwLabel extends AbstractLabel {
        /** 不可达的节点对应 true，可达对应 false. */
        boolean[] isVertexUnreachable;
        /** 不可达（资源约束不满足）的节点的数量，标签对应路径访问过的节点也不可达. */
        int unreachablenVertexNum;
        
        EsppcctwLabel(double cost, double time, double demand, int vertexId) {
            super(cost, time, demand, vertexId, null);
            
            // 默认值为 false
            isVertexUnreachable = new boolean[vertexNum];
            unreachablenVertexNum = 0;
            
            this.updateUnreachableVertexes();
        }
        
        EsppcctwLabel(double cost, double time, double demand, int vertexId, AbstractLabel preLabel) {
            super(cost, time, demand, vertexId, preLabel);
            
            if (!(this.preLabel instanceof EsppcctwLabel)) {
                throw new IllegalArgumentException("上一个标签类型错误，请校验！");
            }
            
            // 强制类型转换
            EsppcctwLabel label = (EsppcctwLabel) preLabel;
            
            // 需求量，时间都是 non-decreasing，并且访问过的节点不能再访问
            // 所以上一个标签不可达节点在当前标签中必然不可达
            this.isVertexUnreachable = new boolean[vertexNum];
            for (int j = 0; j < vertexNum; j++) {
                this.isVertexUnreachable[j] = label.isVertexUnreachable[j];
            }
            this.unreachablenVertexNum = label.unreachablenVertexNum;

            this.updateUnreachableVertexes();
        }
        
        /**
         * 相同起点和终点的两个标签之间的优超准则判别：<br>
         * 1. 各个“资源”情况 “this” 不大于 “other” <br>
         * 2. this 中访问过的节点是否包含了所有 other 中访问过的节点 <br>
         * 则 “this” 优超 “other”, 注意这里并未排除相等的情况，会在其他方法中考虑.
         * 
         * @param other 待比较的标签
         * @return 当前标签 “this” 是否"优超"给定标签 “other”
         */
        @Override
        boolean dominate(AbstractLabel other) {
            if (!(other instanceof EsppcctwLabel)) {
                throw new IllegalArgumentException("输入标签类型错误，请校验！");
            }
            
            // 对于到达终点 dummy end depot 的标签，只需要比较 cost
            if (this.vertexId == vertexNum - 1) {
                if (this.cost > other.cost) {
                    return false;
                }
                
                return true;
            }
            
            // 强制类型转换
            EsppcctwLabel that = (EsppcctwLabel) other;
            
            // 如果 this 的不可达节点数量大于 that ，则 this 没有“优超” that
            if (this.unreachablenVertexNum > that.unreachablenVertexNum) {
                return false;
            }
            
            // 比较成本、到达节点的时间、需求总量，只要 this 有一项大于 that，则 this 没有“优超” that
            if (this.demand > that.demand || this.cost > that.cost || this.time > that.time) {
                return false;
            }
            
            // 如果存在 this 不可达，但 that 可达的节点，则 this 没有“优超” that
            for (int i = 0; i < vertexNum; i++) {
                if (this.isVertexUnreachable[i] && !that.isVertexUnreachable[i]) {
                    return false;
                }
            }
            
            return true;
        }

        private void updateUnreachableVertexes() {
            // 当前节点本身不再可达
            isVertexUnreachable[vertexId] = true;
            unreachablenVertexNum++;
            
            // Are preLabel's reachable vertexes still reachable for current vertex?
            for (int j = 0; j < vertexNum; j++) {
                if (isVertexUnreachable[j]) {
                    continue;
                }
                
                // check capacity constraints
                double newDemand = demand + vertexes[j].getDemand();
                if (newDemand > capacity) {
                    isVertexUnreachable[j] = true;
                    unreachablenVertexNum++;
                    continue;
                }
                
                // check time window constraints
                double newTime = time + vertexes[vertexId].getServiceTime() + timeMatrix[vertexId][j];
                if (newTime > vertexes[j].getLatestTime()) {
                    isVertexUnreachable[j] = true;
                    unreachablenVertexNum++;
                }
            }

        }
                
    }
    
}

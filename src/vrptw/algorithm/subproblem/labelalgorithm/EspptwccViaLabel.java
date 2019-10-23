package vrptw.algorithm.subproblem.labelalgorithm;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;

import vrptw.algorithm.subproblem.AbstractSubProblem;
import vrptw.parameter.Parameters;
import vrptw.problem.Vrptw;

/**
 * Solve an ESPPTWCC via dynamic programming labeling approach <br>
 * see "3.4. Description of the Algorithm"in Feillet et al. (2006): <br>
 * An Exact Algorithm for the Elementary Shortest Path Problem with Resource Constraints: <br>
 * Application to Some Vehicle Routing Problems.
 * 
 * @author Xiong Wangqi
 * @version V1.0
 * @since JDK1.8
 */
public class EspptwccViaLabel extends AbstractSubProblem implements LabelAlgorithm {
    /** 待处理的节点队列. */
    private Queue<Integer> vertexeToTreat;
    /** Set of labels extended from vertex chosen to its successors. */
    private ArrayList<EsppcctwLabel> labelExtendedFromCurrToNext;

    /** labels on every vertex，外层索引对应节点索引. */
    ArrayList<ArrayList<AbstractLabel>> labelList;

    /**
     * Create a Instance EspptwccViaLabel.
     * 
     * @param vrptwIns VRPTW 问题实例
     * @param lambda   dual prices corresponding to sum(x[i][j][k] for j in V, k in K) = 1
     */
    public EspptwccViaLabel(Vrptw vrptwIns, double[] lambda) {
        super(vrptwIns, lambda);

        int initialCapcity = (int) (vertexNum / Parameters.LOADER_FACTOR) + 1;
        labelList = new ArrayList<>(initialCapcity);
        for (int i = 0; i < vertexNum; i++) {
            labelList.add(new ArrayList<>(Parameters.INITIAL_CAPACITY));
        }

        vertexeToTreat = new LinkedList<>();
    }

    /**
     * Solve an ESPPTWCC via dynamic programming labeling approach: <br>
     * Step 0: Initialization <br>
     * Step 1: Selection of the vertex to be treated <br>
     * Step 2: Exploration of the successor of the current vertex <br>
     * Step 3: add non-dominated labels to labelList add update vertexToTreat <br>
     * Step 4: Filtering.
     */
    @Override
    public void solve() {
        // Step 0: Initialization
        EsppcctwLabel initialLabel = new EsppcctwLabel(0, 0, 0, 0);
        labelList.get(0).add(initialLabel);
        vertexeToTreat.offer(0);

        while (!vertexeToTreat.isEmpty()) {
            // Step 1: Selection of the vertex to be treated, I choose FIFO rules
            int currVertexIndex = vertexeToTreat.poll();

            for (int j = 0; j < vertexNum; j++) {
                // Step 2: Exploration of the successor of the current vertex

                // Clear the set of labels extended from vertex chosen to its successors
                labelExtendedFromCurrToNext = new ArrayList<>(Parameters.INITIAL_CAPACITY);

                // for all labels on vertex i
                for (AbstractLabel label : labelList.get(currVertexIndex)) {
                    // Extend to the reachable vertexes
                    this.labelExtension(label, j);
                }

                // Step 3: add non-dominated labels to labelList add update vertexToTreat
                for (EsppcctwLabel labelExtended : labelExtendedFromCurrToNext) {
                    this.useDominanceRules(labelExtended);
                }

            }

        }

        // Step 4: Filtering
        AbstractLabel optLabel = this.filtering(labelList.get(vertexNum - 1));

        // 设置最短路径信息
        this.reducedCost = optLabel.getCost();
        ArrayList<Integer> vertexIndices = this.labelToVisitVertexes(optLabel);
        this.shortestPath = this.createPath(vertexIndices);
    }

    @Override
    public void labelExtension(AbstractLabel currLabel, int nextVertexIndex) {
        if (!(currLabel instanceof EsppcctwLabel)) {
            throw new IllegalArgumentException("输入标签类型错误，请校验！");
        }

        // Extend to the reachable vertexes
        EsppcctwLabel currentLabel = (EsppcctwLabel) currLabel;
        if (currentLabel.unreachableArray[nextVertexIndex] == 1) {
            return;
        }
        
        // whether the extension is feasible
        double demand = currentLabel.getDemand() + vrptwIns.getVertexByIndex(nextVertexIndex).getDemand();

        // Attention: add service time
        double time = currentLabel.getTime() + vrptwIns.getVertexByIndex(currentLabel.getVertex()).getServiceTime()
                + vrptwIns.getTimeMatrix()[currentLabel.getVertex()][nextVertexIndex];
        
        if (time < vrptwIns.getVertexByIndex(nextVertexIndex).getEarliestTime()) {
            time = vrptwIns.getVertexByIndex(nextVertexIndex).getEarliestTime();
        }

        double cost = currentLabel.getCost() + revisedCostMatrix[currentLabel.getVertex()][nextVertexIndex];
        EsppcctwLabel labelExtended = new EsppcctwLabel(cost, time, demand, nextVertexIndex, currentLabel);

        this.labelExtendedFromCurrToNext.add(labelExtended);
    }

    @Override
    public void useDominanceRules(AbstractLabel labelToCompare) {
        // TODO Auto-generated method stub
        if (!(labelToCompare instanceof EsppcctwLabel)) {
            throw new IllegalArgumentException("输入标签类型错误，请校验！");
        }

        int currVertexIndex = labelToCompare.getVertex();
        ArrayList<AbstractLabel> labels = labelList.get(currVertexIndex);

        // Is the labels on current vertex changed?
        boolean isLabelsChanged = false;

        // whether the new label dominates or dominated by other labels
        boolean isDominated = false;

        // labelToCompare 是否可能被接下来的标签“优超”
        boolean isPossibleDominatedByNextLabel = true;

        // 注意不要在 forEach 循环中使用 remove
        Iterator<AbstractLabel> iterator = labels.iterator();
        while (iterator.hasNext()) {
            AbstractLabel other = iterator.next();

            if (other.isDominatedBy(labelToCompare)) {
                /*
                 * label 是按顺序添加的，留下来的 label 都是没有被后面的 label “优超” 的，
                 * 也就是说排在后面的 lable 至少有一项资源是大于当前的 label 的；
                 * 
                 * 而labelToCompare “优超”当前的 label 说明其所有资源都小于当前的 label，
                 * 所以 labelToCompare 不可能被更后面的 label “优超”
                 */
                isPossibleDominatedByNextLabel = false;

                iterator.remove();
                isLabelsChanged = true;
            }

            if (isPossibleDominatedByNextLabel && labelToCompare.isDominatedBy(other)) {
                isDominated = true;
                /*
                 * label 是按顺序添加的，添加的前提就是不能被之前的label “优超”，
                 * 也就是说排在后面的 lable 至少有一项资源是小于当前的 label 的；
                 * 
                 * 而 labelToCompare 被“优超” 说明其所有资源都大于当前的 label，
                 * 所以 labelToCompare 不可能“优超”更后面的 label，可以跳出循环。
                 */
                break;
            }

        }

        // add only if labelToCompare is non-dominated
        if (!isDominated) {
            labels.add(labelToCompare);
            isLabelsChanged = true;
        }

        // Is current vertex not in vertexToTreat? Is the labels on current vertex j changed?
        if (!vertexeToTreat.contains(currVertexIndex) && isLabelsChanged) {
            vertexeToTreat.offer(currVertexIndex);
        }

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
        /** 不可达的节点对应 1，可达对应 0. */
        int[] unreachableArray;
        /** 不可达（资源约束不满足）的节点的数量，标签对应路径访问过的节点也不可达. */
        private int unreachablenVertexNum;

        EsppcctwLabel(double cost, double time, double demand, int vertexIndex) {
            super(cost, time, demand, vertexIndex, null);

            // int 默认值为 0
            unreachableArray = new int[vertexNum];
            unreachablenVertexNum = 0;

            this.updateUnreachableVertexes();
        }

        EsppcctwLabel(double cost, double time, double demand, int vertexIndex, AbstractLabel preLabel) {
            super(cost, time, demand, vertexIndex, preLabel);

            if (!(this.getPreLabel() instanceof EsppcctwLabel)) {
                throw new IllegalArgumentException("上一个标签类型错误，请校验！");
            }
            // 强制类型转换
            EsppcctwLabel label = (EsppcctwLabel) preLabel;

            // 需求量，时间都是 non-decreasing，并且访问过的节点不能再访问
            // 所以上一个标签不可达节点在当前标签中必然不可达
            this.unreachableArray = new int[vertexNum];
            for (int j = 0; j < vertexNum; j++) {
                this.unreachableArray[j] = label.unreachableArray[j];
            }
            this.unreachablenVertexNum = label.unreachablenVertexNum;

            this.updateUnreachableVertexes();
        }

        /**
         * 优超准则判别：<br>
         * 1. 各个“资源”情况 “this” <= “other” <br>
         * 2. this 中访问过的节点是否包含了所有 other 中访问过的节点 <br>
         * 则 “this” dominates “other”.
         * 
         * @param other 待比较的标签
         * @return 当前标签（this）是否优超给定标签 other
         */
        @Override
        boolean isDominatedBy(AbstractLabel other) {
            if (!(other instanceof EsppcctwLabel)) {
                throw new IllegalArgumentException("输入标签类型错误，请校验！");
            }

            // 强制类型转换
            EsppcctwLabel newOther = (EsppcctwLabel) other;

            // 比较成本、耗时、需求总量，只要 newOther 有一项大于 this，则 this 没有被“优超”
            if (newOther.getCost() > this.getCost() || newOther.getTime() > this.getTime()
                    || newOther.getDemand() > this.getDemand()) {
                return false;
            }

            // 如果 newOther 访问过的节点的数量如果少于 this ，则 this 没有被“优超”
            if (newOther.unreachablenVertexNum > this.unreachablenVertexNum) {
                return false;
            }

            // 如果存在 this 访问过，但 newOther 没有访问过的节点，则 this 没有被“优超”
            for (int i = 0; i < vertexNum; i++) {
                if (newOther.unreachableArray[i] > this.unreachableArray[i]) {
                    return false;
                }
            }

            return true;
        }

        private void updateUnreachableVertexes() {
            // 当前节点本身不再可达
            unreachableArray[this.getVertex()] = 1;
            unreachablenVertexNum++;

            // Are preLabel's reachable vertexes still reachable for current vertex?
            for (int j = 0; j < vertexNum; j++) {
                if (unreachableArray[j] == 1) {
                    continue;
                }

                // check capacity constraints
                double newDemand = this.getDemand() + vrptwIns.getVertexByIndex(j).getDemand();
                if (newDemand > vrptwIns.getVehicle().getCapacity()) {
                    unreachableArray[j] = 1;
                    unreachablenVertexNum++;
                    continue;
                }

                // check time window constraints
                double newTime = this.getTime() + vrptwIns.getVertexByIndex(this.getVertex()).getServiceTime()
                        + vrptwIns.getTimeMatrix()[this.getVertex()][j];
                if (newTime > vrptwIns.getVertexByIndex(j).getLatestTime()) {
                    unreachableArray[j] = 1;
                    unreachablenVertexNum++;
                }

            }

        }

    }

}

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
    private Queue<Integer> nodesToTreat;
    /** Set of labels extended from node chosen to its successors. */
    private ArrayList<EsppcctwLabel> labelExtendedFromCurrToNext;

    /** labels on every node，外层索引对应节点索引. */
    ArrayList<ArrayList<AbstractLabel>> labelList;

    /**
     * Create a Instance EspptwccViaLabel.
     * 
     * @param vrptwIns VRPTW 问题实例
     * @param lambda   dual prices corresponding to sum(x[i][j][k] for j in V, k in K) = 1
     */
    public EspptwccViaLabel(Vrptw vrptwIns, double[] lambda) {
        super(vrptwIns, lambda);

        int initialCapcity = (int) (nodeNum / Parameters.LOADER_FACTOR) + 1;
        labelList = new ArrayList<>(initialCapcity);
        for (int i = 0; i < nodeNum; i++) {
            labelList.add(new ArrayList<>(Parameters.INITIAL_CAPACITY));
        }

        nodesToTreat = new LinkedList<>();
    }

    /**
     * Solve an ESPPTWCC via dynamic programming labeling approach: <br>
     * Step 0: Initialization <br>
     * Step 1: Selection of the node to be treated <br>
     * Step 2: Exploration of the successor of the current node <br>
     * Step 3: add non-dominated labels to labelList add update nodesToTreat <br>
     * Step 4: Filtering.
     */
    @Override
    public void solve() {
        // Step 0: Initialization
        EsppcctwLabel initialLabel = new EsppcctwLabel(0, 0, 0, 0);
        labelList.get(0).add(initialLabel);
        nodesToTreat.offer(0);

        while (!nodesToTreat.isEmpty()) {
            // Step 1: Selection of the node to be treated, I choose FIFO rules
            int currNode = nodesToTreat.poll();

            for (int j = 0; j < nodeNum; j++) {
                // Step 2: Exploration of the successor of the current node

                // Clear the set of labels extended from node chosen to its successors
                labelExtendedFromCurrToNext = new ArrayList<>(Parameters.INITIAL_CAPACITY);

                // for all labels on node i
                for (AbstractLabel label : labelList.get(currNode)) {
                    // Extend to the reachable nodes
                    this.labelExtension(label, j);
                }

                // Step 3: add non-dominated labels to labelList add update nodesToTreat
                for (EsppcctwLabel labelExtended : labelExtendedFromCurrToNext) {
                    this.useDominanceRules(labelExtended);
                }

            }

        }

        // Step 4: Filtering
        AbstractLabel optLabel = this.filtering(labelList.get(nodeNum - 1));

        // 设置最短路径信息
        this.reducedCost = optLabel.getCost();
        ArrayList<Integer> nodeIndices = this.labelToVisitNodes(optLabel);
        this.shortestPath = this.createPath(nodeIndices);
    }

    @Override
    public void labelExtension(AbstractLabel currLabel, int nextNodeIndex) {
        if (!(currLabel instanceof EsppcctwLabel)) {
            throw new IllegalArgumentException("输入标签类型错误，请校验！");
        }

        // Extend to the reachable nodes
        EsppcctwLabel currentLabel = (EsppcctwLabel) currLabel;
        if (currentLabel.unreachableArray[nextNodeIndex] == 1) {
            return;
        }

        // whether the extension is feasible
        double demand = currentLabel.getDemand() + vrptwIns.getNodeByIndex(nextNodeIndex).getDemand();

        // Attention: add service time
        double time = currentLabel.getTime() + vrptwIns.getNodeByIndex(currentLabel.getNode()).getServiceTime()
                + vrptwIns.getDistanceBetween(currentLabel.getNode(), nextNodeIndex);

        if (time < vrptwIns.getNodeByIndex(nextNodeIndex).getEarliestTime()) {
            time = vrptwIns.getNodeByIndex(nextNodeIndex).getEarliestTime();
        }

        double cost = currentLabel.getCost() + revisedCostMatrix[currentLabel.getNode()][nextNodeIndex];
        EsppcctwLabel labelExtended = new EsppcctwLabel(cost, time, demand, nextNodeIndex, currentLabel);

        this.labelExtendedFromCurrToNext.add(labelExtended);
    }

    @Override
    public void useDominanceRules(AbstractLabel labelToCompare) {
        // TODO Auto-generated method stub
        if (!(labelToCompare instanceof EsppcctwLabel)) {
            throw new IllegalArgumentException("输入标签类型错误，请校验！");
        }

        int currNodeIndex = labelToCompare.getNode();
        ArrayList<AbstractLabel> labels = labelList.get(currNodeIndex);

        // Is the labels on current node changed?
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

        // Is current node not in nodesToTreat? Is the labels on current node j changed?
        if (!nodesToTreat.contains(currNodeIndex) && isLabelsChanged) {
            nodesToTreat.offer(currNodeIndex);
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
        private int unreachablenNodeNum;

        EsppcctwLabel(double cost, double time, double demand, int node) {
            super(cost, time, demand, node, null);

            // int 默认值为 0
            unreachableArray = new int[nodeNum];
            unreachablenNodeNum = 0;

            this.updateUnreachableNodes();
        }

        EsppcctwLabel(double cost, double time, double demand, int node, AbstractLabel preLabel) {
            super(cost, time, demand, node, preLabel);

            if (!(this.getPreLabel() instanceof EsppcctwLabel)) {
                throw new IllegalArgumentException("上一个标签类型错误，请校验！");
            }
            // 强制类型转换
            EsppcctwLabel label = (EsppcctwLabel) preLabel;

            // 需求量，时间都是 non-decreasing，并且访问过的节点不能再访问
            // 所以上一个标签不可达节点在当前标签中必然不可达
            this.unreachableArray = new int[nodeNum];
            for (int j = 0; j < nodeNum; j++) {
                this.unreachableArray[j] = label.unreachableArray[j];
            }
            this.unreachablenNodeNum = label.unreachablenNodeNum;

            this.updateUnreachableNodes();
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
            if (newOther.unreachablenNodeNum > this.unreachablenNodeNum) {
                return false;
            }

            // 如果存在 this 访问过，但 newOther 没有访问过的节点，则 this 没有被“优超”
            for (int i = 0; i < nodeNum; i++) {
                if (newOther.unreachableArray[i] > this.unreachableArray[i]) {
                    return false;
                }
            }

            return true;
        }

        private void updateUnreachableNodes() {
            // 当前节点本身不再可达
            unreachableArray[this.getNode()] = 1;
            unreachablenNodeNum++;

            // Are preLabel's reachable nodes still reachable for current node?
            for (int j = 0; j < nodeNum; j++) {
                if (unreachableArray[j] == 1) {
                    continue;
                }

                // check capacity constraints
                double newDemand = this.getDemand() + vrptwIns.getNodeByIndex(j).getDemand();
                if (newDemand > vrptwIns.getVehicle().getCapacity()) {
                    unreachableArray[j] = 1;
                    unreachablenNodeNum++;
                    continue;
                }

                // check time window constraints
                double newTime = this.getTime() + vrptwIns.getNodeByIndex(this.getNode()).getServiceTime()
                        + vrptwIns.getDistanceBetween(this.getNode(), j);
                if (newTime > vrptwIns.getNodeByIndex(j).getLatestTime()) {
                    unreachableArray[j] = 1;
                    unreachablenNodeNum++;
                }

            }

        }

    }

}

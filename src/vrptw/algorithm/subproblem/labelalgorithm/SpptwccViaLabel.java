package vrptw.algorithm.subproblem.labelalgorithm;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.PriorityQueue;

import vrptw.algorithm.subproblem.AbstractSubProblem;
import vrptw.parameter.Parameters;
import vrptw.problem.Vrptw;

/**
 * Solve a SPPTWCC via dynamic programming labeling approach <br>
 * see "Algorithm 2.1. The general label setting algorithm (GLSA)" in Boland et al. (2006): <br>
 * Accelerated label setting algorithms for the elementary resource constrained shortest path problem.
 * 
 * @author Xiong Wangqi
 * @version V1.0
 * @since JDK1.8
 */
public class SpptwccViaLabel extends AbstractSubProblem implements LabelAlgorithm {
    /** 待处理的 Labels，采用基于 lexicographically minimal 的优先队列. */
    private PriorityQueue<SppcctwLabel> unprocessedLabels;
    
    /** labels on every node，外层索引对应节点索引. */
    ArrayList<ArrayList<AbstractLabel>> labelList;

    /**
     * Create a Instance SPPTWCCViaLabel.
     * 
     * @param vrptwIns VRPTW 问题实例
     * @param lambda   Dual prices of sum(x[i][j][k] for j in V, k in K) = 1
     */
    public SpptwccViaLabel(Vrptw vrptwIns, double[] lambda) {
        super(vrptwIns, lambda);

        int initialCapcity = (int) (nodeNum / Parameters.LOADER_FACTOR) + 1;
        unprocessedLabels = new PriorityQueue<>(initialCapcity, new LabelComparator());

        labelList = new ArrayList<>(initialCapcity);
        for (int i = 0; i < nodeNum; i++) {
            labelList.add(new ArrayList<>(Parameters.INITIAL_CAPACITY));
        }
    }

    /**
     * Solve a SPPTWCC via dynamic programming labeling approach: <br>
     * Step 0: Initialization <br>
     * Step 1: Selection of the label to be extended <br>
     * Step 2: Extension <br>
     * Step 3: Dominance <br>
     * Step 4: Filtering.
     */
    @Override
    public void solve() {
        // Step 0: Initialization
        SppcctwLabel initialLabel = new SppcctwLabel(0, 0, 0, 0);
        labelList.get(0).add(initialLabel);
        unprocessedLabels.offer(initialLabel);

        while (!unprocessedLabels.isEmpty()) {
            // Step 1: Selection of the label to be extended
            // choose lexicographically minimal label and remove it from unprocessedLabels
            SppcctwLabel currlabel = unprocessedLabels.poll();

            // Step 2&3: Extension and Dominance
            for (int i = 0; i < nodeNum; i++) {
                // all nodes
                this.labelExtension(currlabel, i);
            }

        }

        // Step 4: Filtering
        AbstractLabel optLabel = this.filtering(labelList.get(nodeNum - 1));
        
        this.reducedCost = optLabel.getCost();
        ArrayList<Integer> nodeIndices = this.labelToVisitNodes(optLabel);
        this.shortestPath = this.createPath(nodeIndices);
    }

    @Override
    public void labelExtension(AbstractLabel currLabel, int nextNodeIndex) {
        if (!(currLabel instanceof SppcctwLabel)) {
            throw new IllegalArgumentException("输入参数类型异常，请校验！");
        }

        if (nextNodeIndex == currLabel.getNode()) {
            return;
        }

        // whether the extension is feasible
        double demand = currLabel.getDemand() + vrptwIns.getNodeByIndex(nextNodeIndex).getDemand();
        if (demand > vrptwIns.getVehicle().getCapacity()) {
            return;
        }

        // Attention: add service time
        double time = currLabel.getTime() + vrptwIns.getNodeByIndex(currLabel.getNode()).getServiceTime()
                + vrptwIns.getDistanceBetween(currLabel.getNode(), nextNodeIndex);

        if (time > vrptwIns.getNodeByIndex(nextNodeIndex).getLatestTime()) {
            return;
        }

        if (time < vrptwIns.getNodeByIndex(nextNodeIndex).getEarliestTime()) {
            time = vrptwIns.getNodeByIndex(nextNodeIndex).getEarliestTime();
        }

        double cost = currLabel.getCost() + revisedCostMatrix[currLabel.getNode()][nextNodeIndex];
        SppcctwLabel newLabel = new SppcctwLabel(cost, time, demand, nextNodeIndex, currLabel);

        this.useDominanceRules(newLabel);
    }

    @Override
    public void useDominanceRules(AbstractLabel labelToCompare) {
        if (!(labelToCompare instanceof SppcctwLabel)) {
            throw new IllegalArgumentException("输入参数类型异常，请校验！");
        }

        int currNodeIndex = labelToCompare.getNode();
        ArrayList<AbstractLabel> processedLabels = labelList.get(currNodeIndex);

        // whether the new label dominates or dominated by other labels
        boolean isDominated = false;

        // labelToCompare 是否可能被接下来的标签“优超”
        boolean isPossibleDominatedByNextLabel = true;

        // 注意不要在 forEach 循环中使用 remove
        Iterator<AbstractLabel> iterator = processedLabels.iterator();
        while (iterator.hasNext()) {
            AbstractLabel other = iterator.next();

            if (other.isDominatedBy(labelToCompare)) {
                /*
                 * label 是按顺序添加的，留下来的 label 都是没有被后面的 label “优超” 的，
                 * 也就是说排在后面的 lable 至少有一项资源是大于当前的 label 的；
                 * 
                 * 而 labelToCompare “优超”当前的 label 说明其所有资源都小于当前的 label，
                 * 所以 labelToCompare 不可能被更后面的 label “优超”
                 */
                isPossibleDominatedByNextLabel = false;

                // remove 会检查是否包含 other
                unprocessedLabels.remove(other);
                iterator.remove();
            }

            if (isPossibleDominatedByNextLabel && labelToCompare.isDominatedBy(other)) {
                isDominated = true;
                /*
                 * label 是按顺序添加的，添加的前提就是不能被之前的 label “优超”，
                 * 也就是说排在后面的 lable 至少有一项资源是小于当前的 label 的；
                 * 
                 * 而 labelToCompare 被“优超” 说明其所有资源都大于当前的 label，
                 * 所以 labelToCompare 不可能“优超”更后面的 label，可以跳出循环
                 */
                break;
            }

        }
        
        // add only if labelToCompare is non-dominated
        if (!isDominated) {
            processedLabels.add(labelToCompare);

            // 对于已经到达终点的 Label 不用再进行 extension
            if (currNodeIndex != nodeNum - 1) {
                unprocessedLabels.offer((SppcctwLabel) labelToCompare);
            }

        }

    }

    private class SppcctwLabel extends AbstractLabel {
        SppcctwLabel(double cost, double time, double demand, int node) {
            super(cost, time, demand, node, null);
        }

        SppcctwLabel(double cost, double time, double demand, int node, AbstractLabel preLabel) {
            super(cost, time, demand, node, preLabel);

            if (!(preLabel instanceof SppcctwLabel)) {
                throw new IllegalArgumentException("输入标签类型错误，请校验！");
            }
        }

        /**
         * 优超准则判别：<br>
         * 1. 结束点相同 <br>
         * 2. 各个“资源”情况 “other” <= “this” <br>
         * 则 “this” is dominated by “other”.
         * 
         * @param other 待比较的标签
         * @return 给定标签 other 是否"优超"当前标签（this）
         */
        @Override
        boolean isDominatedBy(AbstractLabel other) {
            // 结束点相同的 Label 才具有可比性
            if (other.getNode() != this.getNode()) {
                return false;
            }
            // 比较成本、耗时、需求总量，只要 other 有一项大于 this，则 this 没有被“优超”
            if (other.getCost() > this.getCost() || other.getTime() > this.getTime()
                    || other.getDemand() > this.getDemand()) {
                return false;
            }

            return true;
        }

    }

    /**
     * compare labels in lexicographic order(cost, time, demand).
     * 
     * @author Xiong Wangqi
     * @version V1.0
     * @since JDK1.8
     */
    private class LabelComparator implements Comparator<SppcctwLabel> {

        @Override
        public int compare(SppcctwLabel first, SppcctwLabel second) {
            if (first.getCost() < second.getCost()) {
                return -1;
            }
            if (first.getCost() > second.getCost()) {
                return 1;
            }

            if (first.getTime() < second.getTime()) {
                return -1;
            }
            if (first.getTime() > second.getTime()) {
                return 1;
            }

            if (first.getDemand() < second.getDemand()) {
                return -1;
            }
            if (first.getDemand() > second.getDemand()) {
                return 1;
            }

            return 0;
        }

    }

}

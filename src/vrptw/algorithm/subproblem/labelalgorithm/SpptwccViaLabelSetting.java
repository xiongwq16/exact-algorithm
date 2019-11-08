package vrptw.algorithm.subproblem.labelalgorithm;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.PriorityQueue;

import vrptw.algorithm.subproblem.AbstractPriceProblem;
import vrptw.parameter.Parameters;
import vrptw.problem.Vertex;
import vrptw.problem.Vrptw;
import vrptw.solution.Path;

/**
 * Solve a SPPTWCC via dynamic programming labeling approach <br>
 * see "Algorithm 2.1. The general label setting algorithm (GLSA)" in Boland et al. (2006): <br>
 * Accelerated label setting algorithms for the elementary resource constrained shortest path problem.
 * 
 * @author Xiong Wangqi
 * @version V1.0
 * @since JDK1.8
 */
public class SpptwccViaLabelSetting extends AbstractPriceProblem implements LabelAlgorithm {
    private Vertex[] vertexes;
    
    /** 待处理的 Labels，采用基于 lexicographically minimal 的优先队列. */
    private PriorityQueue<SppcctwLabel> unprocessedLabels;
    
    /** labels on every vertex，外层索引对应节点 ID. */
    private ArrayList<ArrayList<AbstractLabel>> labelList;
    
    /**
     * Create a Instance SPPTWCC.
     * 
     * @param vrptwIns VRPTW 问题实例
     */
    public SpptwccViaLabelSetting(Vrptw vrptwIns) {
        super(vrptwIns);
        int initialCapcity = (int) (vertexNum / Parameters.LOADER_FACTOR) + 1;
        unprocessedLabels = new PriorityQueue<>(initialCapcity, new LabelComparator());
        
        vertexes = new Vertex[vertexNum];
        for (int i = 0; i < vertexNum; i++) {
            vertexes[i] = vrptwIns.getVertexes().get(i);
        }
        
        labelList = new ArrayList<>(initialCapcity);
        for (int i = 0; i < vertexNum; i++) {
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
     * 
     * @param dualValues dual values
     * @param timeMatrix time matrix after branch
     */
    @Override
    public void solve(Map<Integer, Double> dualValues, double[][] timeMatrix) {
        // 清空 unprocessedLabels，labelList，shortestPath
        this.reset();
        
        this.timeMatrix = timeMatrix;
        this.updateDistAndCostMatrix(dualValues);
        
        // Step 0: Initialization
        SppcctwLabel initialLabel = new SppcctwLabel(0, 0, 0, 0);
        labelList.get(0).add(initialLabel);
        unprocessedLabels.offer(initialLabel);
        
        while (!unprocessedLabels.isEmpty()) {
            // Step 1: Selection of the label to be extended
            // choose lexicographically minimal label and remove it from unprocessedLabels
            SppcctwLabel currlabel = unprocessedLabels.poll();
            
            // Step 2&3: Extension and Dominance
            for (int i = 0; i < vertexNum; i++) {
                // all vertexes
                this.labelExtension(currlabel, i);
            }
            
        }
        
        // Step 4: Filtering
        ArrayList<AbstractLabel> optLabels = this.filtering(labelList.get(vertexNum - 1));
        
        // 这里我们只取一条路径
        this.revisedCostOfShortestPath = optLabels.get(0).cost;
        ArrayList<Integer> vertexIds = optLabels.get(0).getVisitVertexes();
        this.shortestPaths.add(new Path(vrptwIns, vertexIds));
    }

    @Override
    public void labelExtension(AbstractLabel currLabel, int nextVertexId) {
        if (!(currLabel instanceof SppcctwLabel)) {
            throw new IllegalArgumentException("输入参数类型异常，请校验！");
        }

        if (nextVertexId == currLabel.vertexId) {
            return;
        }
        
        // whether the extension is feasible
        double demand = currLabel.demand + vertexes[nextVertexId].getDemand();
        if (demand > capacity) {
            return;
        }
        
        // Attention: add service time
        double time = currLabel.time + vertexes[currLabel.vertexId].getServiceTime()
                + timeMatrix[currLabel.vertexId][nextVertexId];
        
        if (time > vertexes[nextVertexId].getLatestTime()) {
            return;
        }
        
        if (time < vertexes[nextVertexId].getEarliestTime()) {
            time = vertexes[nextVertexId].getEarliestTime();
        }
        
        double cost = currLabel.cost + revisedCostMatrix[currLabel.vertexId][nextVertexId];
        SppcctwLabel newLabel = new SppcctwLabel(cost, time, demand, nextVertexId, currLabel);

        this.useDominanceRules(newLabel);
    }
    
    @Override
    public void useDominanceRules(AbstractLabel labelToCompare) {
        if (!(labelToCompare instanceof SppcctwLabel)) {
            throw new IllegalArgumentException("输入参数类型异常，请校验！");
        }
        
        int currVertexId = labelToCompare.vertexId;
        ArrayList<AbstractLabel> processedLabels = labelList.get(currVertexId);
        
        // whether the new label dominates or dominated by other labels
        boolean isDominated = false;
        
        // labelToCompare 是否可能被接下来的标签“优超”
        boolean isPossibleDominatedByNextLabel = true;

        // 注意不要在 forEach 循环中使用 remove
        Iterator<AbstractLabel> iterator = processedLabels.iterator();
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

                // remove 会检查是否包含 other
                unprocessedLabels.remove(other);
                iterator.remove();
            }
            
            if (isPossibleDominatedByNextLabel && other.dominate(labelToCompare)) {
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
            if (currVertexId != vertexNum - 1) {
                unprocessedLabels.offer((SppcctwLabel) labelToCompare);
            }

        }

    }
    
    @Override
    protected void reset() {
        this.unprocessedLabels.clear();
        this.labelList.forEach(labels -> labels.clear());
        this.shortestPaths.clear();
    }

    private class SppcctwLabel extends AbstractLabel {
        SppcctwLabel(double cost, double time, double demand, int vertexId) {
            super(cost, time, demand, vertexId, null);
        }
        
        SppcctwLabel(double cost, double time, double demand, int vertexId, AbstractLabel preLabel) {
            super(cost, time, demand, vertexId, preLabel);

            if (!(preLabel instanceof SppcctwLabel)) {
                throw new IllegalArgumentException("输入标签类型错误，请校验！");
            }
        }
        
        @Override
        boolean dominate(AbstractLabel that) {
            // 对于到达终点 dummy end depot 的标签，只需要比较 cost
            if (this.vertexId == vertexNum - 1) {
                if (this.cost >= that.cost) {
                    return false;
                }
                
                return true;
            }
            
            // 比较成本、到达节点的时间、需求总量，只要 this 有一项大于 that，则 this 没有“优超” that
            if (this.demand > that.demand || this.cost > that.cost || this.time > that.time) {
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
            if (first.cost < second.cost) {
                return -1;
            }
            if (first.cost > second.cost) {
                return 1;
            }
            
            if (first.time < second.time) {
                return -1;
            }
            if (first.time > second.time) {
                return 1;
            }

            if (first.demand < second.demand) {
                return -1;
            }
            if (first.demand > second.demand) {
                return 1;
            }

            return 0;
        }

    }

}

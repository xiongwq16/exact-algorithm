package vrptw.algorithm.subproblem.labelalgorithm;

/**
 * 抽象标签类，包括成本，以及两类“资源”-总需求量和耗时.
 * 
 * @author Xiong Wangqi
 * @version V1.0
 * @since JDK1.8
 */
abstract class AbstractLabel {
    /** 到达当前节点的成本，也是表示的路径的 reduced cost. */
    private double cost;
    /** 到达当前节点的耗时. */
    private double time;
    /** 当前路径上所有客户的总需求量. */
    private double demand;
    /** 标签所在节点（路径上的最后一个节点）. */
    private int vertex;
    /** 上一个 label. */
    private AbstractLabel preLabel;
    
    AbstractLabel(double cost, double time, double demand, int vertex, AbstractLabel preLabel) {
        this.cost = cost;
        this.time = time;
        this.demand = demand;
        this.vertex = vertex;
        this.preLabel = preLabel;
    }

    /**
     * 优超准则判别.
     * 
     * @param other 待比较的标签
     * @return 给定标签 other 是否"优超"当前标签（this）
     */
    abstract boolean isDominatedBy(AbstractLabel other);

    double getCost() {
        return cost;
    }

    double getTime() {
        return time;
    }

    double getDemand() {
        return demand;
    }

    int getVertex() {
        return vertex;
    }

    AbstractLabel getPreLabel() {
        return this.preLabel;
    }
}

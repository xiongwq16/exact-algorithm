package vrptw.algorithm.subproblem.labelalgorithm;

import java.util.ArrayList;
import java.util.Collections;

import vrptw.parameter.Parameters;

/**
 * 抽象标签类，包括成本，以及两类“资源”-总需求量和到达节点的时间.
 * 
 * @author Xiong Wangqi
 * @version V1.0
 * @since JDK1.8
 */
abstract class AbstractLabel {
    /** 到达当前节点的成本，也是表示的路径的 revised cost. */
    final double cost;
    /** 到达当前节点的时间. */
    final double time;
    /** 当前路径上所有客户的总需求量. */
    final double demand;
    /** 标签所在节点（路径上的最后一个节点）. */
    final int vertexId;
    /** 上一个 label. */
    final AbstractLabel preLabel;
    
    AbstractLabel(double cost, double time, double demand, int vertexId, AbstractLabel preLabel) {
        this.cost = cost;
        this.time = time;
        this.demand = demand;
        this.vertexId = vertexId;
        this.preLabel = preLabel;
    }

    /**
     * 相同起点和终点的两个标签之间的优超准则判别：如果各个“资源”情况 “this” 不大于 “that”, 则“this” 优超 “that”。<br>
     * 注意这里并未排除相等的情况，会在其他方法中考虑.
     * 
     * @param that 待比较的标签
     * @return “this” 是否优超 “that”
     */
    abstract boolean dominate(AbstractLabel that);
    
    /**
     * 将标签转换为节点访问序列.
     * 
     * @return 标签对应的路径上的节点序列
     */
    ArrayList<Integer> getVisitVertexes() {
        ArrayList<Integer> vertexIds = new ArrayList<>(Parameters.INITIAL_CAPACITY);
        vertexIds.add(vertexId);
        
        AbstractLabel label = this;
        while ((label = label.preLabel) != null) {
            vertexIds.add(label.vertexId);
        }
        
        Collections.reverse(vertexIds);
        
        return vertexIds;
    }
    
    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        
        if (other == null || !(other instanceof AbstractLabel)) {
            return false;
        }
        
        AbstractLabel label = this;
        AbstractLabel that = (AbstractLabel) other;
        if (that.vertexId != label.vertexId) {
            return false;
        }
        
        while ((label = label.preLabel) != null) {
            that = that.preLabel;
            if (that == null) {
                return false;
            }
            
            if (that.vertexId != label.vertexId) {
                return false;
            }
        }
        
        if (that.preLabel != null) {
            return false;
        }
        
        return true;
    }
    
    @Override
    public String toString() {
        ArrayList<Integer> vertexIds = new ArrayList<>(Parameters.INITIAL_CAPACITY);
        vertexIds.add(this.vertexId);
        
        AbstractLabel label = this;
        while ((label = label.preLabel) != null) {
            vertexIds.add(label.vertexId);
        }
        
        Collections.reverse(vertexIds);
        
        StringBuilder sb = new StringBuilder();
        
        vertexIds.forEach(id -> sb.append(id + "-"));
        sb.deleteCharAt(sb.length() - 1);
        
        sb.append(String.format("\ncost: %f, time: %f, demand: %f\n", cost, time, demand));
        
        return sb.toString();
    }
    
}

package vrptw.algorithm.subproblem.labelalgorithm;

import java.util.ArrayList;

/**
 * Dynamic programming labeling approach.
 * 
 * @author Xiong Wangqi
 * @version V1.0
 * @since JDK1.8
 */
interface LabelAlgorithm {
    /**
     * 对选中的 Label 进行扩展（需要满足时间窗、容量约束）.
     * 
     * @param currLabel       待扩展的标签
     * @param nextVertexId 待添加到标签中的节点
     */
    void labelExtension(AbstractLabel currLabel, int nextVertexId);
    
    /**
     * 根据优超准则添加判断是否添加“优超”的新标签，删除已存在但被“优超”的标签.
     * 
     * @param labelToCompare 待比较的新标签
     */
    void useDominanceRules(AbstractLabel labelToCompare);
    
    /**
     * 筛选出最短路径对应的标签.
     * 
     * @param allFinalLabels 到达最终点的标签
     * @return 最短路径对应的标签
     */
    default ArrayList<AbstractLabel> filtering(ArrayList<AbstractLabel> allFinalLabels) {        
        if (allFinalLabels.isEmpty()) {
            throw new NullPointerException("未找到最短路径");
        }
        
        // 这里不做处理，直接返回所有到达终点的路径，也可以控制返回的路径数量
        return allFinalLabels;
    }
}

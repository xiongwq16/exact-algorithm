package vrptw.algorithm.subproblem.labelalgorithm;

import java.util.ArrayList;
import java.util.Collections;

import vrptw.parameter.Parameters;

/**
 * Abstract class of dynamic programming labeling approach.
 * 
 * @author Xiong Wangqi
 * @version V1.0
 * @since JDK1.8
 */
interface LabelAlgorithm {
    /**
     * 对选中的 Label 进行扩展（需要满足时间窗、容量约束）.
     * 
     * @param currLabel     待扩展的标签
     * @param nextNodeIndex 待添加到标签中的节点
     */
    void labelExtension(AbstractLabel currLabel, int nextNodeIndex);

    /**
     * 根据优超准则添加判断是否添加“优超”的新标签，删除已存在但被“优超”的 Label.
     * 
     * @param labelToCompare 待比较的新标签
     */
    void useDominanceRules(AbstractLabel labelToCompare);

    /**
     * 筛选出最短路径对应的标签.
     * 
     * @param allFinalLabels 到达终点（dummy endDepot）的标签
     * @return 最短路径对应的标签
     */
    default AbstractLabel filtering(ArrayList<AbstractLabel> allFinalLabels) {
        if (allFinalLabels.isEmpty()) {
            throw new NullPointerException("未找到最短路径");
        }

        // 选用排序，后期如需要取多条路径比较方便，也可以通过循环找出 reduced cost 最小的标签
        allFinalLabels.sort((label1, label2) -> Double.compare(label1.getCost(), label2.getCost()));

        return allFinalLabels.get(0);
    }

    /**
     * 将标签转换为节点访问序列.
     * 
     * @param label 给定的标签
     * @return 标签对应的路径
     */
    default ArrayList<Integer> labelToVisitNodes(AbstractLabel label) {
        ArrayList<Integer> nodeIndices = new ArrayList<>(Parameters.INITIAL_CAPACITY);
        nodeIndices.add(label.getNode());

        while ((label = label.getPreLabel()) != null) {
            nodeIndices.add(label.getNode());
        }

        Collections.reverse(nodeIndices);

        return nodeIndices;
    }
}

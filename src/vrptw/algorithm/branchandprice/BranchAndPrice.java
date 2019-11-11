package vrptw.algorithm.branchandprice;

import ilog.concert.IloException;

import java.util.PriorityQueue;

import vrptw.algorithm.VrptwExactAlgorithm;
import vrptw.algorithm.subproblem.AbstractPriceProblem;
import vrptw.algorithm.subproblem.labelalgorithm.EspptwccViaLabelCorrecting;
import vrptw.algorithm.subproblem.labelalgorithm.SpptwccViaLabelSetting;
import vrptw.algorithm.subproblem.pulsealgorithm.EspptwccViaPulse;
import vrptw.parameter.Parameters;
import vrptw.problem.Arc;
import vrptw.problem.Vrptw;
import vrptw.solution.VrptwSolution;

/**
 * Branch and Price Algorithm.
 * 
 * @author Xiong Wangqi
 * @version V1.0
 * @since JDK1.8
 */
public class BranchAndPrice implements VrptwExactAlgorithm {
    private Vrptw vrptwIns;
    
    /** Price Problem. */
    private AbstractPriceProblem priceProblem;
    /** 主问题. */
    private VrptwMasterProblem masterProblem;
    
    /** 分支过程中的上界，也是当前找到的最好可行解的目标值. */
    private double upperBound;
    /** 最好的分支节点. */
    private BabNode bestNode;
    
    /** Node number in the branch and bound tree. */
    private int nodeNum;
    /** Best first search，分支节点的选择的优先队列. */
    private PriorityQueue<BabNode> nodePq;
    
    private VrptwSolution vrptwSol;
    
    /**
     * Create a Instance of Branch and Price.
     * 
     * @param vrptwIns VRPTW instance
     * @throws IloException 
     */
    public BranchAndPrice(Vrptw vrptwIns, String priceProblemType) throws IloException {
        if (priceProblemType != Parameters.SPPTWCC_LABEL 
                && priceProblemType != Parameters.ESPPTWCC_LABEL 
                && priceProblemType != Parameters.ESPPTWCC_PULSE) {
            throw new IllegalArgumentException(
                    String.format("%s can only be SPPTWCC or ESPPTWCC.", priceProblemType));
        }
        
        this.vrptwIns = vrptwIns;
        
        // Price Problem Initialization
        switch (priceProblemType) {
            case Parameters.SPPTWCC_LABEL:
                priceProblem = new SpptwccViaLabelSetting(vrptwIns);
                break;
            case Parameters.ESPPTWCC_LABEL:
                priceProblem = new EspptwccViaLabelCorrecting(vrptwIns);
                break;
            case Parameters.ESPPTWCC_PULSE:
                priceProblem = new EspptwccViaPulse(vrptwIns);
                break;
            default:
                throw new IllegalArgumentException(
                        String.format("%s can only be SPPTWCC or ESPPTWCC.", priceProblemType));
        }
        
        this.masterProblem = new VrptwMasterProblem(vrptwIns);
        
        this.upperBound = Parameters.BB_INITIAL_UPPERBOUND;
        this.nodeNum = 0;
        this.nodePq = new PriorityQueue<>(Parameters.INITIAL_CAPACITY);
    }
    
    @Override
    public void solve() throws IloException {
        System.out.println("Branch and price algorithm");
        System.out.println("--------------------------------------------");
        
        double startTime = System.currentTimeMillis();
        
        // Solve initial master problem
        BabNode root = new BabNode(vrptwIns);
        if (!this.tryToaddNodeToPriorityQueue(root)) {
            System.out.println("VTPTW given is infeasible");
            return;
        }
        
        while (!nodePq.isEmpty()) {
            BabNode node = nodePq.poll();
            
            if (canBePruned(node)) {
                continue;
            }
            
            Arc arcToBranch = node.getArcToBranch();
            if (arcToBranch == null) {
                // the solution of the node is feasible, compare with the current upper bound
                if (Double.compare(node.getNodeLpObj(), upperBound) < 0) {
                    this.updateUpperBound(node);
                }
                
                continue;
            }
            
            // branch
            int from = arcToBranch.getFromVertexId();
            int to = arcToBranch.getToVertexId();
            
            // Left node, if the start / end of branch arc is depot, the left node is infeasible
            if (from != 0 && to != vrptwIns.getVertexNum() - 1) {
                BabNode left = new BabNode(node, from, to, 0);
                this.tryToaddNodeToPriorityQueue(left);
            }
            
            BabNode right = new BabNode(node, from, to, 1);
            this.tryToaddNodeToPriorityQueue(right);
        }
        
        double timeConsume = System.currentTimeMillis() - startTime;
        vrptwSol = new VrptwSolution(
                vrptwIns, masterProblem.getPaths(), bestNode.getPathIndicesInNodeSol(), bestNode.getNodeLpObj());
        vrptwSol.output(timeConsume, nodeNum);
        
//        masterProblem.rmlpSolver.exportModel("/Users/xiong/学习资源/实践/Algorithm/exactalgorithm/results/rmlp.lp");
        
        masterProblem.end();
    }
    
    /**
     * add given node to Priority Queue when it's LP feasible and having a LP objective smaller the upper bound.
     * 
     * @param newNode node to add
     * @return add or not
     * @throws IloException
     */
    private boolean tryToaddNodeToPriorityQueue(BabNode newNode) throws IloException {
        newNode.columnGeneration(masterProblem, priceProblem);
        
        if (!this.canBePruned(newNode)) {
            nodePq.add(newNode);
            nodeNum++;
            return true;
        }
        
        return false;
    }
        
    /**
     * 剪枝操作.
     * 
     * @param node given node
     * @return can the given be pruned?
     */
    private boolean canBePruned(BabNode node) {
        if (!node.isLpFeasible() || node.getNodeLpObj() > this.upperBound) {
            return true;
        }
        
        return false;
    }
    
    private void updateUpperBound(BabNode bestNode) {
        upperBound = bestNode.getNodeLpObj();
        this.bestNode = bestNode;
    }
    
    private void outputIncubment() {
        System.out.println(String.format("Current best node's depth is %d, UB is %.3f", 
                this.bestNode.getDepth(), this.upperBound));
    }
    
}

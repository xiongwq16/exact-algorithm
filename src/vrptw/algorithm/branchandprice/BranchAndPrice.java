package vrptw.algorithm.branchandprice;

import ilog.concert.IloException;

import java.util.PriorityQueue;

import vrptw.algorithm.VrptwExactAlgorithm;
import vrptw.algorithm.solomoninsertion.SolomonInsertion;
import vrptw.algorithm.subproblem.AbstractPriceProblem;
import vrptw.algorithm.subproblem.pulsealgorithm.EspptwccViaPulse;
import vrptw.parameter.Parameters;
import vrptw.problem.Arc;
import vrptw.problem.Vrptw;
import vrptw.solution.Path;
import vrptw.solution.VrptwSolution;

/**
 * Branch and Price Algorithm.
 * 
 * @author Xiong Wangqi
 * @version V1.0
 * @since JDK1.8
 */
public class BranchAndPrice implements VrptwExactAlgorithm {
    private Vrptw originVrptwIns;
    
    /** Price Problem. */
    private AbstractPriceProblem priceProblem;
    /** 主问题. */
    private BapMasterProblem masterProblem;
    
    /** 分支过程中的上界，也是当前找到的最好可行解的目标值. */
    private double upperBound;
    /** 最好的分支节点. */
    private BapNode bestNode;
    
    /** Node number in the branch and bound tree. */
    private int nodeNum;
    /** Best first search，分支节点的选择的优先队列. */
    private PriorityQueue<BapNode> nodePq;
    
    private VrptwSolution vrptwSol;
    
    /**
     * Create a Instance of Branch and Price.
     * 
     * @param vrptwIns VRPTW instance
     * @throws IloException 
     */
    public BranchAndPrice(Vrptw vrptwIns) throws IloException {
        this.originVrptwIns = vrptwIns;
        
        // Price and master Problem Initialization
        this.priceProblem = new EspptwccViaPulse(vrptwIns);
        this.masterProblem = new BapMasterProblem(vrptwIns);
        
        this.upperBound = Parameters.BB_INITIAL_UPPERBOUND;
        
        this.nodeNum = 0;
        this.nodePq = new PriorityQueue<>(Parameters.INITIAL_CAPACITY);
    }
    
    @Override
    public void solve() throws IloException {
        System.out.println("Branch and price algorithm");
        System.out.println("--------------------------------------------");
        
        final double startTime = System.currentTimeMillis();
        
        // use Solomon Insertion to generate initial solution
        SolomonInsertion i1 = new SolomonInsertion(originVrptwIns, originVrptwIns.getTimeMatrix());
        
        i1.constructRoutes();
        Path[] initialPaths = i1.getPaths();
        for (int i = 0; i < initialPaths.length; i++) {
            masterProblem.addColumn(initialPaths[i]);
        }
        //  use the solution of Solomon Insertion as upper bound 
        this.upperBound = i1.getCost();
        
        // Solve initial RMLP
        BapNode root = new BapNode(originVrptwIns);
        this.addNodeToPriorityQueue(root);
        
        if (!root.isLpFeasible()) {
            System.out.println("VTPTW given is infeasible");
            return;
        }
        
        while (!nodePq.isEmpty()) {
            BapNode node = nodePq.poll();
            
            if (canBePruned(node)) {
                break;
            }
            
            // branch
            Arc arcToBranch = node.getArcToBranch();
            int from = arcToBranch.getFromVertexId();
            int to = arcToBranch.getToVertexId();
            
            BapNode left = new BapNode(node, from, to, 0);
            this.addNodeToPriorityQueue(left);
            
            BapNode right = new BapNode(node, from, to, 1);
            this.addNodeToPriorityQueue(right);
        }
        
        double timeConsume = System.currentTimeMillis() - startTime;
        vrptwSol = new VrptwSolution(
                originVrptwIns, masterProblem.getPaths(), bestNode.getPathIndicesInMipSol(), bestNode.getNodeLpObj());
        vrptwSol.output(timeConsume, nodeNum);
                
        masterProblem.end();
    }
    
    /**
     * add given node to Priority Queue when it's LP feasible and having a LP objective smaller the upper bound.
     * 
     * @param newNode node to add
     * @throws IloException
     */
    private void addNodeToPriorityQueue(BapNode newNode) throws IloException {        
        nodeNum++;
        newNode.columnGeneration(masterProblem, priceProblem);
        
        if (newNode.isLpFeasible() && newNode.getArcToBranch() == null && newNode.getNodeLpObj() < upperBound) {
            this.updateUpperBound(newNode);
            this.outputIncubment();
            return;
        }
        
        if (!this.canBePruned(newNode)) {
            nodePq.add(newNode);
        }
        
    }
    
    /**
     * 剪枝操作.
     * 
     * @param node given node
     * @return can the given be pruned?
     */
    private boolean canBePruned(BapNode node) {
        if (!node.isLpFeasible() || node.getNodeLpObj() >= this.upperBound) {
            return true;
        }
        
        return false;
    }
    
    private void updateUpperBound(BapNode bestNode) {
        upperBound = bestNode.getNodeLpObj();
        this.bestNode = bestNode;
    }
    
    private void outputIncubment() {
        System.out.println(String.format("Current best node's depth is %d, UB is %.3f", 
                this.bestNode.getDepth(), this.upperBound));
    }
    
}

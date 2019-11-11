package vrptw.algorithm.branchandprice;

import ilog.concert.IloException;

import java.util.ArrayList;
import java.util.List;

import vrptw.algorithm.subproblem.AbstractPriceProblem;
import vrptw.parameter.Parameters;
import vrptw.problem.Arc;
import vrptw.problem.Vrptw;
import vrptw.solution.Path;

/**
 * Node in the branch and bound tree(branch on arc).
 * 
 * @author Xiong Wangqi
 * @version V1.0
 * @since JDK1.8
 */
class BabNode implements Comparable<BabNode> {
    private static Vrptw vrptwIns;
    
    /** 由于分支被禁止的路径索引. */
    private ArrayList<Integer> infeasiblePathIndices;
    
    /** parent of the node in the branch and bound tree. */
    private BabNode parent;
    /** The depth of current node in the branch and bound tree. */
    private int depth;
    /** store the branch info from parent to current node. */
    private BranchArc branchArcOfParent;
    
    /** LP objective of subproblem corresponding to the node. */
    private boolean isLpFeasible;
    private double nodeLpObj;
    
    /** arc use to generate children nodes. */
    private Arc arcToBranch;
    
    /** 当前节点对应的可行解的路径索引. */
    private ArrayList<Integer> pathIndicesInNodeSol;
    
    /**
     * generate root node.
     * 
     * @param vrptwIns VRPTW instance
     */
    BabNode(Vrptw vrptwIns) {
        BabNode.vrptwIns = vrptwIns;
        infeasiblePathIndices = new ArrayList<>(Parameters.INITIAL_CAPACITY);
        parent = null;
        depth = 0;
    }
    
    /**
     * Create a Instance BabNode.
     * 
     * @param parent parent of current node
     * @param fromVertexId start vertex of the branch arc
     * @param toVertexId end vertex of the branch arc
     * @param branchValue branch value
     */
    BabNode(BabNode parent, int fromVertexId, int toVertexId, int branchValue) {
        if (parent == null || branchValue > 1 || branchValue < -Parameters.EPS) {
            throw new IllegalArgumentException("Error in branch scheme, please check.");
        }
        
        this.infeasiblePathIndices = new ArrayList<>(parent.infeasiblePathIndices);

        this.parent = parent;
        this.depth = parent.depth + 1;
        this.branchArcOfParent = new BranchArc(fromVertexId, toVertexId, branchValue);
    }
    
    /**
     * Use column generation algorithm to solve the RMLP for the node, <br>
     * If it's infeasible, set the IloException{@link #nodeLpObj} to be big enough.
     * 
     * @throws IloException
     */
    void columnGeneration(VrptwMasterProblem masterProblem, AbstractPriceProblem priceProblem) throws IloException {
        
        // Update the masterProblem based on the branch arc
        this.updateInfeasiblePathSet(masterProblem);
        
        // update the time matrix
        ArrayList<BranchArc> historyBranchArcs = this.getHistoryBranchArcs();
        double[][] timeMatrix = this.updateTimeMatrix(historyBranchArcs);
                
        // column generation process
        int countIter = 0;
        while (true) {
            countIter++;
            // solve the RMLP
            if (!masterProblem.solveLp()) {
//                if (countIter == 0 && this.branchArcOfParent.branchValue == 0) {
//                    // 初始路径不可行，由于分支导致之前生成的大量路径被“禁止”，需要生成新的初始路径
//                    
//                }
                
                // The RMLP is infeasible, set the cost to be big enough
                this.isLpFeasible = false;
                return;
            }
                        
            // solve price problem
            priceProblem.solve(masterProblem.getDualValOfCusCstr(), timeMatrix);
            
            // if the reduced cost > 0, MLP's solution found, stop; otherwise, add column
            double reduceCost = priceProblem.getRevisedCostOfShortestPath() - masterProblem.getDualValOfVehNum();
            if (reduceCost > -Parameters.EPS) {
                this.isLpFeasible = true;
                this.nodeLpObj = masterProblem.getObjective();
                this.arcToBranch = masterProblem.findBranchArc();
                
                if (this.arcToBranch == null) {
                    // 可行解，存储对应的路径索引
                    pathIndicesInNodeSol = new ArrayList<>(Parameters.INITIAL_CAPACITY);
                    double[] usePath = masterProblem.getVarValue();
                    int length = usePath.length;
                    for (int i = 0; i < length; i++) {
                        if (usePath[i] > 1 - Parameters.EPS) {
                            pathIndicesInNodeSol.add(i);
                        }
                    }
                }
                
                return;
            }
            
//            System.out.println(masterProblem.getDualValOfCusCstr().values().toString());
//            System.out.println(countIter + ", Obj-" + masterProblem.getObjective() + ", RC-" + reduceCost + "\n");
            
            // add shortest path to RMLP as new column
            for (Path p : priceProblem.getShortestPath()) {
                masterProblem.addColumn(p);
            }
            
        }
        
    }
    
    /**
     * update the feasible path set(columns) in RMLP based on the branch arc.
     * 
     * @param masterProblem RMLP
     * @throws IloException 
     */
    private void updateInfeasiblePathSet(VrptwMasterProblem masterProblem) throws IloException {       
        if (parent == null) {
            return;
        }
        
        Path[] paths = masterProblem.getPaths();
        int pathNum = paths.length;
        
        for (int i = 0; i < pathNum; i++) {
            if (infeasiblePathIndices.contains(i)) {
                continue;
            }
            
            ArrayList<Integer> vertexIds = paths[i].getVertexIds();
            int posOfBranchArcFrom = vertexIds.indexOf(branchArcOfParent.getFromVertexId());
            
            if (branchArcOfParent.branchValue == 0) {
                // Path pass through branch arc is infeasible
                if (posOfBranchArcFrom != -1 
                        && vertexIds.get(posOfBranchArcFrom + 1) == branchArcOfParent.getToVertexId()) {
                    infeasiblePathIndices.add(i);
                }
                continue;
            }            
            
            // branchValue = 1 is below
            // Path with arc starting from "branchArcFromId" but not ending at "branchArcToId" is infeasible
            if (posOfBranchArcFrom != -1 
                    && vertexIds.get(posOfBranchArcFrom + 1) != branchArcOfParent.getToVertexId()) {
                infeasiblePathIndices.add(i);
            }
            
            
            // Path with arc ending at "branchArcToId" but not starting from "branchArcFromId" is infeasible
            // Except the end of branch arc is end depot
            if (branchArcOfParent.getToVertexId() == vrptwIns.getVertexNum() - 1) {
                continue;
            }
            int posOfBranchArcTo = vertexIds.indexOf(branchArcOfParent.getToVertexId());
            if (posOfBranchArcTo != -1 && vertexIds.get(posOfBranchArcTo - 1) != branchArcOfParent.getFromVertexId()) {
                infeasiblePathIndices.add(i);
            }
            
        }
        
        // update the decision variables in the RMLP
        masterProblem.updateFeasiblePathSet(infeasiblePathIndices);
    }
    
    /**
     * get branch info, here we forbid arcs by setting their travel time to big enough: <br>
     * 1 if brachValue = 0，set the travel time of branch arc to big enough; <br>
     * 2 if branchValue = 1，set the travel time of the arcs starting from "fromVertex" or ending at
     * "toVertex" to be big enough except branch arc. <br>
     * 
     * @param historyBranchArcs all branch arcs of current node's ancestors in the branch and bound tree
     * @return new time matrix after branch
     */
    private double[][] updateTimeMatrix(List<BranchArc> historyBranchArcs) {
        if (parent == null) {
            return vrptwIns.getTimeMatrix();
        }
        
        // time matrix initialization
        int vertexNum = vrptwIns.getVertexNum();
        double[][] timeMatrix = new double[vertexNum][vertexNum];
        for (int i = 0; i < vertexNum; i++) {
            for (int j = 0; j < vertexNum; j++) {
                timeMatrix[i][j] = vrptwIns.getTimeMatrix()[i][j];
            }
        }
        
        for (BranchArc arc: historyBranchArcs) {
            // branch value = 0
            if (arc.branchValue == 0) {
                timeMatrix[arc.getFromVertexId()][arc.getToVertexId()] = Parameters.BIG_TRAVEL_TIME;
                continue;
            }
            
            // branch value = 1, special case:
            // 1. when "fromVertexId" = 0(startDepot), set the travel time(TT) of arcs ending at "toVertex"
            // to be big enough except branch arc, so the vehicle must pass branch arc to serve "toVertex"
            // 2. when "toVertexId" = vertexNum(endDepot), just set the TT of arcs starting from "fromVertex"
            // to be big enough except branch arc, so the vehicle must pass branch arc after serve "fromVertex"
            // Attention: the arc from start depot to end depot is impossible to be chosen in path
            if (arc.getFromVertexId() != 0) {
                // fromVertex is not the start depot
                int j;
                for (j = 0; j < arc.getToVertexId(); j++) {
                    timeMatrix[arc.getFromVertexId()][j] = Parameters.BIG_TRAVEL_TIME;
                }
                for (j = j + 1; j < vertexNum; j++) {
                    timeMatrix[arc.getFromVertexId()][j] = Parameters.BIG_TRAVEL_TIME;
                }
                
            }
            
            if (arc.getToVertexId() != vertexNum) {
                // toVertex is not the end depot
                int i;
                for (i = 0; i < arc.getFromVertexId(); i++) {
                    timeMatrix[i][arc.getToVertexId()] = Parameters.BIG_TRAVEL_TIME;
                }
                for (i = i + 1; i < vertexNum; i++) {
                    timeMatrix[i][arc.getToVertexId()] = Parameters.BIG_TRAVEL_TIME;
                }
                
            }
            
        }
        
        return timeMatrix;
    }
        
    /**
     * Get all branch arcs of current node's ancestors in the branch and bound tree.
     * 
     * @return the array list of the branch arcs
     */
    private ArrayList<BranchArc> getHistoryBranchArcs() {
        ArrayList<BranchArc> branchArcs = new ArrayList<>(Parameters.INITIAL_CAPACITY);
        if (parent == null) {
            return branchArcs;
        }
        
        // add the branch arcs of current node's ancestors
        BabNode node = this;
        branchArcs.add(node.branchArcOfParent);
        while ((node = node.parent) != null) {
            if (node.branchArcOfParent != null) {
                branchArcs.add(node.branchArcOfParent);
            }
        }
        
        return branchArcs;
    }
        
    /**
     * Branch and Bound Tree 的分支过程中采用的弧.
     * 
     * @author Xiong Wangqi
     * @version V1.0
     * @since JDK1.8
     */
    private class BranchArc extends Arc {
        /** value of the branch arc, 1 if the route must pass the arc, 0 otherwise. */
        final int branchValue;
        
        BranchArc(int fromVertexId, int toVertexId, int branchValue) {
            super(fromVertexId, toVertexId);
            this.branchValue = branchValue;
        }
        
        @Override
        public String toString() {
            return String.format("Arc (%d, %d) -> %d", this.getFromVertexId(), this.getToVertexId(), branchValue);
        }
    }
    
    ArrayList<Integer> getPathIndicesInNodeSol() {
        return pathIndicesInNodeSol;
    }
    
    int getDepth() {
        return depth;
    }
        
    boolean isLpFeasible() {
        return isLpFeasible;
    }
    
    double getNodeLpObj() {
        return nodeLpObj;
    }
    
    Arc getArcToBranch() {
        return arcToBranch;
    }
    
    @Override
    public int compareTo(BabNode that) {
        // TODO Auto-generated method stub
        return Double.compare(nodeLpObj, that.nodeLpObj);
    }
    
}

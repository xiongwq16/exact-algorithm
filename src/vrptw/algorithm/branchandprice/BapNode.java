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
class BapNode implements Comparable<BapNode> {
    private static Vrptw originVrptwIns;
    
    /** 由于分支被禁止的路径索引. */
    private ArrayList<Integer> infeasiblePathIndices;
    
    /** parent of the node in the branch and bound tree. */
    private BapNode parent;
    /** The depth of current node in the branch and bound tree. */
    private int depth;
    /** store the branch info from parent to current node. */
    private BranchArc branchArcFromParent;
    
    /** LP objective of subproblem corresponding to the node. */
    private boolean isLpFeasible;
    private double nodeLpObj;
    
    /** arc use to generate children nodes. */
    private Arc arcToBranch;
    
    /** 当前节点对应的 MIP 解的路径索引. */
    private ArrayList<Integer> pathIndicesInMipSol;
    
    /**
     * Create root node.
     * 
     * @param vrptwIns VRPTW instance
     */
    BapNode(Vrptw vrptwIns) {
        BapNode.originVrptwIns = vrptwIns;
        
        isLpFeasible = true;
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
    BapNode(BapNode parent, int fromVertexId, int toVertexId, int branchValue) {
        if (parent == null || branchValue > 1 || branchValue < -Parameters.EPS) {
            throw new IllegalArgumentException("Error in branch scheme, please check.");
        }
        
        this.isLpFeasible = true;
        this.infeasiblePathIndices = new ArrayList<>(parent.infeasiblePathIndices);

        this.parent = parent;
        this.depth = parent.depth + 1;
        
        this.branchArcFromParent = new BranchArc(fromVertexId, toVertexId, branchValue);
    }
    
    /**
     * Use column generation algorithm to solve the RMLP for the node.
     * 
     * @throws IloException
     */
    void columnGeneration(BapMasterProblem masterProblem, AbstractPriceProblem priceProblem) throws IloException {
        
        // Update the masterProblem based on the branch arc
        this.updateInfeasiblePathSet(masterProblem);
        
        // update the time matrix
        ArrayList<BranchArc> historyBranchArcs = this.getHistoryBranchArcs();
        double[][] timeMatrix = this.calTimeMatrix(historyBranchArcs);
                
        // column generation process
        int countIter = 0;
        while (true) {
            countIter++;
            // solve the RMLP
            if (!masterProblem.solveLp()) {
                // TODO 分支使得已生成的部分路径被“禁止”，导致子节点无初始可行解，采用 Solomon Insertion 生成初始路径，需进一步优化
                if (countIter == 1) {
                    Path[] initialPaths = masterProblem.generateInitailPaths(originVrptwIns, timeMatrix);
                    if (initialPaths == null) {
                        // The RMLP is infeasible, set the cost to be big enough
                        this.isLpFeasible = false;
                        return;
                    }
                    
                    for (int i = 0; i < initialPaths.length; i++) {
                        int index = masterProblem.isPathExit(initialPaths[i]);
                        if (index != -1) {
                            masterProblem.addColumn(initialPaths[i]);
                        }
                    }
                    
                } else {
                    this.isLpFeasible = false;
                    return;
                }
                
            }
            
            // solve price problem, update the time matrix first
            priceProblem.updateTimeMatrix(timeMatrix);
            priceProblem.solve(masterProblem.getDualValOfCusCstr());
            
            // if the reduced cost > 0, MLP's solution found, stop; otherwise, add column
            double reduceCost = priceProblem.getRevisedCostOfShortestPath();
            if (reduceCost > -Parameters.EPS) {
                this.nodeLpObj = masterProblem.getObjective();
                this.arcToBranch = masterProblem.findBranchArc();
                
                if (this.arcToBranch == null) {
                    // 可行解，存储对应的路径索引
                    pathIndicesInMipSol = new ArrayList<>(Parameters.INITIAL_CAPACITY);
                    double[] usePath = masterProblem.getVarValue();
                    int length = usePath.length;
                    for (int i = 0; i < length; i++) {
                        if (usePath[i] > 1 - Parameters.EPS) {
                            pathIndicesInMipSol.add(i);
                        }
                    }
                    
                }
                
                System.out.println("LP Optimal" + ", Obj-" + this.nodeLpObj);
                
                return;
            }
            
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
    private void updateInfeasiblePathSet(BapMasterProblem masterProblem) throws IloException {       
        if (parent == null) {
            return;
        }
        
        Path[] paths = masterProblem.getPaths();
        int pathNum = paths.length;
        
        int fromVertexId = branchArcFromParent.getFromVertexId();
        int toVertexId = branchArcFromParent.getToVertexId();
        for (int i = 0; i < pathNum; i++) {
            if (infeasiblePathIndices.contains(i)) {
                continue;
            }
            
            ArrayList<Integer> vertexIds = paths[i].getVertexIds();
            int posOfBranchArcFrom = vertexIds.indexOf(fromVertexId);
            int posOfBranchArcTo = vertexIds.indexOf(toVertexId);
            
            if (branchArcFromParent.branchValue == 0) {
                // Path pass through branch arc is infeasible
                if (posOfBranchArcFrom != -1 && vertexIds.get(posOfBranchArcFrom + 1) == toVertexId) {
                    infeasiblePathIndices.add(i);
                }
                continue;
            }
            
            // branch value = 1 is below
            // Attention: the arc from start depot to end depot is impossible to be chosen to branch
            
            // 1. when the fromVertex is start depot, path visiting "toVertex" but not as first customer, is infeasible
            if (fromVertexId == 0) {
                if (posOfBranchArcTo != -1 && vertexIds.get(posOfBranchArcTo - 1) != 0) {
                    infeasiblePathIndices.add(i);
                }
                continue;
            }
            
            // 2. when the toVertex is end depot, path visiting "fromVertex" but not as last customer, is infeasible
            if (toVertexId == originVrptwIns.getVertexNum() - 1) {
                if (posOfBranchArcFrom != -1 
                        && vertexIds.get(posOfBranchArcFrom + 1) != originVrptwIns.getVertexNum() - 1) {
                    infeasiblePathIndices.add(i);
                }
                continue;
            }
            
            // 3. when both the start and end vertex of branch arc are customer
            if (posOfBranchArcTo != -1 && vertexIds.get(posOfBranchArcTo - 1) != fromVertexId) {
                // path with arc ending at "branchArcTo" but not starting from "branchArcFromId" is infeasible
                infeasiblePathIndices.add(i);
            }
            if (posOfBranchArcFrom != -1 && vertexIds.get(posOfBranchArcFrom + 1) != toVertexId) {
                // path with arc starting from "branchArcFrom" but not ending at "branchArcTo" is infeasible
                infeasiblePathIndices.add(i);
            }
            
        }
        
        // update the decision variables in the RMLP
        masterProblem.updateFeasiblePathSet(infeasiblePathIndices);
    }
    
    /**
     * get new time matrix after branch, here we forbid arcs by setting their travel time to big enough: <br>
     * 1 if brachValue = 0，set the travel time of branch arc to big enough; <br>
     * 2 if branchValue = 1，set the travel time of the arcs starting from "fromVertex" or ending at
     * "toVertex" to be big enough except branch arc. <br>
     * 
     * @param historyBranchArcs all branch arcs of current node's ancestors in the branch and bound tree
     * @return new time matrix after branch
     */
    private double[][] calTimeMatrix(List<BranchArc> historyBranchArcs) {
        if (parent == null) {
            return originVrptwIns.getTimeMatrix();
        }
        
        // time matrix initialization
        int vertexNum = originVrptwIns.getVertexNum();
        double[][] timeMatrix = new double[vertexNum][vertexNum];
        for (int i = 0; i < vertexNum; i++) {
            for (int j = 0; j < vertexNum; j++) {
                timeMatrix[i][j] = originVrptwIns.getTimeMatrix()[i][j];
            }
        }
        
        // calculate new time matrix
        for (BranchArc arc: historyBranchArcs) {
            int fromVertexId = arc.getFromVertexId();
            int toVertexId = arc.getToVertexId();
            // branch value = 0
            if (arc.branchValue == 0) {
                timeMatrix[fromVertexId][toVertexId] = Parameters.BIG_TRAVEL_TIME;
                continue;
            }
            
            // branch value = 1
            // Attention: the arc from start depot to end depot is impossible to be chosen to branch
            
            // 1. when "fromVertexId" = 0(startDepot), set the travel time(TT) of arcs ending at "toVertex"
            // to be big enough except branch arc, so the vehicle must pass branch arc to serve "toVertex"
            if (fromVertexId == 0) {
                for (int i = 1; i < vertexNum; i++) {
                    timeMatrix[i][toVertexId] = Parameters.BIG_TRAVEL_TIME;
                }
                continue;
            }
            
            // 2. when "toVertexId" = vertexNum - 1(endDepot), just set the TT of arcs starting from "fromVertex"
            // to be big enough except branch arc, so the vehicle must pass branch arc after serve "fromVertex"
            if (toVertexId == vertexNum - 1) {
                for (int j = 0; j < vertexNum - 1; j++) {
                    timeMatrix[fromVertexId][j] = Parameters.BIG_TRAVEL_TIME;
                }
                continue;
            }
            
            // 3. both the start and end vertex of branch arc are customer
            int j;
            for (j = 0; j < toVertexId; j++) {
                timeMatrix[fromVertexId][j] = Parameters.BIG_TRAVEL_TIME;
            }
            for (j = j + 1; j < vertexNum; j++) {
                timeMatrix[fromVertexId][j] = Parameters.BIG_TRAVEL_TIME;
            }
            
            int i;
            for (i = 0; i < fromVertexId; i++) {
                timeMatrix[i][toVertexId] = Parameters.BIG_TRAVEL_TIME;
            }
            for (i = i + 1; i < vertexNum; i++) {
                timeMatrix[i][toVertexId] = Parameters.BIG_TRAVEL_TIME;
            }
            
            // forbid the arc in the opposite direction
            timeMatrix[toVertexId][fromVertexId] = Parameters.BIG_TRAVEL_TIME;
            
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
        BapNode node = this;
        branchArcs.add(node.branchArcFromParent);
        while ((node = node.parent) != null) {
            if (node.branchArcFromParent != null) {
                branchArcs.add(node.branchArcFromParent);
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
    
    ArrayList<Integer> getPathIndicesInMipSol() {
        return pathIndicesInMipSol;
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
    public int compareTo(BapNode that) {
        return Double.compare(nodeLpObj, that.nodeLpObj);
    }
    
}

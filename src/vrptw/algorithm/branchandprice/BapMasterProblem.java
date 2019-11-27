package vrptw.algorithm.branchandprice;

import ilog.concert.IloColumn;
import ilog.concert.IloException;
import ilog.concert.IloNumVar;
import ilog.concert.IloObjective;
import ilog.concert.IloRange;
import ilog.cplex.IloCplex;
import ilog.cplex.IloCplex.UnknownObjectException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import vrptw.algorithm.solomoninsertion.SolomonInsertion;
import vrptw.parameter.Parameters;
import vrptw.problem.Arc;
import vrptw.problem.Vertex;
import vrptw.problem.Vrptw;
import vrptw.solution.Path;

/**
 * Master Problem of VRPTW in the Branch and Price.
 * 
 * @author Xiong Wangqi
 * @version V1.0
 * @since JDK1.8
 */
class BapMasterProblem {
    private Vrptw vrptwIns;
    
    private IloCplex rmlpSolver;
    /** 每个客户都应该被访问一次. */
    private Map<Integer, IloRange> cusVisitedCstrs;
    /** Dual values of sum(x[i][j][k] for j in vertexes, k in vehicles) = 1. */
    private Map<Integer, Double> dualValOfCusCstr;
    
    /** 求解过程中生成的路径数量. */
    private int pathNum;
    /** 决策变量，路径是否采用. */
    private IloNumVar[] usePath;
    /** 决策变量对应的路径. */
    private Path[] paths;
    
    BapMasterProblem(Vrptw vrptwIns) throws IloException {
        this.vrptwIns = vrptwIns;
        
        cusVisitedCstrs = new HashMap<Integer, IloRange>((int)(vrptwIns.getCusNum() / Parameters.LOADER_FACTOR) + 1);
        dualValOfCusCstr = new HashMap<Integer, Double>((int)(vrptwIns.getCusNum() / Parameters.LOADER_FACTOR) + 1);
        
        pathNum = 0;
        usePath = new IloNumVar[vrptwIns.getVehNum()];
        paths = new Path[vrptwIns.getVehNum()];
        
        this.initialModel();
    }
    
    /**
     * add column based on the new path.
     * 
     * @param p path to add
     * @throws IloException
     */
    void addColumn(Path p) throws IloException {
        // 1. coefficient in objective - route cost
        IloObjective obj = rmlpSolver.getObjective();
        IloColumn col = rmlpSolver.column(obj, p.getCost());
        
        // 2.1. coefficient in service constraints - visit time of customers on the route
        for (Vertex cus: vrptwIns.getCustomers()) {
            // customer service constraint
            int cusId = cus.getId();
            col = col.and(rmlpSolver.column(cusVisitedCstrs.get(cusId), p.getCusVisitedTime().get(cusId)));
        }
        
        // 3. add variable and store the path
        IloNumVar use = rmlpSolver.numVar(col, 0, Double.MAX_VALUE, "Path " + (pathNum + 1));
        this.addPath(p, use);
    }
        
    /**
     * solve RMLP.
     * 
     * @return is the RMLP solvable?
     * @throws IloException
     */
    boolean solveLp() throws IloException {
        if (!rmlpSolver.solve()) {
            return false;
        }
        
        for (Map.Entry<Integer, IloRange> entry: cusVisitedCstrs.entrySet()) {
            dualValOfCusCstr.put(entry.getKey(), rmlpSolver.getDual(entry.getValue()));
        }
        
        return true;
    }
    
    void updateFeasiblePathSet(ArrayList<Integer> infeasiblePathIndices) throws IloException {
        for (int i = 0; i < pathNum; i++) {
            usePath[i].setUB(Double.MAX_VALUE);
        }
        
        for (int index: infeasiblePathIndices) {
            usePath[index].setUB(0);
        }
    }
    
    /**
     * find a branch arc, if the solution is feasible, return [-1, -1].
     * 
     * @return fromVertexId and toVertexId of the branch arc
     * @throws IloException 
     * @throws UnknownObjectException 
     */
    Arc findBranchArc() throws UnknownObjectException, IloException {
        int vertexNum = vrptwIns.getVertexNum();
        double[][] flow = new double[vertexNum][vertexNum];
        for (int k = 0; k < pathNum; k++) {
            double theta = rmlpSolver.getValue(usePath[k]);
            if (theta > Parameters.EPS) {
                ArrayList<Integer> vertexIds = paths[k].getVertexIds();
                
                int currVertexId;
                int nextVertexId;
                for (int i = 0; i < vertexIds.size() - 1; i++) {
                    currVertexId = vertexIds.get(i);
                    nextVertexId = vertexIds.get(i + 1);
                    flow[currVertexId][nextVertexId] += theta;
                }
            }
            
        }
        
        Arc branchArc = null;
        // choose the fractional arc with max {c[i][j] * ( min {flow[i][j], |1 - flow[i][j]|})}
        double maxCost = Double.NEGATIVE_INFINITY;
        double cost;
        double[][] distmatrix = vrptwIns.getDistMatrix();
        for (int i = 0; i < vertexNum; i++) {
            for (int j = 0; j < vertexNum; j++) {
                if (flow[i][j] < Parameters.EPS) {
                    continue;
                }
                
                if (flow[i][j] < 1 - Parameters.EPS || flow[i][j] > 1 + Parameters.EPS) {
                    cost = Math.min(flow[i][j], Math.abs(1 - flow[i][j]));
                    cost = cost * distmatrix[i][j];
                    if (cost > maxCost) {
                        maxCost = cost;
                        branchArc = new Arc(i, j);
                    }
                    
                }
                
            }
            
        }
        
        return branchArc;
    }
        
    double[] getVarValue() throws UnknownObjectException, IloException {
        double[] varValues = new double[pathNum];
        for (int i = 0; i < pathNum; i++) {
            varValues[i] = rmlpSolver.getValue(usePath[i]);
        }
        return varValues;
    }
    
    double getObjective() throws IloException {
        return rmlpSolver.getObjValue();
    }
    
    /**
     * Releases all Cplex objects attached to the RMLP.
     */
    void end() {
        rmlpSolver.end();
    }
    
    /**
     * 生成初始路径.
     * 
     * @param vrptwIns VRPTW Instance
     * @param timeMatrix 考虑 branchArc 后的时间矩阵
     * @return 初始路径数组
     */
    Path[] generateInitailPaths(Vrptw vrptwIns, double[][] timeMatrix) {
        // 调用 Solomon Insertion 生成初始解
        SolomonInsertion i1 = new SolomonInsertion(vrptwIns, timeMatrix);
        
        if (!i1.constructRoutes()) {
            return null;
        }
        
        return i1.getPaths();
    }
    
    Path[] getPaths() {
        // 去除因 resizing 导致的空路径
        Path[] pathsWithoutNull = new Path[pathNum];
        for (int i = 0; i < pathNum; i++) {
            pathsWithoutNull[i] = paths[i];
        }
        
        return pathsWithoutNull;
    }
    
    int isPathExit(Path p) {
        for (int i = 0; i < pathNum; i++) {
            if (p.equals(paths[i])) {
                return i;
            }
        }
        
        return -1;
    }
    
    private void initialModel() throws IloException {
        rmlpSolver = new IloCplex();
        rmlpSolver.addMinimize();
        
        // 客户必须被服务约束
        for (Vertex cus: vrptwIns.getCustomers()) {
            // Revise Set Partition to Set Covering Model
            cusVisitedCstrs.put(cus.getId(), rmlpSolver.addRange(1, Double.MAX_VALUE, "Cus " + cus.getId()));
        }
        
        // Parameter settings
        this.setCplexParams();
    }
    
    /**
     * record the path and the decision variable corresponding to it.
     * 
     * @param path given path
     * @param use decision variable，use the path or not
     */
    private void addPath(Path path, IloNumVar use) {
        if (pathNum == usePath.length) {
            resizing(2 * pathNum);
        }
        
        paths[pathNum] = path;
        usePath[pathNum] = use;
        
        pathNum++;
    }
    
    private void setCplexParams() throws IloException {
        // Use primal simplex
        rmlpSolver.setParam(IloCplex.IntParam.RootAlgorithm, IloCplex.Algorithm.Primal);
        // set simplex tolerances
        rmlpSolver.setParam(IloCplex.Param.Simplex.Tolerances.Optimality, 1e-9);
        rmlpSolver.setParam(IloCplex.Param.Simplex.Tolerances.Markowitz, 0.999);
        rmlpSolver.setParam(IloCplex.Param.Simplex.Tolerances.Feasibility, Parameters.EPS);
        
        rmlpSolver.setOut(null);
    }
    
    /**
     * resizing {@link #usePath} and {@link #paths}.
     * 
     * @param capacity 新容量
     */
    private void resizing(int capacity) {
        assert capacity >= pathNum;
        IloNumVar[] tempVar = new IloNumVar[capacity];
        Path[] tempPaths = new Path[capacity];

        for (int i = 0; i < pathNum; i++) {
            tempVar[i] = usePath[i];
            tempPaths[i] = paths[i];
        }
        usePath = tempVar;
        paths = tempPaths;
    }
    
    /**
     * @return 客户必须被访问约束对应的对偶变量值.
     */
    Map<Integer, Double> getDualValOfCusCstr() {
        return dualValOfCusCstr;
    }
        
}

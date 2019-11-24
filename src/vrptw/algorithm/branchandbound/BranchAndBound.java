package vrptw.algorithm.branchandbound;

import ilog.concert.IloException;
import ilog.concert.IloIntVar;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloNumExpr;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;
import ilog.cplex.IloCplex.BranchDirection;
import ilog.cplex.IloCplex.UnknownObjectException;

import java.util.ArrayList;

import vrptw.algorithm.VrptwExactAlgorithm;
import vrptw.parameter.Parameters;
import vrptw.problem.Vertex;
import vrptw.problem.Vrptw;
import vrptw.solution.Path;
import vrptw.solution.VrptwSolution;

/**
 * Branch and Bound Algorithm，如求解耗时过长，可以将 Best known solution 中的车辆数作为车辆数约束，
 * 本代码的核心是展示自定义 BranchCallback 的使用，使用的分支策略也较为简单，没有考虑太多的优化，
 * 有兴趣的可以继续优化 Cplex 的求解参数，或者参考 branch and price 中的分支框架设计自己的分支定界算法.
 * 
 * @author Xiong Wangqi
 * @version V1.0
 * @since JDK1.8
 */
public class BranchAndBound implements VrptwExactAlgorithm {    
    private Vrptw vrptwIns;
    private int vertexNum;
    private int vehNum;
    /** false if arc(i, j) doesn't satisfy the capacity or time window constraints, true otherwise. */
    private boolean[][] isFeasibleArc;    
    
    private IloCplex vrptwModel;
    /** decision variables x[i][j][k], 1 if vehicle k pass arc (i, j), 0 otherwise. */
    private IloNumVar[][][] x;
    /** decision variables s[i][k], start service time of vehicle k at vertex i. */
    private IloNumVar[][] s;
    
    private int nodeNum;
    private VrptwSolution vrptwSol;
    
    /**
     * Create a Instance of Branch and Price.
     * 
     * @param vrptwIns VRPTW instance
     * @throws IloException 
     */
    public BranchAndBound(Vrptw vrptwIns) throws IloException {                
        this.vrptwIns = vrptwIns;
        vertexNum = vrptwIns.getVertexNum();
        // TODO  可使用 Best known solution 的车辆数，加快求解速度，否则耗时容易过长
        vehNum = vrptwIns.getVehNum();
        
        isFeasibleArc = new boolean[vertexNum][vertexNum];
        for (int i = 0; i < vertexNum; i++) {
            for (int j = 0; j < vertexNum; j++) {
                isFeasibleArc[i][j] = true;
            }
        }
        this.preprocessArcs();
        
        vrptwModel = new IloCplex();
        x = new IloIntVar[vertexNum][vertexNum][vehNum];
        s = new IloNumVar[vertexNum][vehNum];
        
        nodeNum = 1;
    }
    
    @Override
    public void solve() throws IloException {
        System.out.println("Branch and bound algorithm");
        System.out.println("--------------------------------------------");
        
        final double startTime = System.currentTimeMillis();
        
        // build model
        this.buildModel();
        
        // set cplex parameters
        this.setCplexParams();
        
        // use DIY branch callback
        vrptwModel.use(new BranchCallback());
        
        if (!vrptwModel.solve()) {
            System.out.println("VRPTW given is infeasible");
            vrptwModel.end();
            return;
        }
        
        double timeConsume = System.currentTimeMillis() - startTime;
        
        vrptwSol = new VrptwSolution(vrptwIns, this.getPaths(), vrptwModel.getObjValue());
        vrptwSol.output(timeConsume, nodeNum);
        
        vrptwModel.end();
    }
    
    private void buildModel() throws IloException {
        // define decision variables
        for (int i = 0; i < vertexNum; i++) {
            for (int k = 0; k < vehNum; k++) {
                s[i][k] = vrptwModel.numVar(0, Double.MAX_VALUE, "s" + i + "," + k);
            }
            
            for (int j = 0; j < vertexNum; j++) {
                if (!isFeasibleArc[i][j]) {
                    x[i][j] = null;
                    continue;
                }
                
                for (int k = 0; k < vehNum; k++) {
                    x[i][j][k] = vrptwModel.boolVar("x" + i + "," + j + "," + k);
                }
            }
            
        }
        
        // objective
        double[][] distMatrix = vrptwIns.getDistMatrix();
        IloLinearNumExpr obj = vrptwModel.linearNumExpr();
        for (int i = 0; i < vertexNum; i++) {
            for (int j = 0; j < vertexNum; j++) {
                if (!isFeasibleArc[i][j]) {
                    continue;
                }
                
                for (int k = 0; k < vehNum; k++) {
                    obj.addTerm(distMatrix[i][j], x[i][j][k]);
                }
                
            }
            
        }
        vrptwModel.addMinimize(obj);
        
        // constraints 2
        for (int i = 1; i < vertexNum - 1; i++) {
            IloLinearNumExpr expr = vrptwModel.linearNumExpr();
            for (int k = 0; k < vehNum; k++) {
                for (int j = 1; j < vertexNum; j++) {
                    if (isFeasibleArc[i][j]) {
                        expr.addTerm(1.0, x[i][j][k]);
                    }
                }
                
            }
            vrptwModel.addEq(expr, 1);
        }
        
        // constraints 3
        ArrayList<Vertex> vertexes = vrptwIns.getVertexes();
        double capacity = vrptwIns.getVehicle().getCapacity();
        for (int k = 0; k < vehNum; k++) {
            IloLinearNumExpr expr = vrptwModel.linearNumExpr();
            for (int i = 1; i < vertexNum - 1; i++) {
                Vertex vi = vertexes.get(i);
                for (int j = 0; j < vertexNum; j++) {
                    if (isFeasibleArc[i][j]) {
                        expr.addTerm(vi.getDemand(), x[i][j][k]);
                    }
                }
            }
            vrptwModel.addLe(expr, capacity);
        }
        
        // constraints 4
        for (int k = 0; k < vehNum; k++) {
            IloLinearNumExpr expr = vrptwModel.linearNumExpr();
            for (int j = 1; j < vertexNum; j++) {
                if (isFeasibleArc[0][j]) {
                    expr.addTerm(1.0, x[0][j][k]);
                }
                
            }
            vrptwModel.addEq(expr, 1);
        }
        
        // constraints 5
        for (int k = 0; k < vehNum; k++) {
            for (int h = 1; h < vertexNum - 1; h++) {
                IloLinearNumExpr subExpr1 = vrptwModel.linearNumExpr();
                IloLinearNumExpr subExpr2 = vrptwModel.linearNumExpr();
                for (int i = 0; i < vertexNum; i++) {
                    if (isFeasibleArc[i][h]) {
                        subExpr1.addTerm(1.0, x[i][h][k]);
                    }
                    
                    if (isFeasibleArc[h][i]) {
                        subExpr2.addTerm(-1.0, x[h][i][k]);
                    }
                    
                }
                vrptwModel.addEq(vrptwModel.sum(subExpr1, subExpr2), 0);
            }
            
        }
        
        // constraints 6
        for (int k = 0; k < vehNum; k++) {
            IloLinearNumExpr expr = vrptwModel.linearNumExpr();
            for (int i = 0; i < vertexNum - 1; i++) {
                if (isFeasibleArc[i][vertexNum - 1]) {
                    expr.addTerm(1, x[i][vertexNum - 1][k]);
                }
                
            }
            vrptwModel.addEq(expr, 1);
        }
        
        // constraints 7
        double bigm = Parameters.BIG_M;
        double[][] timeMatrix = vrptwIns.getTimeMatrix();
        for (int k = 0; k < vehNum; k++) {
            for (int i = 0; i < vertexNum; i++) {
                Vertex vi = vertexes.get(i);
                for (int j = 0; j < vertexNum; j++) {
                    if (isFeasibleArc[i][j]) {
                        IloNumExpr expr1 = vrptwModel.sum(s[i][k], vi.getServiceTime() + timeMatrix[i][j]);
                        expr1 = vrptwModel.sum(expr1, vrptwModel.prod(-1, s[j][k]));
                        IloNumExpr expr2 = vrptwModel.prod(bigm, vrptwModel.sum(1, vrptwModel.prod(-1, x[i][j][k])));
                        vrptwModel.addLe(expr1, expr2);
                    }
                    
                }
                
            }
            
        }
        
        // constraints 8
        for (int i = 0; i < vertexNum; i++) {
            Vertex vi = vertexes.get(i);
            for (int k = 0; k < vehNum; k++) {
                vrptwModel.addLe(vi.getEarliestTime(), s[i][k]);
                vrptwModel.addLe(s[i][k], vi.getLatestTime());
            }
            
        }
        
    }
    
    private void setCplexParams() throws IloException {
        vrptwModel.setParam(IloCplex.Param.MIP.Tolerances.Integrality, Parameters.EPS);
        // Traditional: Use traditional branch-and-cut search.
        vrptwModel.setParam(IloCplex.Param.MIP.Strategy.Search, IloCplex.MIPSearch.Traditional);
        // 默认使用 BestBound 策略，不设置也可以
        vrptwModel.setParam(IloCplex.Param.MIP.Strategy.NodeSelect, IloCplex.NodeSelect.BestBound);
        // 间隔 50 个节点显示一次解的信息
        vrptwModel.setParam(IloCplex.Param.MIP.Interval, 50);
    }
    
    /**
     * find infeasible arcs.
     */
    private void preprocessArcs() {
        ArrayList<Vertex> vertexes = vrptwIns.getVertexes();
        double[][] timeMatrix = vrptwIns.getTimeMatrix();
        double capacity = vrptwIns.getVehicle().getCapacity();
        
        // find the arcs which don't satisfy the capacity or time window constraints
        for (int i = 0; i < vertexNum; i++) {
            Vertex v1 = vertexes.get(i);
            for (int j = 0; j < vertexNum; j++) {
                if (i == j) {
                    isFeasibleArc[i][j] = false;
                    continue;
                }
                
                Vertex v2 = vertexes.get(j);
                if (v1.getEarliestTime() + v1.getServiceTime() + timeMatrix[i][j] > v2.getLatestTime() 
                        || v1.getDemand() + v2.getDemand() > capacity) {
                    isFeasibleArc[i][j] = false;
                }
                
            }
            
        }
        
        for (int n = 0; n < vertexNum; n++) {
            // arc start end at start depot is infeasible
            isFeasibleArc[n][0] = false;
            // arc start from dummy depot is infeasible
            isFeasibleArc[vertexNum - 1][n] = false;
        }
        
    }
        
    /**
     * 根据解的情况生成路径.
     * 
     * @return 所有有效路径
     * @throws UnknownObjectException
     * @throws IloException
     */
    private ArrayList<Path> getPaths() throws UnknownObjectException, IloException {
        ArrayList<Path> usefulPaths = new ArrayList<>(Parameters.INITIAL_CAPACITY);
        for (int k = 0; k < vehNum; k++) {
            ArrayList<Integer> vertexIds =  new ArrayList<>(Parameters.INITIAL_CAPACITY);
            int i = 0;
            // add start depot
            vertexIds.add(0);
            
            while (true) {
                for (int j = 0; j < vertexNum; j++) {
                    if (isFeasibleArc[i][j] && vrptwModel.getValue(x[i][j][k]) > 1 - Parameters.EPS) {
                        vertexIds.add(j);
                        i = j;
                    }
                }
                
                if (i == vertexNum - 1) {
                    // arrive at dummy depot, break to find other paths
                    break;
                }
                
            }
            
            // only add useful paths
            if (vertexIds.size() > 2) {
                usefulPaths.add(new Path(vrptwIns, vertexIds));
            }
            
        }
        
        return usefulPaths;
    }
    
    /**
     * Branch on arc based on legacy callback.
     * 
     * @author Xiong Wangqi
     * @version V1.0
     * @since JDK1.8
     */
    private class BranchCallback extends IloCplex.BranchCallback {
        @Override
        protected void main() throws IloException {
            if (!getBranchType().equals(IloCplex.BranchType.BranchOnVariable)) {
                return;
            }

            int arcFrom = -1;
            int arcTo = -1;
            int vehicleId = -1;
            boolean loop = true;
            for (int i = 0; i < vertexNum && loop; i++) {
                for (int j = 0; j < vertexNum && loop; j++) {
                    if (!isFeasibleArc[i][j]) {
                        continue;
                    }
                    
                    for (int k = 0; k < vehNum && loop; k++) {
                        if (getFeasibility(x[i][j][k]).equals(IloCplex.IntegerFeasibilityStatus.Infeasible)) {
                            // cur_dif = get_dif(x[i][j][k]);
                            arcFrom = i;
                            arcTo = j;
                            vehicleId = k;
                            loop = false;
                        }

                    }

                }

            }

            if (arcFrom >= 0) {
                /*
                 * The invoking instance of Cplex may use this estimate to select nodes to process. 
                 * A poor estimate will not influence the correctness of the solution, but it may influence performance.
                 * Using the objective value of the current node is usually a safe choice.
                 */
                makeBranch(x[arcFrom][arcTo][vehicleId], 0, BranchDirection.Down, getObjValue());
                makeBranch(x[arcFrom][arcTo][vehicleId], 1, BranchDirection.Up, getObjValue());
                
                nodeNum += 2;
            }
            
        }

    }
    
}

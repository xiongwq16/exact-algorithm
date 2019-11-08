package vrptw.parameter;

/**
 * Parameters setting class.
 * 
 * @author Xiong Wangqi
 * @version V1.0
 * @since JDK1.8
 */
public class Parameters {
    public static final int RANDOM_SEED = 2048;
    
    public static final int INITIAL_CAPACITY = 16;
    public static final double LOADER_FACTOR = 0.75;
    
    /** Travel time big enough. */
    public static final double BIG_TRAVEL_TIME = Double.MAX_VALUE;
    /** EPS limits how small a number can be and still be treated as nonzero. */
    public static final double EPS = 1e-6;
    
    /** Solomon I1 插入算法的参数. */
    public static final int ALPHA1 = 1;
    public static final int ALPHA2 = 1 - ALPHA1;
    public static final int MIU = 1;
    public static final int LAMBDA = 1;
    
    /** Initial upper bound in the branch and bound algorithm. */
    public static final double BB_INITIAL_UPPERBOUND = Double.MAX_VALUE;
    /** BOUND_GAP limits how small the gap between upper bound and lower bound can be accepted. */
    public static final double BOUND_GAP = 1e-6;
    
    /** Use ESPPTWCC as price problem of VRPTW and solve it by label algorithm. */
    public static final String ESPPTWCC_LABEL = "ESPPTWCCViaLabel";
    /** Use SPPTWCC as price problem of VRPTW and solve it by label algorithm. */
    public static final String SPPTWCC_LABEL = "SPPTWCCViaLabel";
    
    /** Use SPPTWCC as price problem of VRPTW and solve it by label algorithm. */
    public static final String ESPPTWCC_PULSE = "ESPPTWCCViaPulse";
    
    /** Threads number of Pulse Algorithm. */
    public static final int THREAD_NUM = 10;
    /** Time step of Pulse Algorithm. */
    public static final double TIME_STEP = 4;
    /** Lower time limit to stop the bounding procedure, use 50 for 100-series and 100 for 200-series. */
    public static final double TIME_LIMIT_LB = 100;
}

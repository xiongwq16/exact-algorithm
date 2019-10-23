package vrptw.parameter;

/**
 * 常数类.
 * 
 * @author Xiong Wangqi
 * @version V1.0
 * @since JDK1.8
 */
public class Parameters {
    public static final int INITIAL_CAPACITY = 16;
    public static final double LOADER_FACTOR = 0.75;
    
    /** EPS limits how small a number can be and still be treated as nonzero. */
    public static final double EPS = 1e-6;
    
    /** Bound step for bound procedure of pulse algorithm. */
    public static final double BOUND_STEP = 4;
    
    /**
     * Lower time (resource) limit to stop the bounding procedure of pulse algorithm.
     * For 100-series we used 50 and for 200-series we used 100;
     */
    public static final double TIME_LB = 50;
}

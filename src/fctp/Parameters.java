package fctp;


/**
 * 常数设置.
 * 
 * @author Xiong Wangqi
 * @version V1.0
 * @since JDK1.8
 */
class Parameters {
    static final double LOADER_FACTOR = 0.75;
    
    /** The cutoff for rounding values of binary variables up. */
    static final double ROUNDUP = 0.5;
    
    /** EPS limits how small a flow can be and still be treated as nonzero. */
    static final double EPS = 1e-6;
    
    /** The milliseconds to seconds conversion factor. */
    static final double MS_TO_SEC = 0.001;
}

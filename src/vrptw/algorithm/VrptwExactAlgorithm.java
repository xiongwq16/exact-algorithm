package vrptw.algorithm;

import ilog.concert.IloException;

import java.io.IOException;

/**
 * VRPTW 精确算法统一接口.
 * 
 * @author Xiong Wangqi
 * @version V1.0
 * @since JDK1.8
 */
public interface VrptwExactAlgorithm {
    /**
     * 调用精确算法求解 VRPTW.
     * @throws IloException 
     * @throws IOException 
     */
    void solve() throws IloException, IOException;
}

package vrptw.problem;

/**
 * Arc class.
 * 
 * @author Xiong Wangqi
 * @version V1.0
 * @since JDK1.8
 */
public class Arc {
    /** start vertex of the fractional arc. */
    private int fromVertexId;
    /** end vertex of the fractional arc. */
    private int toVertexId;
    
    public Arc(int fromVertexId, int toVertexId) {
        this.fromVertexId = fromVertexId;
        this.toVertexId = toVertexId;
    }
    
    public int getFromVertexId() {
        return fromVertexId;
    }
    
    public int getToVertexId() {
        return toVertexId;
    }
        
}

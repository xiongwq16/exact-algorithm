package testdemo;

import csp.ColumnGeneration;

import ilog.concert.IloException;

import java.io.IOException;

/**
 * 调用模拟.
 * 
 * @author Xiong Wangqi
 * @version V1.0
 * @since JDK1.8
 */
public class CspDemo {
    public static void main(String[] args) throws IOException, IloException {
        String filename = "./instances/cutstock.txt";
        try {
            // 调用列生成求解算例并输出切割方案
            ColumnGeneration cg = new ColumnGeneration(filename);
            cg.solve();

        } catch (IOException e) {
            System.err.println("IOException '" + e + "' caught");
        } catch (IloException e) {
            System.err.println("Concert exception '" + e + "' caught");
        }

    }
    
}

package testdemo;

import java.io.IOException;

import vrptw.algorithm.solomoninsertion.SolomonInsertion;
import vrptw.problem.Vrptw;

/**
 *  Solomon Insertion Algorithm Test.
 * 
 * @author Xiong Wangqi
 * @version V1.0
 * @since JDK1.8
 */
public class SolomonInsertionDemo {
    public static void main(String[] args) {
        String filename = "./instances/solomon_100/c101.txt";
        
        try {
            Vrptw vrptwIns = new Vrptw(filename);
            SolomonInsertion i1 = new SolomonInsertion(vrptwIns, vrptwIns.getTimeMatrix());

            i1.constructRoutes();

            i1.output();
            
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
    }
}

package vrptw.problem;

/**
 * 车辆类.
 * 
 * @author Xiong Wangqi
 * @version V1.0
 * @since JDK1.8
 */
public class Vehicle {
    private double speed;
    private double capacity;
    
    public Vehicle(double speed, double capacity) {
        this.speed = speed;
        this.capacity = capacity;
    }
    
    public Vehicle(Vehicle veh) {
        this.speed = veh.speed;
        this.capacity = veh.capacity;
    }
    
    public double getSpeed() {
        return speed;
    }
    
    public double getCapacity() {
        return capacity;
    }
}

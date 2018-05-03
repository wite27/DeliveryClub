package models;

public class CalculateCostResult {
    public String point;
    public String previousPoint;
    public String nextPoint;

    public CalculateCostResult(double cost) {
        this.cost = cost;
    }

    public double cost;

    public CalculateCostResult(String point, double cost, String previousPoint, String nextPoint) {
        this.point = point;
        this.cost = cost;
        this.previousPoint = previousPoint;
        this.nextPoint = nextPoint;
    }
}

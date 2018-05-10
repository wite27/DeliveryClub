package models;

import java.util.ArrayList;

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

    public ArrayList<String> getNewRoute(ArrayList<String> currentRoute) {
        var newRoute = new ArrayList<>(currentRoute);
        var indexBetweenPoints = newRoute.indexOf(nextPoint);
        newRoute.add(indexBetweenPoints, point);

        return newRoute;
    }
}

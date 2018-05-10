package models;

import java.util.ArrayList;

public class DeliveryProposeStrategy {
    private DeliveryProposeStrategyType type;
    private double cost;
    private DeliveryContract proposedContract;
    private String newDeliveryPoint;
    private String oldDeliveryPoint;
    private ArrayList<String> newRoute;

    public DeliveryProposeStrategy(
            DeliveryProposeStrategyType type,
            double cost,
            DeliveryContract proposedContract,
            String newDeliveryPoint,
            String oldDeliveryPoint,
            ArrayList<String> newRoute) {
        this.type = type;
        this.cost = cost;
        this.proposedContract = proposedContract;
        this.newDeliveryPoint = newDeliveryPoint;
        this.oldDeliveryPoint = oldDeliveryPoint;
        this.newRoute = newRoute;
    }

    public DeliveryProposeStrategy(double cost, String point,
                                   DeliveryContract proposedContract) {
        this.type = DeliveryProposeStrategyType.Static;
        this.oldDeliveryPoint = point;
        this.newDeliveryPoint = point;
        this.cost = cost;
        this.proposedContract = proposedContract;
    }

    public DeliveryProposeStrategyType getType() {
        return type;
    }

    public double getCost() {
        return cost;
    }

    public String getNewDeliveryPoint() {
        return newDeliveryPoint;
    }

    public String getOldDeliveryPoint() {
        return oldDeliveryPoint;
    }

    public ArrayList<String> getNewRoute() {
        return newRoute;
    }

    public DeliveryContract getProposedContract() {
        return proposedContract;
    }
}

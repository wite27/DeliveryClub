package messages;

import models.DeliveryContract;

import java.util.ArrayList;
import java.util.HashSet;

public class DayResultMessageContent {
    private DeliveryContract receiveContract;
    private HashSet<DeliveryContract> produceContracts;
    private double routeDelta;
    private ArrayList<String> route;

    public DayResultMessageContent(
            DeliveryContract receiveContract,
            HashSet<DeliveryContract> produceContracts,
            double routeDelta,
            ArrayList<String> route) {
        this.receiveContract = receiveContract;
        this.produceContracts = produceContracts;
        this.routeDelta = routeDelta;
        this.route = route;
    }

    public HashSet<DeliveryContract> getProduceContracts() {
        return produceContracts;
    }

    public DeliveryContract getReceiveContract() {
        return receiveContract;
    }

    public double getRouteDelta() {
        return routeDelta;
    }

    public ArrayList<String> getRoute() {
        return route;
    }
}

package messages;

import models.DeliveryContract;

import java.util.HashSet;

public class DayResultMessageContent {
    private DeliveryContract receiveContract;
    private HashSet<DeliveryContract> produceContracts;
    private double routeDelta;

    public DayResultMessageContent(
            DeliveryContract receiveContract,
            HashSet<DeliveryContract> produceContracts,
            double routeDelta) {
        this.receiveContract = receiveContract;
        this.produceContracts = produceContracts;
        this.routeDelta = routeDelta;
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
}

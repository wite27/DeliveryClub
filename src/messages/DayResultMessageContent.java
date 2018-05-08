package messages;

import models.DeliveryContract;

import java.util.HashSet;

public class DayResultMessageContent {
    private DeliveryContract receiveContract;
    private HashSet<DeliveryContract> produceContracts;
    private boolean needNextDay;
    private double routeDelta;

    public DayResultMessageContent(
            DeliveryContract receiveContract,
            HashSet<DeliveryContract> produceContracts,
            boolean needNextDay,
            double routeDelta) {
        this.receiveContract = receiveContract;
        this.produceContracts = produceContracts;
        this.needNextDay = needNextDay;
        this.routeDelta = routeDelta;
    }

    public boolean isNeedNextDay() {
        return needNextDay;
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

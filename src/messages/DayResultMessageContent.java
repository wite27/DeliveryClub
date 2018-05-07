package messages;

import models.DeliveryContract;

import java.util.HashSet;

public class DayResultMessageContent {
    private DeliveryContract receiveContract;
    private HashSet<DeliveryContract> produceContracts;
    private boolean needNextDay;

    public DayResultMessageContent(
            DeliveryContract receiveContract,
            HashSet<DeliveryContract> produceContracts,
            boolean needNextDay) {
        this.receiveContract = receiveContract;
        this.produceContracts = produceContracts;
        this.needNextDay = needNextDay;
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
}

package messages;

import models.DeliveryContract;

public class MakeContractMessageContent {
    public DeliveryContract contract;

    private MakeContractMessageContent() {}

    public MakeContractMessageContent(DeliveryContract contract) {
        this.contract = contract;
    }
}

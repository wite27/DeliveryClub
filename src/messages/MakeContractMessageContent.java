package messages;

import models.DeliveryContract;

public class MakeContractMessageContent extends MessageContentBase {
    public DeliveryContract contract;

    private MakeContractMessageContent() {}

    public MakeContractMessageContent(DeliveryContract contract) {
        this.contract = contract;
    }
}

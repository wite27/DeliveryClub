package messages;

import models.DeliveryContract;

public class CancelContractMessageContent extends MessageContentBase {
    public DeliveryContract contract;

    private CancelContractMessageContent() {}

    public CancelContractMessageContent(DeliveryContract contract) {
        this.contract = contract;
    }
}

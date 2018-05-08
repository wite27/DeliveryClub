package messages;

import models.DeliveryContract;

public class CancelContractMessageContent {
    public DeliveryContract contract;

    private CancelContractMessageContent() {}

    public CancelContractMessageContent(DeliveryContract contract) {
        this.contract = contract;
    }
}

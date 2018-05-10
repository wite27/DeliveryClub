package messages;

import models.DeliveryContract;

public class AwaitingContractDecisionMessageContent {
    private String producerId;
    private DeliveryContract newContract;

    public AwaitingContractDecisionMessageContent(String producerId, DeliveryContract newContract) {
        this.producerId = producerId;
        this.newContract = newContract;
    }

    public String getProducerId() {
        return producerId;
    }

    public DeliveryContract getNewContract() {
        return newContract;
    }

    public boolean isSuccess() {
        return newContract != null;
    }

    public static AwaitingContractDecisionMessageContent failed(String producerId)
    {
        return new AwaitingContractDecisionMessageContent(producerId, null);
    }
}

package messages;

import models.DeliveryContract;

public class ContractUpdatedContentMessage {
    private String checkId;
    private DeliveryContract oldContract;
    private DeliveryContract newContract;

    public ContractUpdatedContentMessage(String checkId, DeliveryContract oldContract, DeliveryContract newContract) {
        this.checkId = checkId;
        this.oldContract = oldContract;
        this.newContract = newContract;
    }

    public String getCheckId() {
        return checkId;
    }

    public DeliveryContract getOldContract() {
        return oldContract;
    }

    public DeliveryContract getNewContract() {
        return newContract;
    }
}

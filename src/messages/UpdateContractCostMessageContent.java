package messages;

import models.DeliveryContract;

public class UpdateContractCostMessageContent {
    private DeliveryContract contract;
    private double newCost;


    public UpdateContractCostMessageContent(DeliveryContract contract, double newCost) {
        this.contract = contract;
        this.newCost = newCost;
    }

    public DeliveryContract getContract() {
        return contract;
    }

    public double getNewCost() {
        return newCost;
    }
}

package messages;

import models.interfaces.IShortContactInfo;

public class PotentialContractMessageContent implements IShortContactInfo {
    private String proposeId;
    private String producerId;
    private String consumerId;
    private String point;
    private double cost;

    private PotentialContractMessageContent() {}

    public PotentialContractMessageContent(String proposeId, String producerId, String consumerId,
                                           String point, double cost) {
        this.proposeId = proposeId;
        this.producerId = producerId;
        this.consumerId = consumerId;
        this.point = point;
        this.cost = cost;
    }

    public String getProposeId() {
        return proposeId;
    }

    public String getProducerId() {
        return producerId;
    }

    public String getConsumerId() {
        return consumerId;
    }

    public String getPoint() {
        return point;
    }

    public double getCost() {
        return cost;
    }
}

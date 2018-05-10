package messages;

import jade.core.AID;
import models.DeliveryContractHistoryItem;
import models.interfaces.IShortContactInfo;

import java.util.ArrayList;

public class PotentialContractMessageContent implements IShortContactInfo {
    private String proposeId;
    private String producerId;
    private String consumerId;
    private String point;
    private double cost;
    private ArrayList<DeliveryContractHistoryItem> previousContracts;

    private PotentialContractMessageContent() {}

    public PotentialContractMessageContent(String proposeId, String producerId, String consumerId,
                                           String point, double cost,
                                           ArrayList<DeliveryContractHistoryItem> previousContracts) {
        this.proposeId = proposeId;
        this.producerId = producerId;
        this.consumerId = consumerId;
        this.point = point;
        this.cost = cost;
        this.previousContracts = previousContracts;
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

    public ArrayList<DeliveryContractHistoryItem> getPreviousContracts() {
        return previousContracts;
    }

    // TODO potential contract and delivery contract merge
    public boolean isProducerInThisChain(AID agent) {
        return this.producerId.equals(agent.getName())
                || previousContracts.stream()
                .map(DeliveryContractHistoryItem::getProducer)
                .anyMatch(x -> x.getId().equals(agent.getName()));
    }
}

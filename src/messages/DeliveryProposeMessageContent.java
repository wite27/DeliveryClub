package messages;

import models.DeliveryContractHistoryItem;

import java.util.ArrayList;
import java.util.UUID;

public class DeliveryProposeMessageContent {
    public String proposeId;
    public ArrayList<String> points;
    public double cost;
    public ArrayList<DeliveryContractHistoryItem> previousContracts;

    private DeliveryProposeMessageContent() {}

    public DeliveryProposeMessageContent(ArrayList<String> points, double cost,
                                         ArrayList<DeliveryContractHistoryItem> previousContracts) {
        this.proposeId = UUID.randomUUID().toString();
        this.points = points;
        this.cost = cost;
        this.previousContracts = previousContracts;
    }
}

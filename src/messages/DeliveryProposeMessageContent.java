package messages;

import models.DeliveryContract;

import java.util.ArrayList;
import java.util.UUID;

public class DeliveryProposeMessageContent {
    private String proposeId;
    private ArrayList<DeliveryContract> contracts;

    private DeliveryProposeMessageContent() {}

    public DeliveryProposeMessageContent(ArrayList<DeliveryContract> contracts) {
        this.proposeId = UUID.randomUUID().toString();
        this.contracts = contracts;
    }

    public ArrayList<DeliveryContract> getContracts() {
        return contracts;
    }

    public String getProposeId() {
        return proposeId;
    }
}

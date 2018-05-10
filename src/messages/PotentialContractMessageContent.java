package messages;

import jade.core.AID;
import models.DeliveryContract;

public class PotentialContractMessageContent {
    private DeliveryContract contract;
    private String proposeId;

    private PotentialContractMessageContent() {}

    public PotentialContractMessageContent(DeliveryContract contract, String proposeId) {
        this.contract = contract;
        this.proposeId = proposeId;
    }

    // TODO potential contract and delivery contract merge
    public boolean isProducerInThisChain(AID agent) {
        return this.contract.isProducerInThisChain(agent);
    }

    public DeliveryContract getContract() {
        return contract;
    }

    public String getProposeId() {
        return proposeId;
    }
}

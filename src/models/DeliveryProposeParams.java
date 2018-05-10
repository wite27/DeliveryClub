package models;

import helpers.MessageHelper;
import jade.lang.acl.ACLMessage;
import messages.DeliveryProposeMessageContent;

public class DeliveryProposeParams {
    private DeliveryProposeStrategy strategy;
    private ACLMessage proposeMessage;
    private DeliveryProposeMessageContent proposeContent;

    public DeliveryProposeParams(DeliveryProposeStrategy strategy,
                                 ACLMessage proposeMessage) {
        this.strategy = strategy;
        this.proposeMessage = proposeMessage;
        this.proposeContent = MessageHelper.parse(proposeMessage, DeliveryProposeMessageContent.class);
    }

    public DeliveryProposeStrategy getStrategy() {
        return strategy;
    }

    public ACLMessage getProposeMessage() {
        return proposeMessage;
    }

    public DeliveryProposeMessageContent getProposeContent() {
        return proposeContent;
    }
}

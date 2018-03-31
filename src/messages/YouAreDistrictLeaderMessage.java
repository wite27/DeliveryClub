package messages;

import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.lang.acl.ACLMessage;

public class YouAreDistrictLeaderMessage extends ACLMessage {
    public YouAreDistrictLeaderMessage(DFAgentDescription leader) {
        this.setPerformative(ACLMessage.INFORM);
        this.addReceiver(leader.getName());
    }
}

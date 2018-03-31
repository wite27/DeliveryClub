package messages;

import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.lang.acl.ACLMessage;

public class YouAreDistrictLeaderMessage extends ACLMessage {
    public static final String Content = "YOU_ARE_LEADER";

    public YouAreDistrictLeaderMessage(DFAgentDescription leader) {
        super(ACLMessage.INFORM);

        this.setContent(Content);

        this.addReceiver(leader.getName());
    }
}

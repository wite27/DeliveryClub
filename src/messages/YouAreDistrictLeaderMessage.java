package messages;

import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class YouAreDistrictLeaderMessage extends ACLMessage {
    public static final String Content = "YOU_ARE_LEADER";
    public static final int Performative = ACLMessage.INFORM;

    public YouAreDistrictLeaderMessage(DFAgentDescription leader) {
        super(Performative);
        this.setContent(Content);

        this.addReceiver(leader.getName());
    }

    public static MessageTemplate Template(){
        return new MessageTemplate(msg ->
                msg.getPerformative() == Performative
                && msg.getContent().equals(YouAreDistrictLeaderMessage.Content));
    }
}

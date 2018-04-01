package agents;

import behaviours.DistrictLeaderBehaviour;
import behaviours.ReceiverWithHandlerBehaviour;
import jade.core.Agent;
import jade.core.behaviours.ReceiverBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.Property;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import messages.YouAreDistrictLeaderMessage;
import models.AgentType;
import models.Consts;

/**
 * Created by K750JB on 24.03.2018.
 */
public class AgentBase extends Agent {
    protected int District;

    protected void RegisterOnYellowPages(AgentType agentType, int district) {
        ServiceDescription sd  = new ServiceDescription();
        sd.setType(agentType.name());
        sd.setName(getLocalName());
        sd.addProperties(new Property(Consts.District, district));
        register(sd);
    }

    protected void StartListenYouAreLeaderMessage() {
        var template = YouAreDistrictLeaderMessage.Template();
        addBehaviour(new ReceiverWithHandlerBehaviour(this, Long.MAX_VALUE, template, aclMessage -> {
            System.out.println("Agent " + this.getName() + " got leader message from " + aclMessage.getSender().getName());
            addBehaviour(new DistrictLeaderBehaviour(this));
        }));
    }

    public int GetDistrict() {
        return District;
    }

    private void register( ServiceDescription sd)
    {
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());

        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
        }
        catch (FIPAException fe) { fe.printStackTrace(); }
    }
}

package agents;

import behaviours.CyclicReceiverWithHandlerBehaviour;
import behaviours.DistrictLeaderBehaviour;
import behaviours.ReceiverWithHandlerBehaviour;
import helpers.Log;
import jade.core.Agent;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.Property;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import messages.YouAreDistrictLeaderMessage;
import models.AgentType;
import models.Consts;

/**
 * Created by K750JB on 24.03.2018.
 */
public class AgentBase extends Agent {
    protected int district;

    protected void registerOnYellowPages(AgentType agentType, int district) {
        ServiceDescription sd  = new ServiceDescription();
        sd.setType(agentType.name());
        sd.setName(getLocalName());
        sd.addProperties(new Property(Consts.District, district));
        register(sd);
    }

    protected void startListenYouAreLeaderMessage() {
        var template = YouAreDistrictLeaderMessage.template();
        addBehaviour(new CyclicReceiverWithHandlerBehaviour(this, template, aclMessage -> {
            Log.fromAgent(this, " got leader message from " + aclMessage.getSender().getName());
            addBehaviour(new DistrictLeaderBehaviour(this));
        }));
    }

    public int getDistrict() {
        return district;
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

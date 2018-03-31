package agents;

import jade.core.Agent;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.Property;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import models.AgentType;
import models.Consts;

/**
 * Created by K750JB on 24.03.2018.
 */
public class AgentBase extends Agent {
    protected void RegisterOnYellowPages(AgentType agentType, int district) {
        ServiceDescription sd  = new ServiceDescription();
        sd.setType(agentType.name());
        sd.setName(getLocalName());
        sd.addProperties(new Property(Consts.District, district));
        register(sd);
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

    public DFAgentDescription[] FindAgents(AgentType type)
    {
        try {
            DFAgentDescription dfd = new DFAgentDescription();
            ServiceDescription sd = new ServiceDescription();
            sd.setType(type.name());
            dfd.addServices(sd);
            DFAgentDescription[] result = DFService.search(this, dfd);
            return result;
        }
        catch (FIPAException fe) {
            fe.printStackTrace();
            return new DFAgentDescription[0];
        }
    }

    public DFAgentDescription[] FindAgents(AgentType type, int district)
    {
        try {
            DFAgentDescription dfd = new DFAgentDescription();
            ServiceDescription sd = new ServiceDescription();
            sd.setType(type.name());
            sd.addProperties(new Property(Consts.District, district));
            dfd.addServices(sd);
            DFAgentDescription[] result = DFService.search(this, dfd);
            return result;
        }
        catch (FIPAException fe) {
            fe.printStackTrace();
            return new DFAgentDescription[0];
        }
    }

    public DFAgentDescription[] FindAgents(int district)
    {
        try {
            DFAgentDescription dfd = new DFAgentDescription();
            ServiceDescription sd = new ServiceDescription();
            sd.addProperties(new Property(Consts.District, district));
            dfd.addServices(sd);
            DFAgentDescription[] result = DFService.search(this, dfd);
            return result;
        }
        catch (FIPAException fe) {
            fe.printStackTrace();
            return new DFAgentDescription[0];
        }
    }

}

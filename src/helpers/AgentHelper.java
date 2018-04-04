package helpers;

import jade.core.Agent;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.Property;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import models.AgentType;
import models.Consts;

public class AgentHelper {

    public static DFAgentDescription[] findAgents(Agent self, AgentType type)
    {
        try {
            DFAgentDescription dfd = new DFAgentDescription();
            ServiceDescription sd = new ServiceDescription();
            sd.setType(type.name());
            dfd.addServices(sd);
            DFAgentDescription[] result = DFService.search(self, dfd);
            return result;
        }
        catch (FIPAException fe) {
            fe.printStackTrace();
            return new DFAgentDescription[0];
        }
    }

    public static DFAgentDescription[] findAgents(Agent self, AgentType type, int district)
    {
        try {
            DFAgentDescription dfd = new DFAgentDescription();
            ServiceDescription sd = new ServiceDescription();
            sd.setType(type.name());
            sd.addProperties(new Property(Consts.District, district));
            dfd.addServices(sd);
            DFAgentDescription[] result = DFService.search(self, dfd);
            return result;
        }
        catch (FIPAException fe) {
            fe.printStackTrace();
            return new DFAgentDescription[0];
        }
    }

    public static DFAgentDescription[] findAgents(Agent self, int district)
    {
        try {
            DFAgentDescription dfd = new DFAgentDescription();
            ServiceDescription sd = new ServiceDescription();
            sd.addProperties(new Property(Consts.District, district));
            dfd.addServices(sd);
            DFAgentDescription[] result = DFService.search(self, dfd);
            return result;
        }
        catch (FIPAException fe) {
            fe.printStackTrace();
            return new DFAgentDescription[0];
        }
    }
}

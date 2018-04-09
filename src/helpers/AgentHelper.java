package helpers;

import jade.core.Agent;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.Property;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import models.AgentType;
import models.Consts;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;

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


    public static ArrayList<DFAgentDescription> findAgents(Agent self, AgentType type, int district)
    {
        return findAgents(self, type, district, true);
    }

    public static ArrayList<DFAgentDescription> findAgents(Agent self, AgentType type, int district, boolean includeMyself)
    {
        try {
            DFAgentDescription dfd = new DFAgentDescription();
            ServiceDescription sd = new ServiceDescription();
            sd.setType(type.name());
            sd.addProperties(new Property(Consts.District, district));
            dfd.addServices(sd);
            var result = new ArrayList<>(Arrays.asList(DFService.search(self, dfd)));

            if (includeMyself)
                return result;

            return excludeAgent(result, self);
        }
        catch (FIPAException fe) {
            fe.printStackTrace();
            return new ArrayList<>();
        }
    }

    public static ArrayList<DFAgentDescription> findAgents(Agent self, int district)
    {
        return findAgents(self, district, true);
    }

    public static ArrayList<DFAgentDescription> findAgents(Agent self, int district, boolean includeMyself)
    {
        try {
            DFAgentDescription dfd = new DFAgentDescription();
            ServiceDescription sd = new ServiceDescription();
            sd.addProperties(new Property(Consts.District, district));
            dfd.addServices(sd);
            var result =  new ArrayList<>(Arrays.asList(DFService.search(self, dfd)));

            if (includeMyself)
                return result;

            return excludeAgent(result, self);
        }
        catch (FIPAException fe) {
            fe.printStackTrace();
            return new ArrayList<>();
        }
    }

    private static ArrayList<DFAgentDescription> excludeAgent(ArrayList<DFAgentDescription> agents, Agent agentToExclude)
    {
        return agents
                .stream()
                .filter(x -> !x.getName().equals(agentToExclude.getAID()))
                .collect(Collectors.toCollection(ArrayList::new));
    }
}

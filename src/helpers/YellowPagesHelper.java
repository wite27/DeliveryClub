package helpers;

import jade.core.Agent;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import models.Consts;


public class YellowPagesHelper {
    public static void register(Agent agent, ServiceDescription sd) {
        var dfd = new DFAgentDescription();
        dfd.setName(agent.getAID());

        dfd.addServices(sd);
        try {
            DFService.register(agent, dfd);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
    }

    public static DFAgentDescription findStatsman(Agent agent)
    {
        try {
            DFAgentDescription dfd = new DFAgentDescription();
            ServiceDescription sd = new ServiceDescription();
            sd.setType(Consts.StatsmanType);
            dfd.addServices(sd);
            return DFService.search(agent, dfd)[0];
        }
        catch (Exception fe) {
            fe.printStackTrace();
            return null;
        }
    }
}

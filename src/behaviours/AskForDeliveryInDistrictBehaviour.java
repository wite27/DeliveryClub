package behaviours;

import agents.AgentBase;
import helpers.AgentHelper;
import helpers.Log;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.SequentialBehaviour;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import models.AgentType;
import models.Consts;

import java.util.Comparator;

public class AskForDeliveryInDistrictBehaviour extends OneShotBehaviour {
    private AgentBase agent;
    private DFAgentDescription[] dynamicAgentsInThisDistrict;

    public AskForDeliveryInDistrictBehaviour(AgentBase agent) {
        super(agent);

        this.agent = agent;
        dynamicAgentsInThisDistrict = AgentHelper
                .findAgents(agent, AgentType.Dynamic, agent.getDistrict());
    }

    @Override
    public void action() {
        var msg = new ACLMessage(ACLMessage.REQUEST);
        msg.setContent(Consts.HowMuchCostDeliveryToDistrict);
        for (var potentialCourier: dynamicAgentsInThisDistrict) {
            msg.addReceiver(potentialCourier.getName());
        }

        agent.send(msg);
    }

    public int getReceiversCount() {
        return dynamicAgentsInThisDistrict.length;
    }
}
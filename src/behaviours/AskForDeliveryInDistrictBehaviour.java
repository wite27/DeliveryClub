package behaviours;

import agents.AgentBase;
import helpers.AgentHelper;
import jade.core.behaviours.OneShotBehaviour;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.lang.acl.ACLMessage;
import models.AgentType;
import models.Consts;

import java.util.ArrayList;

public class AskForDeliveryInDistrictBehaviour extends OneShotBehaviour {
    private AgentBase agent;
    private ArrayList<DFAgentDescription> dynamicAgentsInThisDistrict;

    public AskForDeliveryInDistrictBehaviour(AgentBase agent) {
        super(agent);

        this.agent = agent;
        dynamicAgentsInThisDistrict = AgentHelper
                .findAgents(
                        agent,
                        AgentType.Dynamic,
                        agent.getDistrict(),
                        false);
    }

    @Override
    public void action() {
        var msg = new ACLMessage(ACLMessage.CFP);
        msg.setContent(Consts.HowMuchCostDeliveryToDistrict);
        for (var potentialCourier: dynamicAgentsInThisDistrict) {
                msg.addReceiver(potentialCourier.getName());
        }
        agent.send(msg);
    }

    public int getReceiversCount() {
        return dynamicAgentsInThisDistrict.size();
    }
}
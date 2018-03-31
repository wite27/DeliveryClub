package behaviours;

import agents.AgentBase;
import helpers.AgentHelper;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.SequentialBehaviour;
import jade.core.behaviours.SimpleBehaviour;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.lang.acl.ACLMessage;
import models.AgentType;
import models.Consts;

import java.util.ArrayList;

public class DistrictLeaderBehaviour extends SequentialBehaviour {
    private AgentBase agent;
    private DFAgentDescription[] dynamicAgentsInThisDistrict;

    public DistrictLeaderBehaviour(AgentBase agent) {
        super(agent);

        this.agent = agent;
        dynamicAgentsInThisDistrict = AgentHelper
                .FindAgents(agent, AgentType.Dynamic, agent.GetDistrict());

        addSubBehaviour(new OneShotBehaviour() {
            @Override
            public void action() {
                for (var potentialCourier: dynamicAgentsInThisDistrict) {
                    var msg = new ACLMessage(ACLMessage.REQUEST);
                    msg.setContent(Consts.HowMuchCostDeliveryToDistrict);
                    msg.addReceiver(potentialCourier.getName());
                    agent.send(msg);
                }
            }
        });
        addSubBehaviour(new BatchReceiverWithHandlerBehaviour(agent, dynamicAgentsInThisDistrict.length, 10000,
                null, aclMessages -> {
        }));
    }
}
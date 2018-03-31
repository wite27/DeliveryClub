package behaviours;

import agents.AgentBase;
import helpers.AgentHelper;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.SequentialBehaviour;
import jade.core.behaviours.SimpleBehaviour;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import models.AgentType;

import java.util.ArrayList;

public class DistrictLeaderBehaviour extends SequentialBehaviour {
    private AgentBase agent;
    private DFAgentDescription[] dynamicAgentsInThisDistrict;

    public DistrictLeaderBehaviour(AgentBase agent) {
        super(agent);

        this.agent = agent;

        addSubBehaviour(new OneShotBehaviour() {
            @Override
            public void action() {
                 dynamicAgentsInThisDistrict = AgentHelper
                        .FindAgents(agent, AgentType.Dynamic, agent.GetDistrict());

                for (var potentialCourier: dynamicAgentsInThisDistrict) {
                }
            }
        });
        addSubBehaviour(new BatchReceiverWithHandlerBehaviour(agent, dynamicAgentsInThisDistrict.length, 10000,
                null, aclMessages -> {
            
        }));
    }
}
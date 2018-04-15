package behaviours;

import agents.AgentBase;
import helpers.AgentHelper;
import jade.core.behaviours.OneShotBehaviour;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import models.AgentType;
import models.Consts;

import java.util.ArrayList;

public class AskForDeliveryInDistrictBehaviour extends OneShotBehaviour {
    private AgentBase agent;
    private ArrayList<DFAgentDescription> dynamicAgentsInThisDistrict;
    private String conversationId;

    public AskForDeliveryInDistrictBehaviour(AgentBase agent, String conversationId) {
        super(agent);

        this.agent = agent;
        this.conversationId = conversationId;
    }

    @Override
    public void action() {
        dynamicAgentsInThisDistrict = AgentHelper
                .findAgents(
                        agent,
                        AgentType.Dynamic,
                        agent.getDistrict(),
                        false);

        var msg = new ACLMessage(ACLMessage.CFP);
        msg.setContent(Consts.HowMuchCostDeliveryToDistrict);
        msg.setConversationId(conversationId);
        for (var potentialCourier: dynamicAgentsInThisDistrict) {
                msg.addReceiver(potentialCourier.getName());
        }
        agent.send(msg);
    }

    public int getReceiversCount() {
        return dynamicAgentsInThisDistrict.size();
    }

    public String getConversationId() {
        return conversationId;
    }

    public MessageTemplate getAnswerMessageTemplate()
    {
        return new MessageTemplate(msg ->
                msg.getPerformative() == ACLMessage.PROPOSE
                        && msg.getContent().startsWith(Consts.IWillDeliverToDistrictPrefix)
                        && conversationId.equals(msg.getConversationId())
        );
    }
}
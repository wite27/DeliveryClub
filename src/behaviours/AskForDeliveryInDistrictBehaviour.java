package behaviours;

import agents.AgentBase;
import helpers.AgentHelper;
import helpers.MessageHelper;
import jade.core.behaviours.OneShotBehaviour;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import messages.CallForDeliveryProposeMessageContent;
import messages.DeliveryProposeMessageContent;
import models.AgentType;
import models.Consts;

import java.util.ArrayList;
import java.util.stream.Collectors;

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

        var msg = MessageHelper.buildMessage(
                ACLMessage.CFP,
                CallForDeliveryProposeMessageContent.class,
                null);

        msg.setConversationId(conversationId);
        MessageHelper.addReceivers(
                msg,
                dynamicAgentsInThisDistrict.stream()
                        .map(DFAgentDescription::getName)
                        .collect(Collectors.toList()));

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
                (msg.getPerformative() == ACLMessage.PROPOSE
                || msg.getPerformative() == ACLMessage.REFUSE)
                        && DeliveryProposeMessageContent.class.getName().equals(msg.getOntology())
                        && conversationId.equals(msg.getConversationId())
        );
    }
}
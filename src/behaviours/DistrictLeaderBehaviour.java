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

public class DistrictLeaderBehaviour extends SequentialBehaviour {
    private AgentBase agent;
    private DFAgentDescription[] dynamicAgentsInThisDistrict;

    public DistrictLeaderBehaviour(AgentBase agent) {
        super(agent);

        this.agent = agent;
        dynamicAgentsInThisDistrict = AgentHelper
                .findAgents(agent, AgentType.Dynamic, agent.getDistrict());

        addSubBehaviour(new OneShotBehaviour() {
            @Override
            public void action() {
                for (var potentialCourier: dynamicAgentsInThisDistrict) {
                    askHowMuchCostDeliveryToDistrict(potentialCourier);
                }
            }
        });

        var mt = new MessageTemplate(msg ->
            msg.getPerformative() == ACLMessage.AGREE
                    && msg.getContent().startsWith(Consts.IWillDeliverToDistrictPrefix)
        );
        addSubBehaviour(new BatchReceiverWithHandlerBehaviour(agent, dynamicAgentsInThisDistrict.length, 10000,
                mt, aclMessages -> {
            var bestDeal = aclMessages.stream()
                    .min(Comparator.comparingInt(o ->
                            Integer.parseInt(o.getContent().substring(Consts.IWillDeliverToDistrictPrefix.length()))))
                    .get(); // TODO isPresent() check

            Log.fromAgent(this.agent,"choosed best deal: " + bestDeal.getContent() +
                    " from " + bestDeal.getSender().getName());
        }));
    }

    private void askHowMuchCostDeliveryToDistrict(DFAgentDescription potentialCourier) {
        var msg = new ACLMessage(ACLMessage.REQUEST);
        msg.setContent(Consts.HowMuchCostDeliveryToDistrict);
        msg.addReceiver(potentialCourier.getName());
        agent.send(msg);
    }
}
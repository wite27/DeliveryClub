package behaviours;

import agents.AgentBase;
import helpers.AgentHelper;
import helpers.Log;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.SequentialBehaviour;
import jade.core.behaviours.SimpleBehaviour;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import models.AgentType;
import models.Consts;

import java.util.ArrayList;
import java.util.Comparator;

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
                    AskHowMuchCostDeliveryToDistrict(potentialCourier);
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

            Log.FromAgent(this.agent,"choosed best deal: " + bestDeal.getContent() +
                    " from " + bestDeal.getSender().getName());
        }));
    }

    private void AskHowMuchCostDeliveryToDistrict(DFAgentDescription potentialCourier) {
        var msg = new ACLMessage(ACLMessage.REQUEST);
        msg.setContent(Consts.HowMuchCostDeliveryToDistrict);
        msg.addReceiver(potentialCourier.getName());
        agent.send(msg);
    }
}
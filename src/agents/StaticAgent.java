package agents;

import behaviours.AskForDeliveryInDistrictBehaviour;
import behaviours.BatchReceiverWithHandlerBehaviour;
import behaviours.CyclicReceiverWithHandlerBehaviour;
import environment.CityMap;
import helpers.Log;
import helpers.MessageHelper;
import helpers.StringHelper;
import jade.core.AID;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.SequentialBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import messages.CancelContractMessageContent;
import messages.DeliveryProposeMessageContent;
import messages.MakeContractMessageContent;
import messages.PotentialContractMessageContent;
import models.AgentType;

import java.util.Comparator;

/**
 * Created by K750JB on 24.03.2018.
 */
public class StaticAgent extends AgentBase {
    public StaticAgent() {
        type = AgentType.Static;
    }

    @Override
    protected void setup() {
        super.setup();

        startAnswerOnMakeContract();
        startListenCancelledContracts();

        receiveContract = null;
    }

    @Override
    protected double getCurrentReceiveCost() {
        if (receiveContract == null){
            return Double.MAX_VALUE;
        }

        return receiveContract.getCost();
    }

    @Override
    protected double getRouteDelta() {
        if (receiveContract == null)
            return 0;

        // TODO Static agents has no route delta, it's dynamic's responsibility to deliver to static agent
        return CityMap.getInstance().getPathWeight(getHome(), receiveContract.getPoint());
    }

    private void startAnswerOnMakeContract(){
        var mt = new MessageTemplate(msg ->
                (msg.getPerformative() == ACLMessage.AGREE
                        || msg.getPerformative() == ACLMessage.CANCEL)
                        && StringHelper.safeEquals(msg.getOntology(), MakeContractMessageContent.class.getName()));

        addBehaviour(new CyclicReceiverWithHandlerBehaviour(this, mt, aclMessage -> {
            var content = MessageHelper.parse(aclMessage, MakeContractMessageContent.class);

            if (aclMessage.getPerformative() == ACLMessage.CANCEL)
                return;

            if (receiveContract != null)
                cancelCurrentReceiveContract();

            receiveContract = content.contract;
        }));
    }

    private void cancelCurrentReceiveContract() {
        if (receiveContract == null)
        {
            Log.warn("Agent " + this.getName() + " tried to cancel receive contract, but he had no one!");
            return;
        }

        var whoDeliversToMe = receiveContract.getProducer();

        if (whoDeliversToMe.isStore())
        {
            receiveContract = null;
            return;
        }

        var message = MessageHelper.buildMessage2(
                ACLMessage.REFUSE,
                CancelContractMessageContent.class,
                new CancelContractMessageContent(receiveContract));
        message.addReceiver(new AID(whoDeliversToMe.getId(), true));
    }

    private void startListenCancelledContracts() {
        var mt = new MessageTemplate(msg ->
                msg.getPerformative() == ACLMessage.REFUSE
                        && msg.getOntology().equals(CancelContractMessageContent.class.getName()));

        addBehaviour(new CyclicReceiverWithHandlerBehaviour(this, mt, aclMessage -> {
            var content = MessageHelper.parse(aclMessage, CancelContractMessageContent.class);

            if (produceContracts.remove(content.contract))
            {
                // TODO recalculate cost for others
            } else {
                Log.warn("Agent " + this.getName() + " got cancellation for contract he hadn't own!");
            }
        }));
    }

    @Override
    protected double getProposeDeliveryCost(ACLMessage message) {
        var propose = MessageHelper.parse(message, DeliveryProposeMessageContent.class);
        return propose.cost +
                propose.points.stream()
                        .map(this::calculateCostToPoint)
                        .min(Double::compareTo)
                        .get();
    }

    @Override
    protected void betterReceiveContractFound(ACLMessage message, DeliveryProposeMessageContent content) {
        var potentialContract = new PotentialContractMessageContent(
                content.proposeId, getHome(), content.cost);
        var answer = MessageHelper.buildMessage2(
                ACLMessage.ACCEPT_PROPOSAL,
                PotentialContractMessageContent.class,
                potentialContract);
        answer.setConversationId(content.proposeId);
        Log.fromAgent(this, "choosed best deal: " + message.getContent() +
                " from " + message.getSender().getName());
        answer.addReceiver(message.getSender());

        this.send(answer);
    }

    private double calculateCostToPoint(String point) {
        return CityMap.getInstance().getPathWeight(getHome(), point);
    }
}

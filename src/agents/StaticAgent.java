package agents;

import behaviours.CyclicReceiverWithHandlerBehaviour;
import environment.CityMap;
import factories.MessageTemplateFactory;
import helpers.Log;
import helpers.MessageHelper;
import helpers.StringHelper;
import jade.core.AID;
import jade.core.behaviours.Behaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import messages.CancelContractMessageContent;
import messages.DeliveryProposeMessageContent;
import messages.MakeContractMessageContent;
import messages.PotentialContractMessageContent;
import models.AgentType;
import models.DeliveryContract;
import models.DeliveryProposeParams;
import models.DeliveryProposeStrategy;

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

        receiveContract = null;
    }

    @Override
    protected double getCurrentReceiveCost() {
        if (receiveContract == null){
            return Double.MAX_VALUE;
        }

        return receiveContract.getCost()
                + CityMap.getInstance().getPathWeight(getHome(), receiveContract.getPoint());
    }

    @Override
    protected double getRouteDelta() {
        if (receiveContract == null)
            return 0.01337;

        return 0;
    }

    private void startAnswerOnMakeContract(){
        var mt = MessageTemplateFactory.create(
                ACLMessage.AGREE, ACLMessage.CANCEL,
                MakeContractMessageContent.class);

        addBehaviour(new CyclicReceiverWithHandlerBehaviour(this, mt, aclMessage -> {
            var content = MessageHelper.parse(aclMessage, MakeContractMessageContent.class);

            if (aclMessage.getPerformative() == ACLMessage.CANCEL)
                return;

            if (receiveContract != null)
                cancelCurrentReceiveContract();

            receiveContract = content.contract;
            Log.fromAgent(this, "got new receive contract: " + receiveContract.toShortString());
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
            Log.fromAgent(this, "canceled receive contract with store.");
            receiveContract = null;
            return;
        }

        Log.fromAgent(this, "canceled receive contract from " + whoDeliversToMe.getId());

        var message = MessageHelper.buildMessage(
                ACLMessage.REFUSE,
                CancelContractMessageContent.class,
                new CancelContractMessageContent(receiveContract));
        message.addReceiver(new AID(whoDeliversToMe.getId(), true));
        send(message);
    }

    @Override
    protected DeliveryProposeStrategy getDeliveryProposeStrategy(ACLMessage message) {
        var propose = MessageHelper.parse(message, DeliveryProposeMessageContent.class);
        return propose.getContracts().stream()
                .filter(x -> x.getPoint().equals(getHome()) && x.isProducerDelivery) // i'm static :(
                .min(Comparator.comparingDouble(DeliveryContract::getCost))
                .map(x -> new DeliveryProposeStrategy(
                        x.getCost(),
                        getHome(),
                        x))
                .orElse(null);
    }

    @Override
    protected Behaviour betterReceiveContractFound(DeliveryProposeParams params) {
        var proposeId = params.getProposeContent().getProposeId();
        var potentialContract = new PotentialContractMessageContent(
                params.getStrategy().getProposedContract(), proposeId);

        var answer = MessageHelper.buildMessage(
                ACLMessage.ACCEPT_PROPOSAL,
                PotentialContractMessageContent.class,
                potentialContract);
        answer.setConversationId(proposeId);
        answer.addReceiver(params.getProposeMessage().getSender());
        Log.fromAgent(this, " choosed best deal: " + potentialContract.getContract().toShortString());
        this.send(answer);

        return null;
    }

    private double calculateCostToPoint(String point) {
        return CityMap.getInstance().getPathWeight(getHome(), point);
    }
}

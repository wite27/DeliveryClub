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

    private double bestDeliveryToDistrictDeal = Double.MAX_VALUE;

    @Override
    protected void setup() {
        super.setup();

        startAnswerOnMakeContract();
        startListenCancelledContracts();

        receiveContract = null;
    }

    @Override
    protected double getRouteDelta() {
        if (receiveContract == null)
            return 0;

        // TODO Static agents has no route delta, it's dynamic's responsibility to deliver to static agent
        return CityMap.getInstance().getPathWeight(getHome(), receiveContract.getPoint());
    }

    @Override
    protected void onDayStart() {
        startAskingForDelivery();
    }

    @Override
    protected void onDayEnd() {

    }

    private void startAskingForDelivery() {
        var sequentialBehaviour = new SequentialBehaviour();

        var askForDeliveryInDistrictBehaviour = new AskForDeliveryInDistrictBehaviour(this,
                currentConversationId);
        sequentialBehaviour.addSubBehaviour(askForDeliveryInDistrictBehaviour);

        var mt = askForDeliveryInDistrictBehaviour.getAnswerMessageTemplate();
        var self = this;
        sequentialBehaviour.addSubBehaviour(new OneShotBehaviour() { // need to resolve receiversCount in lazy way
            @Override
            public void action() {
                sequentialBehaviour.addSubBehaviour(new BatchReceiverWithHandlerBehaviour(self,
                        askForDeliveryInDistrictBehaviour.getReceiversCount(),
                        1000,
                        mt,
                        aclMessages -> {
                            var currentCost = receiveContract == null
                                    ? Double.MAX_VALUE
                                    : receiveContract.getCost();

                            var bestDealOptional = aclMessages.stream()
                                    .filter(x -> x.getPerformative() != ACLMessage.REFUSE) // ignore refuses
                                    .filter(x -> getProposeDeliveryCost(x) < currentCost)
                                    .min(Comparator.comparingDouble(self::getProposeDeliveryCost));

                            if (!bestDealOptional.isPresent())
                            {
                                enoughForMeInThisDay(false);
                                return;
                            }

                            var bestDeal = bestDealOptional.get();
                            var content = MessageHelper.parse(bestDeal, DeliveryProposeMessageContent.class);

                            var potentialContract = new PotentialContractMessageContent(
                                    content.proposeId, getHome(), content.cost);
                            var message = MessageHelper.buildMessage2(
                                    ACLMessage.ACCEPT_PROPOSAL,
                                    PotentialContractMessageContent.class.getName(),
                                    potentialContract);
                            message.setConversationId(content.proposeId);
                            Log.fromAgent(self, "choosed best deal: " + bestDeal.getContent() +
                                    " from " + bestDeal.getSender().getName());
                            message.addReceiver(bestDeal.getSender());
                            self.send(message);

                            var dealCost = getProposeDeliveryCost(bestDeal);
                            var needNextDay = Math.abs(dealCost - bestDeliveryToDistrictDeal) > 0.001;
                            bestDeliveryToDistrictDeal = dealCost;
                            enoughForMeInThisDay(needNextDay);
                        }));
            }
        });

        addBehaviour(sequentialBehaviour);
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
                CancelContractMessageContent.class.getName(),
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

    private double getProposeDeliveryCost(ACLMessage message) {
        var propose = MessageHelper.parse(message, DeliveryProposeMessageContent.class);
        return propose.cost +
                propose.points.stream()
                        .map(this::calculateCostToPoint)
                        .min(Double::compareTo)
                        .get();
    }

    private double calculateCostToPoint(String point) {
        return CityMap.getInstance().getPathWeight(getHome(), point);
    }
}

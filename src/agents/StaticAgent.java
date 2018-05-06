package agents;

import behaviours.AskForDeliveryInDistrictBehaviour;
import behaviours.BatchReceiverWithHandlerBehaviour;
import environment.CityMap;
import helpers.Log;
import helpers.MessageHelper;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.SequentialBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import messages.PotentialContractMessageContent;
import models.AgentSettings;
import models.AgentType;
import models.Consts;

import java.util.Comparator;
import java.util.UUID;

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
                            var bestDeal = aclMessages.stream()
                                    .min(Comparator.comparingDouble(self::getProposeDeliveryCost))
                                    .get(); // TODO isPresent
                            var content = MessageHelper.getDeliveryProposeMessageContent(bestDeal.getContent());

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

    private double getProposeDeliveryCost(ACLMessage message) {
        var propose = MessageHelper.getDeliveryProposeMessageContent(message.getContent());
        return propose.cost +
                propose.points.stream()
                        .map(x -> calculateCostToPoint(x))
                        .min(Double::compareTo)
                        .get();
    }

    protected double calculateCostToPoint(String point) {
        return CityMap.getInstance().getPathWeight(getHome(), point);
    }
}

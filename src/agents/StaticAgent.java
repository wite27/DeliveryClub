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
                            var proposeId = MessageHelper.getParams(bestDeal)[4];

                            var message = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
                            message.setContent(Consts.IChooseYou);
                            message.setConversationId(proposeId);
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

    private double getProposeDeliveryCost(ACLMessage x) {
        var messageParams = MessageHelper.getParams(x);
        var cost = messageParams[1];
        var pointA = messageParams[2];
        var pointB = messageParams[3];
        return Double.parseDouble(cost) + calculateBestDeliveryPoint(pointA, pointB);
    }

    @Override
    protected double calculateCostToPoint(String point) {
        return CityMap.getInstance().getPathWeight(getHome(), point);
    }
}

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
    public StaticAgent(){
        type = AgentType.Static;
    }

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
                            var bestDeals = aclMessages.stream()
                                    .sorted(Comparator.comparingDouble(self::getProposeDeliveryCost))
                                    .limit((long) Math.ceil(aclMessages.size() * 0.1));
                            bestDeals.forEach(x ->
                            {
                                var message = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
                                message.setContent(Consts.IChooseYou);
                                message.setConversationId(x.getConversationId());
                                Log.fromAgent(self,"choosed best deal: " + x.getContent() +
                                        " from " + x.getSender().getName());
                                message.addReceiver(x.getSender());
                                self.send(message);
                            });

                            enoughForMeInThisDay();
                        }));
            }
        });

        addBehaviour(sequentialBehaviour);
    }

    private double getProposeDeliveryCost(ACLMessage x) {
        var messageParams = MessageHelper.getParams(x.getContent());
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

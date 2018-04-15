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
    private final AgentType type = AgentType.Static;

    private int neededProductsCount;
    private int currentMoney;
    private String currentConversationId;

    @Override
    protected void setup() {
        super.setup();

        init();
        registerOnYellowPages(type, district);

        startAskingForDelivery();
    }

    private void init() {
        Object[] args = getArguments();
        AgentSettings settings = (AgentSettings)args[0];
        neededProductsCount = settings.NeededProductsCount;
        route = settings.Route;
        currentMoney = settings.StartMoney;
        district = settings.District;
    }

    private void startAskingForDelivery() {
        var sequentialBehaviour = new SequentialBehaviour();

        currentConversationId = UUID.randomUUID().toString();
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
                        10000,
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

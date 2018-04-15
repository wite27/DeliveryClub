package agents;

import behaviours.AskForDeliveryInDistrictBehaviour;
import behaviours.BatchReceiverWithHandlerBehaviour;
import environment.CityMap;
import helpers.Log;
import helpers.MessageHelper;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import models.AgentSettings;
import models.AgentType;
import models.Consts;

import java.util.Comparator;

/**
 * Created by K750JB on 24.03.2018.
 */
public class StaticAgent extends AgentBase {
    private final AgentType type = AgentType.Static;

    private int neededProductsCount;
    private int currentMoney;

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
        var askForDeliveryInDistrictBehaviour = new AskForDeliveryInDistrictBehaviour(this);
        addBehaviour(askForDeliveryInDistrictBehaviour);

        var mt = new MessageTemplate(msg ->
                msg.getPerformative() == ACLMessage.PROPOSE
                        && msg.getContent().startsWith(Consts.IWillDeliverToDistrictPrefix)
        );
        addBehaviour(new BatchReceiverWithHandlerBehaviour(this,
                askForDeliveryInDistrictBehaviour.getReceiversCount(),
                10000,
                mt,
                aclMessages -> {
                    var bestDeal = aclMessages.stream()
                            .sorted(Comparator.comparingDouble(this::getProposeDeliveryCost))
                            .limit((long) Math.ceil(aclMessages.size() * 0.1));
                    var message = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
                    message.setContent("I_CHOOSE_YOU");
                    bestDeal.forEach(x ->
                    {
                        Log.fromAgent(this,"choosed best deal: " + x.getContent() +
                                " from " + x.getSender().getName());
                        message.addReceiver(x.getSender());
                    });
                    this.send(message);
                }));
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

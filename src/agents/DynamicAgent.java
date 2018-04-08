package agents;

import behaviours.AskForDeliveryInDistrictBehaviour;
import behaviours.BatchReceiverWithHandlerBehaviour;
import behaviours.CyclicReceiverWithHandlerBehaviour;
import environment.CityMap;
import environment.Store;
import helpers.AgentHelper;
import helpers.Log;
import helpers.MessageHelper;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import models.AgentSettings;
import models.AgentType;
import models.Consts;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.stream.Collectors;

/**
 * Created by K750JB on 24.03.2018.
 */
public class DynamicAgent extends AgentBase {
    private final AgentType type = AgentType.Dynamic;

    private int neededProductsCount;
    private int currentMoney;

    @Override
    protected void setup() {
        super.setup();

        init();
        registerOnYellowPages(type, district);
        startAskingForDelivery();
        startListenHowMuchCostDeliveryToRegion();
    }

    private void init() {
        var args = getArguments();
        var settings = (AgentSettings)args[0];
        neededProductsCount = settings.NeededProductsCount;
        route = settings.Route;
        currentMoney = settings.StartMoney;
        district = settings.District;
    }

    private void startListenHowMuchCostDeliveryToRegion() {
        var mt = new MessageTemplate(msg ->
            msg.getPerformative() == ACLMessage.CFP
            && msg.getContent().equals(Consts.HowMuchCostDeliveryToDistrict)
        );
        addBehaviour(new CyclicReceiverWithHandlerBehaviour(this, mt, aclMessage -> {
            var answerTo = aclMessage.getSender();
            var answer = MessageHelper.buildMessage(
                    ACLMessage.PROPOSE,
                    Consts.IWillDeliverToDistrictPrefix,
                    String.valueOf(calculateDeliveryCost()),
                    this.route.get(0),
                    this.route.get(1)
            );
            answer.addReceiver(answerTo);

            send(answer);
        }));
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
                    var myDeliveryCost = calculateDeliveryCost();
                    var bestDeals = aclMessages.stream()
                            .sorted(Comparator.comparingInt(this::getDeliveryCost))
                            .limit((long) Math.ceil(aclMessages.size()*0.1))
                            .filter(x -> getDeliveryCost(x) < myDeliveryCost).collect(Collectors.toList());
                    if (bestDeals.size() > 0)
                    {
                        var message = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
                        message.setContent(Consts.IChooseYou);
                        bestDeals.forEach(x ->
                        {
                            Log.fromAgent(this,"choosed best deal: " + x.getContent() +
                                " from " + x.getSender().getName());
                            message.addReceiver(x.getSender());
                        });
                        this.send(message);
                    }
                    else
                    {
                        var agentsInThisDistrict = AgentHelper
                                .findAgents(this, this.getDistrict());
                        var msg = new ACLMessage(ACLMessage.INFORM);
                        msg.setContent(Consts.IWillGoToStore);
                        for (var agent: agentsInThisDistrict) {
                            Log.fromAgent(this,"send " + msg.getContent() +
                                    " to " + agent.getName());
                            msg.addReceiver(agent.getName());
                        }
                        this.send(msg);
                    }
                }
        ));
    }

    private int getDeliveryCost(ACLMessage x) {
        var messageParams = MessageHelper.getParams(x.getContent());
        var cost = messageParams[1];
        var pointA = messageParams[2];
        var pointB = messageParams[3];
        return Integer.parseInt(cost) + calculateBestDeliveryPoint(pointA, pointB, getHome());
    }

    private int calculateDeliveryCost()
    {
        var home = getHome();
        var work = route.get(1);
        var store = Store.getInstance().getName();
        var map = CityMap.getInstance();

        var costWithoutStore = map.getPathWeight(home, work);
        Log.fromAgent(this, " without store = " + costWithoutStore);
        var costWithStore = map.getPathWeight(home, store) + map.getPathWeight(store, work);
        Log.fromAgent(this, " with store = " + costWithStore);

        var delta = (costWithStore - costWithoutStore);

        return delta > 0
                ? delta
                : 0;
    }
}

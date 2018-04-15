package agents;

import behaviours.AskForDeliveryInDistrictBehaviour;
import behaviours.BatchReceiverWithHandlerBehaviour;
import behaviours.CyclicReceiverWithHandlerBehaviour;
import environment.CityMap;
import environment.Store;
import helpers.AgentHelper;
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
import java.util.stream.Collectors;

/**
 * Created by K750JB on 24.03.2018.
 */
public class DynamicAgent extends AgentBase {
    private final AgentType type = AgentType.Dynamic;

    private int neededProductsCount;
    private int currentMoney;
    private boolean isGoingToStore = false;
    private int votesForMe = 0;

    @Override
    protected void setup() {
        super.setup();

        init();
        registerOnYellowPages(type, district);
        startAskingForDelivery();
        startListenHowMuchCostDeliveryToRegion();
        startCountVotes();
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
                    getHome(),
                    getWork()
            );
            answer.setConversationId(aclMessage.getConversationId());
            answer.addReceiver(answerTo);

            send(answer);
        }));
    }

    private void startAskingForDelivery() {
        var sequentialBehaviour = new SequentialBehaviour(this);

        var askForDeliveryInDistrictBehaviour = new AskForDeliveryInDistrictBehaviour(this);
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
                            var myDeliveryCost = calculateDeliveryCost();
                            var bestDeals = aclMessages.stream()
                                    .sorted(Comparator.comparingDouble(self::getProposeDeliveryCost))
                                    .limit((long) Math.ceil(aclMessages.size()*0.1))
                                    .filter(x -> getProposeDeliveryCost(x) < myDeliveryCost)
                                    .collect(Collectors.toList());
                            if (bestDeals.size() > 0)
                            {
                                var message = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
                                message.setContent(Consts.IChooseYou);
                                bestDeals.forEach(x ->
                                {
                                    Log.fromAgent(self,"choosed best deal: " + x.getContent() +
                                            " from " + x.getSender().getName());
                                    message.addReceiver(x.getSender());
                                });
                                self.send(message);
                            }
                            else
                            {
                                goToStoreAndNotify();
                            }
                        }
                ));
            }
        });


        addBehaviour(sequentialBehaviour);
    }

    private void goToStoreAndNotify() {
        if (this.isGoingToStore) // we are going to store already
            return;

        this.isGoingToStore = true;
        Log.fromAgent(this," will go to store");

        var agentsInThisDistrict = AgentHelper
                .findAgents(this, this.getDistrict(), false);
        var msg = new ACLMessage(ACLMessage.INFORM);
        msg.setContent(Consts.IWillGoToStore);
        for (var agent: agentsInThisDistrict) {
            msg.addReceiver(agent.getName());
        }
        this.send(msg);
    }

    private void startCountVotes(){
        var maxVotesCount = AgentHelper
                .findAgents(this, getDistrict(), false)
                .size();
        var neededVotesCount = (int) (maxVotesCount * Consts.VotesThreshold);

        var mt = new MessageTemplate(msg ->
                msg.getPerformative() == ACLMessage.ACCEPT_PROPOSAL
                && msg.getContent().equals(Consts.IChooseYou));

        addBehaviour(new CyclicReceiverWithHandlerBehaviour(this, mt, aclMessage -> {
            votesForMe++;

            if (votesForMe >= neededVotesCount)
            {
                goToStoreAndNotify();
            }
        }));
    }

    private double getProposeDeliveryCost(ACLMessage x) {
        var messageParams = MessageHelper.getParams(x.getContent());
        var cost = messageParams[1];
        var pointA = messageParams[2];
        var pointB = messageParams[3];
        return Double.parseDouble(cost) + calculateBestDeliveryPoint(pointA, pointB);
    }

    private double calculateDeliveryCost()
    {
        var store = Store.getInstance().getName();
        return calculateCostToPoint(store);
    }

    private String getWork() {
        return this.route.get(route.size() - 1);
    }

    @Override
    protected double calculateCostToPoint(String point) {
        var home = getHome();
        var work = getWork();
        var map = CityMap.getInstance();

        var costWithoutPoint = map.getPathWeight(home, work);
        var costWithPoint = map.getPathWeight(home, point) + map.getPathWeight(point, work);

        var delta = (costWithPoint - costWithoutPoint);

        return delta > 0
                ? delta
                : 0;
    }
}

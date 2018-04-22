package agents;

import behaviours.AskForDeliveryInDistrictBehaviour;
import behaviours.BatchReceiverWithHandlerBehaviour;
import behaviours.CyclicReceiverWithHandlerBehaviour;
import environment.CityMap;
import environment.GlobalParams;
import environment.Store;
import helpers.AgentHelper;
import helpers.Log;
import helpers.MessageHelper;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.SequentialBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import models.AgentSettings;
import models.AgentType;
import models.Consts;

import java.util.Comparator;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Created by K750JB on 24.03.2018.
 */
public class DynamicAgent extends AgentBase {
    private boolean isGoingToStore = false;
    private int votesForMe = 0;
    private int previousDayVotesForMe = 0;

    public DynamicAgent(){
        type = AgentType.Dynamic;
    }

    @Override
    protected void setup() {
        super.setup();
        startListenHowMuchCostDeliveryToDistrict();
        startCountVotes();
    }

    @Override
    protected void onDayStart() {
        previousDayVotesForMe = votesForMe;
        votesForMe = 0;
        startAskingForDelivery();
    }

    @Override
    protected void onDayEnd() {

    }

    private void startListenHowMuchCostDeliveryToDistrict() {
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
                    getWork(),
                    currentConversationId // to get votes for this propose
            );
            answer.setConversationId(aclMessage.getConversationId());
            answer.addReceiver(answerTo);

            send(answer);
        }));
    }

    private void startAskingForDelivery() {
        var sequentialBehaviour = new SequentialBehaviour(this);

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
                            var myDeliveryCost = calculateDeliveryCost();
                            var bestDeals = aclMessages.stream()
                                    .sorted(Comparator.comparingDouble(self::getProposeDeliveryCost))
                                    .filter(x -> getProposeDeliveryCost(x) < myDeliveryCost)
                                    .findFirst()
                                    .ifPresentOrElse(bestDeal -> {
                                        isGoingToStore = false;
                                        var proposeId = MessageHelper.getParams(bestDeal)[4];
                                        var message = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
                                        message.setContent(Consts.IChooseYou);
                                        message.setConversationId(proposeId);
                                        Log.fromAgent(self, "choosed best deal: " + bestDeal.getContent() +
                                                " from " + bestDeal.getSender().getName());
                                        message.addReceiver(bestDeal.getSender());
                                        self.send(message);
                                    }, () -> goToStoreAndNotify());
                        }
                ));
            }
        });
        sequentialBehaviour.addSubBehaviour(new TickerBehaviour(this, 1000) {
            @Override
            protected void onTick() {
                enoughForMeInThisDay(votesForMe != previousDayVotesForMe); // TODO wait for votes !!!
                stop();
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
        var neededVotesCount = (int) (maxVotesCount * GlobalParams.VotesThreshold);

        var mt = new MessageTemplate(msg ->
                msg.getPerformative() == ACLMessage.ACCEPT_PROPOSAL
                && msg.getContent().equals(Consts.IChooseYou)
                && currentConversationId.equals(msg.getConversationId()));

        addBehaviour(new CyclicReceiverWithHandlerBehaviour(this, mt, aclMessage -> {
            votesForMe++;

            if (votesForMe >= neededVotesCount)
            {
                goToStoreAndNotify();
            }
        }));
    }

    private double getProposeDeliveryCost(ACLMessage x) {
        var messageParams = MessageHelper.getParams(x);
        var cost = messageParams[1];
        var pointA = messageParams[2];
        var pointB = messageParams[3];
        return Double.parseDouble(cost) + calculateBestDeliveryPoint(pointA, pointB);
    }

    private double calculateDeliveryCost()
    {
        return calculateCostToStore() /
                (previousDayVotesForMe == 0
                ? 2 // you and me
                : previousDayVotesForMe + 1);
    }

    private double calculateCostToStore()
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

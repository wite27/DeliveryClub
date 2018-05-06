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
import jade.core.AID;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.SequentialBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import messages.DeliveryProposeMessageContent;
import messages.PotentialContractMessageContent;
import models.*;

import java.util.ArrayList;
import java.util.Comparator;

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
        // firstly, make contract with store
        var costToPoint = getCostToPoint(Store.getInstance().getName());
        receiveContract = new DeliveryContract(
                new StoreContractParty(),
                this.toContractParty(),
                costToPoint.cost,
                costToPoint.point,
                new ArrayList<>());
        startListenHowMuchCostDeliveryToDistrict();
        startAnswerOnPotentialContracts();
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
            var content = new DeliveryProposeMessageContent(route, calculateProposeDeliveryCost());

            var answer = MessageHelper.buildMessage2(
                    ACLMessage.PROPOSE,
                    DeliveryProposeMessageContent.class.getName(),
                    content
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
                            var currentCost = calculateCurrentDeliveryCost();
                            aclMessages.stream()
                                    .sorted(Comparator.comparingDouble(self::getProposeDeliveryCost))
                                    .filter(x -> getProposeDeliveryCost(x) < currentCost)
                                    .findFirst()
                                    .ifPresent(bestDeal -> {
                                        isGoingToStore = false;
                                        var content = MessageHelper.getDeliveryProposeMessageContent(bestDeal.getContent());
                                        var calc = getProposeDeliveryCalcResult(bestDeal); // get best point from propose
                                        var potentialContract = new PotentialContractMessageContent(
                                                content.proposeId, calc.point, content.cost);
                                        var message = MessageHelper.buildMessage2(
                                                ACLMessage.ACCEPT_PROPOSAL,
                                                PotentialContractMessageContent.class.getName(),
                                                potentialContract);
                                        message.setConversationId(content.proposeId);
                                        Log.fromAgent(self, "choosed best deal: " + bestDeal.getContent() +
                                                " from " + bestDeal.getSender().getName());
                                        message.addReceiver(bestDeal.getSender());
                                        self.send(message);
                                    });
                        }
                ));
            }
        });
        sequentialBehaviour.addSubBehaviour(new TickerBehaviour(this, 1000) {
            @Override
            protected void onTick() {
                enoughForMeInThisDay(true); // TODO wait for votes !!!
                stop();
            }
        });

        addBehaviour(sequentialBehaviour);
    }

    private void startAnswerOnPotentialContracts(){
        var mt = new MessageTemplate(msg ->
                msg.getPerformative() == ACLMessage.ACCEPT_PROPOSAL
                && msg.getOntology().equals(PotentialContractMessageContent.class.getName()));

        addBehaviour(new CyclicReceiverWithHandlerBehaviour(this, mt, aclMessage -> {
            var content = PotentialContractMessageContent.fromMessage(aclMessage);
            var isConditionsInForce = calculateProposeDeliveryCost() <= content.cost;
            if (!isConditionsInForce)
            {
                var answer = new ACLMessage(ACLMessage.CANCEL);
                answer.setConversationId(content.getProposeId());
                answer.addReceiver(aclMessage.getSender());
                send(answer);

                return;
            }

            var contract = new DeliveryContract(
                    this.toContractParty(), new AgentContractParty(aclMessage.getSender()),
                    content.cost, content.point, this.receiveContract.makeChain());
            var answer = MessageHelper.buildMessage2(
                    ACLMessage.AGREE,
                    DeliveryContract.class.getName(),
                    contract);
            answer.addReceiver(aclMessage.getSender());

            send(answer);
        }));
    }

    private double getProposeDeliveryCost(ACLMessage message) {
        var propose = MessageHelper.getDeliveryProposeMessageContent(message.getContent());
        return propose.cost +
               propose.points.stream()
                       .map(x -> getCostToPoint(x).cost)
                       .min(Double::compareTo)
                       .get();
    }

    private CalculateCostResult getProposeDeliveryCalcResult(ACLMessage message) {
        var propose = MessageHelper.getDeliveryProposeMessageContent(message.getContent());
        return propose.points.stream()
                        .map(this::getCostToPoint)
                        .min(Comparator.comparingDouble(x -> x.cost))
                        .get();
    }

    private double calculateProposeDeliveryCost()
    {
        return receiveContract.getCost() / (produceContracts.size() + 2); // my receivers, me and you
    }

    private double calculateCurrentDeliveryCost()
    {
        return receiveContract.getCost() / (produceContracts.size() + 1); // my receivers and me
    }

    private String getWork() {
        return this.route.get(route.size() - 1);
    }

    private CalculateCostResult getCostToPoint(String point) {
        var map = CityMap.getInstance();
        var best = new CalculateCostResult(Double.MAX_VALUE);
        best.point = point;

        for (int i = 0; i < route.size() - 1; i++)
        {
            var oldCost = map.getPathWeight(route.get(i), route.get(i+1));
            var cost = map.getPathWeight(route.get(i), point) + map.getPathWeight(point, route.get(i+1));
            var delta = cost - oldCost;
            if (delta < best.cost)
            {
                best.cost = delta;
                best.previousPoint = route.get(i);
                best.nextPoint = route.get(i+1);
            }
        }
        return best;
    }
}

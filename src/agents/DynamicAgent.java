package agents;

import behaviours.AskForDeliveryInDistrictBehaviour;
import behaviours.BatchReceiverWithHandlerBehaviour;
import behaviours.CyclicReceiverWithHandlerBehaviour;
import environment.CityMap;
import environment.Store;
import helpers.Log;
import helpers.MessageHelper;
import helpers.StringHelper;
import jade.core.AID;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.SequentialBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import messages.CancelContractMessageContent;
import messages.DeliveryProposeMessageContent;
import messages.MakeContractMessageContent;
import messages.PotentialContractMessageContent;
import models.*;

import java.util.ArrayList;
import java.util.Comparator;

/**
 * Created by K750JB on 24.03.2018.
 */
public class DynamicAgent extends AgentBase {
    public DynamicAgent(){
        type = AgentType.Dynamic;
    }

    @Override
    protected void setup() {
        super.setup();
        // firstly, make contract with store
        var costToStore = getCostToPoint(Store.getInstance().getName());
        addReceiveContract(new DeliveryContract(
                ContractParty.store(),
                this.toContractParty(),
                costToStore.cost,
                costToStore.point,
                new ArrayList<>()));

        startListenHowMuchCostDeliveryToDistrict();
        startAnswerOnPotentialContracts();
        startAnswerOnMakeContract();
        startListenCancelledContracts();
    }

    @Override
    protected double getCurrentReceiveCost() {
        return receiveContract.getCost() / (produceContracts.size() + 1); // my receivers and me;
    }

    @Override
    protected double getRouteDelta() {
        var map = CityMap.getInstance();
        var baseRouteCost = map.getPathWeight(getHome(), getWork());

        var currentRouteCost = 0.0;
        for (int i = 0; i < route.size() - 1; i++)
        {
            currentRouteCost += map.getPathWeight(route.get(i), route.get(i + 1));
        }

        return currentRouteCost - baseRouteCost;
    }

    private void startListenHowMuchCostDeliveryToDistrict() {
        var mt = new MessageTemplate(msg ->
            msg.getPerformative() == ACLMessage.CFP
            && StringHelper.safeEquals(msg.getContent(), Consts.HowMuchCostDeliveryToDistrict)
        );
        addBehaviour(new CyclicReceiverWithHandlerBehaviour(this, mt, aclMessage -> {
            var answerTo = aclMessage.getSender();

            ACLMessage answer;
            if (receiveContract.isProducerInThisChain(aclMessage.getSender()))
            {
                // found cycle, can't propose anything
                answer = MessageHelper.buildMessage2(
                        ACLMessage.REFUSE,
                        DeliveryProposeMessageContent.class,
                        null);
            } else {
                var content = new DeliveryProposeMessageContent(route, calculateProposeDeliveryCost());
                answer = MessageHelper.buildMessage2(
                        ACLMessage.PROPOSE,
                        DeliveryProposeMessageContent.class,
                        content
                );
            }

            answer.addReceiver(answerTo);
            answer.setConversationId(aclMessage.getConversationId());

            send(answer);
        }));
    }

    private void startAnswerOnPotentialContracts(){
        var mt = new MessageTemplate(msg ->
                msg.getPerformative() == ACLMessage.ACCEPT_PROPOSAL
                && StringHelper.safeEquals(msg.getOntology(), PotentialContractMessageContent.class.getName()));

        addBehaviour(new CyclicReceiverWithHandlerBehaviour(this, mt, aclMessage -> {
            var content = MessageHelper.parse(aclMessage, PotentialContractMessageContent.class);
            var isConditionsInForce = calculateProposeDeliveryCost() <= content.cost;
            if (!isConditionsInForce || receiveContract.isProducerInThisChain(aclMessage.getSender()))
            {
                var answer = MessageHelper.buildMessage2(
                        ACLMessage.CANCEL,
                        MakeContractMessageContent.class,
                        null
                );
                answer.addReceiver(aclMessage.getSender());
                send(answer);

                return;
            }

            var contract = new DeliveryContract(
                    this.toContractParty(),ContractParty.agent(aclMessage.getSender()),
                    content.cost, content.point, this.receiveContract.makeChain());
            var answer = MessageHelper.buildMessage2(
                    ACLMessage.AGREE,
                    MakeContractMessageContent.class,
                    new MakeContractMessageContent(contract));
            answer.addReceiver(aclMessage.getSender());
            send(answer);

            this.produceContracts.add(contract);
        }));
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

            addReceiveContract(content.contract);
        }));
    }

    private void addReceiveContract(DeliveryContract contract) {
        if (receiveContract != null)
        {
            Log.warn("Agent " + this.getName() + " tried to add receive contract, but he already has one!");
            return;
        }

        this.receiveContract = contract;

        var calc = getCostToPoint(contract.getPoint());

        if (!route.contains(calc.point))
        {
            // now we are going through new point
            this.route.add(this.route.indexOf(calc.nextPoint), calc.point);
        }
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
                CancelContractMessageContent.class,
                new CancelContractMessageContent(receiveContract));
        message.addReceiver(new AID(whoDeliversToMe.getId(), true));
        send(message);

        removeReceiveContract();
    }

    private void removeReceiveContract() {
        if (receiveContract == null)
        {
            Log.warn("Agent " + this.getName() + " tried to remove receive contract, but he had no one!");
            return;
        }

        var point = receiveContract.getPoint();
        if (point.equals(getHome()) || point.equals(getWork()))
            return;

        route.remove(point);

        receiveContract = null;
    }

    private void startListenCancelledContracts() {
        var mt = new MessageTemplate(msg ->
                msg.getPerformative() == ACLMessage.REFUSE
                && StringHelper.safeEquals(msg.getOntology(), CancelContractMessageContent.class.getName()));

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

    @Override
    protected double getProposeDeliveryCost(ACLMessage message) {
        var propose = MessageHelper.parse(message, DeliveryProposeMessageContent.class);
        return propose.cost +
               propose.points.stream()
                       .map(x -> getCostToPoint(x).cost)
                       .min(Double::compareTo)
                       .get();
    }

    @Override
    protected void betterReceiveContractFound(ACLMessage message, DeliveryProposeMessageContent content) {
        var calc = getProposeDeliveryCalcResult(message); // get best point from propose
        var potentialContract = new PotentialContractMessageContent(
                content.proposeId, calc.point, content.cost);
        var answer = MessageHelper.buildMessage2(
                ACLMessage.ACCEPT_PROPOSAL,
                PotentialContractMessageContent.class,
                potentialContract);
        answer.setConversationId(content.proposeId);
        Log.fromAgent(this, "choosed best deal: " + message.getContent() +
                " from " + message.getSender().getName());
        answer.addReceiver(message.getSender());

        this.send(answer);
    }

    private CalculateCostResult getProposeDeliveryCalcResult(ACLMessage message) {
        var propose = MessageHelper.parse(message, DeliveryProposeMessageContent.class);
        return propose.points.stream()
                        .map(this::getCostToPoint)
                        .min(Comparator.comparingDouble(x -> x.cost))
                        .get();
    }

    private double calculateProposeDeliveryCost()
    {
        return receiveContract.getCost() / (produceContracts.size() + 2); // my receivers, me and you
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

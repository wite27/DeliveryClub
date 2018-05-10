package agents;

import behaviours.BatchReceiverWithHandlerBehaviour;
import behaviours.CyclicReceiverWithHandlerBehaviour;
import behaviours.ReceiverWithHandlerBehaviour;
import environment.CityMap;
import environment.Store;
import factories.MessageTemplateFactory;
import helpers.Log;
import helpers.MessageHelper;
import helpers.StringHelper;
import jade.core.AID;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.SequentialBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import messages.*;
import models.*;
import models.interfaces.IShortContactInfo;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Created by K750JB on 24.03.2018.
 */
public class DynamicAgent extends AgentBase {
    public DynamicAgent(){
        type = AgentType.Dynamic;
    }

    private boolean isChangingReceiveContractInProcess = false;
    private PotentialContractMessageContent awaitingPotentialContract;

    @Override
    protected void setup() {
        super.setup();
        // firstly, make contract with store
        // TODO multipoint
        var costToStore = getCostToPointSimple(Store.getInstance().getName());
        addReceiveContract(new DeliveryContract(
                ContractParty.store(),
                this.toContractParty(),
                0,
                costToStore.point,
                new ArrayList<>()));

        startListenHowMuchCostDeliveryToDistrict();
        startAnswerOnPotentialContracts();
        //startAnswerOnMakeContract(); PART OF SEQUENCE
        startListenCancelledContracts();
    }

    @Override
    protected double getCurrentReceiveCost() {
        return getCostIfAlone() / (produceContracts.size() + 1); // my receivers and me;
    }

    private double getCostIfAlone() {
        // pay to deliveryman + road cost
        return receiveContract.getCost() +
                // TODO multipoint
                getCostToPointSimple(receiveContract.getPoint()).cost;
    }

    @Override
    protected double getRouteDelta() {
        var map = CityMap.getInstance();
        var baseRouteCost = map.getPathWeight(getHome(), getWork());

        return getCostToPointSimple(receiveContract.getPoint()).cost;
        // TODO multipoint
        /*
        var currentRouteCost = 0.0;
        for (int i = 0; i < route.size() - 1; i++)
        {
            currentRouteCost += map.getPathWeight(route.get(i), route.get(i + 1));
        }

        return currentRouteCost - baseRouteCost;*/
    }

    private void startListenHowMuchCostDeliveryToDistrict() {
        var mt = MessageTemplateFactory.create(
                ACLMessage.CFP,
                CallForDeliveryProposeMessageContent.class);
        addBehaviour(new CyclicReceiverWithHandlerBehaviour(this, mt, aclMessage -> {
            var answerTo = aclMessage.getSender();

            ACLMessage answer;

            if (!isAvailableToMakeContractWithAgent(answerTo))
            {
                // found cycle, can't propose anything
                // or we are blocked by parent check
                answer = MessageHelper.buildMessage(
                        ACLMessage.REFUSE,
                        DeliveryProposeMessageContent.class,
                        null);
            } else {
                var content = new DeliveryProposeMessageContent(
                        route, calculateCostWhichIPropose(), receiveContract.getPreviousContracts());
                answer = MessageHelper.buildMessage(
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
            var isConditionsInForce = calculateCostWhichIPropose() <= content.getCost();
            if (!isConditionsInForce
                || !isAvailableToMakeContractWithAgent(aclMessage.getSender()))
            {
                var answer = MessageHelper.buildMessage(
                        ACLMessage.CANCEL,
                        MakeContractMessageContent.class,
                        null
                );
                answer.addReceiver(aclMessage.getSender());
                send(answer);

                return;
            }

            var contract = new DeliveryContract(
                    this.toContractParty(),
                    ContractParty.agent(aclMessage.getSender()),
                    content.getCost(),
                    content.getPoint(),
                    this.receiveContract.makeChain());

            var answer = MessageHelper.buildMessage(
                    ACLMessage.AGREE,
                    MakeContractMessageContent.class,
                    new MakeContractMessageContent(contract));
            answer.addReceiver(aclMessage.getSender());
            send(answer);

            this.produceContracts.add(contract);
        }));
    }

    private void addReceiveContract(DeliveryContract contract) {
        if (receiveContract != null)
        {
            Log.warn("Agent " + this.getName() + " tried to add receive contract, but he already has one!");
            return;
        }

        receiveContract = contract;
        Log.fromAgent(this, "got new receive contract: " + IShortContactInfo.print(this.receiveContract));

        // TODO multipoint
        var calc = getCostToPointSimple(contract.getPoint());

        if (!route.contains(calc.point))
        {
            // now we are going through new point
            // TODO multipoint
            //this.route.add(this.route.indexOf(calc.nextPoint), calc.point);
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
            Log.fromAgent(this, "canceled receive contract from store");
            removeReceiveContract();
            return;
        }

        Log.fromAgent(this, "canceled receive contract from " + whoDeliversToMe.getId());

        var message = MessageHelper.buildMessage(
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

        // TODO multipoint
        //route.remove(point);

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
                updateProduceContractsCostIfNeed();
            } else {
                Log.warn("Agent " + this.getName() + " got cancellation for contract he hadn't own!");
            }
        }));
    }

    @Override
    protected void receiveContractCostUpdated(ACLMessage message, UpdateContractCostMessageContent content) {
        updateProduceContractsCostIfNeed();
    }

    private void updateProduceContractsCostIfNeed() {
        if (produceContracts.size() == 0)
            return;

        var newCost = getCurrentReceiveCost();
        produceContracts.forEach(x -> {
            var message = MessageHelper.buildMessage(
                    ACLMessage.INFORM,
                    UpdateContractCostMessageContent.class,
                    new UpdateContractCostMessageContent(x, newCost)
            );
            x.updateCost(newCost);
            message.addReceiver(x.getConsumer().getAID());
            send(message);
        });
    }

    @Override
    protected double getProposeDeliveryCost(ACLMessage message) {
        var propose = MessageHelper.parse(message, DeliveryProposeMessageContent.class);

        return propose.points.stream()
                .map(x -> getCostToPointSimple(x))
                .min(Comparator.comparingDouble(x -> x.cost)).get().cost;
        // TODO multipoint
        /*var currentCost = getCurrentReceiveCost();
        var currentDeliveryPoint = receiveContract != null ? receiveContract.getPoint() : null;

        var newCostIfChangePoint = propose.cost +
               propose.points.stream()
                       .map(x -> getCostToPointIfRouteWithoutPoint(x, currentDeliveryPoint).cost)
                       .min(Double::compareTo)
                       .get();
        var newCostIfGoToBothPoints = propose.cost +
                propose.points.stream()
                        .map(x -> getCostToPoint(x).cost)
                        .min(Double::compareTo)
                        .get();

        //TODO optimistic way, supposing all children will agree
        return newCostIfChangePoint / (produceContracts.size() + 1);*/
        /*
        var producingContractsWithOldPoint = produceContracts.stream()
                .filter(x -> x.getPoint().equals(currentDeliveryPoint))
                .count();

        if (producingContractsWithOldPoint == 0)
        {
            // TODO newCostIfChangePoint always >= newCostIfGoToBothPoints ??
            return Math.min(newCostIfChangePoint, newCostIfGoToBothPoints);
        }

        if (newCostIfGoToBothPoints <= newCostIfChangePoint)
        {
            // easy, need not to change our producing contracts
            return newCostIfGoToBothPoints;
        }

        var minIfChange = newCostIfChangePoint / produceContracts.size();

        var maxIfChange = newCostIfChangePoint / producingContractsWithOldPoint;

        var costIfBoth = newCostIfGoToBothPoints / produceContracts.size();*/
    }

    @Override
    protected Behaviour betterReceiveContractFound(ACLMessage message, DeliveryProposeMessageContent content) {
        if (awaitingPotentialContract != null) {
            // we already waiting for confirmation on awaiting contract
            return null;
        }
        var proposerAid = message.getSender();

        var calc = getProposeDeliveryCalcResult(message); // get best point from propose
        var potentialContract = new PotentialContractMessageContent(
                content.proposeId, proposerAid.getName(), this.getName(), calc.point, content.cost,
                content.previousContracts);

        awaitingPotentialContract = potentialContract;
        Log.fromAgent(this, " got awaiting contract: " + IShortContactInfo.print(potentialContract));

        if (produceContracts.size() == 0) {
            Log.fromAgent(this, " has no produceContract and accepted awaiting contract: "
                    + IShortContactInfo.print(potentialContract));
            // no need to wait commit from children
            acceptContractImmediately(proposerAid, potentialContract);
            return waitForContractConfirmationWithoutCheck();
        }

        var myReceivers = getMyReceivers();
        var check = new CheckChainRequestMessageContent(proposerAid.getName());

        var self = this;
        var sequence = new SequentialBehaviour(this);
        sequence.addSubBehaviour(new OneShotBehaviour() {
            @Override
            public void action() {
                var message = MessageHelper.buildMessage(
                        ACLMessage.INFORM_IF,
                        CheckChainRequestMessageContent.class,
                        check
                );
                message.setConversationId(check.getCheckId());
                MessageHelper.addReceivers(message, myReceivers);
                send(message);
                Log.fromAgent(self, "initiated check on " +
                        check.getProducerId() + ", id: " + check.getCheckId());
            }
        });
        sequence.addSubBehaviour(new BatchReceiverWithHandlerBehaviour(this,
                myReceivers.size(),
                1000,
                MessageTemplateFactory.create(
                        ACLMessage.INFORM,
                        CheckChainResponseMessageContent.class,
                        check.getCheckId()), results -> {
            var isCheckFailed =
                    results.size() != myReceivers.size()
                    || results.stream()
                        .anyMatch(x -> MessageHelper.parse(x, CheckChainResponseMessageContent.class)
                        .isPresent());
            if (isCheckFailed)
            {
                Log.fromAgent(self, "check on " + check.getProducerId() + " with id: " + check.getCheckId() +
                    " failed");
                cancelAwaitingContract(check.getCheckId(), myReceivers);
            } else {
                Log.fromAgent(self, "check on " + check.getProducerId() + " with id: " + check.getCheckId() +
                        " succeced");
                acceptContractImmediately(proposerAid, awaitingPotentialContract);

                var mt = new MessageTemplate(msg ->
                        (msg.getPerformative() == ACLMessage.AGREE
                                || msg.getPerformative() == ACLMessage.CANCEL)
                                && StringHelper.safeEquals(msg.getOntology(), MakeContractMessageContent.class.getName()));

                var receiveContractBehaviour = new ReceiverWithHandlerBehaviour(
                        this,1000, mt, aclMessage -> {
                    var contractContent = MessageHelper.parse(aclMessage, MakeContractMessageContent.class);

                    if (aclMessage.getPerformative() == ACLMessage.CANCEL)
                    {
                        cancelAwaitingContract(check.getCheckId(), myReceivers);
                        return;
                    }

                    if (receiveContract != null)
                        cancelCurrentReceiveContract();

                    addReceiveContract(contractContent.contract);

                    notifyContractUpdated(check.getCheckId(), myReceivers);
                });

                sequence.addSubBehaviour(receiveContractBehaviour);
            }
        }));

        return sequence;
    }

    private void acceptContractImmediately(AID proposerAid, PotentialContractMessageContent potentialContract) {
        var answer = MessageHelper.buildMessage(
                ACLMessage.ACCEPT_PROPOSAL,
                PotentialContractMessageContent.class,
                potentialContract);
        answer.setConversationId(potentialContract.getProposeId());
        answer.addReceiver(proposerAid);

        this.send(answer);
    }

    private Behaviour waitForContractConfirmationWithoutCheck() {
        var mt = new MessageTemplate(msg ->
                (msg.getPerformative() == ACLMessage.AGREE
                        || msg.getPerformative() == ACLMessage.CANCEL)
                        && StringHelper.safeEquals(msg.getOntology(), MakeContractMessageContent.class.getName()));

        return new ReceiverWithHandlerBehaviour(
                this,1000, mt, aclMessage -> {
            var contractContent = MessageHelper.parse(aclMessage, MakeContractMessageContent.class);

            awaitingPotentialContract = null;

            if (aclMessage.getPerformative() == ACLMessage.CANCEL)
            {
                return;
            }

            if (receiveContract != null)
                cancelCurrentReceiveContract();

            addReceiveContract(contractContent.contract);
        });
    }

    private void cancelAwaitingContract(String checkId, List<AID> receiversToNotifyUnblock) {
        if (awaitingPotentialContract == null) {
            Log.warn("Agent " + this.getName() + " tried to cancel awaiting contract, but he had not one!");
            return;
        }
        var content = AwaitingContractDecisionMessageContent.failed(awaitingPotentialContract.getProducerId());
        var message = MessageHelper.buildMessage(
                ACLMessage.INFORM,
                AwaitingContractDecisionMessageContent.class,
                content);
        message.setConversationId(checkId);
        MessageHelper.addReceivers(message, receiversToNotifyUnblock);

        this.send(message);

        awaitingPotentialContract = null;
    }

    private boolean isAvailableToMakeContractWithAgent(AID agent) {
        var isCycle = receiveContract.isProducerInThisChain(agent);
        var isBlockedByParentsCheck = currentChecks.stream()
                .anyMatch(x -> x.getProducerId().equals(agent.getName()));
        var isAwaitingHimAsDeliveryman = awaitingPotentialContract != null
                && awaitingPotentialContract.getProducerId().equals(agent.getName());
        var isPotentialCycle = awaitingPotentialContract != null
            && awaitingPotentialContract.isProducerInThisChain(agent);

        if (isCycle) Log.fromAgent(this, "found cycle with " + agent.getName());
        if (isBlockedByParentsCheck) Log.fromAgent(this, "has block on " + agent.getName());
        if (isAwaitingHimAsDeliveryman) Log.fromAgent(this, "already awaiting " + agent.getName() +
                " as deliveryman");
        if (isPotentialCycle) Log.fromAgent(this, "potential cycle with " + agent.getName() +
                " because awaiting " + awaitingPotentialContract.getProducerId());

        return (!isCycle
                && !isBlockedByParentsCheck
                && !isAwaitingHimAsDeliveryman
                && !isPotentialCycle);
    }

    private CalculateCostResult getProposeDeliveryCalcResult(ACLMessage message) {
        var propose = MessageHelper.parse(message, DeliveryProposeMessageContent.class);
        return propose.points.stream()
                // TODO multipoint
                .map(x -> getCostToPointSimple(x))
                .min(Comparator.comparingDouble(x -> x.cost))
                .get();
    }

    private double calculateCostWhichIPropose()
    {
        return getCostIfAlone() / (produceContracts.size() + 2); // my receivers, me and you
    }

    private String getWork() {
        return this.route.get(route.size() - 1);
    }

    private CalculateCostResult getCostToPoint(String point) {
        return getCostToPointIfRouteWithoutPoint(point, null);
    }

    private CalculateCostResult getCostToPointIfRouteWithoutPoint(String point, String pointToRemove) {
        var map = CityMap.getInstance();
        var best = new CalculateCostResult(Double.MAX_VALUE);
        best.point = point;

        var routeToCheck = new ArrayList<>(route);
        if (pointToRemove != null) {
            routeToCheck.remove(pointToRemove);
        }

        for (int i = 0; i < routeToCheck.size() - 1; i++)
        {
            var oldCost = map.getPathWeight(routeToCheck.get(i), routeToCheck.get(i+1));
            var cost = map.getPathWeight(routeToCheck.get(i), point) + map.getPathWeight(point, routeToCheck.get(i+1));
            var delta = cost - oldCost;
            if (delta < best.cost)
            {
                best.cost = delta;
                best.previousPoint = routeToCheck.get(i);
                best.nextPoint = routeToCheck.get(i+1);
            }
        }
        return best;
    }

    private CalculateCostResult getCostToPointSimple(String point)
    {
        var map = CityMap.getInstance();
        var oldCost = map.getPathWeight(getHome(), getWork());
        var newCost = map.getPathWeight(getHome(), point) + map.getPathWeight(point, getWork());
        return new CalculateCostResult(point, newCost - oldCost, getHome(), getWork());
    }
}

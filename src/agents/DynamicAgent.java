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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by K750JB on 24.03.2018.
 */
public class DynamicAgent extends AgentBase {
    public DynamicAgent(){
        type = AgentType.Dynamic;
    }

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
        return getReceiveCostIfAlone() - produceContracts.stream()
                .map(DeliveryContract::getCost)
                .reduce((x, y) -> x + y).orElse(0.0);
    }

    private double getReceiveCostIfAlone() {
        // pay to deliveryman + road cost
        return receiveContract.getCost() +
                getCostToPointIfRouteWithoutPoint(receiveContract.getPoint(), receiveContract.getPoint()).cost;
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
        var mt = MessageTemplateFactory.create(
                ACLMessage.CFP,
                CallForDeliveryProposeMessageContent.class);
        addBehaviour(new CyclicReceiverWithHandlerBehaviour(this, mt, aclMessage -> {
            var answerTo = aclMessage.getSender();
            var content = aclMessage.getContent() != null
                    ? MessageHelper.parse(aclMessage, CallForDeliveryProposeMessageContent.class)
                    : null;

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
                var propose = content != null && content.isNeedDeliveryToPoint()
                    ? createProposeTo(answerTo, content.getNeededPoint())
                    : createProposeTo(answerTo);
                answer = MessageHelper.buildMessage(
                        ACLMessage.PROPOSE,
                        DeliveryProposeMessageContent.class,
                        propose
                );
            }

            answer.addReceiver(answerTo);
            answer.setConversationId(aclMessage.getConversationId());

            send(answer);
        }));
    }

    private DeliveryProposeMessageContent createProposeTo(AID agent) {
        return new DeliveryProposeMessageContent(
                route.stream()
                .map(x -> new DeliveryContract(
                        this.toContractParty(),
                        ContractParty.agent(agent),
                        getCostToPoint(x).cost,
                        x,
                        receiveContract.makeChain()
                )).collect(Collectors.toCollection(ArrayList::new)));
    }

    private DeliveryProposeMessageContent createProposeTo(AID agent, String specifiedPoint) {
        var propose = new DeliveryContract(
                this.toContractParty(),
                ContractParty.agent(agent),
                getCostToPoint(specifiedPoint).cost,
                specifiedPoint,
                receiveContract.makeChain()
        );
        propose.isProducerDelivery = true;

        return new DeliveryProposeMessageContent(new ArrayList<>(){{
            add(propose);
        }});
    }

    private void startAnswerOnPotentialContracts(){
        var mt = new MessageTemplate(msg ->
                msg.getPerformative() == ACLMessage.ACCEPT_PROPOSAL
                && StringHelper.safeEquals(msg.getOntology(), PotentialContractMessageContent.class.getName()));

        addBehaviour(new CyclicReceiverWithHandlerBehaviour(this, mt, aclMessage -> {
            var content = MessageHelper.parse(aclMessage, PotentialContractMessageContent.class);

            var costIfMakeContractNow = createProposeTo(
                    aclMessage.getSender(), content.getContract().getPoint())
                    .getContracts().get(0).getCost();
            var isConditionsInForce = costIfMakeContractNow <= content.getContract().getCost();

            if (awaitingPotentialContract != null
                || !isConditionsInForce
                || !content.getContract().hasEqualProducersChain(receiveContract.makeChain())
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
                    content.getContract().getCost(),
                    content.getContract().getPoint(),
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
        Log.fromAgent(this, "got new receive contract: " + this.receiveContract.toShortString());

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
        if (point.equals(getHome()) || point.equals(getWork())) {
            receiveContract = null;
            return;
        }

        var hasDependencyOnThisPoint = produceContracts.stream()
                .anyMatch(x -> x.getPoint().equals(point));

        if (!hasDependencyOnThisPoint) {
            route.remove(point);
        }

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
    protected DeliveryProposeStrategy getDeliveryProposeStrategy(ACLMessage message) {
        var proposeContracts = MessageHelper.parse(message, DeliveryProposeMessageContent.class)
                .getContracts();
        var currentDeliveryPoint = receiveContract != null ? receiveContract.getPoint() : null;

        var bestStrategy = proposeContracts.stream()
                .flatMap(x -> List.of(getChangePointStrategy(x, currentDeliveryPoint),
                    getBothPointsStrategy(x, currentDeliveryPoint)).stream())
                .min(Comparator.comparingDouble(DeliveryProposeStrategy::getCost))
                .get();

        return bestStrategy;
    }

    private DeliveryProposeStrategy getChangePointStrategy(DeliveryContract proposeContract, String currentDeliveryPoint) {
        var newPoint = proposeContract.getPoint();
        var contractsNotDependentOnCurrentPoint = produceContracts.stream()
                .filter(x -> !x.getPoint().equals(currentDeliveryPoint))
                .collect(Collectors.toList());

        var routeCopy = removeFromRouteSafe(route, currentDeliveryPoint);
        var changePointCalc = getCostToPointForRoute(routeCopy, newPoint, null);
        var routeIfChangePoint = changePointCalc.getNewRoute(routeCopy);

        var totalCost = (proposeContract.getCost() + changePointCalc.cost)
                - (contractsNotDependentOnCurrentPoint.stream()
                .map(x -> Math.min(x.getCost(),
                        getCostToPointForRoute(routeIfChangePoint, x.getPoint(), null).cost))
                .reduce((x, y) -> (x + y)).orElse(0.0));

        return new DeliveryProposeStrategy(
                DeliveryProposeStrategyType.ChangePoint,
                totalCost,
                proposeContract,
                newPoint,
                currentDeliveryPoint,
                routeIfChangePoint);
    }

    private DeliveryProposeStrategy getBothPointsStrategy(DeliveryContract proposeContract, String currentDeliveryPoint) {
        var newPoint = proposeContract.getPoint();

        var routeCopy = removeFromRouteSafe(route, currentDeliveryPoint);
        var changePointCalc = getCostToPointForRoute(routeCopy, newPoint, null);
        var routeIfChangePoint = changePointCalc.getNewRoute(routeCopy);

        var calcIfBothPoints = getCostToPointForRoute(routeIfChangePoint, currentDeliveryPoint, null);
        var roadCostIfBothPoints = changePointCalc.cost + calcIfBothPoints.cost;
        var routeIfBothPoints = calcIfBothPoints.getNewRoute(routeIfChangePoint);

        var totalCost = (proposeContract.getCost() + roadCostIfBothPoints)
                - (produceContracts.stream()
                .map(x -> Math.min(x.getCost(),
                        getCostToPointForRoute(routeIfBothPoints, x.getPoint(), null).cost))
                .reduce((x, y) -> x + y).orElse(0.0));

        return new DeliveryProposeStrategy(
                DeliveryProposeStrategyType.BothPoints,
                totalCost,
                proposeContract,
                newPoint,
                currentDeliveryPoint,
                routeIfBothPoints);
    }

    @Override
    protected Behaviour betterReceiveContractFound(DeliveryProposeParams params) {
        if (awaitingPotentialContract != null) {
            // we already waiting for confirmation on awaiting contract
            return null;
        }
        var proposerAid = params.getProposeMessage().getSender();

        var bestStrategy = params.getStrategy();
        var potentialContract = new PotentialContractMessageContent(
                bestStrategy.getProposedContract(), params.getProposeContent().getProposeId());

        awaitingPotentialContract = potentialContract;
        Log.fromAgent(this, " got awaiting contract: " + potentialContract.getContract().toShortString());

        if (produceContracts.size() == 0) {
            Log.fromAgent(this, " has no produceContracts and accepted awaiting contract: "
                    + potentialContract.getContract().toShortString());
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
                    if (aclMessage == null // something went wrong
                        || aclMessage.getPerformative() == ACLMessage.CANCEL)
                    {
                        cancelAwaitingContract(check.getCheckId(), myReceivers);
                        return;
                    }

                    var contractContent = MessageHelper.parse(aclMessage, MakeContractMessageContent.class);

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
        var content = AwaitingContractDecisionMessageContent.failed(
                awaitingPotentialContract.getContract().getProducer().getId());
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
                && awaitingPotentialContract.getContract().getProducer().getId().equals(agent.getName());
        var isPotentialCycle = awaitingPotentialContract != null
            && awaitingPotentialContract.isProducerInThisChain(agent);

        if (isCycle) Log.fromAgent(this, "found cycle with " + agent.getName());
        if (isBlockedByParentsCheck) Log.fromAgent(this, "has block on " + agent.getName());
        if (isAwaitingHimAsDeliveryman) Log.fromAgent(this, "already awaiting " + agent.getName() +
                " as deliveryman");
        if (isPotentialCycle) Log.fromAgent(this, "potential cycle with " + agent.getName() +
                " because awaiting " + awaitingPotentialContract.getContract().getProducer().getId());

        return (!isCycle
                && !isBlockedByParentsCheck
                && !isAwaitingHimAsDeliveryman
                && !isPotentialCycle);
    }

    private DeliveryContract getProposeDeliveryCalcResult(ACLMessage message) {
        var propose = MessageHelper.parse(message, DeliveryProposeMessageContent.class);
        return propose.getContracts().stream()
                // TODO multipoint
                .min(Comparator.comparingDouble(x -> x.getCost() + getCostToPointSimple(x.getPoint()).cost))
                .get();
    }

    private double calculateCostWhichIPropose()
    {
        return getReceiveCostIfAlone() / (produceContracts.size() + 2); // my receivers, me and you
    }

    private String getWork() {
        return this.route.get(route.size() - 1);
    }

    private CalculateCostResult getCostToPoint(String point) {
        return getCostToPointForRoute(route, point, null);
    }

    private CalculateCostResult getCostToPointIfRouteWithoutPoint(String point, String pointToRemove) {
        return getCostToPointForRoute(route, point, pointToRemove);
    }

    private CalculateCostResult getCostToPointForRoute(
            ArrayList<String> routeToCheck, String point, String pointToRemove) {
        var map = CityMap.getInstance();
        var best = new CalculateCostResult(Double.MAX_VALUE);
        best.point = point;

        if (pointToRemove != null) {
            routeToCheck = removeFromRouteSafe(routeToCheck, pointToRemove);
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

    private ArrayList<String> removeFromRouteSafe(ArrayList<String> route, String point) {
        var result = new ArrayList<>(route);

        if (route.size() <= 2
            || route.get(0).equals(point)
            || route.get(route.size() - 1).equals(point))
            return result;

        result.remove(point);
        return result;
    }

    private CalculateCostResult getCostToPointSimple(String point)
    {
        var map = CityMap.getInstance();
        var oldCost = map.getPathWeight(getHome(), getWork());
        var newCost = map.getPathWeight(getHome(), point) + map.getPathWeight(point, getWork());
        return new CalculateCostResult(point, newCost - oldCost, getHome(), getWork());
    }
}

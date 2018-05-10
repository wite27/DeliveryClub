package agents;

import behaviours.AskForDeliveryInDistrictBehaviour;
import behaviours.BatchReceiverWithHandlerBehaviour;
import behaviours.CyclicReceiverWithHandlerBehaviour;
import behaviours.ReceiverWithHandlerBehaviour;
import factories.MessageTemplateFactory;
import helpers.Log;
import helpers.MessageHelper;
import helpers.YellowPagesHelper;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.*;
import jade.domain.FIPAAgentManagement.Property;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import messages.*;
import models.*;

import java.util.*;
import java.util.stream.Collectors;

import static models.Consts.ContractCancelPenaltyInSec;

/**
 * Created by K750JB on 24.03.2018.
 */
public abstract class AgentBase extends Agent {
    protected AgentType type;
    protected int district;
    protected ArrayList<String> route;

    protected int neededProductsCount;
    protected int currentMoney;

    protected String currentConversationId;

    protected HashSet<DeliveryContract> produceContracts = new HashSet<DeliveryContract>();
    protected DeliveryContract receiveContract;

    protected HashSet<CheckEntry> currentChecks = new HashSet<>();

    private long startTime;
    private double getCurrentSeconds() {
        return (System.currentTimeMillis() - startTime) / 1000.0;
    }
    private double getCurrentContractCancelPenalty() {
        return getCurrentSeconds() * ContractCancelPenaltyInSec;
    }

    @Override
    protected void setup() {
        super.setup();
        startTime = System.currentTimeMillis();
        init();

        startAskingForDelivery();
        startListenUpdateContractCostMessages();
        StartListenCheckRequests();

        startPeriodicallySendStats();
    }

    private void startPeriodicallySendStats() {
        addBehaviour(new TickerBehaviour(this, 500) {
            @Override
            protected void onTick() {
                sendStats();
            }
        });
    }

    private void init() {
        var args = getArguments();
        var settings = (AgentSettings) args[0];
        neededProductsCount = settings.NeededProductsCount;
        route = settings.Route;
        currentMoney = settings.StartMoney;
        district = settings.District;

        currentConversationId = UUID.randomUUID().toString();

        registerOnYellowPages();
    }

    protected void registerOnYellowPages() {
        var sd = new ServiceDescription();
        sd.setType(type.name());
        sd.setName(getLocalName());
        sd.addProperties(new Property(Consts.District, district));
        YellowPagesHelper.register(this, sd);
    }

    protected String getHome() {
        return route.get(0);
    }

    public int getDistrict() {
        return district;
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
                            var currentCost = getCurrentReceiveCost() -
                                    (receiveContract == null
                                    ? 0
                                    : getCurrentContractCancelPenalty());
                            aclMessages.stream()
                                    .filter(x -> x.getPerformative() != ACLMessage.REFUSE) // ignore refuses
                                    .filter(x -> !isAgentInCheckStatus(x.getSender())) // ignore agents under check
                                    .filter(x -> produceContracts.stream() // early cycle break
                                            .noneMatch(c -> c.getConsumer().getId().equals(x.getSender().getName())))
                                    .sorted(Comparator.comparingDouble(self::getProposeDeliveryCost))
                                    .filter(x -> getProposeDeliveryCost(x) < currentCost)
                                    .findFirst()
                                    .ifPresent(bestDeal -> {
                                        var reaction = betterReceiveContractFound(bestDeal,
                                                MessageHelper.parse(bestDeal, DeliveryProposeMessageContent.class));

                                        if (reaction != null)
                                            sequentialBehaviour.addSubBehaviour(reaction);
                                    });

                            sequentialBehaviour.addSubBehaviour(new WakerBehaviour(self, 1000) {
                                @Override
                                protected void onWake() {
                                    super.onWake();
                                    onIterationEnd();

                                    self.startAskingForDelivery(); // recursive
                                }
                            });
                        }));
            }
        });

        addBehaviour(sequentialBehaviour);
    }

    private void onIterationEnd() {
        sendStats();
        currentConversationId = UUID.randomUUID().toString(); // change propose id
    }

    private void sendStats() {
        var statsman = YellowPagesHelper.findStatsman(this);
        if (statsman == null)
            return;

        var message = MessageHelper.buildMessage(
                ACLMessage.INFORM,
                DayResultMessageContent.class,
                new DayResultMessageContent(receiveContract, produceContracts, getRouteDelta(), route));
        message.addReceiver(statsman.getName());
        send(message);
    }

    private void startListenUpdateContractCostMessages(){
        var mt = MessageTemplateFactory.create(ACLMessage.INFORM, UpdateContractCostMessageContent.class);

        addBehaviour(new CyclicReceiverWithHandlerBehaviour(this, mt, x -> {
            var content = MessageHelper.parse(x, UpdateContractCostMessageContent.class);
            if (!receiveContract.equals(content.getContract())) {
                /*Log.warn("Agent " + this.getName() + " got update contract message for unknown contract! " +
                         " From " + x.getSender().getName());*/
                // ok, possibly we canceled contract but previous owner didn't receive message yet
                return;
            }
            receiveContract.updateCost(content.getNewCost());

            receiveContractCostUpdated(x, content);
        }));
    }

    private boolean isAgentInCheckStatus(AID agent) {
        return currentChecks.stream().anyMatch(x -> x.getProducerId().equals(agent.getName()));
    }

    private void StartListenCheckRequests() {
        var mt = MessageTemplateFactory.create(
                ACLMessage.INFORM_IF,
                CheckChainRequestMessageContent.class);

        addBehaviour(new CyclicReceiverWithHandlerBehaviour(this, mt, x -> {
            var content = MessageHelper.parse(x, CheckChainRequestMessageContent.class);
            var requesterAid = x.getSender();
            var nameUnderCheck = content.getProducerId();
            var checkId = content.getCheckId();

            this.currentChecks.add(new CheckEntry(checkId, nameUnderCheck));
            Log.fromAgent(this, "got check on " + nameUnderCheck + ", id: " + checkId +
                " from " + x.getSender().getName());

            var isCheckFailed = false;
            if (nameUnderCheck.equals(this.getName())) {
                Log.warn("Agent " + this.getName() + " got check request on himself." +
                        " Why parent " + x.getSender().getName() + " didn't declined it before?");
                isCheckFailed = true;
            }
            isCheckFailed = isCheckFailed
                    || produceContracts.stream()
                                       .anyMatch(c -> c.getConsumer().getId().equals(nameUnderCheck));

            if (isCheckFailed) {
                checkEnded(requesterAid, checkId, nameUnderCheck,true);
                return;
            }

            if (produceContracts.size() == 0) {
                checkEnded(requesterAid, checkId, nameUnderCheck, false);
                Log.fromAgent(this, "succeed check on " + nameUnderCheck + ", id: " + checkId +
                    ". Waiting parent " + requesterAid.getName() + " to realise lock.");
            }

            var askChildrenAndWaitParent = new SequentialBehaviour(this);
            var myReceivers = getMyReceivers();

            var waitResponseIfCheckSucceed = new ReceiverWithHandlerBehaviour(this, 1000,
                    MessageTemplateFactory.create(
                            ACLMessage.INFORM,
                            AwaitingContractDecisionMessageContent.class,
                            checkId),
                    parentResultMessage -> {
                        var parentResult = parentResultMessage != null
                                ? MessageHelper.parse(parentResultMessage, AwaitingContractDecisionMessageContent.class)
                                // something went wrong, suppose check as failed
                                : AwaitingContractDecisionMessageContent.failed(nameUnderCheck);

                        currentChecks.remove(new CheckEntry(checkId, nameUnderCheck));
                        Log.fromAgent(this, "realised lock on " + nameUnderCheck + ", id: " + checkId);

                        if (!parentResult.isSuccess()) {
                            var answerToChildren = MessageHelper.buildMessage(
                                    ACLMessage.INFORM,
                                    AwaitingContractDecisionMessageContent.class,
                                    AwaitingContractDecisionMessageContent.failed(nameUnderCheck));
                            answerToChildren.setConversationId(checkId);
                            MessageHelper.addReceivers(answerToChildren, myReceivers);
                            send(answerToChildren);

                            return;
                        }

                        receiveContract = parentResult.getNewContract();

                        notifyContractUpdated(checkId, myReceivers);
                    });

            if (myReceivers.size() != 0) {
                var furtherRequest = MessageHelper.buildMessage(
                        ACLMessage.INFORM_IF,
                        CheckChainRequestMessageContent.class,
                        new CheckChainRequestMessageContent(checkId, nameUnderCheck));
                furtherRequest.setConversationId(checkId);
                MessageHelper.addReceivers(furtherRequest, myReceivers);

                send(furtherRequest);

                askChildrenAndWaitParent.addSubBehaviour(new BatchReceiverWithHandlerBehaviour(this,
                        myReceivers.size(), 1000,
                        MessageTemplateFactory.create(
                                ACLMessage.INFORM,
                                CheckChainResponseMessageContent.class,
                                checkId),
                        childResults -> {
                            var childrenCheckFailed =
                                    childResults.size() != myReceivers.size() // not all children answered, suppose as failed
                                            || childResults.stream()
                                            .anyMatch(c -> MessageHelper.parse(c, CheckChainResponseMessageContent.class)
                                                    .isPresent());

                            checkEnded(requesterAid, checkId, nameUnderCheck, childrenCheckFailed);
                            if (childrenCheckFailed) {
                                var answerToChildren = MessageHelper.buildMessage(
                                        ACLMessage.INFORM,
                                        AwaitingContractDecisionMessageContent.class,
                                        AwaitingContractDecisionMessageContent.failed(nameUnderCheck));
                                MessageHelper.addReceivers(answerToChildren, myReceivers);
                                send(answerToChildren);

                                return;
                            }

                            askChildrenAndWaitParent.addSubBehaviour(waitResponseIfCheckSucceed);
                        }));
            } else {
                askChildrenAndWaitParent.addSubBehaviour(waitResponseIfCheckSucceed);
            }

            this.addBehaviour(askChildrenAndWaitParent);
        }));
    }

    private void checkEnded(AID requesterAid, String checkId, String nameUnderCheck, boolean isFailed) {
        var answer = MessageHelper.buildMessage(
                ACLMessage.INFORM,
                CheckChainResponseMessageContent.class,
                new CheckChainResponseMessageContent(isFailed)
        );
        answer.setConversationId(checkId);
        answer.addReceiver(requesterAid);

        this.send(answer);

        if (isFailed) {
            Log.fromAgent(this, "failed check on " + nameUnderCheck + ", id: " + checkId);
            currentChecks.remove(new CheckEntry(checkId, nameUnderCheck));
        }
    }

    protected abstract double getCurrentReceiveCost();
    protected abstract double getProposeDeliveryCost(ACLMessage message);
    protected abstract Behaviour betterReceiveContractFound(ACLMessage message, DeliveryProposeMessageContent content);
    protected void receiveContractCostUpdated(ACLMessage message, UpdateContractCostMessageContent content) {};

    protected ContractParty toContractParty() {
        return ContractParty.agent(this.getAID());
    }

    protected List<AID> getMyReceivers() {
        return produceContracts.stream()
                .map(x -> x.getConsumer().getAID()).collect(Collectors.toList());
    }

    protected void notifyContractUpdated(String checkId, List<AID> receiversToNotifyUnblock) {
        receiversToNotifyUnblock.forEach(x -> {
            var oldContract = produceContracts.stream()
                    .filter(y -> y.getConsumer().getId().equals(x.getName()))
                    .findFirst()
                    .orElse(null);

            if (oldContract == null) {
                Log.warn("Agent " + x.getName() + " was blocked because of new contract," +
                        "but now he is not consumer of " + this.getName());
                return;
            }

            var newCost = getCurrentReceiveCost();
            if (newCost > oldContract.getCost())
            {
                Log.warn("UpdateContract: new cost is higher than old: " +
                        newCost + ", was: " + oldContract.getCost());
            }

            var newContract = new DeliveryContract(
                    this.toContractParty(),
                    ContractParty.agent(x),
                    newCost,
                    oldContract.getPoint(),
                    this.receiveContract.makeChain());

            var message = MessageHelper.buildMessage(ACLMessage.INFORM,
                    AwaitingContractDecisionMessageContent.class,
                    new AwaitingContractDecisionMessageContent(checkId, newContract));
            message.setConversationId(checkId);
            message.addReceiver(x);

            this.send(message);
        });
    }

    protected abstract double getRouteDelta();
}

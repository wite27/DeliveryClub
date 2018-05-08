package agents;

import behaviours.AskForDeliveryInDistrictBehaviour;
import behaviours.BatchReceiverWithHandlerBehaviour;
import behaviours.CyclicReceiverWithHandlerBehaviour;
import helpers.Log;
import helpers.MessageHelper;
import helpers.YellowPagesHelper;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.SequentialBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.core.behaviours.WakerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.Property;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import messages.DayResultMessageContent;
import messages.DeliveryProposeMessageContent;
import messages.PotentialContractMessageContent;
import models.*;

import java.util.*;

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

    @Override
    protected void setup() {
        super.setup();

        init();

        startAskingForDelivery();
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
                            var currentCost = getCurrentReceiveCost();
                            aclMessages.stream()
                                    .filter(x -> x.getPerformative() != ACLMessage.REFUSE) // ignore refuses
                                    .sorted(Comparator.comparingDouble(self::getProposeDeliveryCost))
                                    .filter(x -> getProposeDeliveryCost(x) < currentCost)
                                    .findFirst()
                                    .ifPresent(bestDeal -> {
                                        betterReceiveContractFound(bestDeal,
                                                MessageHelper.parse(bestDeal, DeliveryProposeMessageContent.class));
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

        var message = MessageHelper.buildMessage2(
                ACLMessage.INFORM,
                DayResultMessageContent.class,
                new DayResultMessageContent(receiveContract, produceContracts, getRouteDelta()));
        message.addReceiver(statsman.getName());
        send(message);
    }

    protected abstract double getCurrentReceiveCost();
    protected abstract double getProposeDeliveryCost(ACLMessage message);
    protected abstract void betterReceiveContractFound(ACLMessage message, DeliveryProposeMessageContent content);

    protected ContractParty toContractParty() {
        return ContractParty.agent(this.getAID());
    }

    protected abstract double getRouteDelta();
}

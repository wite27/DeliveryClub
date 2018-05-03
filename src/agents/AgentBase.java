package agents;

import behaviours.BatchReceiverWithHandlerBehaviour;
import behaviours.CyclicReceiverWithHandlerBehaviour;
import behaviours.AskForDeliveryInDistrictBehaviour;
import environment.CityMap;
import environment.Store;
import helpers.Log;
import helpers.MessageHelper;
import jade.core.AID;
import jade.core.Agent;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.Property;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import messages.YouAreDistrictLeaderMessage;
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

    protected AID coordinatorAid;
    protected String dayId;

    protected abstract void onDayStart();
    protected abstract void onDayEnd();
    protected abstract double calculateCostToPoint(String point);

    @Override
    protected void setup() {
        super.setup();

        init();

        addBehaviour(new CyclicReceiverWithHandlerBehaviour(this,
            new MessageTemplate(x -> Consts.GoodMorning.equals(x.getContent())),
            x -> {
                coordinatorAid = x.getSender();
                dayId = x.getConversationId();
                currentConversationId = UUID.randomUUID().toString();
                onDayStart();
            }));
        addBehaviour(new CyclicReceiverWithHandlerBehaviour(this,
            new MessageTemplate(x -> Consts.GoodNight.equals(x.getContent())
                                     && dayId.equals(x.getConversationId())),
            x -> {
                onDayEnd();
            }));
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
        register(sd);
    }

    protected String getHome() {
        return route.get(0);
    }

    public int getDistrict() {
        return district;
    }

    private void register(ServiceDescription sd) {
        var dfd = new DFAgentDescription();
        dfd.setName(getAID());

        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
    }

    protected double calculateBestDeliveryPoint(String pointA, String pointB) {
        return Math.min(calculateCostToPoint(pointA),
                calculateCostToPoint(pointB));
    }

    protected void enoughForMeInThisDay(boolean needNextDay){
        var message = MessageHelper.buildMessage(ACLMessage.INFORM, Consts.IGoToTheBedPrefix,
                needNextDay ? "TRUE" : "FALSE");
        message.addReceiver(coordinatorAid);
        message.setConversationId(dayId);
        send(message);
    }

    protected ContractParty toContractParty(){
        return new AgentContractParty(this.getAID());
    }
}

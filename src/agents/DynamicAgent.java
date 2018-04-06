package agents;

import behaviours.CyclicReceiverWithHandlerBehaviour;
import environment.CityMap;
import environment.Store;
import helpers.Log;
import helpers.MessageHelper;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import models.AgentSettings;
import models.AgentType;
import models.Consts;

import java.util.ArrayList;

/**
 * Created by K750JB on 24.03.2018.
 */
public class DynamicAgent extends AgentBase {
    private final AgentType type = AgentType.Dynamic;

    private int neededProductsCount;
    private int currentMoney;
    private ArrayList<String> route;

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

    private int calculateDeliveryCost()
    {
        var home = route.get(0);
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

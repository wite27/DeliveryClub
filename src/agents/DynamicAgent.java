package agents;

import behaviours.ReceiverWithHandlerBehaviour;
import environment.Map;
import environment.Store;
import jade.core.Agent;
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
    private final AgentType Type = AgentType.Dynamic;

    private int NeededProductsCount;
    private int CurrentMoney;
    private ArrayList<String> Route;

    @Override
    protected void setup() {
        super.setup();

        Init();
        RegisterOnYellowPages(Type, District);

        StartListenYouAreLeaderMessage();
        StartListenHowMuchCostDeliveryToRegion();
    }

    private void Init() {
        var args = getArguments();
        var settings = (AgentSettings)args[0];
        NeededProductsCount = settings.NeededProductsCount;
        Route = settings.Route;
        CurrentMoney = settings.StartMoney;
        District = settings.District;
    }

    private void StartListenHowMuchCostDeliveryToRegion() {
        var mt = new MessageTemplate(msg ->
            msg.getPerformative() == ACLMessage.REQUEST
            && msg.getContent().equals(Consts.HowMuchCostDeliveryToDistrict)
        );
        addBehaviour(new ReceiverWithHandlerBehaviour(this, 10000, mt, aclMessage -> {
            var answerTo = aclMessage.getSender();

            var answer = new ACLMessage(ACLMessage.AGREE);
            answer.setContent(Consts.IWillDeliverToDistrictPrefix + String.valueOf(CalculateDeliveryCost()));
            answer.addReceiver(answerTo);

            send(answer);
        }));
    }

    private int CalculateDeliveryCost()
    {
        var home = Route.get(0);
        var work = Route.get(Route.size() - 1);
        var store = Store.GetInstance().GetName();
        var map = Map.GetInstance();

        var costWithoutStore = map.GetPathWeight(home, work);
        System.out.println(getName() + " without store = " + costWithoutStore);
        var costWithStore = map.GetPathWeight(home, store) + map.GetPathWeight(store, work);
        System.out.println(getName() + " with store = " + costWithStore);

        var delta = (costWithStore - costWithoutStore);

        return delta > 0
                ? delta
                : 0;
    }
}

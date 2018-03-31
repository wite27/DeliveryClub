package agents;

import jade.core.Agent;
import models.AgentSettings;
import models.AgentType;

import java.util.ArrayList;

/**
 * Created by K750JB on 24.03.2018.
 */
public class DynamicAgent extends AgentBase {
    private final AgentType Type = AgentType.Dynamic;

    private int NeededProductsCount;
    private int CurrentMoney;
    private ArrayList<String> Route;
    private int District;

    @Override
    protected void setup() {
        super.setup();

        Init();
        RegisterOnYellowPages(Type, District);
    }

    private void Init() {
        var args = getArguments();
        var settings = (AgentSettings)args[0];
        NeededProductsCount = settings.NeededProductsCount;
        Route = settings.Route;
        CurrentMoney = settings.StartMoney;
        District = settings.District;
    }
}

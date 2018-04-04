package agents;

import models.AgentSettings;
import models.AgentType;

/**
 * Created by K750JB on 24.03.2018.
 */
public class StaticAgent extends AgentBase {
    private final AgentType type = AgentType.Static;

    private int neededProductsCount;
    private int currentMoney;
    private String location;

    @Override
    protected void setup() {
        super.setup();

        init();
        registerOnYellowPages(type, district);

        startAskingForDelivery();
    }

    private void init() {
        Object[] args = getArguments();
        AgentSettings settings = (AgentSettings)args[0];
        neededProductsCount = settings.NeededProductsCount;
        location = settings.Route.get(0);
        currentMoney = settings.StartMoney;
        district = settings.District;
    }
}

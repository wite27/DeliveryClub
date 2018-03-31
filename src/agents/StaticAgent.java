package agents;

import models.AgentSettings;
import models.AgentType;

/**
 * Created by K750JB on 24.03.2018.
 */
public class StaticAgent extends AgentBase {
    private final AgentType Type = AgentType.Static;

    private int NeededProductsCount;
    private int CurrentMoney;
    private String Location;

    @Override
    protected void setup() {
        super.setup();

        Init();
        RegisterOnYellowPages(Type, getDistrict());

        StartListenYouAreLeaderMessage();
    }

    private void Init() {
        Object[] args = getArguments();
        AgentSettings settings = (AgentSettings)args[0];
        NeededProductsCount = settings.NeededProductsCount;
        Location = settings.Route.get(0);
        CurrentMoney = settings.StartMoney;
        District = settings.District;
    }
}

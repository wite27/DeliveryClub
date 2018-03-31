package agents;

import jade.core.Agent;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
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
    private int District;

    @Override
    protected void setup() {
        super.setup();

        Init();
        RegisterOnYellowPages(Type, District);

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

package agents;

import behaviours.CoordinatorSelectDistrictLeadersBehaviour;
import environment.Map;
import environment.Store;
import jade.core.Agent;
import jade.core.Timer;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.introspection.AddedBehaviour;
import jade.wrapper.StaleProxyException;
import models.*;


import java.util.ArrayList;
import java.util.List;

/**
 * Created by K750JB on 24.03.2018.
 */
public class CoordinatorAgent2 extends Agent {
    private StartupSettings startupSettings; // TODO parse from json

    @Override
    protected void setup() {
        super.setup();
        CreateStartupSettings();
        try {
            Map.GetInstance().Initialize(startupSettings.Vertices);
            Store.GetInstance().Initialize(startupSettings.Store);
            CreateAgents(startupSettings.Agents);
        } catch (StaleProxyException e) {
            e.printStackTrace();
        }

        var self = this;
        addBehaviour(new TickerBehaviour(this, 100) { // wait other agents to register on yellow pages
            @Override
            protected void onTick() {
                addBehaviour(new CoordinatorSelectDistrictLeadersBehaviour(self));
                stop();
            }
        });
    }

    public int[] GetAllDistricts() {
        return new int[] {0}; // TODO read from settings
    }

    private void CreateStartupSettings() {
        startupSettings = new StartupSettings();
        startupSettings.Vertices = new ArrayList<>() {{
            add(new VertexSettings("A", new ArrayList<>() {{
                add("B");
                add("E");
            }}));
            add(new VertexSettings("B", new ArrayList<>() {{
                add("A");
                add("S");
                add("C");
            }}));
            add(new VertexSettings("C", new ArrayList<>() {{
                add("B");
                add("D");
            }}));
            add(new VertexSettings("D", new ArrayList<>() {{
                add("C");
                add("F");
            }}));
            add(new VertexSettings("F", new ArrayList<>() {{
                add("D");
                add("E");
            }}));
            add(new VertexSettings("E", new ArrayList<>() {{
                add("F");
                add("A");
            }}));
            add(new VertexSettings("S", new ArrayList<>() {{
                add("B");
            }}));
        }};

        startupSettings.Store = new StoreSettings() {{
            Name = "S";
            ProductsCount = 5;
        }};

        startupSettings.Agents = new ArrayList<>();

        var anna = new AgentSettings() {{
            Name = "Anna";
            District = 0;
            NeededProductsCount = 0;
            Type = AgentType.Static;
            Route = new ArrayList<>(){{
                add("D");
            }};
        }};
        startupSettings.Agents.add(anna);

        var andrey = new AgentSettings(){{
            Name = "Andrey";
            District = 0;
            NeededProductsCount = 0;
            Type = AgentType.Dynamic;
            Route = new ArrayList<>() {{
                add("A");
                add("C");
            }};
        }};
        startupSettings.Agents.add(andrey);

        var haska = new AgentSettings() {{
            Name = "Haska";
            District = 0;
            NeededProductsCount = 0;
            Type = AgentType.Dynamic;
            Route = new ArrayList<>() {{
                add("E");
                add("D");
            }};
        }};
        startupSettings.Agents.add(haska);
    }

    private void CreateAgents(ArrayList<AgentSettings> agentSettings) throws StaleProxyException {
        for (var settings : agentSettings) {
            getContainerController()
                    .createNewAgent(settings.Name, GetClassByType(settings.Type), new Object[]{settings})
                    .start();
        }
    }

    private String GetClassByType(AgentType type) {
        switch (type){
            case Static:
                return StaticAgent.class.getName();

            case Dynamic:
            default:
                return DynamicAgent.class.getName();
        }
    }
}

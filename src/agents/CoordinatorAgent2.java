package agents;

import behaviours.CoordinatorSelectDistrictLeadersBehaviour;
import environment.Map;
import environment.Store;
import jade.core.Agent;
import jade.domain.introspection.AddedBehaviour;
import jade.wrapper.StaleProxyException;
import models.*;


import java.util.ArrayList;

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

        addBehaviour(new CoordinatorSelectDistrictLeadersBehaviour(this));
    }

    public int[] GetAllDistricts() {
        return new int[] {0}; // TODO read from settings
    }

    private void CreateStartupSettings() {
        startupSettings = new StartupSettings();
        var vertices = new ArrayList<VertexSettings>();
        startupSettings.Vertices = vertices;

        var vertexSettings = new VertexSettings("A", new ArrayList<String>());
        vertexSettings.AdjacentVertices.add("B");
        vertexSettings.AdjacentVertices.add("D");
        vertices.add(vertexSettings);

        var vertexSettings2 = new VertexSettings("B", new ArrayList<String>());
        vertexSettings2.AdjacentVertices.add("D");
        vertices.add(vertexSettings2);

        var vertexSettings3 = new VertexSettings("D", new ArrayList<String>());
        vertices.add(vertexSettings3);

        var storeSettings = new StoreSettings();
        storeSettings.Name = "B";
        storeSettings.ProductsCount = 5;
        startupSettings.Store =storeSettings;

        var agentSettings = new AgentSettings();
        startupSettings.Agents = new ArrayList<AgentSettings>();
        agentSettings.NeededProductsCount = 5;
        agentSettings.Type = AgentType.Static;
        agentSettings.Name = "Anna";
        agentSettings.District = 0;
        agentSettings.Route = new ArrayList<String>(1);
        agentSettings.Route.add("D");
        startupSettings.Agents.add(agentSettings);

        var agentSettings2 = new AgentSettings();
        agentSettings2.Name = "Andrey";
        agentSettings2.District = 0;
        agentSettings2.NeededProductsCount = 0;
        agentSettings2.Type = AgentType.Dynamic;
        startupSettings.Agents.add(agentSettings2);
        agentSettings2.Route = new ArrayList<String>();
        agentSettings2.Route.add("B");
        agentSettings2.Route.add("A");
        agentSettings2.Route.add("D");

        var agentSettings3 = new AgentSettings();
        agentSettings3.Name = "Haska";
        agentSettings3.District = 0;
        agentSettings3.NeededProductsCount = 0;
        agentSettings3.Type = AgentType.Dynamic;
        startupSettings.Agents.add(agentSettings3);
        agentSettings3.Route = new ArrayList<String>();
        agentSettings3.Route.add("B");
        agentSettings3.Route.add("A");
        agentSettings3.Route.add("D");
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

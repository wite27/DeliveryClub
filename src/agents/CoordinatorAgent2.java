package agents;

import behaviours.CyclicReceiverWithHandlerBehaviour;
import com.alibaba.fastjson.JSON;
import environment.CityMap;
import environment.Store;
import helpers.*;
import jade.core.Agent;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.wrapper.StaleProxyException;
import messages.DayResultMessageContent;
import models.*;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.function.Consumer;

/**
 * Created by K750JB on 24.03.2018.
 */
public class CoordinatorAgent2 extends Agent {
    private HashMap<String, DayResultMessageContent> agentsResults = new HashMap<>();

    @Override
    protected void setup() {
        super.setup();
        File input = FileUtils.getFile("input.json");
        String str = null;
        try {
            str = FileUtils.readFileToString(input,"utf-8");
        } catch (IOException e) {
            e.printStackTrace();
        }
        var startupSettings = JSON.parseObject(str, StartupSettings.class);
        try {
            CityMap.getInstance().Initialize(startupSettings.Vertices);
            Store.getInstance().Initialize(startupSettings.Store);
            createAgents(startupSettings.Agents);
        } catch (StaleProxyException e) {
            e.printStackTrace();
        }

        registerAsStatsman();
    }

    private void registerAsStatsman() {
        var sd = new ServiceDescription();
        sd.setType(Consts.StatsmanType);
        sd.setName(getLocalName());
        YellowPagesHelper.register(this, sd);

        addStatsmanBehvaiour();
    }

    private void addStatsmanBehvaiour() {
        var mt = new MessageTemplate(x -> x.getPerformative() == ACLMessage.INFORM
                && StringHelper.safeEquals(x.getOntology(), DayResultMessageContent.class.getName()));

        Consumer<ACLMessage> handler = x -> {
            var content = MessageHelper.parse(x, DayResultMessageContent.class);
            agentsResults.put(x.getSender().getName(), content);
        };

        this.addBehaviour(new CyclicReceiverWithHandlerBehaviour(this, mt, handler));

        this.addBehaviour(new TickerBehaviour(this, 1000) {
            private int metricCount = 0;
            @Override
            protected void onTick() {
                metricCount++;
                if (agentsResults.isEmpty())
                    return;

                var dailyRouteDelta = agentsResults.values().stream()
                        .map(DayResultMessageContent::getRouteDelta)
                        .reduce((x, y) -> x+y).get(); // sum() :(

                Log.write("[BUS]Snapshot #" + metricCount + ". Total delta: " + dailyRouteDelta);
                agentsResults.entrySet().stream().forEach(x -> {
                    var content = x.getValue();
                    var receiveContract = content.getReceiveContract();

                    String deliveryChain = "EMPTY";

                    if (receiveContract != null)
                    {
                        deliveryChain = getChainElement(receiveContract.getConsumer(), receiveContract.getPoint())
                                + " "
                                + receiveContract.getPreviousContracts().stream()
                                .map(y -> getChainElement(y.getConsumer(), y.getPoint()))
                                .reduce((s1, s2) -> s1 + " " + s2)
                                .orElse("");
                    }

                    Log.write("[BUS]" + x.getKey() + "'s chain is: " + deliveryChain +
                            ". Delta: " + content.getRouteDelta() +
                            ". Route: " + printRoute(content.getRoute()));
                });
            }
        });
    }

    private String printRoute(ArrayList<String> route) {
        return route.stream().reduce((s1, s2) -> s1+"-"+s2).orElse(null);
    }

    private String getChainElement(ContractParty party, String point)
    {
        var producer = AgentHelper.getLocalName(party.getId());
        return "(" + producer + "," + point + ")";
    }

    public int[] getAllDistricts() {
        return new int[] {0}; // TODO read from settings
    }

       private void createAgents(ArrayList<AgentSettings> agentSettings) throws StaleProxyException {
        for (var settings : agentSettings) {
            getContainerController()
                    .createNewAgent(settings.Name, getClassByType(settings.Type), new Object[]{settings})
                    .start();
        }
    }

    private String getClassByType(AgentType type) {
        switch (type){
            case Static:
                return StaticAgent.class.getName();

            case Dynamic:
            default:
                return DynamicAgent.class.getName();
        }
    }
}

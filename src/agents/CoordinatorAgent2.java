package agents;

import com.alibaba.fastjson.JSON;
import environment.CityMap;
import environment.Store;
import jade.core.Agent;
import jade.wrapper.StaleProxyException;
import models.*;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by K750JB on 24.03.2018.
 */
public class CoordinatorAgent2 extends Agent {
    private StartupSettings startupSettings; // TODO parse from json

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
        startupSettings = JSON.parseObject(str, StartupSettings.class);
        try {
            CityMap.getInstance().Initialize(startupSettings.Vertices);
            Store.getInstance().Initialize(startupSettings.Store);
            createAgents(startupSettings.Agents);
        } catch (StaleProxyException e) {
            e.printStackTrace();
        }
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

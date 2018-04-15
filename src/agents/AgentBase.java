package agents;

import behaviours.BatchReceiverWithHandlerBehaviour;
import behaviours.CyclicReceiverWithHandlerBehaviour;
import behaviours.AskForDeliveryInDistrictBehaviour;
import environment.CityMap;
import environment.Store;
import helpers.Log;
import helpers.MessageHelper;
import jade.core.Agent;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.Property;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import messages.YouAreDistrictLeaderMessage;
import models.AgentType;
import models.Consts;

import java.util.ArrayList;
import java.util.Comparator;

/**
 * Created by K750JB on 24.03.2018.
 */
public abstract class AgentBase extends Agent {
    protected int district;
    protected ArrayList<String> route;

    protected abstract double calculateCostToPoint(String point);

    protected void registerOnYellowPages(AgentType agentType, int district) {
        var sd  = new ServiceDescription();
        sd.setType(agentType.name());
        sd.setName(getLocalName());
        sd.addProperties(new Property(Consts.District, district));
        register(sd);
    }

    protected String getHome()
    {
        return route.get(0);
    }

    public int getDistrict() {
        return district;
    }

    private void register( ServiceDescription sd)
    {
        var dfd = new DFAgentDescription();
        dfd.setName(getAID());

        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
        }
        catch (FIPAException fe) { fe.printStackTrace(); }
    }

    protected double calculateBestDeliveryPoint(String pointA, String pointB)
    {
        return Math.min(calculateCostToPoint(pointA),
                        calculateCostToPoint(pointB));
    }


    private double calculateDeliveryCostPoint(String pointA, String pointB, String myPoint)
    {
        var map = CityMap.getInstance();
        return Math.min(map.getPathWeight(pointA, myPoint), map.getPathWeight(pointB, myPoint));
    }
}

package agents;

import jade.core.Agent;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.Property;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import models.AgentType;

public class TestAgent extends Agent {
    @Override
    protected void setup() {
        super.setup();
        try {
            ////System.out.println("Agent " + this.getAgent().getLocalName() + " is searching Schedule"); ////
            //поиск сервиса ScheduleService
            DFAgentDescription dfd = new DFAgentDescription();
            ServiceDescription sd = new ServiceDescription();
            sd.addProperties(new Property("District", 0));
            dfd.addServices(sd);
            DFAgentDescription[] result = DFService.search(this, dfd);
            //найденному сервису отправляем запрос

            System.out.println(this + " sending message to " + result[0].getName());
            }
            catch (FIPAException fe) {
            fe.printStackTrace();
        }
    }
}

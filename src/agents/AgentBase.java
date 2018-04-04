package agents;

import behaviours.BatchReceiverWithHandlerBehaviour;
import behaviours.CyclicReceiverWithHandlerBehaviour;
import behaviours.AskForDeliveryInDistrictBehaviour;
import helpers.Log;
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

import java.util.Comparator;

/**
 * Created by K750JB on 24.03.2018.
 */
public class AgentBase extends Agent {
    protected int district;

    protected void registerOnYellowPages(AgentType agentType, int district) {
        ServiceDescription sd  = new ServiceDescription();
        sd.setType(agentType.name());
        sd.setName(getLocalName());
        sd.addProperties(new Property(Consts.District, district));
        register(sd);
    }

    protected void startAskingForDelivery() {
        var askForDeliveryInDistrictBehaviour = new AskForDeliveryInDistrictBehaviour(this);
        addBehaviour(askForDeliveryInDistrictBehaviour);

        var mt = new MessageTemplate(msg ->
                msg.getPerformative() == ACLMessage.PROPOSE
                        && msg.getContent().startsWith(Consts.IWillDeliverToDistrictPrefix)
        );
        addBehaviour(new BatchReceiverWithHandlerBehaviour(this,
                askForDeliveryInDistrictBehaviour.getReceiversCount(),
                10000,
                mt,
                aclMessages -> {
                    var bestDeal = aclMessages.stream()
                            .min(Comparator.comparingInt(o ->
                                    Integer.parseInt(o.getContent().substring(Consts.IWillDeliverToDistrictPrefix.length()))))
                            .get(); // TODO isPresent() check

                    Log.fromAgent(this,"choosed best deal: " + bestDeal.getContent() +
                            " from " + bestDeal.getSender().getName());
                }));
    }

    public int getDistrict() {
        return district;
    }

    private void register( ServiceDescription sd)
    {
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());

        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
        }
        catch (FIPAException fe) { fe.printStackTrace(); }
    }
}

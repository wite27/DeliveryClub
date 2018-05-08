package behaviours;

import helpers.AgentHelper;
import helpers.Log;
import helpers.MessageHelper;
import helpers.StringHelper;
import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.SequentialBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import messages.DayResultMessageContent;
import models.Consts;
import models.ContractParty;
import models.DeliveryContract;

import java.util.ArrayList;
import java.util.stream.Collectors;

import static environment.GlobalParams.MaxDistrictCourierSelectionDays;

public class DailyTimeBehaviour extends SequentialBehaviour {
    private ArrayList<DFAgentDescription> allAgents;
    private String dayId;
    private int dayNumber = 0;
    private boolean isNextDayNeeded = true;

    public DailyTimeBehaviour(Agent a) {
        super(a);

        var self = this;

        if (allAgents == null)
        {
            allAgents = AgentHelper.findAllAgents(myAgent);
        }

        this.addSubBehaviour(new OneShotBehaviour() {
            @Override
            public void action() {
                var message = MessageHelper.buildMessage(
                        ACLMessage.INFORM, Consts.GoodMorning);
                dayNumber++;
                Log.write("DAY INCREASED");
                message.setConversationId(dayId = Integer.toString(dayNumber));
                myAgent.send(MessageHelper.addReceivers(message, allAgents));
            }
        });

        self.addSubBehaviour(new BatchReceiverWithHandlerBehaviour(
                a, allAgents.size(), 500,
                new MessageTemplate(x -> StringHelper.safeEquals(x.getOntology(), DayResultMessageContent.class.getName())
                        && dayId.equals(x.getConversationId())),
                aclMessages -> {
                    var results = aclMessages.stream()
                            .map(x -> MessageHelper.parse(x, DayResultMessageContent.class))
                            .collect(Collectors.toList());

                    //isNextDayNeeded = results.stream().anyMatch(DayResultMessageContent::isNeedNextDay);

                    var dailyRouteDelta = results.stream()
                            .map(DayResultMessageContent::getRouteDelta)
                            .reduce((x, y) -> x+y).get(); // sum() :(

                    Log.write("[BUS]DAY " + dayNumber + " result: " + dailyRouteDelta);

                    aclMessages.stream()
                            .forEach(x -> {
                                var content = MessageHelper.parse(x, DayResultMessageContent.class);
                                var receiveContract = content.getReceiveContract();

                                String deliveryChain = "EMPTY";

                                if (receiveContract != null)
                                {
                                    deliveryChain = getChainElement(receiveContract.getProducer(), receiveContract.getPoint())
                                            + " "
                                            + receiveContract.getPreviousContracts().stream()
                                            .map(y -> getChainElement(y.getProducer(), y.getPoint()))
                                            .reduce((s1, s2) -> s1 + " " + s2)
                                            .orElse("");
                                }

                                Log.write("[BUS]" + x.getSender().getName() + "'s chain is: " + deliveryChain);
                            });

                    var dayEndedMessage = MessageHelper.buildMessage(
                            ACLMessage.INFORM, Consts.GoodNight);
                    myAgent.send(MessageHelper.addReceivers(dayEndedMessage, allAgents));
                }));

        self.addSubBehaviour(new TickerBehaviour(myAgent, 100) {
            @Override
            protected void onTick() {
                stop();
            }
        });
    }

    private String getChainElement(ContractParty party, String point)
    {
        var producer = party.getId();
        if (producer.contains("@")) // AID
        {
            producer = producer.substring(0, producer.indexOf("@"));
        }

        return "(" + producer + "," + point + ")";
    }

    @Override
    public int onEnd() {
        reset();

        if (isNextDayNeeded && dayNumber <= MaxDistrictCourierSelectionDays)
            myAgent.addBehaviour(this); // cyclic repeat sequential behaviour

        return super.onEnd();
    }
}

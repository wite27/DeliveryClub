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
                a, allAgents.size(), 2000,
                new MessageTemplate(x -> StringHelper.safeEquals(x.getOntology(), DayResultMessageContent.class.getName())
                        && dayId.equals(x.getConversationId())),
                aclMessages -> {
                    var results = aclMessages.stream()
                            .map(x -> MessageHelper.parse(x, DayResultMessageContent.class))
                            .collect(Collectors.toList());

                    isNextDayNeeded = results.stream().anyMatch(DayResultMessageContent::isNeedNextDay);


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

    @Override
    public int onEnd() {
        reset();

        if (isNextDayNeeded && dayNumber <= MaxDistrictCourierSelectionDays)
            myAgent.addBehaviour(this); // cyclic repeat sequential behaviour

        return super.onEnd();
    }
}

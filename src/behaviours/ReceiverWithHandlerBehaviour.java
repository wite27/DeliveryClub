package behaviours;

import helpers.Log;
import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.ReceiverBehaviour;
import jade.core.behaviours.SequentialBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.util.function.Consumer;

public class ReceiverWithHandlerBehaviour extends SequentialBehaviour {
    public ReceiverWithHandlerBehaviour(Agent agent, long timeout, MessageTemplate template, Consumer<ACLMessage> handlerFn) {
        super(agent);

        var receiver = new ReceiverBehaviour(agent, timeout, template);
        addSubBehaviour(receiver);
        addSubBehaviour(new OneShotBehaviour() {
            @Override
            public void action() {
                ACLMessage msg = null;
                try {
                    msg = receiver.getMessage();
                } catch (ReceiverBehaviour.TimedOut | ReceiverBehaviour.NotYetReady timedOut) {
                    Log.warn("Agent " + agent.getName() + " didn't got message!");
                }
                handlerFn.accept(msg);
            }
        });
    }
}
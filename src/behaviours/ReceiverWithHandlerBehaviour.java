package behaviours;

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
                try {
                    var message = receiver.getMessage();
                    System.out.println("Agent " + agent.getName() + " got message " + message.getContent()
                                     + " from " + message.getSender().getName());
                    handlerFn.accept(message);
                } catch (ReceiverBehaviour.TimedOut | ReceiverBehaviour.NotYetReady timedOut) {
                    timedOut.printStackTrace();
                }
            }
        });
    }
}

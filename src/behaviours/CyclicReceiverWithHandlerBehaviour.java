package behaviours;

import helpers.Log;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.util.function.Consumer;

public class CyclicReceiverWithHandlerBehaviour extends CyclicBehaviour {
    private final MessageTemplate template;
    private final Consumer<ACLMessage> handlerFn;

    public CyclicReceiverWithHandlerBehaviour(Agent agent, MessageTemplate template, Consumer<ACLMessage> handlerFn)
    {
        super(agent);
        this.template = template;
        this.handlerFn = handlerFn;
    }

    @Override
    public void action() {
        var message = myAgent.receive(template);
        if (message == null)
        {
            block();
            return;
        }

        Log.messageReceived(myAgent, message);
        handlerFn.accept(message);
    }
}

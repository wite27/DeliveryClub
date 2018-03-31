package behaviours;

import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.ReceiverBehaviour;
import jade.core.behaviours.SequentialBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.util.ArrayList;
import java.util.function.Consumer;

public class BatchReceiverWithHandlerBehaviour extends SequentialBehaviour {
    public BatchReceiverWithHandlerBehaviour(Agent agent, int replyCount, long timeout, MessageTemplate mt,
                                             Consumer<ArrayList<ACLMessage>> handlerFn)
    {
        super(agent);

        var batchReceiverBehaviour = new BatchReceiverBehaviour(agent, replyCount, timeout, mt);
        addSubBehaviour(batchReceiverBehaviour);
        addSubBehaviour(new OneShotBehaviour() {
            @Override
            public void action() {
                var readyMessages = new ArrayList<ACLMessage>();

                for (var receiver: batchReceiverBehaviour.receiverBehaviours)
                {
                    try {
                        readyMessages.add(receiver.getMessage());
                    } catch (ReceiverBehaviour.TimedOut | ReceiverBehaviour.NotYetReady timedOut) {
                        timedOut.printStackTrace();
                    }
                }

                handlerFn.accept(readyMessages);
            }
        });
    }
}
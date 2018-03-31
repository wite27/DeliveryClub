package behaviours;

import jade.core.Agent;
import jade.core.behaviours.ParallelBehaviour;
import jade.core.behaviours.ReceiverBehaviour;
import jade.lang.acl.MessageTemplate;

public class BatchReceiverBehaviour extends ParallelBehaviour {
    public ReceiverBehaviour[] receiverBehaviours;

    public BatchReceiverBehaviour(Agent agent, int replyCount, long timeout, MessageTemplate mt)
    {
        super(WHEN_ALL);
        receiverBehaviours = new ReceiverBehaviour[replyCount];
        for (int i = 0; i < replyCount; i++)
        {
            receiverBehaviours[i] = new ReceiverBehaviour(agent, timeout, mt);
            this.addSubBehaviour(receiverBehaviours[i]);
        }
    }
}

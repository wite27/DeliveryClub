package helpers;

import jade.core.Agent;
import jade.lang.acl.ACLMessage;

public class Log {
    public static void MessageReceived(Agent agent, ACLMessage message) {
        System.out.println("Agent " + agent.getName()
                + " got message " + message.getContent()
                + " from " + message.getSender().getName());
    }

    public static void Write(String str)
    {
        System.out.println(str);
    }

    public static void FromAgent(Agent agent, String str)
    {
        System.out.println("Agent " + agent.getName() + " " + str);
    }

    public static void Warn(String str)
    {
        System.out.println("[WARNING] " + str);
    }
}

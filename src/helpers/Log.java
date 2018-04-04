package helpers;

import jade.core.Agent;
import jade.lang.acl.ACLMessage;

import java.io.PrintStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

public class Log {
    private static PrintStream stream = System.out;

    public static void messageReceived(Agent agent, ACLMessage message) {
        printWithTime("Agent " + agent.getName()
                + " got message " + message.getContent()
                + " from " + message.getSender().getName());
    }

    public static void write(String str)
    {
        printWithTime(str);
    }

    public static void fromAgent(Agent agent, String str)
    {
        printWithTime("Agent " + agent.getName() + " " + str);
    }

    public static void warn(String str)
    {
        printWithTime("[WARNING] " + str);
    }

    private static void printWithTime(String str)
    {
        var time = LocalDateTime.now().format(DateTimeFormatter.ofLocalizedTime(FormatStyle.MEDIUM));
        stream.println("[" + time + "] " + str);
    }
}

package helpers;

import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.lang.acl.ACLMessage;

import java.util.ArrayList;

public class MessageHelper {
    private final static String delimiter = "/";

    public static ACLMessage buildMessage(int performative, String prefix, String... params)
    {
        var message = new ACLMessage(performative);

        var content = prefix;
        if (params != null && params.length > 0)
            content += delimiter + String.join(delimiter, params);

        message.setContent(content);
        return message;
    }

    public static String[] getParams(ACLMessage message)
    {
        return message.getContent().split(delimiter);
    }

    public static ACLMessage addReceivers(ACLMessage message, ArrayList<DFAgentDescription> receivers)
    {
        receivers.forEach(x -> message.addReceiver(x.getName()));
        return message;
    }
}

